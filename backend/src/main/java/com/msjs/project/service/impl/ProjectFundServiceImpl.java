package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.FundAttachmentDTO;
import com.msjs.project.dto.FundSaveDTO;
import com.msjs.project.entity.BaseFacilityAttachment;
import com.msjs.project.entity.HouseholdAttachment;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.entity.ProjectFund;
import com.msjs.project.entity.ProjectFundAttachment;
import com.msjs.project.mapper.BaseFacilityAttachmentMapper;
import com.msjs.project.mapper.HouseholdAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.ProjectFundAttachmentMapper;
import com.msjs.project.mapper.ProjectFundMapper;
import com.msjs.project.service.ProjectFundService;
import com.msjs.project.vo.FundMaterialAttachmentVO;
import com.msjs.project.vo.FundNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 资金管理服务实现
 * 树形结构：立项项目（一级） → 申报内容（二级） → 农户/设施明细（三级）
 * 资金口径：一级登记上级拨付金额，二级/三级向下拨付；一级结余 = 上级拨付 - 二级下拨合计
 */
@Service
@RequiredArgsConstructor
public class ProjectFundServiceImpl implements ProjectFundService {

    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectApprovalAttachmentMapper approvalAttachmentMapper;
    private final ProjectFundMapper fundMapper;
    private final ProjectFundAttachmentMapper fundAttachmentMapper;
    private final HouseholdAttachmentMapper householdAttachmentMapper;
    private final BaseFacilityAttachmentMapper facilityAttachmentMapper;

