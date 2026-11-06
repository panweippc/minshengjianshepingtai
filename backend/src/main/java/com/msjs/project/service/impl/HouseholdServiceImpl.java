package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.HouseholdSaveDTO;
import com.msjs.project.entity.Household;
import com.msjs.project.entity.HouseholdAttachment;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.entity.SysUser;
import com.msjs.project.mapper.HouseholdAttachmentMapper;
import com.msjs.project.mapper.HouseholdMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.mapper.SysUserMapper;
import com.msjs.project.service.HouseholdService;
import com.msjs.project.vo.RegionHouseholdCountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 户基础信息服务实现
 * 户号由用户新增时手工录入，全局唯一，新增时校验重复；编辑时不允许修改户号
 */
@Service
@RequiredArgsConstructor
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdMapper householdMapper;
    private final HouseholdAttachmentMapper attachmentMapper;
    private final SysRegionMapper regionMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageVO<Household> page(long pageNum, long pageSize, String headName, String headIdCard, String nodeCode) {
        Page<Household> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Household> wrapper = new LambdaQueryWrapper<Household>()
                // 树节点筛选：所选节点及其所有下级（行政村编码前缀匹配）
                .and(isNotBlank(nodeCode), w -> {
                    // nodeCode 是村/组编码时，直接精确匹配；是乡镇编码时，匹配其下所有村（前缀匹配）
                    SysRegion region = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                            .eq(SysRegion::getCode, nodeCode.trim()));
                    if (region != null && region.getLevel() >= 5) {
                        // 已经是行政村或组，直接精准匹配 regionCode
                        w.eq(Household::getRegionCode, nodeCode.trim());
                    } else {
                        // 乡镇或更上级，匹配下属所有村（regionCode 以 nodeCode 开头且长度更长）
                        w.likeRight(Household::getRegionCode, nodeCode.trim());
                    }
                })
                .like(isNotBlank(headName), Household::getHeadName, headName)
                // 注意：.eq(condition, col, val) 的 val 在 condition 判断之前就会被求值，
                // 这里不能直接写 headIdCard.trim()，否则 headIdCard 为 null 时会 NPE
                .eq(isNotBlank(headIdCard), Household::getHeadIdCard, headIdCard == null ? null : headIdCard.trim())
                .orderByDesc(Household::getCreateTime);
        return PageVO.of(householdMapper.selectPage(page, wrapper));
    }

    @Override
    public List<RegionHouseholdCountVO> regionCounts() {
        // 按所属行政村分组统计农户数量，逻辑删除由 MyBatis-Plus 自动过滤
        QueryWrapper<Household> wrapper = new QueryWrapper<Household>()
                .select("region_code", "COUNT(*) AS cnt")
                .groupBy("region_code");
        List<Map<String, Object>> rows = householdMapper.selectMaps(wrapper);
        List<RegionHouseholdCountVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object code = row.get("region_code");
            if (code == null) {
                continue;
            }
            RegionHouseholdCountVO vo = new RegionHouseholdCountVO();
            vo.setRegionCode(String.valueOf(code));
            Object cnt = row.get("cnt");
            vo.setCount(cnt instanceof Number number ? number.longValue() : 0L);
            result.add(vo);
        }
        return result;
    }

    @Override
    public Household detail(Long id) {
        Household household = householdMapper.selectById(id);
        if (household == null) {
            throw new BizException("户基础信息不存在或已被删除");
        }
        return household;
    }

    @Override
    public List<HouseholdAttachment> listAttachments(Long householdId) {
        return attachmentMapper.selectList(new LambdaQueryWrapper<HouseholdAttachment>()
                .eq(HouseholdAttachment::getHouseholdId, householdId)
                .orderByAsc(HouseholdAttachment::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(HouseholdSaveDTO dto) {
        // 校验行政村存在
        SysRegion region = selectRegionByCode(dto.getRegionCode());
        if (region == null || region.getLevel() < 5) {
            throw new BizException("请选择有效的行政村");
        }

        // 户号由用户录入，全局唯一：先做重复校验，避免触发唯一索引异常
        String householdNo = dto.getHouseholdNo().trim();
        if (householdMapper.countByHouseholdNo(householdNo) > 0) {
            throw new BizException("户号已存在，请重新输入");
        }

        Household household = new Household();
        household.setHouseholdNo(householdNo);
        household.setRegionCode(region.getCode());
        household.setRegionName(region.getName());
        household.setHeadName(trimToNull(dto.getHeadName()));
        household.setHeadIdCard(trimToNull(dto.getHeadIdCard()));
        household.setContactPhone(trimToNull(dto.getContactPhone()));
        household.setFamilyPopulation(dto.getFamilyPopulation());
        household.setHouseArea(dto.getHouseArea());
        household.setHouseType(trimToNull(dto.getHouseType()));
        household.setHouseholdType(trimToNull(dto.getHouseholdType()));
        household.setPropertyNo(trimToNull(dto.getPropertyNo()));
        household.setArchiveTime(dto.getArchiveTime());
        household.setRemark(trimToNull(dto.getRemark()));
        household.setCreateBy(currentUser());
        household.setCreateTime(LocalDateTime.now());
        householdMapper.insert(household);

        // 附件批量保存
        saveAttachments(household.getId(), dto.getAttachments(), null);

        return household.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(HouseholdSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("缺少主键ID");
        }
        Household exist = householdMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BizException("户基础信息不存在或已被删除");
        }

        // 校验行政村存在
        SysRegion region = selectRegionByCode(dto.getRegionCode());
        if (region == null || region.getLevel() < 5) {
            throw new BizException("请选择有效的行政村");
        }

        exist.setRegionCode(region.getCode());
        exist.setRegionName(region.getName());
        exist.setHeadName(trimToNull(dto.getHeadName()));
        exist.setHeadIdCard(trimToNull(dto.getHeadIdCard()));
        exist.setContactPhone(trimToNull(dto.getContactPhone()));
        exist.setFamilyPopulation(dto.getFamilyPopulation());
        exist.setHouseArea(dto.getHouseArea());
        exist.setHouseType(trimToNull(dto.getHouseType()));
        exist.setHouseholdType(trimToNull(dto.getHouseholdType()));
        exist.setPropertyNo(trimToNull(dto.getPropertyNo()));
        exist.setArchiveTime(dto.getArchiveTime());
        exist.setRemark(trimToNull(dto.getRemark()));
        exist.setUpdateBy(currentUser());
        exist.setUpdateTime(LocalDateTime.now());
        householdMapper.updateById(exist);

        // 附件更新：传入的附件列表做差异处理
        saveAttachments(exist.getId(), dto.getAttachments(), exist.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Household exist = householdMapper.selectById(id);
        if (exist == null) {
            throw new BizException("户基础信息不存在或已被删除");
        }
        // 逻辑删除主表：deleted 写入本行ID（墓碑值），释放户号供新记录复用
        householdMapper.logicDeleteById(id);
        // 逻辑删除附件（一并清理，保持数据一致性）
        attachmentMapper.delete(new LambdaQueryWrapper<HouseholdAttachment>()
                .eq(HouseholdAttachment::getHouseholdId, id));
    }

    /**
     * 保存附件：
     * 1. 新增 householdId 关联附件，同时 householdId 为 null 时全量新增
     * 2. 编辑 householdId 不为 null 时，对比已有附件ID集合，缺失的做逻辑删除，新增的做 insert
     */
    private void saveAttachments(Long householdId, List<HouseholdSaveDTO.AttachmentItem> attachments, Long forCompare) {
        if (attachments == null || attachments.isEmpty()) {
            // 编辑场景下若传空列表，不做处理（保留已有附件）；如需清空请显式传入删除
            if (forCompare != null) {
                return;
            }
            return;
        }

        // 编辑场景：对比已有附件
        if (forCompare != null) {
            List<HouseholdAttachment> existList = attachmentMapper.selectList(new LambdaQueryWrapper<HouseholdAttachment>()
                    .eq(HouseholdAttachment::getHouseholdId, householdId));
            Set<Long> inputIds = new HashSet<>();
            for (HouseholdSaveDTO.AttachmentItem item : attachments) {
                if (item.getId() != null) {
                    inputIds.add(item.getId());
                }
            }
            // 已有附件中不在本次输入集合里的 → 逻辑删除
            for (HouseholdAttachment att : existList) {
                if (!inputIds.contains(att.getId())) {
                    attachmentMapper.deleteById(att.getId());
                }
            }
        }

        for (HouseholdSaveDTO.AttachmentItem item : attachments) {
            if (item.getId() != null && forCompare != null) {
                // 已存在的附件 → 更新
                HouseholdAttachment att = attachmentMapper.selectById(item.getId());
                if (att != null) {
                    att.setAttachmentType(trimToNull(item.getAttachmentType()));
                    att.setFileName(trimToNull(item.getFileName()));
                    att.setFileKey(trimToNull(item.getFileKey()));
                    att.setBucket(trimToNull(item.getBucket()));
                    att.setFileSize(item.getFileSize());
                    att.setRemark(trimToNull(item.getRemark()));
                    attachmentMapper.updateById(att);
                    continue;
                }
            }
            // 新增附件
            HouseholdAttachment att = new HouseholdAttachment();
            att.setHouseholdId(householdId);
            att.setAttachmentType(trimToNull(item.getAttachmentType()));
            att.setFileName(trimToNull(item.getFileName()));
            att.setFileKey(trimToNull(item.getFileKey()));
            att.setBucket(trimToNull(item.getBucket()));
            att.setFileSize(item.getFileSize());
            att.setRemark(trimToNull(item.getRemark()));
            att.setCreateTime(LocalDateTime.now());
            attachmentMapper.insert(att);
        }
    }

    private SysRegion selectRegionByCode(String code) {
        if (!isNotBlank(code)) {
            return null;
        }
        return regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, code.trim()));
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
