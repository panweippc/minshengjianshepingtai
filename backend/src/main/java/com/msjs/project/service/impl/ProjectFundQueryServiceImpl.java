package com.msjs.project.service.impl;

import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.entity.SysDict;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.FundQueryMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.ProjectFundQueryService;
import com.msjs.project.service.SysDictService;
import com.msjs.project.vo.FundQueryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资金记录查询服务实现
 * <p>核心思路：Mapper 返回扁平基础字段，Service 层一次性做：</p>
 * <ol>
 *   <li>递归收集区划后代 code（与 DeclarationServiceImpl.collectDescendantRegionCodes 同逻辑）</li>
 *   <li>查询并构建 4 张字典 value→label 映射</li>
 *   <li>调用 Mapper 拉平查询得到全量命中流水</li>
 *   <li>批量查附件数回填 attachCount</li>
 *   <li>遍历回填 nodeLevelLabel / fundTypeLabel / fundSourceLabel / 项目类别标签 / 项目类型标签 / 明细类型标签 / 时间字符串</li>
 *   <li>内存聚合 Summary + Sidebar</li>
 *   <li>内存分页得到 records 切片</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class ProjectFundQueryServiceImpl implements ProjectFundQueryService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 明细类型 → 中文 */
    private static final Map<String, String> DETAIL_TYPE_LABEL = Map.of(
            "HOUSEHOLD", "农户",
            "FACILITY", "公建设施"
    );

    private final FundQueryMapper fundQueryMapper;
    private final SysDictService dictService;
    private final SysRegionMapper regionMapper;

    /** 最后一次 page/export 产生的汇总卡统计（供 Controller 在一次请求中取出塞进 Result 扩展字段） */
    private FundQueryVO.Summary lastSummary;
    /** 最后一次 page/export 产生的左侧面板统计 */
    private FundQueryVO.Sidebar lastSidebar;

    @Override
    public PageVO<FundQueryVO.Record> page(
            long pageNum, long pageSize,
            String projectName, String approvalCode, String declareCode, String detailName,
            Integer nodeLevel, String fundType, String fundSource,
            String regionCode,
            String startTime, String endTime,
            String createBy, BigDecimal minAmount, BigDecimal maxAmount
    ) {
        List<FundQueryVO.Record> all = doQuery(projectName, approvalCode, declareCode, detailName,
                nodeLevel, fundType, fundSource, regionCode, startTime, endTime,
                createBy, minAmount, maxAmount);

        // summary
        FundQueryVO.Summary summary = buildSummary(all);
        this.lastSummary = summary;
        // sidebar
        FundQueryVO.Sidebar sidebar = buildSidebar(all);
        this.lastSidebar = sidebar;

        // 内存分页
        long total = all.size();
        long fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        long toIndex = Math.min(total, fromIndex + pageSize);
        List<FundQueryVO.Record> records = fromIndex >= total
                ? Collections.emptyList()
                : all.subList((int) fromIndex, (int) toIndex);

        PageVO<FundQueryVO.Record> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public FundQueryVO.Summary lastSummary() {
        return lastSummary;
    }

    @Override
    public FundQueryVO.Sidebar lastSidebar() {
        return lastSidebar;
    }

    @Override
    public byte[] export(
            String projectName, String approvalCode, String declareCode, String detailName,
            Integer nodeLevel, String fundType, String fundSource,
            String regionCode,
            String startTime, String endTime,
            String createBy, BigDecimal minAmount, BigDecimal maxAmount
    ) {
        List<FundQueryVO.Record> all = doQuery(projectName, approvalCode, declareCode, detailName,
                nodeLevel, fundType, fundSource, regionCode, startTime, endTime,
                createBy, minAmount, maxAmount);
        this.lastSummary = buildSummary(all);
        this.lastSidebar = buildSidebar(all);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // BOM，Excel 双击可识别 UTF-8
        // 表头
        String[] headers = {
                "节点层级", "项目编号", "项目名称", "项目类别", "项目类型",
                "申报编号", "申报标题", "明细名称", "明细类型",
                "资金类型", "资金来源", "金额（元）",
                "所属乡镇", "所属村组", "经办人", "经办时间", "拨付说明", "附件数"
        };
        sb.append(String.join(",", headers)).append("\r\n");
        for (FundQueryVO.Record r : all) {
            List<String> cells = new ArrayList<>();
            cells.add(csvCell(r.getNodeLevelLabel()));
            cells.add(csvCell(r.getApprovalCode()));
            cells.add(csvCell(r.getProjectName()));
            cells.add(csvCell(r.getProjectCategoryLabel()));
            cells.add(csvCell(r.getProjectTypeLabel()));
            cells.add(csvCell(r.getDeclareCode()));
            cells.add(csvCell(r.getDeclareName()));
            cells.add(csvCell(r.getDetailName()));
            cells.add(csvCell(r.getDetailType() == null ? "" : DETAIL_TYPE_LABEL.getOrDefault(r.getDetailType(), r.getDetailType())));
            cells.add(csvCell(r.getFundTypeLabel()));
            cells.add(csvCell(r.getFundSourceLabel()));
            cells.add(r.getAmount() == null ? "0" : r.getAmount().toPlainString());
            cells.add(csvCell(r.getTownName()));
            cells.add(csvCell(r.getVillageName()));
            cells.add(csvCell(r.getCreateBy()));
            cells.add(csvCell(r.getCreateTime() == null ? "" : r.getCreateTime().format(DTF)));
            cells.add(csvCell(r.getRemark()));
            cells.add(String.valueOf(r.getAttachCount() == null ? 0 : r.getAttachCount()));
            sb.append(String.join(",", cells)).append("\r\n");
        }
        sb.append('\r');
        sb.append("统计口径：资金记录按单条流水计数；登记合计指 nodeLevel=1 的金额合计；下拨合计指 nodeLevel∈{2,3} 的金额合计。");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ==================== 内部实现 ====================

    /** 一次全量查询 + 字典翻译 + 附件数回填 */
    /** 3 段查询共用的 Mapper 参数 DTO */
    private FundQueryMapper.CommonQueryParams buildParams(
            String projectName, String approvalCode, String declareCode, String detailName,
            String fundType, String fundSource,
            String startTime, String endTime, String createBy,
            BigDecimal minAmount, BigDecimal maxAmount, List<String> regionCodes
    ) {
        FundQueryMapper.CommonQueryParams p = new FundQueryMapper.CommonQueryParams();
        p.setProjectName(blankToNull(projectName));
        p.setApprovalCode(blankToNull(approvalCode));
        p.setDeclareCode(blankToNull(declareCode));
        p.setDetailName(blankToNull(detailName));
        p.setFundType(blankToNull(fundType));
        p.setFundSource(blankToNull(fundSource));
        p.setStartTime(blankToNull(startTime));
        p.setEndTime(blankToNull(endTime));
        p.setCreateBy(blankToNull(createBy));
        p.setMinAmount(minAmount);
        p.setMaxAmount(maxAmount);
        p.setRegionCodes(regionCodes.isEmpty() ? null : regionCodes);
        return p;
    }

    private List<FundQueryVO.Record> doQuery(
            String projectName, String approvalCode, String declareCode, String detailName,
            Integer nodeLevel, String fundType, String fundSource,
            String regionCode,
            String startTime, String endTime,
            String createBy, BigDecimal minAmount, BigDecimal maxAmount
    ) {
        // 1) 递归收集区划后代 code
        List<String> regionCodes = collectDescendantRegionCodes(regionCode);

        FundQueryMapper.CommonQueryParams p = buildParams(
                projectName, approvalCode, declareCode, detailName,
                fundType, fundSource, startTime, endTime, createBy,
                minAmount, maxAmount, regionCodes
        );

        // 2) 分 3 段独立查询，避开 UNION 的 collation 不一致 + JSqlParser 对 COLLATE 的解析不兼容
        List<FundQueryVO.Record> all = new ArrayList<>();
        if (nodeLevel == null || nodeLevel == Constants.FUND_LEVEL_APPROVAL) {
            List<FundQueryVO.Record> r1 = fundQueryMapper.selectLevel1(p);
            if (r1 != null) all.addAll(r1);
        }
        if (nodeLevel == null || nodeLevel == Constants.FUND_LEVEL_DECLARE) {
            List<FundQueryVO.Record> r2 = fundQueryMapper.selectLevel2(p);
            if (r2 != null) all.addAll(r2);
        }
        if (nodeLevel == null || nodeLevel == Constants.FUND_LEVEL_DETAIL) {
            List<FundQueryVO.Record> r3 = fundQueryMapper.selectLevel3(p);
            if (r3 != null) all.addAll(r3);
        }
        // 合并后按时间倒序 + id 倒序（与原 UNION ALL ORDER BY 一致）
        all.sort((a, b) -> {
            LocalDateTime ta = a.getCreateTime(), tb = b.getCreateTime();
            int c = (ta == null ? LocalDateTime.MIN : ta).compareTo(tb == null ? LocalDateTime.MIN : tb);
            if (c != 0) return -c;
            Long ia = a.getId(), ib = b.getId();
            return Long.compare(ib == null ? 0 : ib, ia == null ? 0 : ia);
        });

        // 3) 字典缓存（5 种字典，一次构建全量复用）
        Map<String, String> dictZjlx = dictToMap("zi_jin_lei_xing");
        Map<String, String> dictXblx = dictToMap("xia_bo_lei_xing");
        Map<String, String> dictZjly = dictToMap("zi_jin_lai_yuan");
        Map<String, String> dictXmlb = dictToMap("xiang_mu_lei_bie");
        Map<String, String> dictXmlx = dictToMap("xiang_mu_lei_xing");

        // 4) 字典翻译 + 时间格式化 + 节点层级 label
        for (FundQueryVO.Record r : all) {
            r.setNodeLevelLabel(nodeLevelLabel(r.getNodeLevel()));
            r.setFundTypeLabel(dictValueByNode(r.getNodeLevel(), r.getFundType(), dictZjlx, dictXblx));
            r.setFundSourceLabel(dictZjly.getOrDefault(r.getFundSource(), r.getFundSource()));
            r.setProjectCategoryLabel(dictXmlb.getOrDefault(r.getProjectCategory(), r.getProjectCategory()));
            r.setProjectTypeLabel(dictXmlx.getOrDefault(r.getProjectType(), r.getProjectType()));
            if (r.getDetailType() != null && !r.getDetailType().isBlank()) {
                // 明细类型中文 label 交给前端按 detailType 值自行映射，VO 里保留原始值即可
            }
            // 时间不做手工格式化：VO.createTime 为 LocalDateTime，Jackson 用 @JsonFormat 自动序列化为 yyyy-MM-dd HH:mm:ss
            // 空值兜底
            if (r.getAttachCount() == null) r.setAttachCount(0);
        }

        // 5) 批量回填附件数
        if (!all.isEmpty()) {
            List<Long> fundIds = all.stream().map(FundQueryVO.Record::getId).filter(Objects::nonNull).toList();
            List<Map<String, Object>> attachRows = fundQueryMapper.selectAttachCount(fundIds);
            Map<Long, Integer> attMap = new HashMap<>();
            if (attachRows != null) {
                for (Map<String, Object> row : attachRows) {
                    Object fidObj = row.get("fundId");
                    Object cntObj = row.get("cnt");
                    if (fidObj != null && cntObj != null) {
                        Long fid = ((Number) fidObj).longValue();
                        int cnt = ((Number) cntObj).intValue();
                        attMap.put(fid, cnt);
                    }
                }
            }
            for (FundQueryVO.Record r : all) {
                if (r.getId() != null) {
                    r.setAttachCount(attMap.getOrDefault(r.getId(), 0));
                }
            }
        }
        return all;
    }

    /** 顶部汇总卡 */
    private FundQueryVO.Summary buildSummary(List<FundQueryVO.Record> all) {
        FundQueryVO.Summary s = new FundQueryVO.Summary();
        s.setRecordCount(all.size());
        BigDecimal registerTotal = BigDecimal.ZERO;
        BigDecimal allocateTotal = BigDecimal.ZERO;
        Set<Long> projects = new HashSet<>();
        Set<Long> declares = new HashSet<>();
        Set<Long> details = new HashSet<>();
        for (FundQueryVO.Record r : all) {
            BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
            Integer lvl = r.getNodeLevel();
            if (lvl != null && lvl == Constants.FUND_LEVEL_APPROVAL) {
                registerTotal = registerTotal.add(amt);
            } else {
                allocateTotal = allocateTotal.add(amt);
            }
            if (r.getApprovalId() != null) projects.add(r.getApprovalId());
            if (r.getNodeLevel() != null && r.getNodeLevel() >= Constants.FUND_LEVEL_DECLARE && r.getDeclareId() != null) {
                declares.add(r.getDeclareId());
            }
            if (r.getNodeLevel() != null && r.getNodeLevel() == Constants.FUND_LEVEL_DETAIL && r.getDetailId() != null) {
                details.add(r.getDetailId());
            }
        }
        s.setRegisterTotal(registerTotal);
        s.setAllocateTotal(allocateTotal);
        s.setProjectCount(projects.size());
        s.setDeclareCount(declares.size());
        s.setDetailCount(details.size());
        return s;
    }

    /** 左侧面板：按资金类型 / 资金来源 / 节点层级 / 村组 */
    private FundQueryVO.Sidebar buildSidebar(List<FundQueryVO.Record> all) {
        Map<String, String> dictZjlx = dictToMap("zi_jin_lei_xing");
        Map<String, String> dictXblx = dictToMap("xia_bo_lei_xing");
        Map<String, String> dictZjly = dictToMap("zi_jin_lai_yuan");

        FundQueryVO.Sidebar sb = new FundQueryVO.Sidebar();

        // byFundType：nodeLevel 合并后按 (nodeLevel, fundType) 分组
        Map<String, FundQueryVO.StatItem> ftMap = new HashMap<>();
        Map<String, FundQueryVO.StatItem> fsMap = new HashMap<>();
        Map<Integer, FundQueryVO.StatItem> nlMap = new HashMap<>();
        Map<String, FundQueryVO.StatItem> vgMap = new HashMap<>();

        for (FundQueryVO.Record r : all) {
            BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();

            // 资金类型
            if (r.getFundType() != null && !r.getFundType().isBlank()) {
                String key = r.getFundType();
                FundQueryVO.StatItem item = ftMap.computeIfAbsent(key, k -> new FundQueryVO.StatItem());
                item.setValue(key);
                item.setLabel(dictValueByNode(r.getNodeLevel(), key, dictZjlx, dictXblx));
                item.setCount(item.getCount() + 1);
                item.setAmount(item.getAmount() == null ? amt : item.getAmount().add(amt));
            }

            // 资金来源
            if (r.getFundSource() != null && !r.getFundSource().isBlank()) {
                String key = r.getFundSource();
                FundQueryVO.StatItem item = fsMap.computeIfAbsent(key, k -> new FundQueryVO.StatItem());
                item.setValue(key);
                item.setLabel(dictZjly.getOrDefault(key, key));
                item.setCount(item.getCount() + 1);
                item.setAmount(item.getAmount() == null ? amt : item.getAmount().add(amt));
            }

            // 节点层级
            if (r.getNodeLevel() != null) {
                Integer lvl = r.getNodeLevel();
                FundQueryVO.StatItem item = nlMap.computeIfAbsent(lvl, k -> new FundQueryVO.StatItem());
                item.setValue(String.valueOf(lvl));
                item.setLabel(nodeLevelLabel(lvl));
                item.setCount(item.getCount() + 1);
                item.setAmount(item.getAmount() == null ? amt : item.getAmount().add(amt));
            }

            // 村组
            String village = r.getVillageName();
            if (village == null || village.isBlank()) {
                // nodeLevel=1 没有 villageName，退回项目的 regionName（作为所属村组）
                village = r.getTownName();
            }
            if (village != null && !village.isBlank()) {
                FundQueryVO.StatItem item = vgMap.computeIfAbsent(village, k -> new FundQueryVO.StatItem());
                item.setValue(village);
                item.setLabel(village);
                item.setCount(item.getCount() + 1);
                item.setAmount(item.getAmount() == null ? amt : item.getAmount().add(amt));
            }
        }

        sb.setByFundType(sortedStatItems(ftMap.values()));
        sb.setByFundSource(sortedStatItems(fsMap.values()));
        sb.setByNodeLevel(sortedStatItems(nlMap.values()));
        sb.setByVillage(sortedStatItems(vgMap.values()));
        return sb;
    }

    /** StatItem 按 count 倒序，count 相同按 amount 倒序 */
    private List<FundQueryVO.StatItem> sortedStatItems(Collection<FundQueryVO.StatItem> items) {
        return items.stream()
                .sorted((a, b) -> {
                    int c = Long.compare(b.getCount(), a.getCount());
                    if (c != 0) return c;
                    BigDecimal ba = a.getAmount() == null ? BigDecimal.ZERO : a.getAmount();
                    BigDecimal bb = b.getAmount() == null ? BigDecimal.ZERO : b.getAmount();
                    return bb.compareTo(ba);
                })
                .collect(Collectors.toList());
    }

    /** 递归收集后代区划 code（与 DeclarationServiceImpl.collectDescendantRegionCodes 同逻辑，独立实现避免依赖） */
    private List<String> collectDescendantRegionCodes(String regionCode) {
        List<String> result = new ArrayList<>();
        if (regionCode == null || regionCode.isBlank()) return result;
        Set<String> collected = new HashSet<>();
        collected.add(regionCode.trim());
        Set<String> current = new HashSet<>();
        current.add(regionCode.trim());
        while (!current.isEmpty()) {
            List<SysRegion> children = regionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRegion>()
                            .in(SysRegion::getParentCode, current));
            if (children.isEmpty()) break;
            Set<String> next = new HashSet<>();
            for (SysRegion r : children) {
                if (r.getCode() != null && collected.add(r.getCode())) {
                    next.add(r.getCode());
                }
            }
            current = next;
        }
        result.addAll(collected);
        return result;
    }

    /** 按字典类型构建 value→label 映射（空 Map 兜底） */
    private Map<String, String> dictToMap(String dictType) {
        List<SysDict> items;
        try {
            items = dictService.listByType(dictType);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
        if (items == null || items.isEmpty()) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        for (SysDict d : items) {
            if (d.getDictValue() != null) {
                map.put(d.getDictValue(), d.getDictLabel() != null ? d.getDictLabel() : d.getDictValue());
            }
        }
        return map;
    }

    /** nodeLevel 决定用哪个字典翻译 fundType */
    private String dictValueByNode(Integer nodeLevel, String value, Map<String, String> lvl1Map, Map<String, String> lvl23Map) {
        if (value == null) return null;
        if (nodeLevel != null && nodeLevel == Constants.FUND_LEVEL_APPROVAL) {
            return lvl1Map.getOrDefault(value, value);
        }
        return lvl23Map.getOrDefault(value, value);
    }

    /** 节点层级中文 */
    private static String nodeLevelLabel(Integer nodeLevel) {
        if (nodeLevel == null) return "";
        return switch (nodeLevel) {
            case 1 -> "资金登记";
            case 2 -> "二级下拨";
            case 3 -> "三级下拨";
            default -> "层级" + nodeLevel;
        };
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** CSV 单元格转义 */
    private static String csvCell(String s) {
        if (s == null) return "";
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\n') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