    @Override
    public PageVO<FundNodeVO> page(long pageNum, long pageSize, String projectName,
                                   String projectCategory, String projectSource) {
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        // 数据来源：立项管理中审核通过的项目
        wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_APPROVED)
                .like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectSource, projectSource)
                .orderByDesc(ProjectApproval::getSubmitTime);
        Page<ProjectApproval> result = approvalMapper.selectPage(page, wrapper);

        List<ProjectApproval> approvals = result.getRecords();
        List<FundNodeVO> nodes = approvals.isEmpty()
                ? Collections.emptyList()
                : buildTree(approvals);

        PageVO<FundNodeVO> vo = new PageVO<>();
        vo.setRecords(nodes);
        vo.setTotal(result.getTotal());
        vo.setPageNum(result.getCurrent());
        vo.setPageSize(result.getSize());
        return vo;
    }

    @Override
    public List<ProjectFund> listRecords(String nodeType, Long nodeId) {
        LambdaQueryWrapper<ProjectFund> wrapper = new LambdaQueryWrapper<>();
        if (Constants.FUND_NODE_APPROVAL.equals(nodeType)) {
            wrapper.eq(ProjectFund::getApprovalId, nodeId)
                    .isNull(ProjectFund::getDeclareId);
        } else if (Constants.FUND_NODE_DECLARE.equals(nodeType)) {
            // 二级资金流水仅展示向该申报下拨的记录（三级明细下拨在“申报明细”页签展示）
            wrapper.eq(ProjectFund::getDeclareId, nodeId)
                    .eq(ProjectFund::getNodeLevel, Constants.FUND_LEVEL_DECLARE);
        } else if (Constants.FUND_NODE_DETAIL.equals(nodeType)) {
            wrapper.eq(ProjectFund::getDetailId, nodeId);
        } else {
            throw new BizException("节点类型不正确");
        }
        wrapper.orderByDesc(ProjectFund::getCreateTime).orderByDesc(ProjectFund::getId);
        List<ProjectFund> records = fundMapper.selectList(wrapper);
        fillAttachments(records);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(FundSaveDTO dto) {
        ProjectApproval approval = approvalMapper.selectById(dto.getApprovalId());
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        // 业务约束：仅审核通过的立项可进行资金拨付
        if (!Constants.APPROVAL_STATUS_APPROVED.equals(approval.getStatus())) {
            throw new BizException("仅审核通过的立项可进行资金拨付");
        }

        ProjectFund fund = new ProjectFund();
        fund.setApprovalId(approval.getId());
        fund.setFundType(trimToNull(dto.getFundType()));
        fund.setFundSource(trimToNull(dto.getFundSource()));
        fund.setAmount(dto.getAmount());
        fund.setRemark(trimToNull(dto.getRemark()));
        // 操作人、时间由页面人工填写，直接落库用于资金流水展示
        fund.setCreateBy(dto.getOperator().trim());
        fund.setCreateTime(dto.getOperateTime());

        String nodeType = dto.getNodeType();
        if (Constants.FUND_NODE_APPROVAL.equals(nodeType)) {
            // 一级：上级资金登记
            fund.setNodeLevel(Constants.FUND_LEVEL_APPROVAL);
            if (fund.getFundType() == null) {
                throw new BizException("资金类型不能为空");
            }
            if (fund.getFundSource() == null) {
                throw new BizException("资金来源不能为空");
            }
        } else if (Constants.FUND_NODE_DECLARE.equals(nodeType)) {
            // 二级：向申报内容下拨，累计下拨不得超过一级登记金额
            ProjectApprovalDeclare declare = requireDeclare(dto.getDeclareId(), approval.getId());
            if (fund.getFundType() == null) {
                throw new BizException("下拨类型不能为空");
            }
            BigDecimal available = sumByLevel(approval.getId(), Constants.FUND_LEVEL_APPROVAL, null)
                    .subtract(sumByLevel(approval.getId(), Constants.FUND_LEVEL_DECLARE, null));
            checkAvailable(available, dto.getAmount());
            fund.setNodeLevel(Constants.FUND_LEVEL_DECLARE);
            fund.setDeclareId(declare.getId());
        } else if (Constants.FUND_NODE_DETAIL.equals(nodeType)) {
            // 三级：向农户/设施明细下拨，累计下拨不得超过所属申报的二级下拨金额
            ProjectApprovalDetail detail = requireDetail(dto, approval.getId());
            if (fund.getFundType() == null) {
                throw new BizException("下拨类型不能为空");
            }
            BigDecimal available = sumByLevel(approval.getId(), Constants.FUND_LEVEL_DECLARE, detail.getDeclareId())
                    .subtract(sumByLevel(approval.getId(), Constants.FUND_LEVEL_DETAIL, detail.getDeclareId()));
            checkAvailable(available, dto.getAmount());
            fund.setNodeLevel(Constants.FUND_LEVEL_DETAIL);
            fund.setDeclareId(detail.getDeclareId());
            fund.setDetailId(detail.getId());
        } else {
            throw new BizException("节点类型不正确");
        }
        fundMapper.insert(fund);
        saveAttachments(fund.getId(), dto.getAttachments());
    }

    /** 批量保存资金凭证附件 */
    private void saveAttachments(Long fundId, List<FundAttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (FundAttachmentDTO item : attachments) {
            ProjectFundAttachment attachment = new ProjectFundAttachment();
            attachment.setFundId(fundId);
            attachment.setAttachmentType(trimToNull(item.getAttachmentType()));
            attachment.setFileName(item.getFileName().trim());
            attachment.setFileKey(item.getFileKey().trim());
            attachment.setBucket(trimToNull(item.getBucket()));
            attachment.setFileSize(item.getFileSize());
            attachment.setRemark(trimToNull(item.getRemark()));
            attachment.setCreateTime(now);
            fundAttachmentMapper.insert(attachment);
        }
    }

    /** 回填资金拨付记录的凭证附件 */
    private void fillAttachments(List<ProjectFund> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> fundIds = records.stream().map(ProjectFund::getId).toList();
        Map<Long, List<ProjectFundAttachment>> attachMap = fundAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectFundAttachment>()
                                .in(ProjectFundAttachment::getFundId, fundIds)
                                .orderByAsc(ProjectFundAttachment::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectFundAttachment::getFundId));
        records.forEach(record -> record.setAttachments(
                attachMap.getOrDefault(record.getId(), new ArrayList<>())));
    }

    /** 校验申报内容存在且属于当前立项 */
    private ProjectApprovalDeclare requireDeclare(Long declareId, Long approvalId) {
        if (declareId == null) {
            throw new BizException("申报内容ID不能为空");
        }
        ProjectApprovalDeclare declare = declareMapper.selectById(declareId);
        if (declare == null || !approvalId.equals(declare.getApprovalId())) {
            throw new BizException("申报内容不存在或不属于该项目");
        }
        return declare;
    }

    /** 校验申报明细存在且属于当前立项 */
    private ProjectApprovalDetail requireDetail(FundSaveDTO dto, Long approvalId) {
        if (dto.getDetailId() == null) {
            throw new BizException("申报明细ID不能为空");
        }
        ProjectApprovalDetail detail = detailMapper.selectById(dto.getDetailId());
        if (detail == null) {
            throw new BizException("申报明细不存在或已被删除");
        }
        if (dto.getDeclareId() != null && !dto.getDeclareId().equals(detail.getDeclareId())) {
            throw new BizException("申报明细与申报内容不匹配");
        }
        requireDeclare(detail.getDeclareId(), approvalId);
        return detail;
    }

    /** 下拨金额不得超过上级可用结余 */
    private void checkAvailable(BigDecimal available, BigDecimal amount) {
        if (amount.compareTo(available) > 0) {
            throw new BizException("下拨金额不能超过上级可用结余（可用 " + money(available) + " 元）");
        }
    }

    /**
     * 统计资金金额
     * @param declareId 为空时按立项维度统计
     */
    private BigDecimal sumByLevel(Long approvalId, int nodeLevel, Long declareId) {
        LambdaQueryWrapper<ProjectFund> wrapper = new LambdaQueryWrapper<ProjectFund>()
                .eq(ProjectFund::getApprovalId, approvalId)
                .eq(ProjectFund::getNodeLevel, nodeLevel);
        if (declareId != null) {
            wrapper.eq(ProjectFund::getDeclareId, declareId);
        }
        return fundMapper.selectList(wrapper).stream()
                .map(ProjectFund::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 组装三级资金树，批量查询避免 N+1
     */
    private List<FundNodeVO> buildTree(List<ProjectApproval> approvals) {
        List<Long> approvalIds = approvals.stream().map(ProjectApproval::getId).toList();

        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .in(ProjectApprovalDeclare::getApprovalId, approvalIds)
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));

        Map<Long, List<ProjectApprovalDetail>> detailMap = new HashMap<>();
        Map<Long, List<ProjectApprovalAttachment>> declareAttachMap = new HashMap<>();
        // 申报明细材料附件：农户取户附件，公共基础设施取设施附件，按关联对象ID分组
        Map<Long, List<FundMaterialAttachmentVO>> materialAttachMap = new HashMap<>();
        if (!declares.isEmpty()) {
            List<Long> declareIds = declares.stream().map(ProjectApprovalDeclare::getId).toList();
            List<ProjectApprovalDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalDetail>()
                            .in(ProjectApprovalDetail::getDeclareId, declareIds)
                            .orderByAsc(ProjectApprovalDetail::getSort)
                            .orderByAsc(ProjectApprovalDetail::getId));
            detailMap = details.stream().collect(Collectors.groupingBy(ProjectApprovalDetail::getDeclareId));
            // 农户附件
            List<Long> householdIds = details.stream()
                    .filter(detail -> "HOUSEHOLD".equals(detail.getDetailType()))
                    .map(ProjectApprovalDetail::getTargetId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!householdIds.isEmpty()) {
                householdAttachmentMapper.selectList(new LambdaQueryWrapper<HouseholdAttachment>()
                                .in(HouseholdAttachment::getHouseholdId, householdIds)
                                .orderByAsc(HouseholdAttachment::getId))
                        .forEach(attachment -> materialAttachMap
                                .computeIfAbsent(attachment.getHouseholdId(), key -> new ArrayList<>())
                                .add(toMaterial(attachment)));
            }
            // 公共基础设施附件
            List<Long> facilityIds = details.stream()
                    .filter(detail -> "FACILITY".equals(detail.getDetailType()))
                    .map(ProjectApprovalDetail::getTargetId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!facilityIds.isEmpty()) {
                facilityAttachmentMapper.selectList(new LambdaQueryWrapper<BaseFacilityAttachment>()
                                .in(BaseFacilityAttachment::getFacilityId, facilityIds)
                                .orderByAsc(BaseFacilityAttachment::getId))
                        .forEach(attachment -> materialAttachMap
                                .computeIfAbsent(attachment.getFacilityId(), key -> new ArrayList<>())
                                .add(toMaterial(attachment)));
            }
            // 申报材料附件（二级页签展示）
            declareAttachMap = approvalAttachmentMapper.selectList(
                            new LambdaQueryWrapper<ProjectApprovalAttachment>()
                                    .in(ProjectApprovalAttachment::getDeclareId, declareIds)
                                    .orderByAsc(ProjectApprovalAttachment::getId))
                    .stream()
                    .filter(item -> item.getDeclareId() != null)
                    .collect(Collectors.groupingBy(ProjectApprovalAttachment::getDeclareId));
        }
        Map<Long, List<ProjectApprovalDeclare>> declareMap = declares.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDeclare::getApprovalId));

        // 立项资料附件（一级项目资金页签展示，declare_id 为空）
        Map<Long, List<ProjectApprovalAttachment>> approvalAttachMap = approvalAttachmentMapper.selectList(
                        new LambdaQueryWrapper<ProjectApprovalAttachment>()
                                .in(ProjectApprovalAttachment::getApprovalId, approvalIds)
                                .isNull(ProjectApprovalAttachment::getDeclareId)
                                .orderByAsc(ProjectApprovalAttachment::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectApprovalAttachment::getApprovalId));

        // 资金记录按经办时间倒序，首次出现的资金来源即为该节点最新资金来源
        List<ProjectFund> funds = fundMapper.selectList(new LambdaQueryWrapper<ProjectFund>()
                .in(ProjectFund::getApprovalId, approvalIds)
                .orderByDesc(ProjectFund::getCreateTime)
                .orderByDesc(ProjectFund::getId));

        Map<Long, BigDecimal> approvalRegistered = new HashMap<>();
        Map<Long, BigDecimal> approvalAllocated = new HashMap<>();
        Map<Long, String> approvalSource = new HashMap<>();
        Map<Long, BigDecimal> declareAllocated = new HashMap<>();
        Map<Long, String> declareSource = new HashMap<>();
        Map<Long, BigDecimal> detailAllocated = new HashMap<>();
        Map<Long, String> detailSource = new HashMap<>();
        for (ProjectFund fund : funds) {
            BigDecimal amount = fund.getAmount() == null ? BigDecimal.ZERO : fund.getAmount();
            if (fund.getNodeLevel() != null && fund.getNodeLevel() == Constants.FUND_LEVEL_APPROVAL) {
                approvalRegistered.merge(fund.getApprovalId(), amount, BigDecimal::add);
                approvalSource.putIfAbsent(fund.getApprovalId(), fund.getFundSource());
            } else if (fund.getNodeLevel() != null && fund.getNodeLevel() == Constants.FUND_LEVEL_DECLARE) {
                approvalAllocated.merge(fund.getApprovalId(), amount, BigDecimal::add);
                if (fund.getDeclareId() != null) {
                    declareAllocated.merge(fund.getDeclareId(), amount, BigDecimal::add);
                    declareSource.putIfAbsent(fund.getDeclareId(), fund.getFundSource());
                }
            } else if (fund.getNodeLevel() != null && fund.getNodeLevel() == Constants.FUND_LEVEL_DETAIL) {
                if (fund.getDetailId() != null) {
                    detailAllocated.merge(fund.getDetailId(), amount, BigDecimal::add);
                    detailSource.putIfAbsent(fund.getDetailId(), fund.getFundSource());
                }
            }
        }

        // 二级下拨、三级明细：资金凭证附件
        List<Long> detailFundIds = funds.stream()
                .filter(fund -> fund.getNodeLevel() != null
                        && (fund.getNodeLevel() == Constants.FUND_LEVEL_DECLARE
                        || fund.getNodeLevel() == Constants.FUND_LEVEL_DETAIL))
                .map(ProjectFund::getId)
                .toList();
        Map<Long, List<ProjectFundAttachment>> fundAttachMap = detailFundIds.isEmpty()
                ? Collections.emptyMap()
                : fundAttachmentMapper.selectList(new LambdaQueryWrapper<ProjectFundAttachment>()
                        .in(ProjectFundAttachment::getFundId, detailFundIds)
                        .orderByAsc(ProjectFundAttachment::getId))
                .stream()
                .collect(Collectors.groupingBy(ProjectFundAttachment::getFundId));
        Map<Long, String> detailFundRemark = new HashMap<>();
        Map<Long, List<ProjectFundAttachment>> detailFundAttach = new HashMap<>();
        Map<Long, List<ProjectFundAttachment>> declareFundAttach = new HashMap<>();
        // funds 已按下拨时间倒序，首次出现的非空说明即最近一次拨付说明
        for (ProjectFund fund : funds) {
            if (fund.getNodeLevel() == null || fund.getDeclareId() == null) {
                continue;
            }
            if (fund.getNodeLevel() == Constants.FUND_LEVEL_DECLARE) {
                declareFundAttach.computeIfAbsent(fund.getDeclareId(), key -> new ArrayList<>())
                        .addAll(fundAttachMap.getOrDefault(fund.getId(), Collections.emptyList()));
                continue;
            }
            if (fund.getNodeLevel() != Constants.FUND_LEVEL_DETAIL || fund.getDetailId() == null) {
                continue;
            }
            if (!detailFundRemark.containsKey(fund.getDetailId())
                    && fund.getRemark() != null && !fund.getRemark().isBlank()) {
                detailFundRemark.put(fund.getDetailId(), fund.getRemark());
            }
            detailFundAttach.computeIfAbsent(fund.getDetailId(), key -> new ArrayList<>())
                    .addAll(fundAttachMap.getOrDefault(fund.getId(), Collections.emptyList()));
        }

        List<FundNodeVO> roots = new ArrayList<>(approvals.size());
        for (ProjectApproval approval : approvals) {
            FundNodeVO root = new FundNodeVO();
            root.setId(approval.getId());
            root.setNodeType(Constants.FUND_NODE_APPROVAL);
            root.setCode(approval.getApprovalCode());
            root.setName(approval.getProjectName());
            root.setCategory(approval.getProjectCategory());
            root.setType(approval.getProjectSource());
            root.setBudget(approval.getBudget());
            root.setFundSource(approvalSource.get(approval.getId()));
            root.setLeader(approval.getLeader());
            root.setSubmitTime(approval.getSubmitTime());
            root.setStatus(approval.getStatus());
            root.setRemark(approval.getRemark());
            root.setAttachments(approvalAttachMap.getOrDefault(approval.getId(), new ArrayList<>()));
            BigDecimal registered = approvalRegistered.get(approval.getId());
            BigDecimal allocated = approvalAllocated.get(approval.getId());
            root.setAllocatedAmount(registered);
            // 一级结余：上级拨付金额 - 二级下拨金额；未发生拨付时留空，前端展示为 "-"
            root.setBalance(registered == null && allocated == null
                    ? null
                    : nvl(registered).subtract(nvl(allocated)));

            List<FundNodeVO> declareNodes = new ArrayList<>();
            for (ProjectApprovalDeclare declare : declareMap.getOrDefault(approval.getId(), Collections.emptyList())) {
                FundNodeVO declareNode = new FundNodeVO();
                declareNode.setId(declare.getId());
                declareNode.setNodeType(Constants.FUND_NODE_DECLARE);
                declareNode.setCode(declare.getDeclareCode());
                declareNode.setName(declare.getDeclareName());
                declareNode.setCategory(declare.getDeclareCategory());
                declareNode.setType(declare.getDeclareType());
                declareNode.setBudget(declare.getBudget());
                declareNode.setFundSource(declareSource.get(declare.getId()));
                declareNode.setAllocatedAmount(declareAllocated.get(declare.getId()));
                declareNode.setTownName(declare.getTownName());
                declareNode.setVillageName(declare.getVillageName());
                declareNode.setDeclareContent(declare.getDeclareContent());
                declareNode.setAttachments(declareAttachMap.getOrDefault(declare.getId(), new ArrayList<>()));
                declareNode.setFundAttachments(
                        declareFundAttach.getOrDefault(declare.getId(), new ArrayList<>()));

                List<FundNodeVO> detailNodes = new ArrayList<>();
                for (ProjectApprovalDetail detail : detailMap.getOrDefault(declare.getId(), Collections.emptyList())) {
                    FundNodeVO detailNode = new FundNodeVO();
                    detailNode.setId(detail.getId());
                    detailNode.setNodeType(Constants.FUND_NODE_DETAIL);
                    detailNode.setDetailType(detail.getDetailType());
                    detailNode.setCode(detail.getTargetCode());
                    detailNode.setName(detail.getTargetName());
                    detailNode.setCategory(detail.getDetailType());
                    detailNode.setType(detail.getRegionName());
                    detailNode.setBudget(detail.getAmount());
                    detailNode.setFundSource(detailSource.get(detail.getId()));
                    detailNode.setAllocatedAmount(detailAllocated.get(detail.getId()));
                    detailNode.setDetailRemark(detail.getRemark());
                    detailNode.setFundRemark(detailFundRemark.get(detail.getId()));
                    detailNode.setFundAttachments(
                            detailFundAttach.getOrDefault(detail.getId(), new ArrayList<>()));
                    detailNode.setMaterialAttachments(
                            materialAttachMap.getOrDefault(detail.getTargetId(), new ArrayList<>()));
                    detailNodes.add(detailNode);
                }
                declareNode.setChildren(detailNodes);
                declareNodes.add(declareNode);
            }
            root.setChildren(declareNodes);
            roots.add(root);
        }
        return roots;
    }

    /** 农户附件转换为统一材料附件VO */
    private FundMaterialAttachmentVO toMaterial(HouseholdAttachment attachment) {
        FundMaterialAttachmentVO vo = new FundMaterialAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setFileName(attachment.getFileName());
        vo.setFileKey(attachment.getFileKey());
        vo.setBucket(attachment.getBucket());
        vo.setFileSize(attachment.getFileSize());
        vo.setRemark(attachment.getRemark());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    /** 设施附件转换为统一材料附件VO（设施仅存 filePath，固定使用正式归档桶） */
    private FundMaterialAttachmentVO toMaterial(BaseFacilityAttachment attachment) {
        FundMaterialAttachmentVO vo = new FundMaterialAttachmentVO();
        vo.setId(attachment.getId());
        vo.setAttachmentType(attachment.getAttachmentType());
        vo.setFileName(attachment.getFileName());
        vo.setFileKey(attachment.getFilePath());
        vo.setBucket(Constants.BUCKET_ARCHIVE_FORMAL);
        vo.setFileSize(attachment.getFileSize());
        vo.setRemark(attachment.getRemark());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
