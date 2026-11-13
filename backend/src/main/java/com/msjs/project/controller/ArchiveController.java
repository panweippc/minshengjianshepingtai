package com.msjs.project.controller;

import com.msjs.project.common.PageVO;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.common.annotation.RequirePerm;
import com.msjs.project.dto.ArchiveEditDTO;
import com.msjs.project.service.ArchiveService;
import com.msjs.project.vo.ArchiveDetailVO;
import com.msjs.project.vo.ArchiveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 档案管理接口
 * 数据来源：验收管理归档后的项目；查看聚合事项申报、立项、资金、施工、验收、档案六页签数据
 * 档案查看、编辑、导出均写入审计日志
 */
@Tag(name = "档案管理")
@RestController
@RequestMapping("/api/project/archive")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    @Operation(summary = "档案分页查询")
    @RequirePerm("archive:list")
    @OperLog(module = "档案管理", operation = "档案查询")
    @GetMapping("/page")
    public Result<PageVO<ArchiveVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "项目名称") @RequestParam(required = false) String projectName,
            @Parameter(description = "项目类别（字典 xiang_mu_lei_bie）") @RequestParam(required = false) String projectCategory,
            @Parameter(description = "项目类型（字典 xiang_mu_lei_xing）") @RequestParam(required = false) String projectType) {
        return Result.ok(archiveService.page(pageNum, pageSize, projectName, projectCategory, projectType));
    }

    @Operation(summary = "档案查看（六页签数据聚合）")
    @RequirePerm("archive:view")
    @OperLog(module = "档案管理", operation = "档案查看")
    @GetMapping("/detail/{id}")
    public Result<ArchiveDetailVO> detail(@Parameter(description = "档案ID") @PathVariable Long id) {
        return Result.ok(archiveService.detail(id));
    }

    @Operation(summary = "编辑档案位置信息（存储位置、归档时间）")
    @RequirePerm("archive:edit")
    @OperLog(module = "档案管理", operation = "档案编辑")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ArchiveEditDTO dto) {
        archiveService.update(dto);
        return Result.ok();
    }

    @Operation(summary = "导出项目全部资料（压缩包）")
    @RequirePerm("archive:export")
    @OperLog(module = "档案管理", operation = "档案导出")
    @GetMapping("/export/{id}")
    public void export(@Parameter(description = "档案ID") @PathVariable Long id,
                       HttpServletResponse response) {
        archiveService.export(id, response);
    }
}
