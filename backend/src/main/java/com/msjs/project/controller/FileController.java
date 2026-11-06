package com.msjs.project.controller;

import com.msjs.project.common.Constants;
import com.msjs.project.common.Result;
import com.msjs.project.common.annotation.OperLog;
import com.msjs.project.dto.ChunkMergeDTO;
import com.msjs.project.service.FileService;
import com.msjs.project.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件接口：上传、分片续传、时效签名URL（预览/下载）
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
    public Result<FileUploadVO> upload(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型（默认 common）")
            @RequestParam(defaultValue = Constants.DEFAULT_BIZ_TYPE) String bizType) {
        return Result.ok(fileService.upload(file, bizType));
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

    @Operation(summary = "上传单个分片（写入 upload-temp 临时桶）")
    @OperLog(module = "文件管理", operation = "上传文件分片")
    @PostMapping("/chunk")
    public Result<Void> uploadChunk(
            @Parameter(description = "上传标识（仅允许字母、数字、中划线）") @RequestParam String uploadId,
            @Parameter(description = "分片序号（从 0 开始）") @RequestParam int chunkIndex,
            @Parameter(description = "分片文件") @RequestParam("chunk") MultipartFile chunk) {
        fileService.uploadChunk(uploadId, chunkIndex, chunk);
        return Result.ok();
    }

    @Operation(summary = "查询已上传分片序号（断点续传）")
    @GetMapping("/chunk/status")
    public Result<List<Integer>> chunkStatus(
            @Parameter(description = "上传标识（仅允许字母、数字、中划线）") @RequestParam String uploadId) {
        return Result.ok(fileService.chunkStatus(uploadId));
    }

    @Operation(summary = "合并分片（合并后写入正式归档桶，命中秒传则复用已有文件）")
    @OperLog(module = "文件管理", operation = "合并文件分片")
    @PostMapping("/chunk/merge")
    public Result<FileUploadVO> mergeChunks(@Valid @RequestBody ChunkMergeDTO dto) {
        return Result.ok(fileService.mergeChunks(dto.getUploadId(), dto.getTotalChunks(),
                dto.getFileName(), dto.getBizType()));
    }
}
