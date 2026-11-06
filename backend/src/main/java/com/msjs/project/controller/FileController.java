package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.service.FileService;
import com.msjs.project.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口：上传、时效签名URL（预览/下载）
 * 所有接口需登录（AuthInterceptor 统一拦截）；预览/下载写入操作日志满足审计要求
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件（写入正式归档桶，返回对象信息）")
    @OperLog(module = "附件管理", operation = "上传附件")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    @Operation(summary = "生成时效签名URL（预览或下载）")
    @OperLog(module = "附件管理", operation = "预览/下载附件")
    @GetMapping("/presign")
    public Result<String> presign(
            @Parameter(description = "存储桶") @RequestParam String bucket,
            @Parameter(description = "对象 key") @RequestParam String fileKey,
            @Parameter(description = "原始文件名（下载时使用）") @RequestParam(required = false) String fileName,
            @Parameter(description = "是否强制下载") @RequestParam(defaultValue = "false") boolean download) {
        return Result.ok(fileService.presign(bucket, fileKey, fileName, download));
    }
}
