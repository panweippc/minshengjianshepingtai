package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.FacilitySaveDTO;
import com.msjs.project.dto.FacilitySaveDTO.FacilityAttachmentSaveDTO;
import com.msjs.project.entity.BaseFacility;
import com.msjs.project.entity.BaseFacilityAttachment;
import com.msjs.project.entity.BaseFacilityProject;
import com.msjs.project.entity.Project;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.BaseFacilityAttachmentMapper;
import com.msjs.project.mapper.BaseFacilityMapper;
import com.msjs.project.mapper.BaseFacilityProjectMapper;
import com.msjs.project.mapper.ProjectMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.BaseFacilityService;
import com.msjs.project.vo.FacilityAttachmentVO;
import com.msjs.project.vo.FacilityDetailVO;
import com.msjs.project.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公共基础设施服务实现
 */
@Service
@RequiredArgsConstructor
public class BaseFacilityServiceImpl implements BaseFacilityService {

    private final BaseFacilityMapper facilityMapper;
    private final BaseFacilityAttachmentMapper attachmentMapper;
    private final BaseFacilityProjectMapper facilityProjectMapper;
    private final ProjectMapper projectMapper;
    private final SysRegionMapper regionMapper;

    @Override
    public PageVO<BaseFacility> page(long pageNum, long pageSize, String facilityCode, String facilityName,
                                     String facilityType, String nodeCode) {
        Page<BaseFacility> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BaseFacility> wrapper = new LambdaQueryWrapper<BaseFacility>()
                // 选中树节点时展示该村下的设施（或该区划及所有下级区划的设施）
                .and(isNotBlank(nodeCode), w -> {
                    // 先查找nodeCode及其所有下级区划代码
                    List<String> regionCodes = collectRegionCodes(nodeCode);
                    if (!regionCodes.isEmpty()) {
                        w.in(BaseFacility::getRegionCode, regionCodes);
                    } else {
                        w.eq(BaseFacility::getRegionCode, nodeCode);
                    }
                })
                .like(isNotBlank(facilityCode), BaseFacility::getFacilityCode, facilityCode)
                .like(isNotBlank(facilityName), BaseFacility::getFacilityName, facilityName)
                .eq(isNotBlank(facilityType), BaseFacility::getFacilityType, facilityType)
                .orderByDesc(BaseFacility::getCreateTime);
        return PageVO.of(facilityMapper.selectPage(page, wrapper));
    }

