package com.msjs.project.controller;

import com.msjs.project.common.Result;
import com.msjs.project.dto.ProjectApprovalSubmitDTO;
import com.msjs.project.service.ProjectApprovalService;
import com.msjs.project.vo.ProjectApprovalSubmitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 立项管理模块后端 Controller（我同事负责的立项管理前端对接这一组接口即可）。
 * 当前只暴露"提交立项"一条接口（申报审核已通过页签新建立项弹窗用它）。
 * 后续立项管理前端自己的增删改查接口由同事单独扩展这个类。
 */
@Tag(name = "立项管理")
@RestController
@RequestMapping("/api/project/approval")
@RequiredArgsConstructor
public class ProjectApprovalController {

    private final ProjectApprovalService projectApprovalService;

    /**
     * 提交立项申请。
     * 业务动作：保存立项主表 + declare + detail + attachment，
     * 同时把所选申报单下的 approved 明细 audit_status 改成 established，
     * 父申报单状态回算并维持 approved。
     */
    @Operation(summary = "提交立项申请")
    @PostMapping("/submit")
    public Result<ProjectApprovalSubmitVO> submit(@Valid @RequestBody ProjectApprovalSubmitDTO dto) {
        return Result.ok(projectApprovalService.submit(dto));
    }
}
