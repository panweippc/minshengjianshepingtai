package com.msjs.project.service;

import com.msjs.project.common.PageVO;
import com.msjs.project.entity.SysOperLog;

import java.util.List;

/**
 * 操作日志服务
 */
public interface SysOperLogService {

    /** 分页查询（method 为 HTTP 请求方式，用于按 查询/新增/修改/删除 过滤） */
    PageVO<SysOperLog> page(long pageNum, long pageSize, String username, String module, String method);

    /** 写入日志（由切面调用） */
    void save(SysOperLog log);

    /** 批量删除 */
    void deleteBatch(List<Long> ids);

    /** 清空日志 */
    void clear();
}
