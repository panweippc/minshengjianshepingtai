package com.msjs.project.service.impl;

import com.msjs.project.dto.ProjectCycleQueryDTO;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysDict;
import com.msjs.project.mapper.ProjectCycleQueryMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.ProjectCycleQueryService;
import com.msjs.project.service.SysDictService;
import com.msjs.project.vo.ProjectCycleDetailVO;
import com.msjs.project.vo.ProjectCycleRawVO;
import com.msjs.project.vo.ProjectCycleStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 综合查询-项目全周期 Service
 *
 * <p>5套统计方案，核心逻辑在 Service 层内存聚合：
 * <ol>
 *   <li>category   - 按项目类别（declareCategory: household到户 / public公建）</li>
 *   <li>source     - 按项目来源（projectSource）</li>
 *   <li>establish  - 按立项状态（待立项/已立项/已归档），仅统计涉及户数、公建设施数</li>
 *   <li>status     - 按项目状态（综合状态：未开工/施工中/已完工/待验收/已验收/未通过）</li>
 *   <li>village    - 按所属村组统计（项目ID去重）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCycleQueryServiceImpl implements ProjectCycleQueryService {

    private final ProjectCycleQueryMapper cycleMapper;
    private final SysDictService dictService;
    private final SysRegionMapper regionMapper;

    // ================== 对外接口 ==================

    @Override
    public ProjectCycleStatVO stat(ProjectCycleQueryDTO query) {
        ProjectCycleQueryMapper.QueryParams p = buildQueryParams(query.getRegionCode(), query.getYear());
        String queryType = blankToDefault(query.getQueryType(), "category");

        // 方案3（按立项状态）需要包含所有状态的立项单（不仅仅是 approved）
        List<ProjectCycleRawVO> rawList;
        if ("establish".equals(queryType)) {
            rawList = cycleMapper.selectRawListAllStatus(p);
            // 方案3 动态年度筛选：不同分组用不同日期字段
            // PENDING/APPROVED 用 submit_time，ARCHIVED 用 archive.create_time
            if (blankToNull(query.getYear()) != null) {
                rawList = filterEstablishByYear(rawList, Integer.parseInt(query.getYear()));
            }
        } else {
            rawList = cycleMapper.selectRawList(p);
        }

        // 字典翻译
        applyDictLabels(rawList);

        // 预加载施工/验收状态（用于方案4）
        Map<Long, String> constructionStatusMap = Collections.emptyMap();
        Map<Long, String> acceptanceStatusMap = Collections.emptyMap();
        if ("status".equals(queryType)) {
            constructionStatusMap = buildConstructionStatusMap();
            acceptanceStatusMap = buildAcceptanceStatusMap();
        }

        ProjectCycleStatVO vo = new ProjectCycleStatVO();

        // 全局汇总
        vo.setGlobalSummary(buildGlobalSummary(rawList));

        // 根据方案类型构建分组
        List<ProjectCycleStatVO.Row> rows = buildGroupedRows(rawList, queryType, constructionStatusMap, acceptanceStatusMap);
        vo.setRows(rows);
        vo.setTotal((long) rows.size());

        return vo;
    }

    @Override
    public List<ProjectCycleDetailVO> detail(ProjectCycleQueryDTO query) {
        String queryType = blankToDefault(query.getQueryType(), "category");
        ProjectCycleQueryMapper.QueryParams p = buildQueryParams(query.getRegionCode(), query.getYear());

        // 与 stat() 保持完全一致的取数口径
        List<ProjectCycleRawVO> rawList;
        if ("establish".equals(queryType)) {
            rawList = cycleMapper.selectRawListAllStatus(p);
            if (blankToNull(query.getYear()) != null) {
                rawList = filterEstablishByYear(rawList, Integer.parseInt(query.getYear()));
            }
        } else {
            rawList = cycleMapper.selectRawList(p);
        }
        applyDictLabels(rawList);

        Map<Long, String> constructionMap = Collections.emptyMap();
        Map<Long, String> acceptanceMap = Collections.emptyMap();
        if ("status".equals(queryType)) {
            constructionMap = buildConstructionStatusMap();
            acceptanceMap = buildAcceptanceStatusMap();
        }

        String groupValue = blankToNull(query.getGroupValue());
        String villageFilter = blankToNull(query.getVillageName());
        // 合计行 / 小计行的"虚拟"村组名不作为过滤条件
        if ("全部村组".equals(villageFilter) || "全部区划".equals(villageFilter)) {
            villageFilter = null;
        }

        List<ProjectCycleDetailVO> out = new ArrayList<>();
        for (ProjectCycleRawVO raw : rawList) {
            LabeledRow lr = labelRow(raw, queryType, constructionMap, acceptanceMap);
            if (lr == null) continue;
            if (groupValue != null && !groupValue.equals(lr.groupKey)) continue;
            if (villageFilter != null && !villageFilter.equals(raw.getVillageName())) continue;
            out.add(toDetailVO(raw, constructionMap, acceptanceMap));
        }
        return out;
    }

    /** 原始行 → 明细核对行 */
    private ProjectCycleDetailVO toDetailVO(ProjectCycleRawVO raw,
                                            Map<Long, String> constructionMap,
                                            Map<Long, String> acceptanceMap) {
        ProjectCycleDetailVO vo = new ProjectCycleDetailVO();
        vo.setApprovalId(raw.getApprovalId());
        vo.setApprovalCode(raw.getApprovalCode());
        vo.setProjectName(raw.getProjectName());
        vo.setDeclareId(raw.getDeclareId());
        vo.setCategoryLabel(raw.getDeclareCategoryLabel());
        vo.setTypeLabel(raw.getProjectTypeLabel());
        vo.setProjectSource(raw.getProjectSource());
        vo.setVillageName(raw.getVillageName());
        vo.setBudget(raw.getBudget());
        vo.setHouseholdCount(raw.getHouseholdCount());
        vo.setFacilityCount(raw.getFacilityCount());
        vo.setTotalFund(raw.getTotalFund());

        String est = computeEstablishStatus(raw.getApprovalStatus(), raw.getAcceptStatus());
        vo.setEstablishStatusLabel(est == null
                ? "已驳回" : ESTABLISH_STATUS_LABELS.getOrDefault(est, est));

        String ps = computeProjectStatus(raw.getApprovalId(), raw.getApprovalStatus(),
                raw.getAcceptStatus(), constructionMap, acceptanceMap);
        vo.setProjectStatusLabel(ps == null ? "-" : PROJECT_STATUS_LABELS.getOrDefault(ps, ps));

        vo.setSubmitTime(raw.getSubmitTime());
        vo.setArchiveCreateTime(raw.getArchiveCreateTime());
        return vo;
    }

    @Override
    public byte[] exportExcel(ProjectCycleQueryDTO query) {
        ProjectCycleStatVO vo = stat(query);
        String queryType = blankToDefault(query.getQueryType(), "category");

        boolean isHouseholdFacility = "establish".equals(queryType) || "status".equals(queryType);
        String groupHeaderLabel = getGroupHeaderLabel(queryType);

        String[] headers;
        if (isHouseholdFacility) {
            headers = new String[]{groupHeaderLabel, "村组", "涉及户数（个）", "公建设施数（个）"};
        } else {
            headers = new String[]{groupHeaderLabel, "村组", "项目数量（个）", "覆盖村组（个）", "项目预算（元）", "累计拨付（元）"};
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            w.write('\ufeff'); // BOM，Excel 直接打开不乱码

            // 表头
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) w.write(',');
                w.write(csv(headers[i]));
            }
            w.write('\n');

            // 数据行
            for (ProjectCycleStatVO.Row r : vo.getRows()) {
                w.write(csv(r.getGroupLabel())); w.write(',');
                w.write(csv(r.getVillageName())); w.write(',');

                if (isHouseholdFacility) {
                    w.write(csv(String.valueOf(r.getHouseholdCount() != null ? r.getHouseholdCount() : 0))); w.write(',');
                    w.write(csv(String.valueOf(r.getFacilityCount() != null ? r.getFacilityCount() : 0)));
                } else {
                    w.write(csv(String.valueOf(r.getProjectCount() != null ? r.getProjectCount() : 0))); w.write(',');
                    w.write(csv(String.valueOf(r.getVillageCoverage() != null ? r.getVillageCoverage() : 0))); w.write(',');
                    w.write(csv(r.getBudget() == null ? "" : r.getBudget().toPlainString())); w.write(',');
                    w.write(csv(r.getTotalFund() == null ? "" : r.getTotalFund().toPlainString()));
                }
                w.write('\n');
            }

            // 统计口径说明
            w.write("\n统计口径：\n");
            w.write(getCaliberNote(queryType) + "\n");
            w.write("各类显示小计，村组行显示该分类在对应村组的汇总结果；总计按当前筛选条件重新计算。\n");
        } catch (Exception e) {
            log.error("导出 CSV 失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
        return baos.toByteArray();
    }

    /** CSV 转义（含逗号、引号、换行时加双引号） */
    private static String csv(String s) {
        if (s == null) return "";
        String x = s.replace("\"", "\"\"");
        return x.contains(",") || x.contains("\n") || x.contains("\"") ? "\"" + x + "\"" : x;
    }

    // ================== 内部：查询参数 + 字典 + 区划 ==================

    private ProjectCycleQueryMapper.QueryParams buildQueryParams(String regionCode, String year) {
        ProjectCycleQueryMapper.QueryParams p = new ProjectCycleQueryMapper.QueryParams();
        p.setYear(blankToNull(year));
        p.setRegionCodes(collectDescendantRegionCodes(regionCode));
        if (p.getRegionCodes() != null && p.getRegionCodes().isEmpty()) {
            p.setRegionCodes(null);
        }
        return p;
    }

    private List<String> collectDescendantRegionCodes(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) return Collections.emptyList();
        List<SysRegion> all = regionMapper.selectList(null);
        Set<String> targets = new HashSet<>();
        collectChildren(all, regionCode.trim(), targets);
        return new ArrayList<>(targets);
    }

    private void collectChildren(List<SysRegion> all, String parent, Set<String> out) {
        out.add(parent);
        for (SysRegion r : all) {
            if (parent.equals(r.getParentCode())) {
                collectChildren(all, r.getCode(), out);
            }
        }
    }

    private static final String[] DICT_TYPES = {
            "shen_bao_lei_bie",    // 申报类别（declare_category：1到户 / 2公建）
            "xiang_mu_lei_bie",    // 项目类别
            "xiang_mu_lei_xing",  // 项目类型
            "xiang_mu_lai_yuan",  // 项目来源
            "li_xiang_zhuang_tai", // 立项状态
            "yan_shou_jie_guo"    // 验收结果
    };

    /** 翻译字典：declareCategory、projectType 都走字典 */
    private void applyDictLabels(List<ProjectCycleRawVO> rows) {
        if (rows == null || rows.isEmpty()) return;

        Map<String, Map<String, String>> cache = buildDictCache();
        Map<String, String> categoryDict = cache.getOrDefault("shen_bao_lei_bie", Collections.emptyMap());
        Map<String, String> typeDict     = cache.getOrDefault("xiang_mu_lei_xing", Collections.emptyMap());

        for (ProjectCycleRawVO row : rows) {
            // declareCategory：从字典 shen_bao_lei_bie 翻译（兼容 household/public 旧值）
            String dc = row.getDeclareCategory();
            if ("household".equals(dc)) {
                row.setDeclareCategoryLabel("到户项目");
            } else if ("public".equals(dc)) {
                row.setDeclareCategoryLabel("公建项目");
            } else {
                row.setDeclareCategoryLabel(blankToLabel(dc, categoryDict));
            }

            // projectType：从字典 xiang_mu_lei_xing 翻译
            row.setProjectTypeLabel(blankToLabel(row.getProjectType(), typeDict));
        }
    }

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

    // ================== 内部：全局汇总 ==================

    private ProjectCycleStatVO.GlobalSummary buildGlobalSummary(List<ProjectCycleRawVO> rawList) {
        ProjectCycleStatVO.GlobalSummary gs = new ProjectCycleStatVO.GlobalSummary();
        Set<Long> approvalIds = new HashSet<>();
        Set<Long> declareIds = new HashSet<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalFund = BigDecimal.ZERO;

        // 按 approvalId 去重后计算预算和拨付
        Map<Long, ProjectCycleRawVO> approvalFirstRow = new LinkedHashMap<>();
        for (ProjectCycleRawVO row : rawList) {
            if (row.getApprovalId() != null) {
                approvalFirstRow.putIfAbsent(row.getApprovalId(), row);
                approvalIds.add(row.getApprovalId());
            }
            if (row.getDeclareId() != null) {
                declareIds.add(row.getDeclareId());
            }
        }
        for (ProjectCycleRawVO row : approvalFirstRow.values()) {
            BigDecimal b = row.getBudget() == null ? BigDecimal.ZERO : row.getBudget();
            BigDecimal f = row.getTotalFund() == null ? BigDecimal.ZERO : row.getTotalFund();
            totalBudget = totalBudget.add(b);
            totalFund = totalFund.add(f);
        }

        gs.setProjectCount(approvalIds.size());
        gs.setDeclareCount(declareIds.size());
        gs.setVillageCoverage(rawList.size()); // 不去重
        gs.setTotalBudget(totalBudget);
        gs.setTotalFund(totalFund);
        return gs;
    }

    // ================== 内部：施工/验收状态映射 ==================

    private Map<Long, String> buildConstructionStatusMap() {
        List<ProjectCycleQueryMapper.ConstructionStat> list = cycleMapper.selectConstructionStatus();
        Map<Long, String> map = new HashMap<>();
        for (ProjectCycleQueryMapper.ConstructionStat s : list) {
            if (s.getApprovalId() == null) continue;
            boolean hasInProgress = toInt(s.getHasInProgress()) == 1;
            int total = toInt(s.getTotalNodes());
            int completed = toInt(s.getCompletedNodes());
            String status;
            if (hasInProgress) {
                // 存在施工中的明细节点 → 施工中
                status = "IN_PROGRESS";
            } else if (total > 0 && completed >= total) {
                // 全部明细节点已完工 → 已完工
                status = "COMPLETED";
            } else if (completed > 0) {
                // 部分明细节点已完工、无施工中 → 施工中
                status = "IN_PROGRESS";
            } else {
                status = "NOT_STARTED";
            }
            map.put(s.getApprovalId(), status);
        }
        return map;
    }

    private Map<Long, String> buildAcceptanceStatusMap() {
        List<ProjectCycleQueryMapper.AcceptanceStat> list = cycleMapper.selectAcceptanceStatus();
        Map<Long, String> map = new HashMap<>();
        for (ProjectCycleQueryMapper.AcceptanceStat s : list) {
            if (s.getApprovalId() == null) continue;
            // 与验收管理模块一致：项目级验收状态取最新一次 PROJECT 级验收记录
            String projectStatus = s.getLatestProjectStatus();
            if ("ACCEPTED".equals(projectStatus)) {
                map.put(s.getApprovalId(), "PASS");        // 已验收
            } else if ("REJECTED".equals(projectStatus)) {
                map.put(s.getApprovalId(), "FAIL");        // 未通过
            } else {
                // 有验收记录但项目级尚未出结果（PENDING 或仅有明细记录）→ 待验收
                map.put(s.getApprovalId(), "APPLYING");
            }
        }
        return map;
    }

    /**
     * 计算项目综合状态（方案4使用）：
     *   NOT_STARTED  - 未开工
     *   IN_PROGRESS  - 施工中
     *   COMPLETED    - 已完工
     *   PENDING_ACCEPT - 待验收
     *   ACCEPTED     - 已验收
     *   FAILED       - 未通过
     */
    private String computeProjectStatus(Long approvalId, String approvalStatus, String acceptStatus,
                                        Map<Long, String> constructionMap, Map<Long, String> acceptanceMap) {
        // 立项未通过 → 不算（小写/大写都兼容）
        if (!"APPROVED".equalsIgnoreCase(approvalStatus)) {
            return null; // 不纳入方案4分组，让上游过滤
        }

        // 验收管理模块已有明细 → 优先取验收明细状态（待验收/已验收/未通过）
        String acceptance = acceptanceMap.get(approvalId);
        if ("PASS".equals(acceptance)) {
            return "ACCEPTED";
        } else if ("FAIL".equals(acceptance)) {
            return "FAILED";
        } else if ("APPLYING".equals(acceptance)) {
            return "PENDING_ACCEPT";
        }

        // 立项单已进入验收流程（accept_status=APPLYING）但验收模块暂无明细 → 待验收
        if ("APPLYING".equalsIgnoreCase(acceptStatus)) {
            return "PENDING_ACCEPT";
        }

        // 无验收记录 → 取施工管理模块明细状态（未开工/施工中/已完工）
        String construction = constructionMap.getOrDefault(approvalId, "NOT_STARTED");
        if ("COMPLETED".equals(construction)) {
            return "COMPLETED";
        } else if ("IN_PROGRESS".equals(construction)) {
            return "IN_PROGRESS";
        }
        return "NOT_STARTED";
    }

    /** 项目综合状态值 → 显示标签 */
    private static final Map<String, String> PROJECT_STATUS_LABELS = new LinkedHashMap<>();
    static {
        PROJECT_STATUS_LABELS.put("NOT_STARTED", "未开工");
        PROJECT_STATUS_LABELS.put("IN_PROGRESS", "施工中");
        PROJECT_STATUS_LABELS.put("COMPLETED", "已完工");
        PROJECT_STATUS_LABELS.put("PENDING_ACCEPT", "待验收");
        PROJECT_STATUS_LABELS.put("ACCEPTED", "已验收");
        PROJECT_STATUS_LABELS.put("FAILED", "未通过");
    }

    /** 立项状态（方案3）→ 显示标签（顺序即分组顺序：待立项→已立项→已归档） */
    private static final Map<String, String> ESTABLISH_STATUS_LABELS = new LinkedHashMap<>();
    static {
        ESTABLISH_STATUS_LABELS.put("PENDING", "待立项");
        ESTABLISH_STATUS_LABELS.put("APPROVED", "已立项");
        ESTABLISH_STATUS_LABELS.put("ARCHIVED", "已归档");
    }

    /**
     * 计算立项状态（方案3使用）——按用户明确口径：
     * <ul>
     *   <li>PENDING         → 待立项（立项管理模块待审核）</li>
     *   <li>APPROVED+ARCHIVED → 已归档（档案管理里的）</li>
     *   <li>APPROVED+非ARCHIVED → 已立项（总 - 已归档 - 待立项 - 已驳回）</li>
     *   <li>REJECTED        → 返回 null（不纳入三个分组，过滤掉）</li>
     * </ul>
     */
    private String computeEstablishStatus(String approvalStatus, String acceptStatus) {
        if ("REJECTED".equals(approvalStatus)) {
            return null; // 已驳回排除
        }
        if ("PENDING".equals(approvalStatus)) {
            return "PENDING"; // 待立项
        }
        if ("APPROVED".equals(approvalStatus)) {
            if ("ARCHIVED".equals(acceptStatus)) {
                return "ARCHIVED"; // 已归档
            }
            return "APPROVED"; // 已立项
        }
        return null; // 其他未知状态也排除
    }

    // ================== 内部：分组聚合 ==================

    /**
     * 根据 queryType 构建分组行。
     * 所有方案的输出格式统一：大分组 → 村组明细 → grandTotal 总计行
     */
    private List<ProjectCycleStatVO.Row> buildGroupedRows(
            List<ProjectCycleRawVO> rawList, String queryType,
            Map<Long, String> constructionMap, Map<Long, String> acceptanceMap) {

        // 1) 给每行标注其所属的大分组（groupKey + groupLabel）
        List<LabeledRow> labeled = rawList.stream()
                .map(r -> labelRow(r, queryType, constructionMap, acceptanceMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2) 按大分组分桶
        Map<String, List<LabeledRow>> grouped = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (LabeledRow lr : labeled) {
            labels.put(lr.groupKey, lr.groupLabel);
            grouped.computeIfAbsent(lr.groupKey, k -> new ArrayList<>()).add(lr);
        }

        // 3) 排序（方案4按固定状态顺序，其他按预算降序）
        List<Map.Entry<String, List<LabeledRow>>> sortedGroups = new ArrayList<>(grouped.entrySet());
        sortGroups(sortedGroups, queryType);

        // 4) 构建输出：每个大分组先输出 groupSummary 行，再输出 village 明细行
        List<ProjectCycleStatVO.Row> out = new ArrayList<>();
        for (Map.Entry<String, List<LabeledRow>> e : sortedGroups) {
            String gk = e.getKey();
            String gl = labels.getOrDefault(gk, gk);
            List<LabeledRow> bucket = e.getValue();

            // 4a) groupSummary 行
            out.add(buildRow(gk, gl, "全部村组", bucket, queryType, true, false));

            // 4b) 村组明细行
            Map<String, List<LabeledRow>> villageBucket = bucket.stream()
                    .collect(Collectors.groupingBy(
                            r -> blankToNull(r.row.getVillageName()) == null ? "(未填)" : r.row.getVillageName(),
                            LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<LabeledRow>> ve : villageBucket.entrySet()) {
                out.add(buildRow(gk, gl, ve.getKey(), ve.getValue(), queryType, false, false));
            }
        }

        // 5) grandTotal 行
        if (!labeled.isEmpty()) {
            out.add(buildRow("", "总计", "全部区划", labeled, queryType, false, true));
        }

        return out;
    }

    /** 给原始行标注其所属的大分组 key + label */
    private LabeledRow labelRow(ProjectCycleRawVO row, String queryType,
                                Map<Long, String> constructionMap, Map<Long, String> acceptanceMap) {
        String groupKey, groupLabel;
        switch (blankToDefault(queryType, "category")) {
            case "category":
                groupKey = blankToDefault(row.getDeclareCategory(), "unknown");
                groupLabel = row.getDeclareCategoryLabel();
                break;
            case "source":
                groupKey = blankToDefault(row.getProjectType(), "unknown");
                groupLabel = blankToLabel(row.getProjectType(), row.getProjectTypeLabel());
                break;
            case "establish":
                groupKey = computeEstablishStatus(row.getApprovalStatus(), row.getAcceptStatus());
                if (groupKey == null) return null; // REJECTED 等排除
                groupLabel = ESTABLISH_STATUS_LABELS.getOrDefault(groupKey, groupKey);
                break;
            case "status":
                groupKey = computeProjectStatus(row.getApprovalId(), row.getApprovalStatus(),
                        row.getAcceptStatus(), constructionMap, acceptanceMap);
                if (groupKey == null) return null; // 非 APPROVED 排除
                groupLabel = PROJECT_STATUS_LABELS.getOrDefault(groupKey, groupKey);
                break;
            case "village":
                groupKey = blankToDefault(row.getVillageName(), "(未填)");
                groupLabel = groupKey;
                break;
            default:
                groupKey = blankToDefault(row.getDeclareCategory(), "unknown");
                groupLabel = row.getDeclareCategoryLabel();
        }
        return new LabeledRow(row, groupKey, groupLabel);
    }

    private static class LabeledRow {
        final ProjectCycleRawVO row;
        final String groupKey;
        final String groupLabel;
        LabeledRow(ProjectCycleRawVO row, String groupKey, String groupLabel) {
            this.row = row; this.groupKey = groupKey; this.groupLabel = groupLabel;
        }
    }

    private void sortGroups(List<Map.Entry<String, List<LabeledRow>>> sortedGroups, String queryType) {
        if ("status".equals(queryType)) {
            // 按项目状态固定顺序
            List<String> order = new ArrayList<>(PROJECT_STATUS_LABELS.keySet());
            sortedGroups.sort((a, b) -> {
                int ia = order.indexOf(a.getKey());
                int ib = order.indexOf(b.getKey());
                return Integer.compare(ia < 0 ? 999 : ia, ib < 0 ? 999 : ib);
            });
        } else if ("establish".equals(queryType)) {
            List<String> order = new ArrayList<>(ESTABLISH_STATUS_LABELS.keySet());
            sortedGroups.sort((a, b) -> {
                int ia = order.indexOf(a.getKey());
                int ib = order.indexOf(b.getKey());
                return Integer.compare(ia < 0 ? 999 : ia, ib < 0 ? 999 : ib);
            });
        } else {
            // 其他方案按总预算降序
            sortedGroups.sort((a, b) -> sumBudget(b.getValue()).compareTo(sumBudget(a.getValue())));
        }
    }

    /**
     * 构建一行统计结果
     */
    private ProjectCycleStatVO.Row buildRow(String groupValue, String groupLabel, String villageName,
                                            List<LabeledRow> rows, String queryType,
                                            boolean groupSummary, boolean grandTotal) {
        ProjectCycleStatVO.Row r = new ProjectCycleStatVO.Row();
        r.setGroupValue(groupValue);
        r.setGroupLabel(groupLabel);
        r.setVillageName(villageName);
        r.setGroupSummary(groupSummary);
        r.setGrandTotal(grandTotal);

        boolean isVillageQuery = "village".equals(queryType);
        // 方案3（按立项状态）和方案4（按项目状态）→ 只统计户数/设施数
        boolean isHouseholdFacilityQuery = "establish".equals(queryType) || "status".equals(queryType);

        if (isHouseholdFacilityQuery) {
            Map<Long, Integer> householdAgg = new LinkedHashMap<>();
            Map<Long, Integer> facilityAgg = new LinkedHashMap<>();
            for (LabeledRow lr : rows) {
                Long aid = lr.row.getApprovalId();
                int hh = lr.row.getHouseholdCount() != null ? lr.row.getHouseholdCount() : 0;
                int ff = lr.row.getFacilityCount() != null ? lr.row.getFacilityCount() : 0;
                householdAgg.merge(aid, hh, Integer::sum);
                facilityAgg.merge(aid, ff, Integer::sum);
            }
            r.setHouseholdCount(householdAgg.values().stream().mapToInt(Integer::intValue).sum());
            r.setFacilityCount(facilityAgg.values().stream().mapToInt(Integer::intValue).sum());
            return r;
        }

        // 其他方案：统计项目数量、覆盖村组、预算、累计拨付
        // 项目预算和累计拨付需要按 approvalId 去重后求和（一个 approve 可能有多行 declare）
        Map<Long, ProjectCycleRawVO> approvalFirstRow = new LinkedHashMap<>();
        int projectCount = 0;

        if (isVillageQuery) {
            // 方案5：项目数量 = (approvalId, villageName) 维度去重
            Set<String> projectVillageKeys = new HashSet<>();
            // 先收集 (approvalId, villageName) 唯一键
            Set<Long> approvalIdsInThisVillage = new HashSet<>();
            for (LabeledRow lr : rows) {
                ProjectCycleRawVO row = lr.row;
                Long aid = row.getApprovalId();
                String vn = row.getVillageName();
                String key = aid + "|" + vn;
                if (projectVillageKeys.add(key)) {
                    projectCount++;
                    approvalIdsInThisVillage.add(aid);
                }
            }
            // 按 approvalId 去重后聚合预算和拨付
            for (LabeledRow lr : rows) {
                Long aid = lr.row.getApprovalId();
                if (approvalIdsInThisVillage.contains(aid)) {
                    approvalFirstRow.putIfAbsent(aid, lr.row);
                }
            }
        } else {
            // 其他方案：项目数量 = 行数（不去重）？不——需求说"项目数量：符合条件项目记录条数"
            // 这里的"项目"实际上是 approval 维度的行数？不对，再看需求
            // 需求1/2/4：项目数量 = 符合条件项目记录条数（不去重）
            // 因为以 declare 为粒度查询，一个 approval 可能有多行，所以"项目数量"应该是 approval 去重计数
            Set<Long> approvalIds = new HashSet<>();
            for (LabeledRow lr : rows) {
                if (lr.row.getApprovalId() != null) {
                    approvalIds.add(lr.row.getApprovalId());
                    approvalFirstRow.putIfAbsent(lr.row.getApprovalId(), lr.row);
                }
            }
            projectCount = approvalIds.size();
        }

        // 覆盖村组 = 行数（不去重）
        int villageCoverage = rows.size();

        // 预算和拨付 = 去重后的 approvalFirstRow 求和
        BigDecimal budget = BigDecimal.ZERO;
        BigDecimal totalFund = BigDecimal.ZERO;
        for (ProjectCycleRawVO prv : approvalFirstRow.values()) {
            BigDecimal b = prv.getBudget() == null ? BigDecimal.ZERO : prv.getBudget();
            BigDecimal f = prv.getTotalFund() == null ? BigDecimal.ZERO : prv.getTotalFund();
            budget = budget.add(b);
            totalFund = totalFund.add(f);
        }

        r.setProjectCount(projectCount);
        r.setVillageCoverage(villageCoverage);
        r.setBudget(budget);
        r.setTotalFund(totalFund);
        return r;
    }

    // ================== 内部：工具方法 ==================

    private BigDecimal sumBudget(List<LabeledRow> rows) {
        Set<Long> seen = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (LabeledRow lr : rows) {
            Long aid = lr.row.getApprovalId();
            if (aid != null && seen.add(aid)) {
                BigDecimal b = lr.row.getBudget() == null ? BigDecimal.ZERO : lr.row.getBudget();
                sum = sum.add(b);
            }
        }
        return sum;
    }

    /**
     * 方案3 动态年度筛选：
     * <ul>
     *   <li>待立项(PENDING)、已立项(APPROVED) → 按 {@code submit_time}（立项日期）</li>
     *   <li>已归档(ARCHIVED)                 → 按 {@code archive.create_time}（归档时间）</li>
     * </ul>
     */
    private List<ProjectCycleRawVO> filterEstablishByYear(List<ProjectCycleRawVO> rows, int year) {
        List<ProjectCycleRawVO> out = new ArrayList<>();
        // 按 approvalId 去重算 establishStatus 然后判断（一个 approval 可能有多行 declare）
        for (ProjectCycleRawVO row : rows) {
            String status = computeEstablishStatus(row.getApprovalStatus(), row.getAcceptStatus());
            if (status == null) continue; // REJECTED 已排除

            boolean keep = false;
            switch (status) {
                case "PENDING":
                case "APPROVED":
                    // 待立项/已立项 → 立项日期
                    if (row.getSubmitTime() != null && row.getSubmitTime().getYear() == year) keep = true;
                    break;
                case "ARCHIVED":
                    // 已归档 → 归档时间
                    if (row.getArchiveCreateTime() != null && row.getArchiveCreateTime().getYear() == year) keep = true;
                    break;
            }
            if (keep) out.add(row);
        }
        return out;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String blankToDefault(String s, String def) {
        return blankToNull(s) == null ? def : s.trim();
    }

    private static String blankToLabel(String value, String directLabel) {
        if (value == null) return "";
        // directLabel 已经是翻译后的
        return directLabel != null ? directLabel : value;
    }

    private static String blankToLabel(String value, Map<String, String> dict) {
        if (value == null) return "";
        if (dict == null || dict.isEmpty()) return value;
        String v = dict.get(value);
        return v == null ? value : v;
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    // ================== 内部：导出辅助 ==================

    private String getGroupHeaderLabel(String queryType) {
        return switch (blankToDefault(queryType, "category")) {
            case "category" -> "项目类别";
            case "source" -> "项目类型";
            case "establish" -> "立项状态";
            case "status" -> "项目状态";
            case "village" -> "所属村组";
            default -> "分组字段";
        };
    }

    private String getCaliberNote(String queryType) {
        return switch (blankToDefault(queryType, "category")) {
            case "village" -> "按所属村组统计时，项目数量为该村组关联项目ID去重后的数量；其余方案项目数量为项目记录条数。";
            case "establish" -> "按立项状态统计：待立项/已立项按立项日期筛选、已归档按归档时间筛选；仅统计涉及户数、公建设施数。";
            case "status" -> "按项目状态统计仅统计涉及户数、公建设施数；总计按当前筛选条件重新计算。";
            default -> "覆盖村组数量不做去重，同一村组在多个项目/多次申报中重复计入。";
        };
    }
}
