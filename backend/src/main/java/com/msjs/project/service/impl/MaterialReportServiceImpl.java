package com.msjs.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.msjs.project.common.BizException;
import com.msjs.project.common.LoginUser;
import com.msjs.project.common.PageVO;
import com.msjs.project.common.UserContext;
import com.msjs.project.dto.MaterialDamageSaveDTO;
import com.msjs.project.dto.MaterialHandoverSaveDTO;
import com.msjs.project.dto.MaterialInventorySaveDTO;
import com.msjs.project.dto.MaterialReportItemSaveDTO;
import com.msjs.project.entity.MaterialReport;
import com.msjs.project.entity.MaterialReportItem;
import com.msjs.project.mapper.MaterialReportItemMapper;
import com.msjs.project.mapper.MaterialReportMapper;
import com.msjs.project.service.MaterialReportService;
import com.msjs.project.vo.MaterialReportDetailVO;
import com.msjs.project.vo.MaterialReportItemVO;
import com.msjs.project.vo.MaterialReportVO;
import com.msjs.project.vo.MaterialStockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 物资统计报表服务实现
 * 盘点表/交接表/损毁表共用主表和明细表，通过 report_type 区分
 * 主表与明细表同事务写入，避免无明细的脏单或明细孤儿数据
 */
@Service
@RequiredArgsConstructor
public class MaterialReportServiceImpl implements MaterialReportService {

    private final MaterialReportMapper reportMapper;
    private final MaterialReportItemMapper reportItemMapper;

    @Override
    public PageVO<MaterialReportVO> page(long pageNum, long pageSize, String reportName, String reportType,
                                          LocalDate startTime, LocalDate endTime) {
        Page<MaterialReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaterialReport> wrapper = new LambdaQueryWrapper<MaterialReport>()
                .like(StringUtils.hasText(reportName), MaterialReport::getReportName, reportName)
                .eq(StringUtils.hasText(reportType), MaterialReport::getReportType, reportType)
                .ge(startTime != null, MaterialReport::getReportDate, startTime)
                .le(endTime != null, MaterialReport::getReportDate, endTime)
                .orderByDesc(MaterialReport::getReportDate)
                .orderByDesc(MaterialReport::getCreateTime)
                .orderByAsc(MaterialReport::getId);
        IPage<MaterialReport> reportPage = reportMapper.selectPage(page, wrapper);

        List<MaterialReportVO> records = reportPage.getRecords().stream()
                .map(this::toReportVO)
                .toList();

        PageVO<MaterialReportVO> result = new PageVO<>();
        result.setRecords(records);
        result.setTotal(reportPage.getTotal());
        result.setPageNum(reportPage.getCurrent());
        result.setPageSize(reportPage.getSize());
        return result;
    }

    @Override
    public MaterialReportDetailVO detail(Long id) {
        MaterialReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("统计报表不存在或已删除");
        }
        List<MaterialReportItem> items = reportItemMapper.selectList(new LambdaQueryWrapper<MaterialReportItem>()
                .eq(MaterialReportItem::getReportId, id)
                .orderByAsc(MaterialReportItem::getId));

