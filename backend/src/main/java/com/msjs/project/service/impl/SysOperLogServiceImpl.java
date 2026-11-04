package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.PageVO;
import com.msjs.project.entity.SysOperLog;
import com.msjs.project.mapper.SysOperLogMapper;
import com.msjs.project.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysOperLogServiceImpl implements SysOperLogService {

    private final SysOperLogMapper operLogMapper;

    @Override
    public PageVO<SysOperLog> page(long pageNum, long pageSize, String username, String module) {
        Page<SysOperLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .like(username != null && !username.isBlank(), SysOperLog::getUsername, username)
                .like(module != null && !module.isBlank(), SysOperLog::getModule, module)
                .orderByDesc(SysOperLog::getOperTime);
        return PageVO.of(operLogMapper.selectPage(page, wrapper));
    }

    @Override
    public void save(SysOperLog log) {
        operLogMapper.insert(log);
    }

    @Override
    public void deleteBatch(List<Long> ids) {
        operLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void clear() {
        operLogMapper.delete(null);
    }
}
