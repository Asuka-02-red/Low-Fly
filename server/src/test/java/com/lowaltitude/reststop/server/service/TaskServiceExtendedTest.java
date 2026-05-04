package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class TaskServiceExtendedTest {

    private TaskService taskService;
    private TaskMapper taskMapper;
    private UserAccountMapper userAccountMapper;
    private NoFlyZoneMapper noFlyZoneMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        taskMapper = Mockito.mock(TaskMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        noFlyZoneMapper = Mockito.mock(NoFlyZoneMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        taskService = new TaskService(taskMapper, userAccountMapper, noFlyZoneMapper, auditLogService);
    }

    @Test
    public void shouldListTasksForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise_demo", "ENTERPRISE")));

        List<ApiDtos.TaskView> tasks = taskService.listTasks(enterprise);

        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals("巡检任务", tasks.get(0).title());
    }

    @Test
    public void shouldListTasksForPilot() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise_demo", "ENTERPRISE")));

        List<ApiDtos.TaskView> tasks = taskService.listTasks(pilot);

        Assertions.assertEquals(1, tasks.size());
    }

    @Test
    public void shouldGetTaskDetail() {
        TaskEntity task = buildTask(101L, 2L, "INSPECTION", "巡检任务", "重庆", "PUBLISHED");
        task.setLatitude(BigDecimal.valueOf(29.56));
        task.setLongitude(BigDecimal.valueOf(106.55));
        task.setDescription("详细描述");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);
        Mockito.when(userAccountMapper.selectById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));

        ApiDtos.TaskDetailView detail = taskService.getTaskDetail(101L);

        Assertions.assertEquals("巡检任务", detail.title());
        Assertions.assertEquals("INSPECTION", detail.taskType());
        Assertions.assertEquals(900, detail.operationRadiusMeters());
    }

    @Test
    public void shouldThrowWhenTaskNotFound() {
        Mockito.when(taskMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(BizException.class, () -> taskService.getTaskDetail(999L));
    }

    @Test
    public void shouldListNoFlyZones() {
        NoFlyZoneEntity zone = new NoFlyZoneEntity();
        zone.setId(1L);
        zone.setName("机场禁飞区");
        zone.setZoneType("AIRPORT");
        zone.setCenterLat(BigDecimal.valueOf(29.56));
        zone.setCenterLng(BigDecimal.valueOf(106.55));
        zone.setRadius(5000);
        zone.setDescription("机场周边5公里禁飞");
        Mockito.when(noFlyZoneMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(zone));

        List<ApiDtos.NoFlyZoneView> zones = taskService.listZones();

        Assertions.assertEquals(1, zones.size());
        Assertions.assertEquals("机场禁飞区", zones.get(0).name());
        Assertions.assertEquals(5000, zones.get(0).radius());
    }

    @Test
    public void shouldSubmitFlightApplication() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        ApiDtos.FlightApplicationView result = taskService.submitFlightApplication(
                pilot,
                new ApiDtos.FlightApplicationRequest("重庆江北区", "2026-05-01 10:00", "巡检飞行")
        );

        Assertions.assertEquals("REVIEWING", result.status());
        Assertions.assertTrue(result.applicationNo().startsWith("FLY"));
    }

    @Test
    public void shouldRejectNonPilotFlightApplication() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Assertions.assertThrows(BizException.class, () -> taskService.submitFlightApplication(
                enterprise,
                new ApiDtos.FlightApplicationRequest("重庆", "2026-05-01 10:00", "巡检飞行")
        ));
    }

    @Test
    public void shouldFindUsersByIds() {
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(1L, "pilot", "PILOT")));
        Map<Long, UserAccountEntity> result = taskService.findUsersByIds(java.util.Set.of(1L));
        Assertions.assertEquals(1, result.size());
    }

    @Test
    public void shouldHandleEmptyUserIds() {
        Map<Long, UserAccountEntity> result = taskService.findUsersByIds(java.util.Set.of());
        Assertions.assertTrue(result.isEmpty());
    }

    private TaskEntity buildTask(Long id, Long enterpriseId, String type, String title, String location, String status) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setEnterpriseId(enterpriseId);
        task.setTaskType(type);
        task.setTitle(title);
        task.setDescription("测试描述");
        task.setLocation(location);
        task.setDeadline(LocalDateTime.of(2026, 5, 1, 10, 0));
        task.setLatitude(BigDecimal.valueOf(29.56));
        task.setLongitude(BigDecimal.valueOf(106.55));
        task.setBudget(BigDecimal.valueOf(800));
        task.setStatus(status);
        return task;
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
