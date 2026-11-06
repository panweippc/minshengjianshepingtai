package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.Constants;
import com.msjs.project.common.PageVO;
import com.msjs.project.dto.RegionSaveDTO;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.BaseVillageService;
import com.msjs.project.vo.RegionTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 村委会信息服务实现
 * 区划层级：4乡镇、5村、6组；区划类型字典 qu_hua_lei_xing（1乡镇 2村 3组），类型与层级强绑定
 */
@Service
@RequiredArgsConstructor
public class BaseVillageServiceImpl implements BaseVillageService {

    private final SysRegionMapper regionMapper;

    @Override
    public List<RegionTreeVO> tree() {
        List<SysRegion> regions = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .ge(SysRegion::getLevel, Constants.REGION_MANAGE_MIN_LEVEL)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getSort)
                .orderByAsc(SysRegion::getCode));
        Map<String, RegionTreeVO> nodeMap = new LinkedHashMap<>();
        regions.forEach(region -> nodeMap.put(region.getCode(), toTreeNode(region)));
        List<RegionTreeVO> roots = new ArrayList<>();
        for (SysRegion region : regions) {
            RegionTreeVO node = nodeMap.get(region.getCode());
            RegionTreeVO parent = nodeMap.get(region.getParentCode());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    @Override
    public PageVO<SysRegion> page(long pageNum, long pageSize, String name, String secretary,
                                  String director, String nodeCode) {
        Page<SysRegion> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<SysRegion>()
                .ge(SysRegion::getLevel, Constants.REGION_MANAGE_MIN_LEVEL)
                // 选中树节点时展示该节点及其直属下级
                .and(isNotBlank(nodeCode), w -> w.eq(SysRegion::getCode, nodeCode)
                        .or().eq(SysRegion::getParentCode, nodeCode))
                .like(isNotBlank(name), SysRegion::getName, name)
                .like(isNotBlank(secretary), SysRegion::getSecretary, secretary)
                .like(isNotBlank(director), SysRegion::getDirector, director)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getSort)
                .orderByAsc(SysRegion::getCode);
        return PageVO.of(regionMapper.selectPage(page, wrapper));
    }

    @Override
    public List<RegionTreeVO> treePage(String name, String secretary, String director, String nodeCode) {
        // 全量查询管理范围内区划，后续在内存中筛选并组装为树（树形表格不分页，避免父子断裂）
        List<SysRegion> all = regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .ge(SysRegion::getLevel, Constants.REGION_MANAGE_MIN_LEVEL)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getSort)
                .orderByAsc(SysRegion::getCode));

        boolean hasFilter = isNotBlank(name) || isNotBlank(secretary) || isNotBlank(director);
        List<SysRegion> filtered;
        if (!hasFilter && isNotBlank(nodeCode)) {
            // 树节点筛选：该节点及其所有子孙
            filtered = filterSubtree(all, nodeCode);
        } else if (hasFilter) {
            // 条件筛选：匹配节点 + 所有祖先（保证路径完整）
            filtered = filterWithAncestors(all, name, secretary, director);
        } else {
            filtered = all;
        }

        Map<String, RegionTreeVO> nodeMap = new LinkedHashMap<>();
        filtered.forEach(r -> nodeMap.put(r.getCode(), toTreeNode(r)));
        List<RegionTreeVO> roots = new ArrayList<>();
        for (SysRegion region : filtered) {
            RegionTreeVO node = nodeMap.get(region.getCode());
            RegionTreeVO parent = nodeMap.get(region.getParentCode());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    /** 筛选出指定节点及其所有子孙 */
    private List<SysRegion> filterSubtree(List<SysRegion> all, String nodeCode) {
        Map<String, List<SysRegion>> childrenMap = new LinkedHashMap<>();
        for (SysRegion r : all) {
            childrenMap.computeIfAbsent(r.getParentCode(), k -> new ArrayList<>()).add(r);
        }
        List<SysRegion> result = new ArrayList<>();
        collectSubtree(all, nodeCode, childrenMap, result);
        return result;
    }

    private void collectSubtree(List<SysRegion> all, String code,
                                Map<String, List<SysRegion>> childrenMap, List<SysRegion> result) {
        all.stream().filter(r -> code.equals(r.getCode())).findFirst().ifPresent(result::add);
        List<SysRegion> children = childrenMap.getOrDefault(code, new ArrayList<>());
        for (SysRegion child : children) {
            collectSubtree(all, child.getCode(), childrenMap, result);
        }
    }

    /** 条件筛选：匹配节点 + 所有祖先 */
    private List<SysRegion> filterWithAncestors(List<SysRegion> all, String name,
                                                String secretary, String director) {
        Map<String, SysRegion> byCode = new LinkedHashMap<>();
        for (SysRegion r : all) byCode.put(r.getCode(), r);

        java.util.Set<String> keepCodes = new java.util.LinkedHashSet<>();
        for (SysRegion r : all) {
            boolean match = (!isNotBlank(name) || r.getName() != null && r.getName().contains(name))
                    && (!isNotBlank(secretary) || r.getSecretary() != null && r.getSecretary().contains(secretary))
                    && (!isNotBlank(director) || r.getDirector() != null && r.getDirector().contains(director));
            if (match) {
                keepCodes.add(r.getCode());
                // 沿 parentCode 向上收集所有祖先
                String cur = r.getParentCode();
                while (cur != null && byCode.containsKey(cur)) {
                    keepCodes.add(cur);
                    cur = byCode.get(cur).getParentCode();
                }
            }
        }
        if (keepCodes.isEmpty()) return new ArrayList<>();
        List<SysRegion> result = new ArrayList<>();
        for (SysRegion r : all) {
            if (keepCodes.contains(r.getCode())) result.add(r);
        }
        return result;
    }

    @Override
    public SysRegion detail(String code) {
        SysRegion region = selectByCode(code);
        if (region == null) {
            throw new BizException("区划不存在或已被删除");
        }
        return region;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(RegionSaveDTO dto) {
        checkCodeAvailable(dto.getCode());
        int level = resolveLevel(dto.getParentCode(), dto.getRegionType());
        SysRegion region = new SysRegion();
        region.setName(dto.getName().trim());
        region.setCode(dto.getCode().trim());
        region.setParentCode(trimToNull(dto.getParentCode()));
        region.setLevel(level);
        region.setRegionType(dto.getRegionType());
        region.setSecretary(trimToNull(dto.getSecretary()));
        region.setSecretaryPhone(trimToNull(dto.getSecretaryPhone()));
        region.setDirector(trimToNull(dto.getDirector()));
        region.setDirectorPhone(trimToNull(dto.getDirectorPhone()));
        region.setSort(dto.getSort() == null ? 0 : dto.getSort());
        try {
            regionMapper.insert(region);
        } catch (DuplicateKeyException e) {
            // 并发新增时仍可能触发唯一索引冲突，统一转为业务提示
            throw new BizException("区划编码「" + dto.getCode().trim() + "」已存在，请更换编码");
        }
        return region.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(RegionSaveDTO dto) {
        // 区划编码为业务编码，只读且作为编辑定位条件
        SysRegion exist = selectByCode(dto.getCode());
        if (exist == null) {
            throw new BizException("区划不存在或已被删除");
        }
        int level = Constants.REGION_MANAGE_MIN_LEVEL + parseRegionType(dto.getRegionType()) - 1;
        if (level != exist.getLevel()) {
            throw new BizException("区划类型与当前层级不匹配，不允许修改");
        }
        exist.setName(dto.getName().trim());
        exist.setRegionType(dto.getRegionType());
        exist.setSecretary(trimToNull(dto.getSecretary()));
        exist.setSecretaryPhone(trimToNull(dto.getSecretaryPhone()));
        exist.setDirector(trimToNull(dto.getDirector()));
        exist.setDirectorPhone(trimToNull(dto.getDirectorPhone()));
        if (dto.getSort() != null) {
            exist.setSort(dto.getSort());
        }
        regionMapper.updateById(exist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String code) {
        SysRegion exist = selectByCode(code);
        if (exist == null) {
            throw new BizException("区划不存在或已被删除");
        }
        Long childCount = regionMapper.selectCount(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getParentCode, exist.getCode()));
        if (childCount != null && childCount > 0) {
            throw new BizException("该区划存在下级区划，请先删除下级");
        }
        regionMapper.deleteById(exist.getId());
    }

    /** 解析并校验层级：有上级时层级须为上级层级+1，无上级时仅允许乡镇 */
    private int resolveLevel(String parentCode, String regionType) {
        int type = parseRegionType(regionType);
        int level = Constants.REGION_MANAGE_MIN_LEVEL + type - 1;
        if (!isNotBlank(parentCode)) {
            if (level != Constants.REGION_LEVEL_TOWN) {
                throw new BizException("新增顶级区划的类型须为乡镇");
            }
            return level;
        }
        SysRegion parent = regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, parentCode.trim()));
        if (parent == null) {
            throw new BizException("上级区划不存在");
        }
        if (parent.getLevel() >= Constants.REGION_LEVEL_GROUP) {
            throw new BizException("组下不允许再添加下级区划");
        }
        if (level != parent.getLevel() + 1) {
            throw new BizException("下级区划类型须为「" + levelLabel(parent.getLevel() + 1) + "」");
        }
        return level;
    }

    /** 按区划编码查询（业务编码全局唯一） */
    private SysRegion selectByCode(String code) {
        if (!isNotBlank(code)) {
            return null;
        }
        return regionMapper.selectOne(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getCode, code.trim()));
    }

    /** 校验编码可用性：仅有效区划占用编码；已删除区划不占用编码，允许重新新增 */
    private void checkCodeAvailable(String code) {
        String trimmed = code.trim();
        long activeCount = regionMapper.countActiveByCode(trimmed);
        if (activeCount > 0) {
            throw new BizException("区划编码「" + trimmed + "」已存在，请更换编码");
        }
        // 清理历史逻辑删除的同编码记录，释放唯一索引占用
        regionMapper.deleteDeletedByCode(trimmed);
    }

    private int parseRegionType(String regionType) {
        try {
            int type = Integer.parseInt(regionType);
            if (type >= 1 && type <= 3) {
                return type;
            }
        } catch (NumberFormatException ignored) {
            // 统一按非法类型处理
        }
        throw new BizException("区划类型不合法");
    }

    private String levelLabel(int level) {
        return switch (level) {
            case Constants.REGION_LEVEL_TOWN -> "乡镇";
            case Constants.REGION_LEVEL_VILLAGE -> "村";
            case Constants.REGION_LEVEL_GROUP -> "组";
            default -> "未知";
        };
    }

    private RegionTreeVO toTreeNode(SysRegion region) {
        RegionTreeVO vo = new RegionTreeVO();
        vo.setId(region.getId());
        vo.setCode(region.getCode());
        vo.setName(region.getName());
        vo.setParentCode(region.getParentCode());
        vo.setLevel(region.getLevel());
        vo.setRegionType(region.getRegionType());
        return vo;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
