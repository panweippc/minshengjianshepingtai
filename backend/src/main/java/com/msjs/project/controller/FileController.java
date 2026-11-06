package com.msjs.project.controller;

import com.msjs.project.common.BizException;
import com.msjs.project.common.Result;
import com.msjs.project.file.LocalFileStorageService;
import com.msjs.project.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件接口（本地磁盘存储，开发环境临时方案）
 * 安全说明：上传与预览均需登录鉴权，预览按附件类型内联返回
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final LocalFileStorageService localFileStorageService;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<FileUploadVO> upload(
            @Parameter(description = "上传文件") @RequestParam("file") MultipartFile file) {
        return Result.ok(localFileStorageService.upload(file));
    }

    @Operation(summary = "预览/下载文件（需登录鉴权）")
    @GetMapping("/preview")
    public ResponseEntity<Resource> preview(
            @Parameter(description = "文件相对路径") @RequestParam("path") String path) throws IOException {
        Path file = localFileStorageService.resolve(path);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BizException("文件不存在或已被删除");
        }
        // 根据后缀推断内容类型，图片/PDF 直接内联预览
        String contentType = Files.probeContentType(file);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        String fileName = file.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(mediaType)
                .contentLength(Files.size(file))
                .body(new FileSystemResource(file));
    }
}
