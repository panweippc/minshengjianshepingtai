package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.ApprovalEditDTO;
import com.msjs.project.entity.ProjectApproval;
import com.msjs.project.entity.ProjectApprovalAttachment;
import com.msjs.project.entity.ProjectApprovalDeclare;
import com.msjs.project.entity.ProjectApprovalDetail;
import com.msjs.project.mapper.ProjectApprovalAttachmentMapper;
import com.msjs.project.mapper.ProjectApprovalDeclareMapper;
import com.msjs.project.mapper.ProjectApprovalDetailMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.service.ProjectApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 立项管理服务实现
 * 树形结构：立项项目（一级） → 申报内容（二级） → 农户/设施明细（三级）
 * 状态流转：待审核 → 已通过 / 已驳回；已驳回 → 退回待审核
 */
@Service
@RequiredArgsConstructor
public class ProjectApprovalServiceImpl implements ProjectApprovalService {

    private final ProjectApprovalMapper approvalMapper;
    private final ProjectApprovalDeclareMapper declareMapper;
    private final ProjectApprovalDetailMapper detailMapper;
    private final ProjectApprovalAttachmentMapper attachmentMapper;

    @Override
    public PageVO<ProjectApproval> page(long pageNum, long pageSize, String tab, String projectName,
                                        String projectCategory, String projectSource,
                                        LocalDate startTime, LocalDate endTime) {
        Page<ProjectApproval> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        // 页签状态过滤：全部页签不加条件
        if (Constants.APPROVAL_TAB_PENDING.equals(tab)) {
            wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_PENDING);
        } else if (Constants.APPROVAL_TAB_REJECTED.equals(tab)) {
            wrapper.eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_REJECTED);
        }
        wrapper.like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectSource, projectSource)
                .ge(startTime != null, ProjectApproval::getSubmitTime,
                        startTime == null ? null : startTime.atStartOfDay())
                .le(endTime != null, ProjectApproval::getSubmitTime,
                        endTime == null ? null : endTime.atTime(LocalTime.MAX))
                .orderByDesc(ProjectApproval::getSubmitTime);
        Page<ProjectApproval> result = approvalMapper.selectPage(page, wrapper);
        fillTree(result.getRecords());
        return PageVO.of(result);
    }

    @Override
    public Map<String, Long> counts(String projectName, String projectCategory, String projectSource,
                                    LocalDate startTime, LocalDate endTime) {
        // 构建不含状态的筛选条件，三次查询共享
        LambdaQueryWrapper<ProjectApproval> base = buildQueryWrapper(projectName, projectCategory,
                projectSource, startTime, endTime);
        Map<String, Long> result = new HashMap<>(4);
        // 全部
        result.put(Constants.APPROVAL_TAB_ALL, approvalMapper.selectCount(base));
        // 待审核
        LambdaQueryWrapper<ProjectApproval> pending = base.clone()
                .eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_PENDING);
        result.put(Constants.APPROVAL_TAB_PENDING, approvalMapper.selectCount(pending));
        // 已驳回
        LambdaQueryWrapper<ProjectApproval> rejected = base.clone()
                .eq(ProjectApproval::getStatus, Constants.APPROVAL_STATUS_REJECTED);
        result.put(Constants.APPROVAL_TAB_REJECTED, approvalMapper.selectCount(rejected));
        return result;
    }

    /** 构建不含页签状态的公共查询条件 */
    private LambdaQueryWrapper<ProjectApproval> buildQueryWrapper(String projectName, String projectCategory,
                                                                  String projectSource, LocalDate startTime,
                                                                  LocalDate endTime) {
        LambdaQueryWrapper<ProjectApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(isNotBlank(projectName), ProjectApproval::getProjectName, trimToNull(projectName))
                .eq(isNotBlank(projectCategory), ProjectApproval::getProjectCategory, projectCategory)
                .eq(isNotBlank(projectSource), ProjectApproval::getProjectSource, projectSource)
                .ge(startTime != null, ProjectApproval::getSubmitTime,
                        startTime == null ? null : startTime.atStartOfDay())
                .le(endTime != null, ProjectApproval::getSubmitTime,
                        endTime == null ? null : endTime.atTime(LocalTime.MAX));
        return wrapper;
    }

    @Override
    public ProjectApproval detail(Long id) {
        ProjectApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        List<ProjectApproval> list = new ArrayList<>();
        list.add(approval);
        fillTree(list);
        return approval;
    }

    @Override
    public List<ProjectApprovalAttachment> listAttachments(Long approvalId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<ProjectApprovalAttachment>()
                .eq(ProjectApprovalAttachment::getApprovalId, approvalId)
                .orderByAsc(ProjectApprovalAttachment::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(ApprovalEditDTO dto) {
        ProjectApproval exist = approvalMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        // 业务约束：仅待审核状态允许编辑，已通过/已驳回不可修改
        if (!Constants.APPROVAL_STATUS_PENDING.equals(exist.getStatus())) {
            throw new BizException("仅待审核状态的立项可编辑");
        }
        exist.setProjectName(dto.getProjectName().trim());
        exist.setProjectCategory(trimToNull(dto.getProjectCategory()));
        exist.setProjectSource(trimToNull(dto.getProjectSource()));
        exist.setBudget(dto.getBudget());
        exist.setLeader(trimToNull(dto.getLeader()));
        exist.setRemark(trimToNull(dto.getRemark()));
        exist.setUpdateBy(currentUser());
        exist.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(exist);

        saveAttachments(exist.getId(), dto.getAttachments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_PENDING, Constants.APPROVAL_STATUS_APPROVED,
                "仅待审核状态的立项可通过");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_PENDING, Constants.APPROVAL_STATUS_REJECTED,
                "仅待审核状态的立项可驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBack(Long id) {
        changeStatus(id, Constants.APPROVAL_STATUS_REJECTED, Constants.APPROVAL_STATUS_PENDING,
                "仅已驳回状态的立项可退回");
    }

    /**
     * 状态流转：校验当前状态后更新为目标状态
     */
    private void changeStatus(Long id, String from, String to, String errorMsg) {
        ProjectApproval exist = approvalMapper.selectById(id);
        if (exist == null) {
            throw new BizException("立项信息不存在或已被删除");
        }
        if (!from.equals(exist.getStatus())) {
            throw new BizException(errorMsg);
        }
        exist.setStatus(to);
        exist.setUpdateBy(currentUser());
        exist.setUpdateTime(LocalDateTime.now());
        approvalMapper.updateById(exist);
    }

    /**
     * 批量组装树形结构，避免 N+1 查询
     */
    private void fillTree(List<ProjectApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return;
        }
        List<Long> approvalIds = approvals.stream().map(ProjectApproval::getId).toList();
        List<ProjectApprovalDeclare> declares = declareMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalDeclare>()
                        .in(ProjectApprovalDeclare::getApprovalId, approvalIds)
                        .orderByAsc(ProjectApprovalDeclare::getSort)
                        .orderByAsc(ProjectApprovalDeclare::getId));

        Map<Long, List<ProjectApprovalDetail>> detailMap = Map.of();
        Map<Long, List<ProjectApprovalAttachment>> attachMap = Map.of();
        if (!declares.isEmpty()) {
            List<Long> declareIds = declares.stream().map(ProjectApprovalDeclare::getId).toList();
            List<ProjectApprovalDetail> details = detailMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalDetail>()
                            .in(ProjectApprovalDetail::getDeclareId, declareIds)
                            .orderByAsc(ProjectApprovalDetail::getSort)
                            .orderByAsc(ProjectApprovalDetail::getId));
            detailMap = details.stream().collect(Collectors.groupingBy(ProjectApprovalDetail::getDeclareId));

            // 申报材料附件：declare_id 非空，按申报ID分组挂到二级节点，供查看弹窗展示
            List<ProjectApprovalAttachment> attachments = attachmentMapper.selectList(
                    new LambdaQueryWrapper<ProjectApprovalAttachment>()
                            .in(ProjectApprovalAttachment::getApprovalId, approvalIds)
                            .isNotNull(ProjectApprovalAttachment::getDeclareId)
                            .orderByAsc(ProjectApprovalAttachment::getId));
            attachMap = attachments.stream().collect(Collectors.groupingBy(ProjectApprovalAttachment::getDeclareId));
        }

        Map<Long, List<ProjectApprovalDeclare>> declareMap = declares.stream()
                .collect(Collectors.groupingBy(ProjectApprovalDeclare::getApprovalId));
        for (ProjectApprovalDeclare declare : declares) {
            declare.setChildren(detailMap.getOrDefault(declare.getId(), new ArrayList<>()));
            declare.setAttachments(attachMap.getOrDefault(declare.getId(), new ArrayList<>()));
        }
        for (ProjectApproval approval : approvals) {
            approval.setChildren(declareMap.getOrDefault(approval.getId(), new ArrayList<>()));
        }
    }

    /**
     * 保存立项附件：
     * 已存在附件按ID差异更新，本次未提交的已有附件做逻辑删除，新增附件直接插入
     */
    private void saveAttachments(Long approvalId, List<ApprovalEditDTO.AttachmentItem> attachments) {
        if (attachments == null) {
            // 未传附件列表视为不修改附件
            return;
        }
        List<ProjectApprovalAttachment> existList = attachmentMapper.selectList(
                new LambdaQueryWrapper<ProjectApprovalAttachment>()
                        .eq(ProjectApprovalAttachment::getApprovalId, approvalId)
                        // 仅维护立项资料附件（declare_id 为空），申报材料附件不在此处增删
                        .isNull(ProjectApprovalAttachment::getDeclareId));
        Set<Long> inputIds = new HashSet<>();
        for (ApprovalEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null) {
                inputIds.add(item.getId());
            }
        }
        for (ProjectApprovalAttachment att : existList) {
            if (!inputIds.contains(att.getId())) {
                attachmentMapper.deleteById(att.getId());
            }
        }
        for (ApprovalEditDTO.AttachmentItem item : attachments) {
            if (item.getId() != null) {
                ProjectApprovalAttachment att = attachmentMapper.selectById(item.getId());
                if (att != null) {
                    fillAttachment(att, item);
                    attachmentMapper.updateById(att);
                    continue;
                }
            }
            ProjectApprovalAttachment att = new ProjectApprovalAttachment();
            att.setApprovalId(approvalId);
            fillAttachment(att, item);
            att.setCreateTime(LocalDateTime.now());
            attachmentMapper.insert(att);
        }
    }

    private void fillAttachment(ProjectApprovalAttachment att, ApprovalEditDTO.AttachmentItem item) {
        att.setAttachmentType(trimToNull(item.getAttachmentType()));
        att.setFileName(trimToNull(item.getFileName()));
        att.setFileKey(trimToNull(item.getFileKey()));
        att.setBucket(trimToNull(item.getBucket()));
        att.setFileSize(item.getFileSize());
        att.setRemark(trimToNull(item.getRemark()));
    }

    private String currentUser() {
        LoginUser loginUser = UserContext.get();
        if (loginUser != null && loginUser.getUsername() != null && !loginUser.getUsername().isBlank()) {
            return loginUser.getUsername();
        }
        return "system";
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
