package com.carepilot.agentaccount.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.dto.ResetPasswordRequest;
import com.carepilot.agentaccount.dto.UpdateAgentAccountRequest;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.model.OperationLog;
import com.carepilot.agentaccount.repository.AgentAccountRepository;


@Service
public class AgentAccountService {


    private static final String DEFAULT_ROLE =
            "CUSTOMER_SERVICE";


    private static final String DEFAULT_STATUS =
            "ENABLED";


    private final AgentAccountRepository repository;


    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();



    public AgentAccountService(
            AgentAccountRepository repository
    ){

        this.repository = repository;

    }



    /**
     * 创建客服账号
     */
    public AgentAccount createAccount(
            CreateAgentAccountRequest request
    ){


        String name =
                request.getName().trim();


        String phone =
                request.getPhone().trim();


        String email =
                normalizeEmail(
                        request.getEmail()
                );


        String password =
                request.getInitialPassword();



        if(repository.findByPhone(phone).isPresent()){

            throw new IllegalArgumentException(
                    "手机号已存在"
            );

        }



        if(email != null &&
                repository.findByEmail(email).isPresent()){


            throw new IllegalArgumentException(
                    "邮箱已存在"
            );

        }



        AgentAccount account =
                new AgentAccount();



        account.setName(name);

        account.setPhone(phone);

        account.setEmail(email);


        account.setPasswordHash(
                passwordEncoder.encode(password)
        );


        account.setRole(
                DEFAULT_ROLE
        );


        account.setStatus(
                DEFAULT_STATUS
        );



        AgentAccount saved =
                repository.save(account);



        repository.saveOperationLog(
                saved.getUserId(),
                "CREATE",
                "创建客服人员账号"
        );



        return saved;

    }





    /**
     * 查询用户列表
     */
    public List<AgentAccount> listAccounts(
            String keyword,
            String role,
            String status,
            int page,
            int pageSize
    ){

        return repository.findAll(
                keyword,
                role,
                status,
                (page - 1) * pageSize,
                pageSize
        );

    }





    /**
     * 查询数量
     */
    public long countAccounts(
            String keyword,
            String role,
            String status
    ){

        return repository.countAll(
                keyword,
                role,
                status
        );

    }





    /**
     * 用户统计
     */
    public Map<String,Object> getStatistics(){


        Map<String,Object> result =
                new HashMap<>();


        long totalUsers =
                repository.countAll(
                        null,
                        null,
                        null
                );

        long totalLastMonth = repository.countBeforeMonth();
        
        long customerLastMonth = repository.countRoleBeforeMonth("CUSTOMER_SERVICE");
        
        long adminLastMonth = repository.countRoleBeforeMonth("SYSTEM_ADMIN");
        
        long disabledLastMonth = repository.countStatusBeforeMonth("DISABLED");

        long customerServiceCount =
                repository.countAll(
                        null,
                        "CUSTOMER_SERVICE",
                        null
                );


        long adminCount =
                repository.countAll(
                        null,
                        "SYSTEM_ADMIN",
                        null
                );


        long disabledCount =
                repository.countAll(
                        null,
                        null,
                        "DISABLED"
                );

        // 上个月数量
        long lastMonthTotal =
                repository.countBeforeMonth();

        long lastMonthCustomer =
                repository.countRoleBeforeMonth("CUSTOMER_SERVICE");

        long lastMonthAdmin =
                repository.countRoleBeforeMonth("SYSTEM_ADMIN");

        long lastMonthDisabled =
                repository.countStatusBeforeMonth("DISABLED");


        // 增长数量
        result.put(
                "totalUsersTrend",
                totalUsers - lastMonthTotal
        );

        result.put(
                "customerServiceTrend",
                customerServiceCount - lastMonthCustomer
        );

        result.put(
                "adminTrend",
                adminCount - lastMonthAdmin
        );

        result.put(
                "disabledTrend",
                disabledCount - lastMonthDisabled
        );

        result.put(
                "totalUsers",
                totalUsers
        );

        result.put(
                "totalUsersTrend",
                totalUsers-totalLastMonth);
                
        result.put(
                "customerServiceTrend",
                customerServiceCount-customerLastMonth);
                
        result.put(
                "adminTrend",
                adminCount-adminLastMonth);
                
        result.put(
                "disabledTrend",
                disabledCount-disabledLastMonth);

        result.put(
                "customerServiceCount",
                customerServiceCount
        );


        result.put(
                "adminCount",
                adminCount
        );


        result.put(
                "disabledCount",
                disabledCount
        );


        return result;

    }





    /**
     * 编辑用户
     */
    public AgentAccount updateAccount(
            Long userId,
            UpdateAgentAccountRequest request
    ){


        getRequired(userId);


        String email =
                normalizeEmail(
                        request.getEmail()
                );


        validateEmailUnique(
                email,
                userId
        );



        AgentAccount updated =
                repository.updateProfile(
                        userId,
                        request.getName().trim(),
                        email
                );



        repository.saveOperationLog(
                userId,
                "UPDATE_PROFILE",
                "修改用户姓名或邮箱"
        );



        return updated;

    }





    /**
     * 修改状态
     */
    public AgentAccount updateStatus(
            Long userId,
            String status
    ){


        getRequired(userId);



        repository.updateStatus(
                userId,
                status
        );



        repository.saveOperationLog(
                userId,
                "UPDATE_STATUS",
                "账号状态修改为:" + status
        );



        return getRequired(userId);

    }





    /**
     * 重置密码
     */
    public void resetPassword(
            Long userId,
            ResetPasswordRequest request
    ){


        AgentAccount account =
                getRequired(userId);



        if(request.getInitialPassword()
                .equals(account.getPhone())){


            throw new IllegalArgumentException(
                    "初始密码不能与手机号一致"
            );

        }



        repository.updatePassword(
                userId,
                passwordEncoder.encode(
                        request.getInitialPassword()
                )
        );



        repository.saveOperationLog(
                userId,
                "RESET_PASSWORD",
                "重置账号密码"
        );

    }





    /**
     * 查询操作日志
     */
    public List<OperationLog> listOperationLogs(
            Long userId
    ){

        getRequired(userId);


        return repository.findOperationLogs(
                userId
        );

    }







    private AgentAccount getRequired(
            Long userId
    ){

        return repository.findById(userId)
                .orElseThrow(
                        () ->
                        new IllegalArgumentException(
                                "用户不存在"
                        )
                );

    }






    private void validateEmailUnique(
            String email,
            Long excludedId
    ){


        if(email == null){

            return;

        }



        repository.findByEmail(email)
                .filter(
                        a ->
                        excludedId == null ||
                        !excludedId.equals(
                                a.getUserId()
                        )
                )
                .ifPresent(
                        a ->
                        {
                            throw new IllegalArgumentException(
                                    "邮箱已存在"
                            );
                        }
                );

    }





    private String normalizeEmail(
            String email
    ){

        if(email == null ||
                email.isBlank()){

            return null;

        }


        return email.trim()
                .toLowerCase();

    }


}