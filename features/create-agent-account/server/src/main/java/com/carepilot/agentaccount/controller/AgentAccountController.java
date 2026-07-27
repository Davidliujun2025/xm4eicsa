package com.carepilot.agentaccount.controller;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.dto.ResetPasswordRequest;
import com.carepilot.agentaccount.dto.UpdateAgentAccountRequest;
import com.carepilot.agentaccount.dto.UpdateStatusRequest;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.model.OperationLog;
import com.carepilot.agentaccount.service.AgentAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/admin/customer-service-users")
@Tag(name = "客服人员账号管理")
public class AgentAccountController {


    private final AgentAccountService service;



    public AgentAccountController(
            AgentAccountService service
    ){

        this.service = service;

    }



    /**
     * 创建客服账号
     */
    @PostMapping
    @Operation(summary = "创建客服人员账号")
    public ResponseEntity<Map<String,Object>> createAccount(
            @Valid @RequestBody CreateAgentAccountRequest request
    ){

        AgentAccount account =
                service.createAccount(request);


        return ResponseEntity.ok(
                success(
                        "客服人员账号创建成功",
                        userData(account)
                )
        );

    }






    /**
     * 查询用户列表
     */
    @GetMapping
    @Operation(summary = "分页查询客服人员账号")
    public ResponseEntity<Map<String,Object>> listAccounts(

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int pageSize,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            String status

    ){


        int safePage =
                Math.max(page,1);


        int safePageSize =
                Math.min(
                        Math.max(pageSize,1),
                        100
                );



        List<Map<String,Object>> list =
                service.listAccounts(
                        keyword,
                        role,
                        status,
                        safePage,
                        safePageSize
                )
                .stream()
                .map(this::userData)
                .toList();



        Map<String,Object> data =
                new LinkedHashMap<>();


        data.put(
                "list",
                list
        );


        data.put(
                "total",
                service.countAccounts(
                        keyword,
                        role,
                        status
                )
        );


        data.put(
                "page",
                safePage
        );


        data.put(
                "pageSize",
                safePageSize
        );



        return ResponseEntity.ok(
                success(
                        "查询成功",
                        data
                )
        );

    }







    /**
     * 用户统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "查询用户统计")
    public ResponseEntity<Map<String,Object>> statistics(){


        Map<String,Object> data =
                service.getStatistics();



        return ResponseEntity.ok(
                success(
                        "查询成功",
                        data
                )
        );

    }







    /**
     * 编辑用户
     */
    @PatchMapping("/{userId}")
    @Operation(summary = "编辑客服人员姓名和邮箱")
    public ResponseEntity<Map<String,Object>> updateAccount(

            @PathVariable Long userId,

            @Valid
            @RequestBody UpdateAgentAccountRequest request

    ){


        return ResponseEntity.ok(
                success(
                        "用户信息修改成功",
                        userData(
                                service.updateAccount(
                                        userId,
                                        request
                                )
                        )
                )
        );

    }







    /**
     * 修改状态
     */
    @PatchMapping("/{userId}/status")
    @Operation(summary = "启用或禁用客服人员账号")
    public ResponseEntity<Map<String,Object>> updateStatus(

            @PathVariable Long userId,

            @Valid
            @RequestBody UpdateStatusRequest request

    ){


        return ResponseEntity.ok(
                success(
                        "账号状态修改成功",
                        userData(
                                service.updateStatus(
                                        userId,
                                        request.getStatus()
                                )
                        )
                )
        );

    }







    /**
     * 重置密码
     */
    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "重置客服人员密码")
    public ResponseEntity<Map<String,Object>> resetPassword(

            @PathVariable Long userId,

            @Valid
            @RequestBody ResetPasswordRequest request

    ){


        service.resetPassword(
                userId,
                request
        );


        return ResponseEntity.ok(
                success(
                        "密码重置成功",
                        null
                )
        );

    }







    /**
     * 查询操作日志
     */
    @GetMapping("/{userId}/operation-logs")
    @Operation(summary = "查询客服人员操作日志")
    public ResponseEntity<Map<String,Object>> operationLogs(

            @PathVariable Long userId

    ){


        List<Map<String,Object>> list =
                service.listOperationLogs(userId)
                .stream()
                .map(this::logData)
                .toList();



        Map<String,Object> data =
                new LinkedHashMap<>();


        data.put(
                "list",
                list
        );


        data.put(
                "total",
                list.size()
        );



        return ResponseEntity.ok(
                success(
                        "查询成功",
                        data
                )
        );

    }









    private Map<String,Object> userData(
            AgentAccount account
    ){

        Map<String,Object> data =
                new LinkedHashMap<>();


        data.put(
                "userId",
                account.getUserId()
        );


        data.put(
                "name",
                account.getName()
        );


        data.put(
                "phone",
                account.getPhone()
        );


        data.put(
                "email",
                account.getEmail()
        );


        data.put(
                "role",
                account.getRole()
        );


        data.put(
                "roleName",
                "SYSTEM_ADMIN".equals(account.getRole())
                ? "系统管理员"
                : "客服人员"
        );


        data.put(
                "status",
                account.getStatus()
        );


        data.put(
                "statusName",
                "ENABLED".equals(account.getStatus())
                ? "启用"
                : "禁用"
        );


        data.put(
                "createdAt",
                account.getCreatedAt()
        );


        data.put(
                "updatedAt",
                account.getUpdatedAt()
        );


        return data;

    }








    private Map<String,Object> logData(
            OperationLog log
    ){

        Map<String,Object> data =
                new LinkedHashMap<>();


        data.put(
                "logId",
                log.getLogId()
        );


        data.put(
                "userId",
                log.getUserId()
        );


        data.put(
                "action",
                log.getAction()
        );


        data.put(
                "detail",
                log.getDetail()
        );


        data.put(
                "createdAt",
                log.getCreatedAt()
        );


        return data;

    }







    private Map<String,Object> success(
            String message,
            Object data
    ){


        Map<String,Object> response =
                new LinkedHashMap<>();


        response.put(
                "code",
                "SUCCESS"
        );


        response.put(
                "message",
                message
        );


        response.put(
                "data",
                data
        );


        return response;

    }


}