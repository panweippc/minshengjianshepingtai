package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.Constants;
import com.msjs.project.common.DataScopeUtils;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.UserContext;
import com.msjs.project.entity.BaseFacility;
import com.msjs.project.entity.Declaration;
import com.msjs.project.entity.DeclarationHousehold;
import com.msjs.project.entity.DeclarationPublicFacility;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.SysDict;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.BaseFacilityMapper;
import com.msjs.project.mapper.DeclarationHouseholdMapper;
import com.msjs.project.mapper.DeclarationMapper;
import com.msjs.project.mapper.DeclarationPublicFacilityMapper;
import com.msjs.project.mapper.HouseholdMapper;
import com.msjs.project.mapper.MaterialItemMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ScreenStatMapper;
import com.msjs.project.mapper.SysDictMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.DashboardService;
import com.msjs.project.vo.DashboardOverviewVO;
import com.msjs.project.vo.ProjectProgressVO;
import com.msjs.project.vo.ScreenBudgetRankVO;
import com.msjs.project.vo.ScreenChartItemVO;
import com.msjs.project.vo.ScreenEventVO;
import com.msjs.project.vo.ScreenFundTrendVO;
import com.msjs.project.vo.ScreenKpiVO;
import com.msjs.project.vo.ScreenMaterialVO;
import com.msjs.project.vo.ScreenOverviewVO;
import com.msjs.project.vo.ScreenProjectStatusVO;
import com.msjs.project.vo.ScreenTypeDetailSeriesVO;
import com.msjs.project.vo.ScreenTypeDetailStatsVO;
import com.msjs.project.vo.ScreenTypeSeriesVO;
import com.msjs.project.vo.ScreenTypeStatsVO;
import com.msjs.project.vo.ScreenVillageTypeDetailVO;
import com.msjs.project.vo.ScreenVillageTypeVO;
import com.msjs.project.vo.ScreenVillageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 首页概览统计服务实现
 * 统计口径：行政村按区划层级=村统计；农户/公共基础设施按未删除记录统计；
 * 在库物资总数按全部物品库存数量求和；项目进度按每个立项项目当前所处生命周期阶段统计
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /** 资金趋势统计月数（含当月） */
    private static final int TREND_MONTHS = 7;
    /** 百分比保留小数位 */
    private static final int RATE_SCALE = 1;
    /** 百分比换算基数 */
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** 项目类型字典编码 */
    private static final String DICT_PROJECT_TYPE = "xiang_mu_lei_xing";
    /** 资金来源字典编码（事件摘要用） */
    private static final String DICT_FUND_SOURCE = "zi_jin_lai_yuan";
    /** 一级资金类型字典编码（事件摘要用） */
    private static final String DICT_FUND_TYPE = "zi_jin_lei_xing";
    /** 二级/三级下拨类型字典编码（事件摘要用） */
    private static final String DICT_ALLOC_TYPE = "xia_bo_lei_xing";
    /** 项目类型柱状图展示的村庄数量 */
    private static final int TYPE_VILLAGE_LIMIT = 6;
    /** 各村项目类型明细统计展示的村庄数量 */
    private static final int TYPE_DETAIL_VILLAGE_LIMIT = 8;
    /** 实时事件后端返回条数上限（前端再做去重合并，保留流式观感） */
    private static final int EVENT_LIMIT = 10;
    /** 实时事件查询条数：先多取再按摘要去重，避免重复事件占满展示位 */
    private static final int EVENT_FETCH_LIMIT = 60;

    private final SysRegionMapper regionMapper;
    private final HouseholdMapper householdMapper;
    private final BaseFacilityMapper facilityMapper;
    private final MaterialItemMapper materialItemMapper;
    private final ProjectApprovalMapper projectApprovalMapper;
    private final DeclarationMapper declarationMapper;
    private final DeclarationHouseholdMapper declarationHouseholdMapper;
    private final DeclarationPublicFacilityMapper declarationPublicFacilityMapper;
    private final ScreenStatMapper screenStatMapper;
    private final SysDictMapper sysDictMapper;

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        // 数据权限：非全部范围用户（镇/村）按本人区划前缀过滤；未绑定区划时使用不可能匹配编码，保证返回空数据
        LoginUser loginUser = UserContext.get();
        String regionPrefix = null;
        if (loginUser != null && loginUser.getDataScope() != null
                && loginUser.getDataScope() != Constants.DATA_SCOPE_ALL) {
            String regionCode = loginUser.getRegionCode();
            regionPrefix = (regionCode == null || regionCode.isBlank())
                    ? Constants.DATA_SCOPE_NO_MATCH_CODE
                    : regionCode.trim();
        }
        // 行政村总数：区划层级为村（5）的节点数量
        LambdaQueryWrapper<SysRegion> villageWrapper = new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getLevel, Constants.REGION_LEVEL_VILLAGE);
        DataScopeUtils.applyRegionScope(villageWrapper, SysRegion::getCode);
        vo.setVillageCount(regionMapper.selectCount(villageWrapper));
        // 农户总数：户基础信息未删除记录数
        LambdaQueryWrapper<Household> householdWrapper = new LambdaQueryWrapper<>();
        DataScopeUtils.applyRegionScope(householdWrapper, Household::getRegionCode);
        vo.setHouseholdCount(householdMapper.selectCount(householdWrapper));
        // 公共基础设施总数：设施未删除记录数
        LambdaQueryWrapper<BaseFacility> facilityWrapper = new LambdaQueryWrapper<>();
        DataScopeUtils.applyRegionScope(facilityWrapper, BaseFacility::getRegionCode);
        vo.setFacilityCount(facilityMapper.selectCount(facilityWrapper));
        // 在库物资总数：全部物品库存数量之和
        vo.setMaterialStockTotal(materialItemMapper.sumStockQty());
        // 项目进度概览：一个项目一条记录，可同时处于多个阶段；按申报单所属村组过滤数据范围
        List<ProjectProgressVO> progressList = projectApprovalMapper.selectProjectProgress(regionPrefix);
        for (ProjectProgressVO item : progressList) {
            List<Integer> stages = new ArrayList<>(4);
            // 已归档为生命周期终点，优先判定：忽略可能残留的施工中明细状态，仅展示已归档
            if (Constants.APPROVAL_ACCEPT_ARCHIVED.equalsIgnoreCase(item.getAcceptStatus())) {
                stages.add(5);
            } else {
                // 按生命周期顺序收集当前所处阶段，允许同时命中多个
                if (Constants.APPROVAL_STATUS_PENDING.equalsIgnoreCase(item.getApprovalStatus())) {
                    stages.add(1);
                }
                // 已批准项目即已进入资金、施工、验收管理（三个页面均按立项状态展示项目），
                // 因此“资金”“施工中”“待验收”同时作为当前阶段
                if (Constants.APPROVAL_STATUS_APPROVED.equalsIgnoreCase(item.getApprovalStatus())) {
                    stages.add(2);
                    stages.add(3);
                    stages.add(4);
                }
                // 兜底：状态缺失时按已批准处理
                if (stages.isEmpty()) {
                    stages.add(2);
                }
            }
            item.setCurrentStages(stages);
        }
        vo.setProjectProgress(progressList);
        return vo;
    }

    /**
     * 数据大屏聚合统计
     * 所有指标均来自业务库实时聚合，不使用前端模拟数据；
     * 资金口径与资金管理一致：仅统计一级资金登记中"拨付"类型，避免二三级下拨重复累计
     */
    @Override
    public ScreenOverviewVO screen() {
        ScreenOverviewVO vo = new ScreenOverviewVO();

        // ========== 项目状态分类与在管项目数 ==========
        List<ScreenProjectStatusVO> statusList = screenStatMapper.selectProjectStatuses();
        Map<String, Long> statusCounter = new LinkedHashMap<>();
        statusCounter.put("待审核", 0L);
        statusCounter.put("待开工", 0L);
        statusCounter.put("施工中", 0L);
        statusCounter.put("待验收", 0L);
        statusCounter.put("已归档", 0L);
        statusCounter.put("已驳回", 0L);
        long buildingCount = 0L;
        for (ScreenProjectStatusVO item : statusList) {
            String label = resolveStatusLabel(item);
            statusCounter.merge(label, 1L, Long::sum);
            // 在管项目：已通过立项且未归档的项目
            if (Constants.APPROVAL_STATUS_APPROVED.equalsIgnoreCase(item.getApprovalStatus())
                    && !Constants.APPROVAL_ACCEPT_ARCHIVED.equalsIgnoreCase(item.getAcceptStatus())) {
                buildingCount++;
            }
        }
        List<ScreenChartItemVO> statusStats = new ArrayList<>(statusCounter.size());
        statusCounter.forEach((name, value) -> statusStats.add(new ScreenChartItemVO(name, value)));
        vo.setStatusStats(statusStats);

        // ========== 核心指标 ==========
        ScreenKpiVO kpi = new ScreenKpiVO();
        kpi.setTotalBudget(nullToZero(screenStatMapper.sumBudget()));
        kpi.setPaidAmount(nullToZero(screenStatMapper.sumPaid()));
        // 拨付完成率 = 累计拨付金额 ÷ 批复金额（资金类型口径），批复为0时返回0
        kpi.setPayRate(percent(kpi.getPaidAmount(), screenStatMapper.sumApproved()));
        kpi.setProjectCount((long) statusList.size());
        kpi.setBuildingCount(buildingCount);
        kpi.setConstructionCount(nullToZero(screenStatMapper.countConstructionProjects()));
        kpi.setVillageCount(regionMapper.selectCount(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getLevel, Constants.REGION_LEVEL_VILLAGE)));
        kpi.setHouseholdCount(householdMapper.selectCount(null));
        kpi.setFacilityCount(facilityMapper.selectCount(null));
        kpi.setMaterialTotal(nullToZero(materialItemMapper.sumStockQty()));
        kpi.setMaterialWarnCount(nullToZero(screenStatMapper.countMaterialWarn()));
        kpi.setDeclarationCount(declarationMapper.selectCount(null));
        vo.setKpi(kpi);

        // ========== 资金拨付趋势（近7个自然月，缺失月份补0） ==========
        vo.setFundTrend(buildFundTrend());

        // ========== 村域态势与各村项目类型分布 ==========
        List<ScreenVillageVO> villageStats = screenStatMapper.selectVillageStats();
        vo.setVillageStats(villageStats);
        vo.setTypeStats(buildTypeStats());

        // ========== 各村项目类型明细统计（仅立项已通过：横轴村庄，系列为项目类型，数值为明细数） ==========
        vo.setVillageTypeDetailStats(buildTypeDetailStats());

        // ========== 预算投入排名（按首位预算换算占比） ==========
        List<ScreenBudgetRankVO> budgetRank = screenStatMapper.selectBudgetRank();
        if (!budgetRank.isEmpty()) {
            BigDecimal max = nullToZero(budgetRank.get(0).getBudget());
            budgetRank.forEach(item -> item.setRatio(percent(nullToZero(item.getBudget()), max)));
        }
        vo.setBudgetRank(budgetRank);

        // ========== 物资状态与实时事件 ==========
        List<ScreenMaterialVO> materialStats = screenStatMapper.selectMaterialStats();
        for (ScreenMaterialVO item : materialStats) {
            int stock = item.getStockQty() == null ? 0 : item.getStockQty();
            int warning = item.getWarningQty() == null ? 0 : item.getWarningQty();
            item.setWarning(warning > 0 && stock <= warning);
        }
        vo.setMaterialStats(materialStats);
        List<ScreenEventVO> rawEvents = screenStatMapper.selectRecentEvents(EVENT_FETCH_LIMIT);
        // 构建 区划代码→村名 映射，用于从操作参数中还原事件发生地点
        Map<String, String> villageByCode = regionMapper.selectList(
                        new LambdaQueryWrapper<SysRegion>()
                                .eq(SysRegion::getLevel, Constants.REGION_LEVEL_VILLAGE))
                .stream()
                .collect(Collectors.toMap(SysRegion::getCode, SysRegion::getName, (a, b) -> a));
        // 资金来源/资金类型字典，用于资金事件摘要展示中文标签
        Map<String, Map<String, String>> fundDict = loadDictLabels(
                DICT_FUND_SOURCE, DICT_FUND_TYPE, DICT_ALLOC_TYPE);
        for (ScreenEventVO event : rawEvents) {
            event.setSummary(buildEventSummary(event, villageByCode, fundDict));
            event.setCategory(resolveEventCategory(event.getModule(), event.getUrl()));
        }
        // 同一批操作（摘要完全相同）只保留最新一条，避免「城关村7户 · 审核通过」重复刷屏
        Map<String, ScreenEventVO> distinctEvents = new LinkedHashMap<>();
        for (ScreenEventVO event : rawEvents) {
            String key = (event.getSummary() == null || event.getSummary().isBlank())
                    ? String.valueOf(event.getId()) : event.getSummary();
            distinctEvents.putIfAbsent(key, event);
        }
        List<ScreenEventVO> events = distinctEvents.values().stream()
                .limit(EVENT_LIMIT)
                .collect(Collectors.toList());
        vo.setEvents(events);

        vo.setUpdateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return vo;
    }

    /**
     * 项目状态标签：已驳回 > 待审核 > 已归档 > 待验收 > 施工中 > 待开工
     * 已归档为生命周期终点优先判定，避免残留施工数据造成状态不一致
     */
    private String resolveStatusLabel(ScreenProjectStatusVO item) {
        if (Constants.APPROVAL_STATUS_REJECTED.equalsIgnoreCase(item.getApprovalStatus())) {
            return "已驳回";
        }
        if (Constants.APPROVAL_STATUS_PENDING.equalsIgnoreCase(item.getApprovalStatus())) {
            return "待审核";
        }
        if (Constants.APPROVAL_ACCEPT_ARCHIVED.equalsIgnoreCase(item.getAcceptStatus())) {
            return "已归档";
        }
        if (Constants.APPROVAL_ACCEPT_APPLYING.equalsIgnoreCase(item.getAcceptStatus())) {
            return "待验收";
        }
        if (item.getProgressFlag() != null && item.getProgressFlag() > 0) {
            return "施工中";
        }
        return "待开工";
    }

    /** 组装近7个月资金拨付趋势，按月份升序并补零 */
    private List<ScreenFundTrendVO> buildFundTrend() {
        Map<String, BigDecimal> budgetMap = new HashMap<>();
        screenStatMapper.selectMonthlyBudget().forEach(item ->
                budgetMap.put(item.getMonth(), nullToZero(item.getBudget())));
        Map<String, BigDecimal> paidMap = new HashMap<>();
        screenStatMapper.selectMonthlyPaid().forEach(item ->
                paidMap.put(item.getMonth(), nullToZero(item.getPaid())));

        List<ScreenFundTrendVO> trend = new ArrayList<>(TREND_MONTHS);
        YearMonth current = YearMonth.now();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            String month = current.minusMonths(i).toString();
            ScreenFundTrendVO point = new ScreenFundTrendVO();
            point.setMonth(month);
            point.setBudget(budgetMap.getOrDefault(month, BigDecimal.ZERO));
            point.setPaid(paidMap.getOrDefault(month, BigDecimal.ZERO));
            trend.add(point);
        }
        return trend;
    }

    /**
     * 组装各村项目类型统计：横轴取项目数最多的6个行政村，
     * 系列按项目类型字典顺序（常规项目/应急抢险/灾后重建）输出各村项目数
     */
    private ScreenTypeStatsVO buildTypeStats() {
        List<ScreenVillageTypeVO> rows = screenStatMapper.selectVillageTypeCounts();
        // 各村项目总数，用于倒序取前 N 个村
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ScreenVillageTypeVO row : rows) {
            totals.merge(row.getVillageName(), nullToZero(row.getProjectCount()), Long::sum);
        }
        List<String> villages = totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TYPE_VILLAGE_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
        // 村 + 项目类型 → 项目数
        Map<String, Long> counts = new HashMap<>();
        for (ScreenVillageTypeVO row : rows) {
            counts.put(row.getVillageName() + "|" + row.getProjectType(), nullToZero(row.getProjectCount()));
        }
        List<SysDict> types = sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, DICT_PROJECT_TYPE)
                .orderByAsc(SysDict::getSort));
        List<ScreenTypeSeriesVO> series = new ArrayList<>(types.size());
        for (SysDict type : types) {
            List<Long> data = new ArrayList<>(villages.size());
            for (String village : villages) {
                data.add(counts.getOrDefault(village + "|" + type.getDictValue(), 0L));
            }
            series.add(new ScreenTypeSeriesVO(type.getDictLabel(), data));
        }
        ScreenTypeStatsVO stats = new ScreenTypeStatsVO();
        stats.setVillages(villages);
        stats.setSeries(series);
        return stats;
    }

    /**
     * 组装各村项目类型明细统计：横轴取明细总数（农户+公建）最多的前 N 个行政村，
     * 每个项目类型按字典顺序拆分为「农户明细(户)」与「公建设施明细(处)」两条系列，
     * 缺失的「村 × 类型」组合补 0，保证系列数据与 villages 严格对齐。
     * 系列顺序固定为：类型1农户, 类型1公建, 类型2农户, 类型2公建, ...
     * 前端按此固定顺序派生「项目类型→线型」与「明细类别→颜色/单位」的映射。
     */
    private ScreenTypeDetailStatsVO buildTypeDetailStats() {
        List<ScreenVillageTypeDetailVO> rows = screenStatMapper.selectVillageTypeDetailCounts();
        // 各村明细总数（农户+公建），用于倒序取前 N 个村
        Map<String, Long> totals = new LinkedHashMap<>();
        for (ScreenVillageTypeDetailVO row : rows) {
            long sum = nullToZero(row.getHouseholdCount()) + nullToZero(row.getFacilityCount());
            totals.merge(row.getVillageName(), sum, Long::sum);
        }
        List<String> villages = totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TYPE_DETAIL_VILLAGE_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
        // 村 + 项目类型 → 农户/公建明细数
        Map<String, Long> householdMap = new HashMap<>();
        Map<String, Long> facilityMap = new HashMap<>();
        for (ScreenVillageTypeDetailVO row : rows) {
            String key = row.getVillageName() + "|" + row.getProjectType();
            householdMap.put(key, nullToZero(row.getHouseholdCount()));
            facilityMap.put(key, nullToZero(row.getFacilityCount()));
        }
        // 与各村项目类型分布共用同一套字典顺序（常规/应急/灾后），保证两图类型一一对应
        List<SysDict> types = sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, DICT_PROJECT_TYPE)
                .orderByAsc(SysDict::getSort));
        List<ScreenTypeDetailSeriesVO> series = new ArrayList<>(types.size() * 2);
        for (SysDict type : types) {
            String typeLabel = type.getDictLabel();
            List<Long> hhData = new ArrayList<>(villages.size());
            List<Long> facData = new ArrayList<>(villages.size());
            for (String village : villages) {
                String key = village + "|" + type.getDictValue();
                hhData.add(householdMap.getOrDefault(key, 0L));
                facData.add(facilityMap.getOrDefault(key, 0L));
            }
            ScreenTypeDetailSeriesVO hh = new ScreenTypeDetailSeriesVO();
            hh.setName(typeLabel + "·" + Constants.DETAIL_LABEL_HOUSEHOLD);
            hh.setCategory(Constants.DETAIL_CATEGORY_HOUSEHOLD);
            hh.setData(hhData);
            series.add(hh);
            ScreenTypeDetailSeriesVO fac = new ScreenTypeDetailSeriesVO();
            fac.setName(typeLabel + "·" + Constants.DETAIL_LABEL_FACILITY);
            fac.setCategory(Constants.DETAIL_CATEGORY_FACILITY);
            fac.setData(facData);
            series.add(fac);
        }
        ScreenTypeDetailStatsVO stats = new ScreenTypeDetailStatsVO();
        stats.setVillages(villages);
        stats.setSeries(series);
        return stats;
    }

    /** 百分比计算：分母为0时返回0，保留1位小数 */
    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return nullToZero(numerator)
                .multiply(HUNDRED)
                .divide(denominator, RATE_SCALE, RoundingMode.HALF_UP);
    }

    /** 空值归零，避免前端出现 null */
    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 空值归零，避免前端出现 null */
    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 构建事件摘要：优先按业务接口还原「具体对象」，如
     * 「XX申报 · 张三 · 明细审核（通过）」「城关村危房改造 · 立项审核通过」「XX项目 · 下拨资金（2000元）」；
     * 无法定位业务对象时回退为「模块 · 操作」。
     */
    private String buildEventSummary(ScreenEventVO event, Map<String, String> villageByCode,
                                     Map<String, Map<String, String>> fundDict) {
        String url = event.getUrl() == null ? "" : event.getUrl();
        String params = event.getParams();
        String operation = event.getOperation();

        // 申报类：反查申报单标题，明细审核额外反查户主/设施
        if (url.startsWith("/api/declaration/audit-detail")) {
            return buildDetailAuditSummary(params, operation);
        }
        if (url.startsWith("/api/declaration/")) {
            String title = firstNonBlank(extractParam(params, "title"),
                    declarationTitle(toLong(extractParam(params, "id"))));
            return joinSubject(title, withAction(operation, extractParam(params, "action")));
        }
        // 立项类：参数带项目名直接用，否则按立项ID反查
        if (url.startsWith("/api/project/approval")) {
            Long id = toLong(extractParam(params, "id"));
            String name = firstNonBlank(extractParam(params, "projectName"),
                    id == null ? null : approvalProjectName(id));
            return joinSubject(name, operation);
        }
        // 资金类：不展示「资金登记/下拨资金」，改为「项目 · 资金来源/资金类型（金额）」
        if (url.startsWith("/api/project/fund")) {
            String name = approvalProjectName(toLong(extractParam(params, "approvalId")));
            String fundValue = extractParam(params, "fundType");
            String source = dictLabel(fundDict, DICT_FUND_SOURCE, extractParam(params, "fundSource"));
            // 两个字典键值重叠（如 1/2），按操作优先选字典：登记用 zi_jin_lei_xing，下拨用 xia_bo_lei_xing
            boolean register = operation != null && operation.contains("登记");
            String type = register
                    ? firstNonBlank(dictLabel(fundDict, DICT_FUND_TYPE, fundValue),
                            dictLabel(fundDict, DICT_ALLOC_TYPE, fundValue))
                    : firstNonBlank(dictLabel(fundDict, DICT_ALLOC_TYPE, fundValue),
                            dictLabel(fundDict, DICT_FUND_TYPE, fundValue));
            String category = joinNonBlank(source, type, " / ");
            List<String> parts = new ArrayList<>(2);
            if (name != null) {
                parts.add(name);
            }
            if (category != null) {
                parts.add(category);
            }
            String summary = String.join(" · ", parts);
            String amount = formatAmount(extractParam(params, "amount"));
            if (amount != null) {
                summary = summary.isBlank() ? amount + "元" : summary + "（" + amount + "元）";
            }
            return summary.isBlank() ? operation : summary;
        }
        // 施工/验收类：按立项ID反查项目名
        if (url.startsWith("/api/project/construction") || url.startsWith("/api/project/acceptance")) {
            Long approvalId = toLong(extractParam(params, "approvalId"));
            String name = approvalProjectName(approvalId == null ? firstLong(params) : approvalId);
            return joinSubject(name, operation);
        }

        // 其余走通用提取（地点 · 对象 · 操作）
        String where = firstNonBlank(
                extractParam(params, "regionName"),
                extractParam(params, "villageName"),
                villageByCode.get(extractParam(params, "regionCode")));
        // 注意：不提取 fileName——许多 DTO 内嵌附件列表带哈希文件名，会污染摘要。
        String what = firstNonBlank(
                extractParam(params, "itemName"),
                extractParam(params, "facilityName"),
                extractParam(params, "headName"),
                extractParam(params, "projectName"),
                extractParam(params, "title"),
                extractParam(params, "declarationNo"),
                extractParam(params, "dictLabel"),
                extractParam(params, "warehouseName"),
                extractParam(params, "name"));
        String qty = null;
        String quantity = extractParam(params, "quantity");
        if (quantity != null) {
            String unit = extractParam(params, "unit");
            qty = quantity + (unit == null ? "" : unit);
        } else {
            String amount = formatAmount(extractParam(params, "amount"));
            if (amount != null) {
                qty = amount + "元";
            }
        }
        List<String> parts = new ArrayList<>(3);
        if (where != null) {
            parts.add(where);
        }
        if (what != null) {
            parts.add(what);
        }
        if (operation != null && !operation.isBlank()) {
            parts.add(operation);
        }
        String summary = String.join(" · ", parts);
        if (qty != null) {
            summary = summary + "（" + qty + "）";
        }
        if (summary.isBlank()) {
            summary = event.getModule() + " · " + operation;
        }
        return summary;
    }

    /**
     * 明细审核摘要：反查申报单标题与所属村组，用「户数/设施数」代替户主或设施名称，
     * 拼成「申报标题 · 村组N户 · 审核通过」
     */
    private String buildDetailAuditSummary(String params, String operation) {
        Long detailId = toLong(extractParam(params, "detailId"));
        String detailKind = extractParam(params, "detailKind");
        String title = null;
        String region = null;
        String countText = null;
        if (detailId != null && "public_facility".equals(detailKind)) {
            DeclarationPublicFacility f = declarationPublicFacilityMapper.selectById(detailId);
            if (f != null) {
                title = declarationTitle(f.getDeclarationId());
                region = f.getSnapshotRegionName();
                countText = countFacilities(f.getDeclarationId(), f.getSnapshotRegionCode()) + "处";
            }
        } else if (detailId != null) {
            DeclarationHousehold h = declarationHouseholdMapper.selectById(detailId);
            if (h != null) {
                title = declarationTitle(h.getDeclarationId());
                region = h.getSnapshotRegionName();
                countText = countHouseholds(h.getDeclarationId(), h.getSnapshotRegionCode()) + "户";
            }
        }
        List<String> parts = new ArrayList<>(3);
        if (title != null) {
            parts.add(title);
        }
        String target = (region == null ? "" : region) + (countText == null ? "" : countText);
        if (!target.isBlank()) {
            parts.add(target);
        }
        // 直接展示审核结论，不再显示「明细审核」字样
        String action = actionLabel(extractParam(params, "action"));
        parts.add(action == null ? operation : "审核" + action);
        return String.join(" · ", parts);
    }

    /** 统计申报单在某村组下的农户明细数（村组为空时统计全单） */
    private long countHouseholds(Long declarationId, String regionCode) {
        if (declarationId == null) {
            return 1;
        }
        LambdaQueryWrapper<DeclarationHousehold> wrapper = new LambdaQueryWrapper<DeclarationHousehold>()
                .eq(DeclarationHousehold::getDeclarationId, declarationId);
        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.eq(DeclarationHousehold::getSnapshotRegionCode, regionCode);
        }
        Long count = declarationHouseholdMapper.selectCount(wrapper);
        return count == null || count == 0 ? 1 : count;
    }

    /** 统计申报单在某村组下的公建设施明细数（村组为空时统计全单） */
    private long countFacilities(Long declarationId, String regionCode) {
        if (declarationId == null) {
            return 1;
        }
        LambdaQueryWrapper<DeclarationPublicFacility> wrapper = new LambdaQueryWrapper<DeclarationPublicFacility>()
                .eq(DeclarationPublicFacility::getDeclarationId, declarationId);
        if (regionCode != null && !regionCode.isBlank()) {
            wrapper.eq(DeclarationPublicFacility::getSnapshotRegionCode, regionCode);
        }
        Long count = declarationPublicFacilityMapper.selectCount(wrapper);
        return count == null || count == 0 ? 1 : count;
    }

    /** 查询申报单标题（标题为空时回退业务编码） */
    private String declarationTitle(Long id) {
        if (id == null) {
            return null;
        }
        Declaration declaration = declarationMapper.selectById(id);
        return declaration == null ? null : firstNonBlank(declaration.getTitle(), declaration.getBusinessCode());
    }

    /** 查询立项项目名称 */
    private String approvalProjectName(Long approvalId) {
        if (approvalId == null) {
            return null;
        }
        ProjectApproval approval = projectApprovalMapper.selectById(approvalId);
        return approval == null ? null : approval.getProjectName();
    }

    /** 操作 + 审核结论（approve通过 / reject驳回 / return退回） */
    private String withAction(String operation, String action) {
        String label = actionLabel(action);
        if (operation == null || operation.isBlank()) {
            return label == null ? "" : label;
        }
        return label == null ? operation : operation + "（" + label + "）";
    }

    /** 审核动作翻译：approve通过 / reject驳回 / return退回 */
    private String actionLabel(String action) {
        if ("approve".equalsIgnoreCase(action)) {
            return "通过";
        }
        if ("reject".equalsIgnoreCase(action)) {
            return "驳回";
        }
        if ("return".equalsIgnoreCase(action)) {
            return "退回";
        }
        return null;
    }

    /** 批量加载字典标签：类型 → (字典值 → 标签) */
    private Map<String, Map<String, String>> loadDictLabels(String... dictTypes) {
        List<SysDict> list = sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .in(SysDict::getDictType, List.of(dictTypes)));
        Map<String, Map<String, String>> result = new HashMap<>();
        for (SysDict dict : list) {
            result.computeIfAbsent(dict.getDictType(), k -> new HashMap<>())
                    .put(dict.getDictValue(), dict.getDictLabel());
        }
        return result;
    }

    /** 按字典类型与值取中文标签，缺失返回 null */
    private String dictLabel(Map<String, Map<String, String>> dictCache, String dictType, String value) {
        if (dictCache == null || dictType == null || value == null) {
            return null;
        }
        Map<String, String> items = dictCache.get(dictType);
        if (items == null) {
            return null;
        }
        String label = items.get(value);
        return label == null || label.isBlank() ? null : label;
    }

    /** 用指定分隔符拼接非空片段，全部为空返回 null */
    private String joinNonBlank(String first, String second, String delimiter) {
        if (first == null || first.isBlank()) {
            return (second == null || second.isBlank()) ? null : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + delimiter + second;
    }

    /** 主角与操作拼接：有对象时「对象 · 操作」，否则仅操作 */
    private String joinSubject(String subject, String operation) {
        String act = operation == null ? "" : operation;
        if (subject == null || subject.isBlank()) {
            return act;
        }
        return act.isBlank() ? subject : subject + " · " + act;
    }

    /** 字符串转 Long，失败返回 null */
    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 从参数中取第一个较长数字（兼容 @PathVariable 序列化成的裸 ID） */
    private Long firstLong(String params) {
        if (params == null || params.isBlank()) {
            return null;
        }
        Matcher m = Pattern.compile("(\\d{5,})").matcher(params);
        return m.find() ? toLong(m.group(1)) : null;
    }

    /** 金额去尾零：2000.00 → 2000 */
    private String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount.trim()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    /**
     * 事件类别映射：优先按 URL 前缀判定，其次按模块名称关键字兜底，
     * 用于前端彩色圆点的稳定着色（不依赖中文模块名）。
     */
    private String resolveEventCategory(String module, String url) {
        if (url != null) {
            if (url.startsWith("/api/base/")) {
                return "base";
            }
            if (url.startsWith("/api/project/declaration")) {
                return "declaration";
            }
            if (url.startsWith("/api/project/approval")) {
                return "approval";
            }
            if (url.startsWith("/api/project/construction")) {
                return "construction";
            }
            if (url.startsWith("/api/project/acceptance")) {
                return "acceptance";
            }
            if (url.startsWith("/api/project/fund")) {
                return "fund";
            }
            if (url.startsWith("/api/project/archive")) {
                return "archive";
            }
            if (url.startsWith("/api/material/")) {
                return "material";
            }
            if (url.startsWith("/api/system/")) {
                return "system";
            }
            if (url.startsWith("/api/file/")) {
                return "file";
            }
        }
        if (module != null) {
            if (module.contains("物资")) {
                return "material";
            }
            if (module.contains("施工")) {
                return "construction";
            }
            if (module.contains("立项")) {
                return "approval";
            }
            if (module.contains("验收")) {
                return "acceptance";
            }
            if (module.contains("资金")) {
                return "fund";
            }
            if (module.contains("档案")) {
                return "archive";
            }
            if (module.contains("申报")) {
                return "declaration";
            }
            if (module.contains("基础")) {
                return "base";
            }
            if (module.contains("附件")) {
                return "file";
            }
            if (module.contains("系统") || module.contains("字典")
                    || module.contains("用户") || module.contains("角色") || module.contains("菜单")) {
                return "system";
            }
        }
        return "system";
    }

    /** 返回第一个非空白值 */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * 从操作参数字符串中提取指定字段值，兼容两种格式：
     * 1) JSON：{"key":"value"} 或 {"key":123}
     * 2) 对象 toString()：key=value,
     * 提取不到返回 null；toString 格式用单词边界避免命中 totalAmount 等更长字段。
     */
    private String extractParam(String params, String key) {
        if (params == null || params.isBlank() || key == null) {
            return null;
        }
        // JSON 字符串值
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(params);
        if (m.find()) {
            String v = m.group(1);
            return v.isBlank() ? null : v;
        }
        // JSON 数值
        m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(params);
        if (m.find()) {
            return m.group(1);
        }
        // toString() 格式：key=value（单词边界避免命中更长的同后缀字段）
        m = Pattern.compile("\\b" + Pattern.quote(key) + "=([^,\\)]*)").matcher(params);
        if (m.find()) {
            String v = m.group(1).trim();
            return (v.isBlank() || "null".equals(v)) ? null : v;
        }
        return null;
    }
}
