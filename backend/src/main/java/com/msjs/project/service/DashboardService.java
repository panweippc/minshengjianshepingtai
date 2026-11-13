package com.msjs.project.service;

import com.msjs.project.vo.DashboardOverviewVO;

/**
 * 首页概览统计服务
 */
public interface DashboardService {

    /** 首页概览统计：行政村总数、农户总数、公共基础设施总数、在库物资种类数 */
    DashboardOverviewVO overview();
}
