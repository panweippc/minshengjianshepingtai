package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.msjs.project.common.Constants;
import com.msjs.project.entity.SysRegion;
import com.msjs.project.mapper.BaseFacilityMapper;
import com.msjs.project.mapper.HouseholdMapper;
import com.msjs.project.mapper.MaterialItemMapper;
import com.msjs.project.mapper.ProjectApprovalMapper;
import com.msjs.project.mapper.SysRegionMapper;
import com.msjs.project.service.DashboardService;
import com.msjs.project.vo.DashboardOverviewVO;
import com.msjs.project.vo.ProjectProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页概览统计服务实现
 * 统计口径：行政村按区划层级=村统计；农户/公共基础设施按未删除记录统计；
 * 在库物资总数按全部物品库存数量求和；项目进度按每个立项项目当前所处生命周期阶段统计
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysRegionMapper regionMapper;
    private final HouseholdMapper householdMapper;
    private final BaseFacilityMapper facilityMapper;
    private final MaterialItemMapper materialItemMapper;
    private final ProjectApprovalMapper projectApprovalMapper;

    @Override
    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        // 行政村总数：区划层级为村（5）的节点数量
        vo.setVillageCount(regionMapper.selectCount(new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getLevel, Constants.REGION_LEVEL_VILLAGE)));
        // 农户总数：户基础信息未删除记录数
        vo.setHouseholdCount(householdMapper.selectCount(null));
        // 公共基础设施总数：设施未删除记录数
        vo.setFacilityCount(facilityMapper.selectCount(null));
        // 在库物资总数：全部物品库存数量之和
        vo.setMaterialStockTotal(materialItemMapper.sumStockQty());
        // 项目进度概览：一个项目一条记录，可同时处于多个阶段
        List<ProjectProgressVO> progressList = projectApprovalMapper.selectProjectProgress();
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
}
