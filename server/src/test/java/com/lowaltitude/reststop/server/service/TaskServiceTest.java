package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class TaskServiceTest {

    private TaskService taskService;
    private TaskMapper taskMapper;
    private UserAccountMapper userAccountMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        taskMapper = Mockito.mock(TaskMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        NoFlyZoneMapper noFlyZoneMapper = Mockito.mock(NoFlyZoneMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
    }

    @Test
    public void shouldCreateTaskForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            TaskEntity task = invocation.getArgument(0);
            task.setId(101L);
            return 1;
        }).when(taskMapper).insert(ArgumentMatchers.any(TaskEntity.class));

        ApiDtos.TaskView task = taskService.createTask(
                enterprise,
                new ApiDtos.TaskRequest(
                        "INSPECTION",
                        "新区巡检",
                        "测试任务",
                        "重庆两江新区",
                        "2026-05-01 10:00",
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(800))
        );

        Assertions.assertEquals("REVIEWING", task.status());
        Assertions.assertEquals(101L, task.id());
        Assertions.assertEquals("2026-05-01 10:00", task.deadline());
    }

    @Test
    public void shouldUpdateAndRepublishEnterpriseTask() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setTaskType("INSPECTION");
        task.setTitle("旧任务");
        task.setDescription("旧描述");
        task.setLocation("重庆江北区");
        task.setDeadline(LocalDateTime.of(2026, 5, 1, 10, 0));
        task.setLatitude(BigDecimal.ONE);
        task.setLongitude(BigDecimal.TEN);
        task.setBudget(BigDecimal.valueOf(800));
        task.setStatus("PUBLISHED");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));

        ApiDtos.TaskView updated = taskService.updateTask(
                enterprise,
                101L,
                new ApiDtos.TaskRequest(
                        "MAPPING",
                        "新任务标题",
                        "新任务描述",
                        "重庆渝北区",
                        "2026-05-03 12:00",
                        BigDecimal.valueOf(29.56),
                        BigDecimal.valueOf(106.55),
                        BigDecimal.valueOf(3200))
        );
        ApiDtos.TaskView published = taskService.publishTask(enterprise, 101L);

        Assertions.assertEquals("新任务标题", updated.title());
        Assertions.assertEquals("2026-05-03 12:00", updated.deadline());
        Assertions.assertEquals("REVIEWING", published.status());
    }

    @Test
    public void shouldRejectPilotCreatingEnterpriseTask() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        try {
            taskService.createTask(
                    pilot,
                    new ApiDtos.TaskRequest(
                            "INSPECTION",
                            "非法任务",
                            "测试任务",
                            "重庆",
                            "2026-05-01 10:00",
                            BigDecimal.ONE,
                            BigDecimal.TEN,
                            BigDecimal.valueOf(100))
            );
            Assertions.fail("预期抛出 BizException");
        } catch (BizException ex) {
            Assertions.assertEquals(403, ex.getCode());
        }
    }

    private UserAccountEntity buildUser(Long id, String username, String role) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("{noop}demo123");
        user.setPhone("13800138000");
        user.setRole(role);
        user.setRealName("测试用户");
        user.setCompanyName("测试企业");
        return user;
    }
}
