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

    /** 村全量字典缓存（level=5 即村，name 作 join key）——按字典全量 left join，无数据也输出 */
    private volatile List<String> allVillageNamesCache;
    private List<String> getAllVillageNames() {
        if (allVillageNamesCache != null) return allVillageNamesCache;
        synchronized (this) {
            if (allVillageNamesCache != null) return allVillageNamesCache;
            List<SysRegion> levels = regionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRegion>()
                            .eq(SysRegion::getLevel, 5)
                            .orderByAsc(SysRegion::getSort));
            List<String> names = new ArrayList<>();
            for (SysRegion r : levels) if (r.getName() != null && !r.getName().isBlank()) names.add(r.getName());
            allVillageNamesCache = names;
            return names;
        }
    }

    // ================== 对外接口 ==================

    @Override
    public ProjectCycleStatVO stat(ProjectCycleQueryDTO query) {
        // 请求级字典缓存：先构建，finally 里清理，防止 Spring 单例 Service 的字段跨请求残留
        dictCache = buildDictCache();
        try {
            return doStat(query);
        } finally {
            dictCache = null;
        }
    }

    private ProjectCycleStatVO doStat(ProjectCycleQueryDTO query) {
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

        // 方案1（按项目类别）的分组字典：approvalId → 综合 declareCategory（'1'到户 / '2'公建 / '3'到户+公建混合）
        // 一个 approval 下如果同时有 declare_category=1 和 2 的 declare，则归入 '3' 分组（xiang_mu_lei_bie 字典）
        Map<Long, String> approvalCategoryMap = "category".equals(queryType)
                ? buildApprovalMixedCategoryMap(rawList) : Collections.emptyMap();

        // 方案2（按项目类型）的分组字典：approvalId → projectType（approval 级字段，同 approval 下同值）
        Map<Long, String> approvalTypeMap = "source".equals(queryType)
                ? buildApprovalProjectTypeMap(rawList) : Collections.emptyMap();

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
        List<ProjectCycleStatVO.Row> rows = buildGroupedRows(rawList, queryType,
                constructionStatusMap, acceptanceStatusMap, approvalCategoryMap, approvalTypeMap);
        vo.setRows(rows);
        vo.setTotal((long) rows.size());

        return vo;
    }

    @Override
    public List<ProjectCycleDetailVO> detail(ProjectCycleQueryDTO query) {
        dictCache = buildDictCache();
        try {
            return doDetail(query);
        } finally {
            dictCache = null;
        }
    }

    private List<ProjectCycleDetailVO> doDetail(ProjectCycleQueryDTO query) {
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

        // 与 stat() 保持完全一致的分组口径
        Map<Long, String> approvalCategoryMap = "category".equals(queryType)
                ? buildApprovalMixedCategoryMap(rawList) : Collections.emptyMap();
        Map<Long, String> approvalTypeMap = "source".equals(queryType)
                ? buildApprovalProjectTypeMap(rawList) : Collections.emptyMap();

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
            LabeledRow lr = labelRow(raw, queryType, constructionMap, acceptanceMap,
                    approvalCategoryMap, approvalTypeMap);
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

        // 立项状态 label：从字典 li_xiang_zhuang_tai 动态查
        String est = computeEstablishStatus(raw.getApprovalStatus(), raw.getAcceptStatus());
        vo.setEstablishStatusLabel(est == null ? "已驳回" : dictLabel("li_xiang_zhuang_tai", est));

        // 项目状态 label：从字典 xiang_mu_zhuang_tai 动态查
        String ps = computeProjectStatus(raw.getApprovalId(), raw.getApprovalStatus(),
                raw.getAcceptStatus(), constructionMap, acceptanceMap);
        vo.setProjectStatusLabel(ps == null ? "-" : dictLabel("xiang_mu_zhuang_tai", ps));

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
            "xiang_mu_lei_bie",    // 项目类别（方案1 category 对齐）
            "xiang_mu_lei_xing",   // 项目类型（方案2 source 对齐）
            "xiang_mu_lai_yuan",   // 项目来源
            "li_xiang_zhuang_tai", // 立项状态（方案3 establish 对齐）
            "yan_shou_jie_guo",    // 验收结果
            "xiang_mu_zhuang_tai"  // 项目阶段（方案4 status 对齐，综合施工+验收）
    };

    /**
     * 全服务共享的字典缓存（dict_type → {dict_value → dict_label}），
     * 由 stat() / detail() 入口统一构建，labelRow 等下游方法直接使用，
     * 保证同一查询请求内查字典只查一次数据库。
     */
    private Map<String, Map<String, String>> dictCache;

    /** 翻译原始 VO 行上的字段字典（declareCategoryLabel / projectTypeLabel 等） */
    private void applyDictLabels(List<ProjectCycleRawVO> rows) {
        if (rows == null || rows.isEmpty()) return;
        if (dictCache == null) dictCache = buildDictCache();

        Map<String, String> typeDict = dictCache.getOrDefault("xiang_mu_lei_xing", Collections.emptyMap());

        for (ProjectCycleRawVO row : rows) {
            // declareCategory 原始值（shen_bao_lei_bie：1到户 / 2公建）直接翻译，
            // 方案1 统计时会在 approval 粒度聚合后再查 xiang_mu_lei_bie 字典
            row.setDeclareCategoryLabel(blankToLabel(row.getDeclareCategory(),
                    dictCache.getOrDefault("shen_bao_lei_bie", Collections.emptyMap())));

            // projectType：xiang_mu_lei_xing
            row.setProjectTypeLabel(blankToLabel(row.getProjectType(), typeDict));
        }
    }

    /** 构建全服务字典缓存：dict_type → {dict_value → dict_label}，同时记录 sort 用于动态排序 */
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

    /**
     * 获取指定字典类型的全部条目（含 sort 用于排序），
     * 用于 buildGroupedRows 里动态按字典 sort 对分组排序。
     */
    private List<SysDict> listDictsByType(String dictType) {
        return dictService.listByType(dictType);
    }

    /** 给定字典类型和 value，返回 label；查不到时兜底返回 value 本身 */
    private String dictLabel(String dictType, String value) {
        if (value == null) return "";
        Map<String, String> map = dictCache != null
                ? dictCache.get(dictType)
                : null;
        if (map != null && map.containsKey(value)) return map.get(value);
        return value;
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
     * 计算项目综合阶段（方案4使用，输出值对齐字典 xiang_mu_zhuang_tai 的 dict_value）：
     * <pre>
     *   1 未开工    → 施工模块无记录或全部 NOT_STARTED
     *   2 施工中    → 存在 IN_PROGRESS（含部分完工+无施工中的情况）
     *   3 已完工    → 全部施工明细 COMPLETED
     *   4 待验收    → 已发起验收(accept_status=APPLYING) 但未全部验收，或验收模块有 APPLYING 明细
     *   5 已验收    → 全部验收明细 ACCEPTED（整改后验收通过也视为已验收）
     *   6 未通过    → 验收 REJECTED
     * </pre>
     * Service 只输出 value（'1'~'6'），label 统一从字典 xiang_mu_zhuang_tai 查——
     * 用户在字典里加新条目会自动出现在统计方案里。
     */
    private String computeProjectStatus(Long approvalId, String approvalStatus, String acceptStatus,
                                        Map<Long, String> constructionMap, Map<Long, String> acceptanceMap) {
        // 立项未通过 → 不算（小写/大写都兼容）
        if (!"APPROVED".equalsIgnoreCase(approvalStatus)) {
            return null; // 不纳入方案4分组，让上游过滤
        }

        // 验收管理模块已有明细 → 优先取验收结果
        String acceptance = acceptanceMap.get(approvalId);
        if ("PASS".equals(acceptance)) {
            return "5"; // 已验收（含整改后通过）
        } else if ("FAIL".equals(acceptance)) {
            return "6"; // 未通过
        } else if ("APPLYING".equals(acceptance)) {
            return "4"; // 待验收
        }

        // 立项单已进入验收流程（accept_status=APPLYING）但验收模块暂无明细 → 待验收
        if ("APPLYING".equalsIgnoreCase(acceptStatus)) {
            return "4";
        }

        // 无验收记录 → 取施工管理模块明细状态
        String construction = constructionMap.getOrDefault(approvalId, "NOT_STARTED");
        if ("COMPLETED".equals(construction)) {
            return "3"; // 已完工
        } else if ("IN_PROGRESS".equals(construction)) {
            return "2"; // 施工中
        }
        return "1"; // 未开工
    }

    /**
     * 计算立项状态（方案3使用，输出值对齐字典 li_xiang_zhuang_tai 的 dict_value）：
     * <pre>
     *   pending   → 待立项（立项管理待审核）
     *   approved  → 已立项（已通过但未归档也未驳回）
     *   archived  → 已归档（accept_status=ARCHIVED）
     *   rejected  → 已驳回（审批被驳回）
     * </pre>
     * 全部立项状态都纳入统计——4 个分组覆盖所有 approved 状态值，
     * 总计 = 待立项 + 已立项 + 已归档 + 已驳回 = 全量表数。
     */
    private String computeEstablishStatus(String approvalStatus, String acceptStatus) {
        if ("REJECTED".equals(approvalStatus)) {
            return "rejected";
        }
        if ("PENDING".equals(approvalStatus)) {
            return "pending";
        }
        if ("APPROVED".equals(approvalStatus)) {
            if ("ARCHIVED".equals(acceptStatus)) {
                return "archived";
            }
            return "approved";
        }
        return null; // 其他未知状态过滤（目前字典只定义了 4 种）
    }

    // ================== 内部：分组聚合 ==================

    /**
     * 根据 queryType 构建分组行。
     * 所有方案的输出格式统一：大分组 → 村组明细 → grandTotal 总计行
     *
     * @param approvalCategoryMap approvalId → 综合 declareCategory（方案1 category 使用，含 MIXED）
     * @param approvalTypeMap     approvalId → 综合 projectType（方案2 source 使用）
     */
    private List<ProjectCycleStatVO.Row> buildGroupedRows(
            List<ProjectCycleRawVO> rawList, String queryType,
            Map<Long, String> constructionMap, Map<Long, String> acceptanceMap,
            Map<Long, String> approvalCategoryMap, Map<Long, String> approvalTypeMap) {

        boolean isVillageQuery = "village".equals(queryType);

        // ==== 村字典全量（level=5） ====
        List<String> allVillageNames = getAllVillageNames();

        // 1) 给每行标注其所属的大分组（groupKey + groupLabel）
        List<LabeledRow> labeled = rawList.stream()
                .map(r -> labelRow(r, queryType, constructionMap, acceptanceMap,
                        approvalCategoryMap, approvalTypeMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // ==== 先把 labeled 按 villageName 建索引（给外层 village query 和二级村组用） ====
        Map<String, List<LabeledRow>> labeledByVillage = new HashMap<>();
        for (LabeledRow lr : labeled) {
            String vn = blankToNull(lr.row.getVillageName()) == null ? "(未填)" : lr.row.getVillageName();
            labeledByVillage.computeIfAbsent(vn, k -> new ArrayList<>()).add(lr);
        }

        // 2) 按大分组分桶
        Map<String, List<LabeledRow>> dataBucket = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (LabeledRow lr : labeled) {
            labels.put(lr.groupKey, lr.groupLabel);
            dataBucket.computeIfAbsent(lr.groupKey, k -> new ArrayList<>()).add(lr);
        }

        // 3) 构建全量大分组：先按字典全量建基线桶（空也出一行 count=0），
        //    再合并 dataBucket；village 方案 5 本身就是"村组"维度，不走字典基线。
        List<Map.Entry<String, List<LabeledRow>>> sortedGroups;
        if (isVillageQuery) {
            sortedGroups = new ArrayList<>();
            for (String vName : allVillageNames) {
                List<LabeledRow> vRows = labeledByVillage.getOrDefault(vName, Collections.emptyList());
                sortedGroups.add(new java.util.AbstractMap.SimpleEntry<>(vName, vRows));
                labels.put(vName, vName);
            }
        } else {
            String dictType = queryTypeToDictType(queryType);
            Map<String, List<LabeledRow>> allGroups = new LinkedHashMap<>();
            if (dictType != null) {
                List<SysDict> dicts = listDictsByType(dictType);
                // 按 sort 顺序铺全量基线桶
                if (dicts != null) {
                    // 按 sort 排序（已经 sortGroups 里有同样逻辑，这里也保证一次）
                    dicts.sort(Comparator.comparingInt(d -> d.getSort() == null ? 999 : d.getSort()));
                    for (SysDict d : dicts) {
                        if (d.getDictValue() != null) {
                            String v = d.getDictValue();
                            allGroups.put(v, dataBucket.getOrDefault(v, Collections.emptyList()));
                            labels.put(v, d.getDictLabel());
                        }
                    }
                }
                // dataBucket 里字典没覆盖到的"脏数据"也要保留（放到末尾）
                for (Map.Entry<String, List<LabeledRow>> e : dataBucket.entrySet()) {
                    allGroups.computeIfAbsent(e.getKey(), k -> {
                        labels.put(e.getKey(), e.getKey());
                        return e.getValue();
                    });
                }
            } else {
                // 理论走不到（非 village 查询都有字典），兜底用 dataBucket
                allGroups.putAll(dataBucket);
            }
            sortedGroups = new ArrayList<>(allGroups.entrySet());
            // sortGroups 不再需要（基线桶已按字典 sort 铺好），保留调用兼容
            sortGroups(sortedGroups, queryType);
        }

        // 4) 构建输出：每个大分组先输出 groupSummary 行，再输出 village 明细行
        List<ProjectCycleStatVO.Row> out = new ArrayList<>();
        for (Map.Entry<String, List<LabeledRow>> e : sortedGroups) {
            String gk = e.getKey();
            String gl = labels.getOrDefault(gk, gk);
            List<LabeledRow> bucket = e.getValue();

            // 4a) groupSummary 行
            out.add(buildRow(gk, gl, "全部村组", bucket, queryType, true, false));

            // 4b) 村组明细行：按字典全量输出（无数据也出一行 0 值）
            // village query 本身最细粒度就是村组，不再嵌套二级
            if (!isVillageQuery) {
                Map<String, List<LabeledRow>> villageBucket = new HashMap<>();
                for (LabeledRow lr : bucket) {
                    String vn = blankToNull(lr.row.getVillageName()) == null ? "(未填)" : lr.row.getVillageName();
                    villageBucket.computeIfAbsent(vn, k -> new ArrayList<>()).add(lr);
                }
                for (String vName : allVillageNames) {
                    List<LabeledRow> vRows = villageBucket.getOrDefault(vName, Collections.emptyList());
                    out.add(buildRow(gk, gl, vName, vRows, queryType, false, false));
                }
            }
        }

        // 5) grandTotal 行
        if (!labeled.isEmpty()) {
            out.add(buildRow("", "总计", "全部区划", labeled, queryType, false, true));
        }

        return out;
    }

    /**
     * 给原始行标注其所属的大分组 key + label。
     *
     * <p>所有方案的 groupKey 对齐对应字典的 dict_value，groupLabel 从字典查——
     * 字典加条目 → 左侧面板和主表格自动多出分组，无需改代码。</p>
     *
     * <table>
     *   <tr><th>方案</th><th>字典类型</th><th>value 来源</th></tr>
     *   <tr><td>category 按项目类别</td><td>xiang_mu_lei_bie</td><td>approval 粒度聚合 declareCategory（'1'/'2'/'3'混合）</td></tr>
     *   <tr><td>source   按项目类型</td><td>xiang_mu_lei_xing</td><td>approval.projectType（approval 级字段）</td></tr>
     *   <tr><td>establish 按立项状态</td><td>li_xiang_zhuang_tai</td><td>computeEstablishStatus 计算</td></tr>
     *   <tr><td>status   按项目状态</td><td>xiang_mu_zhuang_tai</td><td>computeProjectStatus 计算</td></tr>
     *   <tr><td>village  按所属村组</td><td>无字典</td><td>数据里的 villageName 去重</td></tr>
     * </table>
     */
    private LabeledRow labelRow(ProjectCycleRawVO row, String queryType,
                                Map<Long, String> constructionMap, Map<Long, String> acceptanceMap,
                                Map<Long, String> approvalCategoryMap, Map<Long, String> approvalTypeMap) {
        String groupKey, groupLabel, dictType;
        switch (blankToDefault(queryType, "category")) {
            case "category": {
                dictType = "xiang_mu_lei_bie";
                Long aid = row.getApprovalId();
                groupKey = approvalCategoryMap.getOrDefault(aid, blankToDefault(row.getDeclareCategory(), "unknown"));
                groupLabel = dictLabel(dictType, groupKey);
                break;
            }
            case "source": {
                dictType = "xiang_mu_lei_xing";
                Long aid = row.getApprovalId();
                groupKey = approvalTypeMap.getOrDefault(aid, blankToDefault(row.getProjectType(), "unknown"));
                groupLabel = dictLabel(dictType, groupKey);
                break;
            }
            case "establish": {
                dictType = "li_xiang_zhuang_tai";
                groupKey = computeEstablishStatus(row.getApprovalStatus(), row.getAcceptStatus());
                if (groupKey == null) return null;
                groupLabel = dictLabel(dictType, groupKey);
                break;
            }
            case "status": {
                dictType = "xiang_mu_zhuang_tai";
                groupKey = computeProjectStatus(row.getApprovalId(), row.getApprovalStatus(),
                        row.getAcceptStatus(), constructionMap, acceptanceMap);
                if (groupKey == null) return null;
                groupLabel = dictLabel(dictType, groupKey);
                break;
            }
            case "village":
                // 村组本身就是动态数据，不需要字典翻译
                groupKey = blankToDefault(row.getVillageName(), "(未填)");
                groupLabel = groupKey;
                break;
            default: {
                dictType = "xiang_mu_lei_bie";
                Long aid = row.getApprovalId();
                groupKey = approvalCategoryMap.getOrDefault(aid, blankToDefault(row.getDeclareCategory(), "unknown"));
                groupLabel = dictLabel(dictType, groupKey);
            }
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

    /**
     * 把方案 queryType 映射到对应的 dict_type，用于构建全量基线桶（空也出一行）。
     * 返回 null 表示该方案不依赖字典（目前只有 village 方案 5：按村组统计本身就是动态数据）。
     */
    private static String queryTypeToDictType(String queryType) {
        switch (blankToDefault(queryType, "category")) {
            case "category":  return "xiang_mu_lei_bie";
            case "source":    return "xiang_mu_lei_xing";
            case "establish": return "li_xiang_zhuang_tai";
            case "status":    return "xiang_mu_zhuang_tai";
            default:          return null;
        }
    }

    private void sortGroups(List<Map.Entry<String, List<LabeledRow>>> sortedGroups, String queryType) {
        // 需要按字典 sort 顺序排的方案：方案3/4
        String dictType = null;
        switch (queryType) {
            case "status":    dictType = "xiang_mu_zhuang_tai"; break;
            case "establish": dictType = "li_xiang_zhuang_tai"; break;
            case "category":  dictType = "xiang_mu_lei_bie"; break;
            case "source":    dictType = "xiang_mu_lei_xing"; break;
            default:          dictType = null;
        }
        if (dictType != null) {
            // 构建字典 value → sort 的映射
            List<SysDict> dicts = listDictsByType(dictType);
            Map<String, Integer> valueSort = new HashMap<>();
            if (dicts != null) {
                for (SysDict d : dicts) {
                    if (d.getDictValue() != null) {
                        valueSort.put(d.getDictValue(), d.getSort() == null ? 999 : d.getSort());
                    }
                }
            }
            Map<String, Integer> finalMap = valueSort;
            sortedGroups.sort((a, b) -> {
                Integer ia = finalMap.getOrDefault(a.getKey(), 9999);
                Integer ib = finalMap.getOrDefault(b.getKey(), 9999);
                return Integer.compare(ia, ib);
            });
        } else {
            // 村组等动态方案：按总预算降序
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

        // ===== 方案3/4 先算 householdCount + facilityCount（按 approvalId 聚合去重）=====
        // 方案3（立项状态）/ 方案4（项目状态） 的明细量纲是户数+设施数，
        // 同时也需要 projectCount（approval 去重数）、budget（项目预算）这些通用字段
        boolean needHHFF = "establish".equals(queryType) || "status".equals(queryType);

        Map<Long, Integer> householdAgg = new LinkedHashMap<>();
        Map<Long, Integer> facilityAgg = new LinkedHashMap<>();
        if (needHHFF) {
            for (LabeledRow lr : rows) {
                Long aid = lr.row.getApprovalId();
                if (aid == null) continue;
                int hh = lr.row.getHouseholdCount() != null ? lr.row.getHouseholdCount() : 0;
                int ff = lr.row.getFacilityCount() != null ? lr.row.getFacilityCount() : 0;
                householdAgg.merge(aid, hh, Integer::sum);
                facilityAgg.merge(aid, ff, Integer::sum);
            }
            r.setHouseholdCount(householdAgg.values().stream().mapToInt(Integer::intValue).sum());
            r.setFacilityCount(facilityAgg.values().stream().mapToInt(Integer::intValue).sum());
        }

        // ===== 所有方案通用：统计 projectCount / villageCoverage / budget / totalFund =====
        boolean isVillageQuery = "village".equals(queryType);
        Map<Long, ProjectCycleRawVO> approvalFirstRow = new LinkedHashMap<>();
        int projectCount = 0;

        if (isVillageQuery) {
            Set<String> projectVillageKeys = new HashSet<>();
            Set<Long> approvalIdsInThisVillage = new HashSet<>();
            for (LabeledRow lr : rows) {
                ProjectCycleRawVO row = lr.row;
                Long aid = row.getApprovalId();
                String vn = row.getVillageName();
                if (aid == null) continue;
                String key = aid + "|" + (vn == null ? "" : vn);
                if (projectVillageKeys.add(key)) {
                    projectCount++;
                    approvalIdsInThisVillage.add(aid);
                }
            }
            for (LabeledRow lr : rows) {
                Long aid = lr.row.getApprovalId();
                if (aid != null && approvalIdsInThisVillage.contains(aid)) {
                    approvalFirstRow.putIfAbsent(aid, lr.row);
                }
            }
        } else {
            // 按 approvalId 去重计数
            Set<Long> approvalIds = new HashSet<>();
            for (LabeledRow lr : rows) {
                Long aid = lr.row.getApprovalId();
                if (aid != null) {
                    approvalIds.add(aid);
                    approvalFirstRow.putIfAbsent(aid, lr.row);
                }
            }
            projectCount = approvalIds.size();
        }

        int villageCoverage = rows.size();
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

    // ================== 内部：approval 粒度聚合 helper ==================

    /**
     * 按 approval 粒度聚合 declareCategory。
     * 一个 approval 下的所有 declare，收集其 declare_category 的唯一值，
     * 对齐字典 xiang_mu_lei_bie 的 dict_value：
     * <ul>
     *   <li>只有 '1' → 输出 '1'（到户项目）</li>
     *   <li>只有 '2' → 输出 '2'（公建项目）</li>
     *   <li>同时有 '1' 和 '2' → 输出 '3'（到户+公建混合项目，字典里已配）</li>
     * </ul>
     * 查 label 时由 labelRow 统一走字典 xiang_mu_lei_bie。
     */
    private Map<Long, String> buildApprovalMixedCategoryMap(List<ProjectCycleRawVO> rawList) {
        Map<Long, Set<String>> approvalToCats = new LinkedHashMap<>();
        for (ProjectCycleRawVO row : rawList) {
            Long aid = row.getApprovalId();
            String cat = row.getDeclareCategory();
            if (aid == null || blankToNull(cat) == null) continue;
            approvalToCats.computeIfAbsent(aid, k -> new LinkedHashSet<>()).add(cat);
        }
        Map<Long, String> out = new LinkedHashMap<>();
        for (Map.Entry<Long, Set<String>> e : approvalToCats.entrySet()) {
            Set<String> cats = e.getValue();
            boolean has1 = cats.contains("1");
            boolean has2 = cats.contains("2");
            if (has1 && has2) {
                out.put(e.getKey(), "3"); // 到户+公建混合项目
            } else if (has1) {
                out.put(e.getKey(), "1");
            } else if (has2) {
                out.put(e.getKey(), "2");
            } else if (!cats.isEmpty()) {
                // 其他未知值（字典里可能配了新项），原样输出让字典翻译兜底
                out.put(e.getKey(), cats.iterator().next());
            }
        }
        return out;
    }

    /**
     * 按 approval 粒度聚合 projectType。
     * 一个 approval 下的所有 declare，projectType 理论上是 approval 级字段，
     * 所以同 approval 的所有 declare.projectType 值相同；如果遇到不同值（数据异常），取第一条非空值。
     */
    private Map<Long, String> buildApprovalProjectTypeMap(List<ProjectCycleRawVO> rawList) {
        Map<Long, String> out = new LinkedHashMap<>();
        for (ProjectCycleRawVO row : rawList) {
            Long aid = row.getApprovalId();
            if (aid == null) continue;
            if (!out.containsKey(aid) && blankToNull(row.getProjectType()) != null) {
                out.put(aid, row.getProjectType());
            }
        }
        return out;
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
