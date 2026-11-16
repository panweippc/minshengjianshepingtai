package com.msjs.project.service.impl;

import com.msjs.project.common.Constants;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysDict;
import com.msjs.project.mapper.FundQueryMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.ProjectFundQueryService;
import com.msjs.project.service.SysDictService;
import com.msjs.project.vo.FundDetailRowVO;
import com.msjs.project.vo.FundStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资金记录查询 Service
 *
 * <p>和 FundQueryMapper 配套，负责：三段独立查询合并、字典翻译、附件回填、
 * 内存聚合 GlobalSummary、按 groupType 分组出主统计表、内存分页。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFundQueryServiceImpl implements ProjectFundQueryService {

    private final FundQueryMapper fundQueryMapper;
    private final SysDictService dictService;
    private final SysRegionMapper regionMapper;

    // ============== 对外接口 ==============

    @Override
    public FundStatVO stat(String year, String regionCode, String groupType, String groupValue) {
        List<FundDetailRowVO> allRaw = collectAll(regionCode, year);
        applyDictLabels(allRaw);

        FundStatVO vo = new FundStatVO();
        // 全局 summary：recordCount 用于右上角 "共 N 条源记录"
        FundStatVO.GlobalSummary gs = new FundStatVO.GlobalSummary();
        gs.setRecordCount(allRaw.size());
        gs.setProjectCount((int) allRaw.stream().map(FundDetailRowVO::getApprovalId).filter(Objects::nonNull).distinct().count());
        gs.setDeclareCount((int) allRaw.stream().map(FundDetailRowVO::getDeclareId).filter(Objects::nonNull).distinct().count());
        vo.setGlobalSummary(gs);

        // groupValue 预先过滤
        List<FundDetailRowVO> filtered = allRaw;
        if (groupType != null && groupValue != null && !groupValue.isBlank()) {
            filtered = allRaw.stream().filter(r -> {
                String v = extractGroupValue(r, groupType);
                return v != null && v.equals(groupValue);
            }).toList();
        }

        List<FundStatVO.Row> rows = buildNestedRows(filtered, groupType);
        vo.setRows(rows);
        vo.setTotal((long) rows.size());
        return vo;
    }

    @Override
    public List<FundDetailRowVO> detailList(String year, String regionCode,
                                            String groupType, String groupValue) {
        List<FundDetailRowVO> all = collectAll(regionCode, year);
        applyDictLabels(all);
        attachAttachCount(all);

        if (groupType != null && groupValue != null && !groupValue.isBlank()) {
            all = all.stream().filter(row -> matchGroup(row, groupType, groupValue))
                    .collect(Collectors.toList());
        }
        return all;
    }

    // ============== 内部 ==============

    /** 三段独立查询合并，按 createTime DESC + id DESC 排序 */
    private List<FundDetailRowVO> collectAll(String regionCode, String year) {
        FundQueryMapper.QueryParams p = buildQueryParams(regionCode, year);
        List<FundDetailRowVO> all = new ArrayList<>();
        all.addAll(fundQueryMapper.selectLevel1(p));
        all.addAll(fundQueryMapper.selectLevel2(p));
        all.addAll(fundQueryMapper.selectLevel3(p));

        // 合并后排序
        all.sort((a, b) -> {
            LocalDateTime ta = a.getCreateTime(), tb = b.getCreateTime();
            int c = (ta == null ? LocalDateTime.MIN : ta).compareTo(tb == null ? LocalDateTime.MIN : tb);
            if (c != 0) return -c;
            return Long.compare(b.getId() == null ? 0 : b.getId(), a.getId() == null ? 0 : a.getId());
        });
        return all;
    }

    private FundQueryMapper.QueryParams buildQueryParams(String regionCode, String year) {
        FundQueryMapper.QueryParams p = new FundQueryMapper.QueryParams();
        p.setYear(blankToNull(year));
        p.setRegionCodes(collectDescendantRegionCodes(regionCode));
        if (p.getRegionCodes() != null && p.getRegionCodes().isEmpty()) {
            p.setRegionCodes(null);
        }
        return p;
    }

    /** 递归收集本级区划 + 全部后代区划 code */
    private List<String> collectDescendantRegionCodes(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) return Collections.emptyList();
        // 先看 regionMapper 有没有 listByParentCode 或全量查询
        // 没有就直接查 t_sys_region 全量，内存递归
        List<SysRegion> all = regionMapper.selectList(null);
        Set<String> targets = new HashSet<>();
        collectChildren(all, regionCode.trim(), targets);
        return new ArrayList<>(targets);
    }

    private void collectChildren(List<SysRegion> all, String parent, Set<String> out) {
        // SysRegion.code / parentCode 字段
        out.add(parent);
        for (SysRegion r : all) {
            if (parent.equals(r.getParentCode())) {
                collectChildren(all, r.getCode(), out);
            }
        }
    }

    /** 字典一次性构建 */
    private static final String[] DICT_TYPES = {
            "zi_jin_lei_xing",  // nodeLevel=1 资金类型
            "xia_bo_lei_xing",  // nodeLevel=2/3 下拨类型
            "zi_jin_lai_yuan",  // 资金来源
            "xiang_mu_lei_bie", // 项目类别
            "xiang_mu_lei_xing" // 项目类型
    };

    private Map<String, Map<String, String>> buildDictCache() {
        Map<String, Map<String, String>> cache = new HashMap<>();
        for (String type : DICT_TYPES) {
            List<SysDict> dicts = dictService.listByType(type);
            Map<String, String> inner = new HashMap<>();
            if (dicts != null) {
                for (SysDict d : dicts) {
                    if (d.getDictValue() != null) {
                        inner.put(d.getDictValue(), d.getDictLabel());
                    }
                }
            }
            cache.put(type, inner);
        }
        return cache;
    }

    private void applyDictLabels(List<FundDetailRowVO> rows) {
        if (rows == null || rows.isEmpty()) return;
        Map<String, Map<String, String>> cache = buildDictCache();
        Map<String, String> zjlx   = cache.getOrDefault("zi_jin_lei_xing",   Collections.emptyMap());
        Map<String, String> xblx   = cache.getOrDefault("xia_bo_lei_xing",   Collections.emptyMap());
        Map<String, String> zjly   = cache.getOrDefault("zi_jin_lai_yuan",   Collections.emptyMap());
        Map<String, String> xmlb   = cache.getOrDefault("xiang_mu_lei_bie",  Collections.emptyMap());
        Map<String, String> xmlx   = cache.getOrDefault("xiang_mu_lei_xing", Collections.emptyMap());

        for (FundDetailRowVO row : rows) {
            // 节点层级
            if (row.getNodeLevel() != null) {
                switch (row.getNodeLevel()) {
                    case 1 -> row.setNodeLevelLabel("项目级资金登记");
                    case 2 -> row.setNodeLevelLabel("申报单下拨资金");
                    case 3 -> row.setNodeLevelLabel("到户/公建下拨资金");
                    default -> row.setNodeLevelLabel("未知");
                }
            }
            // 资金类型：1级用 zi_jin_lei_xing，2/3 级用 xia_bo_lei_xing
            Map<String, String> fundTypeDict = row.getNodeLevel() != null && row.getNodeLevel() == 1 ? zjlx : xblx;
            row.setFundTypeLabel(blankToLabel(row.getFundType(), fundTypeDict));
            // 资金来源 / 项目类别 / 项目类型
            row.setFundSourceLabel(blankToLabel(row.getFundSource(), zjly));
            row.setProjectCategoryLabel(blankToLabel(row.getProjectCategory(), xmlb));
            row.setProjectTypeLabel(blankToLabel(row.getProjectType(), xmlx));
        }
    }

    /** 批量回填附件数量 */
    private void attachAttachCount(List<FundDetailRowVO> rows) {
        if (rows == null || rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(FundDetailRowVO::getId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<Map<String, Object>> counts = fundQueryMapper.selectAttachCount(ids);
        Map<Long, Integer> map = new HashMap<>();
        if (counts != null) {
            for (Map<String, Object> m : counts) {
                Object fid = m.get("fundId");
                Object cnt = m.get("cnt");
                if (fid != null) {
                    long id = ((Number) fid).longValue();
                    int c = cnt == null ? 0 : ((Number) cnt).intValue();
                    map.put(id, c);
                }
            }
        }
        for (FundDetailRowVO row : rows) {
            row.setAttachCount(map.getOrDefault(row.getId(), 0));
        }
    }

    // ============== 聚合 ==============

    // ============== 嵌套聚合（仿照 DeclarationQuery 后端模式） ==============

    /**
     * 构造两层嵌套 rows：大分组（按 groupType）→ 每个大分组下按 village 子分组
     * 每个大分组先输出一行 groupSummary=true 的 "全部村组" 汇总行，再依次输出该大分组下的具体村组明细行。
     * 最后追加一行 grandTotal=true 的总计行（remark="全部区划"）。
     *
     * <p>特例：groupType=village 时，大分组就是村组本身，不展示 villageName 子列（前端用 v-if 隐藏），
     * 所有行当作普通明细行，只在最底追加 grandTotal。</p>
     */
    private List<FundStatVO.Row> buildNestedRows(List<FundDetailRowVO> all, String groupType) {
        String gt = (groupType == null || groupType.isBlank()) ? "fundType" : groupType;
        boolean isVillageGroup = "village".equals(gt);

        // 1) 先按大分组 gt 分桶
        Map<String, List<FundDetailRowVO>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (FundDetailRowVO row : all) {
            String val = extractGroupValue(row, gt);
            if (val == null) continue;
            labels.put(val, extractGroupLabel(row, gt));
            grouped.computeIfAbsent(val, k -> new ArrayList<>()).add(row);
        }

        List<FundStatVO.Row> out = new ArrayList<>();

        // nodeLevel 固定按 1→2→3 顺序，其他维度按总金额降序
        List<Map.Entry<String, List<FundDetailRowVO>>> sortedGroups = new ArrayList<>(grouped.entrySet());
        if ("nodeLevel".equals(gt)) {
            sortedGroups.sort((a, b) -> {
                int ia = parseIntOr(a.getKey(), Integer.MAX_VALUE);
                int ib = parseIntOr(b.getKey(), Integer.MAX_VALUE);
                return Integer.compare(ia, ib);
            });
        } else {
            sortedGroups.sort((a, b) -> {
                BigDecimal sa = sumAmount(a.getValue()), sb = sumAmount(b.getValue());
                return sb.compareTo(sa);
            });
        }

        // 2) 遍历每个大分组：先输出 groupSummary 行（全部村组），再输出 village 明细行
        for (Map.Entry<String, List<FundDetailRowVO>> e : sortedGroups) {
            String bigGroupValue = e.getKey();
            String bigGroupLabel = labels.getOrDefault(bigGroupValue, bigGroupValue);
            List<FundDetailRowVO> bucket = e.getValue();

            // 2a) 大分组整体汇总行
            out.add(buildOneRow(bigGroupValue, bigGroupLabel, "全部村组", bucket,
                    true, false, null));

            if (!isVillageGroup) {
                // 2b) 该大分组下再按 village 细分输出明细行
                Map<String, List<FundDetailRowVO>> villageBucket = bucket.stream()
                        .collect(Collectors.groupingBy(
                                r -> blankToNull(r.getVillageName()) == null ? "(未填)" : r.getVillageName(),
                                LinkedHashMap::new, Collectors.toList()));
                for (Map.Entry<String, List<FundDetailRowVO>> ve : villageBucket.entrySet()) {
                    out.add(buildOneRow(bigGroupValue, bigGroupLabel, ve.getKey(), ve.getValue(),
                            false, false, null));
                }
            }
        }

        // 3) 最底追加 grandTotal 行
        if (!all.isEmpty()) {
            out.add(buildOneRow("", "总计", "全部区划", all, false, true, "全部区划"));
        }

        return out;
    }

    /** 按给定明细行集合聚合出一行 Row */
    private FundStatVO.Row buildOneRow(String groupValue, String groupLabel, String villageName,
                                       List<FundDetailRowVO> rows,
                                       boolean groupSummary, boolean grandTotal, String remark) {
        FundStatVO.Row r = new FundStatVO.Row();
        r.setGroupValue(groupValue);
        r.setGroupLabel(groupLabel);
        r.setVillageName(villageName);
        r.setGroupSummary(groupSummary);
        r.setGrandTotal(grandTotal);
        r.setRemark(remark);
        r.setRecordCount(rows.size());

        Set<Long> projs = new HashSet<>();
        Set<String> villages = new HashSet<>();
        BigDecimal amt = BigDecimal.ZERO;
        for (FundDetailRowVO row : rows) {
            if (row.getApprovalId() != null) projs.add(row.getApprovalId());
            String vn = blankToNull(row.getVillageName());
            if (vn != null) villages.add(vn);
            BigDecimal a = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            amt = amt.add(a);
        }
        r.setProjectCount(projs.size());
        r.setVillageCoverage(villages.size());
        r.setAmount(amt);
        return r;
    }

    private String extractGroupValue(FundDetailRowVO r, String gt) {
        if (gt == null) gt = "fundType";
        return switch (gt) {
            case "fundSource" -> blankToNull(r.getFundSource());
            case "nodeLevel"  -> r.getNodeLevel() == null ? null : String.valueOf(r.getNodeLevel());
            case "village"    -> blankToNull(r.getVillageName());
            default -> blankToNull(r.getFundType());
        };
    }

    private String extractGroupLabel(FundDetailRowVO r, String gt) {
        if (gt == null) gt = "fundType";
        return switch (gt) {
            case "fundSource" -> r.getFundSourceLabel() != null ? r.getFundSourceLabel() : blankToNull(r.getFundSource());
            case "nodeLevel"  -> r.getNodeLevelLabel() != null ? r.getNodeLevelLabel() : (r.getNodeLevel() == null ? null : String.valueOf(r.getNodeLevel()));
            case "village"    -> blankToNull(r.getVillageName());
            default -> r.getFundTypeLabel() != null ? r.getFundTypeLabel() : blankToNull(r.getFundType());
        };
    }

    /** detailList 接口里，根据 groupType + groupValue 把明细行过滤出来 */
    private boolean matchGroup(FundDetailRowVO row, String gt, String gv) {
        String val = extractGroupValue(row, gt);
        if (val == null) return false;
        return val.equals(gv);
    }

    // ============== 导出（CSV 格式，和 DeclarationQuery 保持一致） ==============

    /**
     * 导出当前分组下的全部明细到 CSV（UTF-8 BOM，Excel 直接打开不乱码）。
     *
     * @return 文件字节流，Controller 写入 ResponseOutputStream
     */
    @Override
    public byte[] exportCsv(String year, String regionCode,
                            String groupType, String groupValue) {
        List<FundDetailRowVO> rows = detailList(year, regionCode, groupType, groupValue);
        if (rows.size() > 5000) {
            throw new RuntimeException("导出上限 5000 条，请缩小筛选范围");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            w.write('\ufeff'); // BOM
            // 表头（和文档一致）
            w.write("节点层级,立项编号,项目名称,申报编号,申报名称,明细名称,资金类型,资金来源,金额,乡镇,村组,经办人,经办时间,备注\n");
            for (FundDetailRowVO r : rows) {
                w.write(csv(r.getNodeLevelLabel())); w.write(',');
                w.write(csv(r.getApprovalCode()));  w.write(',');
                w.write(csv(r.getProjectName()));   w.write(',');
                w.write(csv(r.getDeclareCode()));   w.write(',');
                w.write(csv(r.getDeclareName()));   w.write(',');
                w.write(csv(r.getDetailName()));    w.write(',');
                w.write(csv(r.getFundTypeLabel())); w.write(',');
                w.write(csv(r.getFundSourceLabel()));w.write(',');
                w.write(csv(r.getAmount() == null ? null : r.getAmount().toPlainString())); w.write(',');
                w.write(csv(r.getTownName()));      w.write(',');
                w.write(csv(r.getVillageName()));   w.write(',');
                w.write(csv(r.getCreateBy()));      w.write(',');
                w.write(csv(r.getCreateTime() == null ? null : r.getCreateTime().toString())); w.write(',');
                w.write(csv(r.getRemark()));        w.write('\n');
            }
            // 尾部统计口径小字
            w.write("\n统计口径：登记合计为 nodeLevel=1 金额，下拨合计为 nodeLevel=2、3 金额。\n");
        } catch (Exception e) {
            log.error("导出 CSV 失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
        return baos.toByteArray();
    }

    private static String csv(String s) {
        if (s == null) return "";
        String x = s.replace("\"", "\"\"");
        return x.contains(",") || x.contains("\n") || x.contains("\"") ? "\"" + x + "\"" : x;
    }

    // ============== 工具 ==============

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String blankToLabel(String value, Map<String, String> dict) {
        if (value == null) return "";
        if (dict == null || dict.isEmpty()) return value;
        String v = dict.get(value);
        return v == null ? value : v;
    }

    private static int parseIntOr(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private static BigDecimal sumAmount(List<FundDetailRowVO> rows) {
        BigDecimal r = BigDecimal.ZERO;
        for (FundDetailRowVO row : rows) {
            BigDecimal a = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            r = r.add(a);
        }
        return r;
    }
}