        MaterialReportDetailVO vo = new MaterialReportDetailVO();
        BeanUtils.copyProperties(report, vo);
        vo.setItems(items.stream().map(this::toItemVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addInventory(MaterialInventorySaveDTO dto) {
        MaterialReport report = buildReportEntity(dto.getReportName(), "INVENTORY", dto.getReportDate(),
                dto.getHandler(), null, dto.getAuditor(), dto.getDepartment());
        report.setRemark(dto.getRemark());
        reportMapper.insert(report);
        saveItems(report.getId(), "INVENTORY", dto.getItems());
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editInventory(MaterialInventorySaveDTO dto) {
        MaterialReport exist = requireReport(dto.getId());
        updateReportEntity(exist, dto.getReportName(), "INVENTORY", dto.getReportDate(),
                dto.getHandler(), null, dto.getAuditor(), dto.getDepartment());
        exist.setRemark(dto.getRemark());
        updateReport(exist);
        replaceItems(exist.getId(), "INVENTORY", dto.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHandover(MaterialHandoverSaveDTO dto) {
        MaterialReport report = buildReportEntity(dto.getReportName(), "HANDOVER", dto.getReportDate(),
                dto.getHandler(), dto.getHandoverPerson(), dto.getAuditor(), dto.getDepartment());
        report.setRemark(dto.getRemark());
        reportMapper.insert(report);
        saveItems(report.getId(), "HANDOVER", dto.getItems());
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editHandover(MaterialHandoverSaveDTO dto) {
        MaterialReport exist = requireReport(dto.getId());
        updateReportEntity(exist, dto.getReportName(), "HANDOVER", dto.getReportDate(),
                dto.getHandler(), dto.getHandoverPerson(), dto.getAuditor(), dto.getDepartment());
        exist.setRemark(dto.getRemark());
        updateReport(exist);
        replaceItems(exist.getId(), "HANDOVER", dto.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDamage(MaterialDamageSaveDTO dto) {
        MaterialReport report = buildReportEntity(dto.getReportName(), "DAMAGE", dto.getReportDate(),
                dto.getHandler(), null, dto.getAuditor(), dto.getDepartment());
        report.setRemark(dto.getRemark());
        reportMapper.insert(report);
        saveItems(report.getId(), "DAMAGE", dto.getItems());
        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editDamage(MaterialDamageSaveDTO dto) {
        MaterialReport exist = requireReport(dto.getId());
        updateReportEntity(exist, dto.getReportName(), "DAMAGE", dto.getReportDate(),
                dto.getHandler(), null, dto.getAuditor(), dto.getDepartment());
        exist.setRemark(dto.getRemark());
        updateReport(exist);
        replaceItems(exist.getId(), "DAMAGE", dto.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialReport exist = reportMapper.selectById(id);
        if (exist == null) {
            throw new BizException("统计报表不存在或已删除");
        }
        // 先逻辑删除明细，再删除主表
        reportItemMapper.delete(new LambdaQueryWrapper<MaterialReportItem>()
                .eq(MaterialReportItem::getReportId, id));
        reportMapper.deleteById(id);
    }

    // ==================== 私有辅助方法 ====================

    @Override
    public List<MaterialStockVO> listStock(String itemName, String warehouseName, String itemCategory) {
        return reportMapper.selectStockList(itemName, warehouseName, itemCategory);
    }

    /** 构建主表实体（新增时使用） */
    private MaterialReport buildReportEntity(String reportName, String reportType, LocalDate reportDate,
                                             String handler, String handoverPerson, String auditor, String department) {
        MaterialReport report = new MaterialReport();
        report.setReportName(reportName);
        report.setReportType(reportType);
        report.setReportDate(reportDate != null ? reportDate : LocalDate.now());
        // 经办人未传时取当前登录人
        report.setHandler(StringUtils.hasText(handler) ? handler : currentUserName());
        report.setHandoverPerson(handoverPerson);
        report.setAuditor(auditor);
        report.setDepartment(department);
        return report;
    }

    /** 更新主表实体（编辑时使用） */
    private void updateReportEntity(MaterialReport report, String reportName, String reportType, LocalDate reportDate,
                                    String handler, String handoverPerson, String auditor, String department) {
        report.setReportName(reportName);
        report.setReportType(reportType);
        // 编辑时允许清空审核日期，空值以 null 落库
        report.setReportDate(reportDate);
        // 编辑时允许清空经办人，空值以 null 落库
        report.setHandler(StringUtils.hasText(handler) ? handler : null);
        report.setHandoverPerson(handoverPerson);
        report.setAuditor(auditor);
        report.setDepartment(department);
    }

    /**
     * 显式更新主表（编辑时使用）
     * MyBatis-Plus 默认忽略 null 字段，会导致编辑中删除的人员、部门、备注信息仍然保留，
     * 因此这里统一使用 UpdateWrapper 逐字段 set，保证清空操作能够落库
     */
    private void updateReport(MaterialReport report) {
        reportMapper.update(null, new LambdaUpdateWrapper<MaterialReport>()
                .eq(MaterialReport::getId, report.getId())
                .set(MaterialReport::getReportName, report.getReportName())
                .set(MaterialReport::getReportType, report.getReportType())
                .set(MaterialReport::getReportDate, report.getReportDate())
                .set(MaterialReport::getHandler, report.getHandler())
                .set(MaterialReport::getHandoverPerson, report.getHandoverPerson())
                .set(MaterialReport::getAuditor, report.getAuditor())
                .set(MaterialReport::getDepartment, report.getDepartment())
                .set(MaterialReport::getRemark, report.getRemark())
                .set(MaterialReport::getUpdateTime, LocalDateTime.now()));
    }

    /** 校验报表存在性并返回 */
    private MaterialReport requireReport(Long id) {
        if (id == null) {
            throw new BizException("请选择要编辑的报表");
        }
        MaterialReport exist = reportMapper.selectById(id);
        if (exist == null) {
            throw new BizException("统计报表不存在或已删除");
        }
        return exist;
    }

    /** 批量写入明细 */
    private void saveItems(Long reportId, String reportType, List<MaterialReportItemSaveDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        for (MaterialReportItemSaveDTO dto : dtos) {
            MaterialReportItem item = new MaterialReportItem();
            item.setId(null);
            item.setReportId(reportId);
            item.setWarehouseName(dto.getWarehouseName());
            item.setItemName(dto.getItemName());
            item.setUnit(dto.getUnit());
            item.setBookQty(Objects.requireNonNullElse(dto.getBookQty(), 0));
            item.setActualQty(dto.getActualQty());
            // 盘点表/交接表自动计算盘盈数量，损毁表不计算
            if ("INVENTORY".equals(reportType) || "HANDOVER".equals(reportType)) {
                int book = Objects.requireNonNullElse(dto.getBookQty(), 0);
                int actual = Objects.requireNonNullElse(dto.getActualQty(), 0);
                item.setProfitQty(actual - book);
            }
            item.setHandler(dto.getHandler());
            item.setRemark(dto.getRemark());
            reportItemMapper.insert(item);
        }
    }

    /** 整体替换明细（编辑时先删后插） */
    private void replaceItems(Long reportId, String reportType, List<MaterialReportItemSaveDTO> dtos) {
        reportItemMapper.delete(new LambdaQueryWrapper<MaterialReportItem>()
                .eq(MaterialReportItem::getReportId, reportId));
        saveItems(reportId, reportType, dtos);
    }

    /** 当前登录人姓名（无姓名时取账号） */
    private String currentUserName() {
        LoginUser user = UserContext.get();
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private MaterialReportVO toReportVO(MaterialReport report) {
        MaterialReportVO vo = new MaterialReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }

    private MaterialReportItemVO toItemVO(MaterialReportItem item) {
        MaterialReportItemVO vo = new MaterialReportItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}