    @Override
    public FacilityDetailVO detail(Long id) {
        BaseFacility facility = facilityMapper.selectById(id);
        if (facility == null) {
            throw new BizException("设施不存在或已被删除");
        }
        FacilityDetailVO vo = new FacilityDetailVO();
        vo.setId(facility.getId());
        vo.setFacilityCode(facility.getFacilityCode());
        vo.setFacilityName(facility.getFacilityName());
        vo.setFacilityType(facility.getFacilityType());
        vo.setRegionCode(facility.getRegionCode());
        vo.setRegionName(facility.getRegionName());
        vo.setLocation(facility.getLocation());
        vo.setBuildYear(facility.getBuildYear());
        vo.setQuantity(facility.getQuantity());
        vo.setUnit(facility.getUnit());
        vo.setRemark(facility.getRemark());
        vo.setCreateTime(facility.getCreateTime());

        // 附件列表
        List<BaseFacilityAttachment> attachments = attachmentMapper.selectList(
                new LambdaQueryWrapper<BaseFacilityAttachment>()
                        .eq(BaseFacilityAttachment::getFacilityId, id)
                        .orderByAsc(BaseFacilityAttachment::getId));
        vo.setAttachments(attachments.stream().map(this::toAttachmentVO).collect(Collectors.toList()));

        // 关联项目列表
        List<ProjectVO> projects = loadRelatedProjects(id);
        vo.setProjects(projects);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(FacilitySaveDTO dto) {
        checkCodeAvailable(dto.getFacilityCode());

        BaseFacility facility = new BaseFacility();
        facility.setFacilityCode(dto.getFacilityCode().trim());
        facility.setFacilityName(dto.getFacilityName().trim());
        facility.setFacilityType(trimToNull(dto.getFacilityType()));
        facility.setRegionCode(dto.getRegionCode().trim());
        facility.setRegionName(resolveRegionName(dto.getRegionCode(), dto.getRegionName()));
        facility.setLocation(trimToNull(dto.getLocation()));
        facility.setBuildYear(dto.getBuildYear());
        facility.setQuantity(dto.getQuantity());
        facility.setUnit(trimToNull(dto.getUnit()));
        facility.setRemark(trimToNull(dto.getRemark()));

        try {
            facilityMapper.insert(facility);
        } catch (DuplicateKeyException e) {
            throw new BizException("设施编号「" + dto.getFacilityCode().trim() + "」已存在");
        }

        // 保存附件
        saveAttachments(facility.getId(), dto.getAttachments(), false);

        return facility.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(FacilitySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("设施ID不能为空");
        }
        BaseFacility exist = facilityMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("设施不存在或已被删除");
        }
        // 设施编号作为业务编码，编辑时不允许修改
        if (!exist.getFacilityCode().equals(dto.getFacilityCode())) {
            throw new BizException("设施编号不允许修改");
        }

        exist.setFacilityName(dto.getFacilityName().trim());
        exist.setFacilityType(trimToNull(dto.getFacilityType()));
        exist.setRegionCode(dto.getRegionCode().trim());
        exist.setRegionName(resolveRegionName(dto.getRegionCode(), dto.getRegionName()));
        exist.setLocation(trimToNull(dto.getLocation()));
        exist.setBuildYear(dto.getBuildYear());
        exist.setQuantity(dto.getQuantity());
        exist.setUnit(trimToNull(dto.getUnit()));
        exist.setRemark(trimToNull(dto.getRemark()));
        facilityMapper.updateById(exist);

        // 保存附件（先删除旧的再插入新的，简化处理）
        saveAttachments(exist.getId(), dto.getAttachments(), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BaseFacility exist = facilityMapper.selectById(id);
        if (exist == null) {
            throw new BizException("设施不存在或已被删除");
        }
        // 删除附件
        attachmentMapper.delete(new LambdaQueryWrapper<BaseFacilityAttachment>()
                .eq(BaseFacilityAttachment::getFacilityId, id));
        // 删除关联关系
        facilityProjectMapper.delete(new LambdaQueryWrapper<BaseFacilityProject>()
                .eq(BaseFacilityProject::getFacilityId, id));
        // 删除设施
        facilityMapper.deleteById(id);
    }

    /**
     * 保存附件列表
     */
    private void saveAttachments(Long facilityId, List<FacilityAttachmentSaveDTO> attachments, boolean clearFirst) {
        if (clearFirst) {
            attachmentMapper.delete(new LambdaQueryWrapper<BaseFacilityAttachment>()
                    .eq(BaseFacilityAttachment::getFacilityId, facilityId));
        }
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (FacilityAttachmentSaveDTO att : attachments) {
            BaseFacilityAttachment entity = new BaseFacilityAttachment();
            entity.setFacilityId(facilityId);
            entity.setAttachmentType(trimToNull(att.getAttachmentType()));
            entity.setFileName(trimToNull(att.getFileName()));
            entity.setFilePath(trimToNull(att.getFilePath()));
            entity.setFileHash(trimToNull(att.getFileHash()));
            entity.setFileSize(att.getFileSize());
            entity.setRemark(trimToNull(att.getRemark()));
            attachmentMapper.insert(entity);
        }
    }

    /**
     * 根据区划代码解析行政村名称
     */
    private String resolveRegionName(String regionCode, String regionName) {
        if (isNotBlank(regionName)) {
            return regionName.trim();
        }
        if (!isNotBlank(regionCode)) {
            return null;
        }
        SysRegion region = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, regionCode.trim()));
        return region != null ? region.getName() : null;
    }

    /**
     * 收集区划代码及其所有下级区划代码
     */
    private List<String> collectRegionCodes(String nodeCode) {
        if (!isNotBlank(nodeCode)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        result.add(nodeCode.trim());
        // 查询所有父级是nodeCode或下级的区划
        List<SysRegion> allChildren = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .likeRight(SysRegion::getCode, nodeCode.trim())
                .ne(SysRegion::getCode, nodeCode.trim()));
        for (SysRegion child : allChildren) {
            result.add(child.getCode());
        }
        return result;
    }

    /**
     * 校验设施编号可用性
     */
    private void checkCodeAvailable(String facilityCode) {
        String trimmed = facilityCode.trim();
        long count = facilityMapper.countActiveByCode(trimmed);
        if (count > 0) {
            throw new BizException("设施编号「" + trimmed + "」已存在，请更换编号");
        }
    }

    /**
     * 加载关联项目列表
     */
    private List<ProjectVO> loadRelatedProjects(Long facilityId) {
        List<BaseFacilityProject> relations = facilityProjectMapper.selectList(
                new LambdaQueryWrapper<BaseFacilityProject>()
                        .eq(BaseFacilityProject::getFacilityId, facilityId));
        if (relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> projectIds = relations.stream()
                .map(BaseFacilityProject::getProjectId)
                .collect(Collectors.toList());
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        // 用Map保持顺序
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));
        List<ProjectVO> result = new ArrayList<>();
        for (Long pid : projectIds) {
            Project p = projectMap.get(pid);
            if (p != null) {
                result.add(toProjectVO(p));
            }
        }
        return result;
    }

    private FacilityAttachmentVO toAttachmentVO(BaseFacilityAttachment att) {
        FacilityAttachmentVO vo = new FacilityAttachmentVO();
        vo.setId(att.getId());
        vo.setFacilityId(att.getFacilityId());
        vo.setAttachmentType(att.getAttachmentType());
        vo.setFileName(att.getFileName());
        vo.setFilePath(att.getFilePath());
        vo.setFileSize(att.getFileSize());
        vo.setRemark(att.getRemark());
        vo.setCreateTime(att.getCreateTime());
        return vo;
    }

    private ProjectVO toProjectVO(Project p) {
        ProjectVO vo = new ProjectVO();
        vo.setId(p.getId());
        vo.setProjectCode(p.getProjectCode());
        vo.setProjectName(p.getProjectName());
        vo.setProjectType(p.getProjectType());
        vo.setProgress(p.getProgress());
        vo.setBudget(p.getBudget());
        vo.setStatus(p.getStatus());
        vo.setStartYear(p.getStartYear());
        vo.setRegionCode(p.getRegionCode());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
        return vo;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
