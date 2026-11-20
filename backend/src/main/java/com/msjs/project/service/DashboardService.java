package com.msjs.project.service;

import com.msjs.project.vo.DashboardOverviewVO;
import com.msjs.project.vo.ScreenOverviewVO;

/**
 * 首页概览统计服务
 */
public interface DashboardService {

    /** 首页概览统计：行政村总数、农户总数、公共基础设施总数、在库物资种类数 */
    DashboardOverviewVO overview();

    /** 数据大屏聚合统计：核心指标、资金趋势、项目状态、村域态势、物资与实时事件 */
    ScreenOverviewVO screen();
}
