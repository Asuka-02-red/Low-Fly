package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AdminProjectServiceTest {

    private AdminProjectService adminProjectService;
    private TaskMapper taskMapper;
    private BizOrderMapper bizOrderMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private NoFlyZoneMapper noFlyZoneMapper;
    private FeedbackTicketMapper feedbackTicketMapper;
    private UserAccountMapper userAccountMapper;

    @BeforeEach
    public void setUp() {
        taskMapper = Mockito.mock(TaskMapper.class);
        bizOrderMapper = Mockito.mock(BizOrderMapper.class);
        paymentOrderMapper = Mockito.mock(PaymentOrderMapper.class);
        noFlyZoneMapper = Mockito.mock(NoFlyZoneMapper.class);
        feedbackTicketMapper = Mockito.mock(FeedbackTicketMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        adminProjectService = new AdminProjectService(taskMapper, bizOrderMapper, paymentOrderMapper, noFlyZoneMapper, feedbackTicketMapper, userAccountMapper);
    }

    @Test
    public void shouldExposeAdminProjectsFromTasks() {
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setTaskType("INSPECTION");
        task.setTitle("长江沿线巡检");
        task.setLocation("重庆江北区");
        task.setBudget(BigDecimal.valueOf(3000));
        task.setStatus("PUBLISHED");
        task.setUpdateTime(LocalDateTime.of(2026, 4, 22, 11, 15));
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));
        Mockito.when(userAccountMapper.selectBatchIds(ArgumentMatchers.any())).thenReturn(List.of(buildUser(2L, "enterprise_demo", "ENTERPRISE")));
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of());

        List<ApiDtos.AdminProjectView> projects = adminProjectService.listAdminProjects();

        Assertions.assertEquals(1, projects.size());
        Assertions.assertEquals("执行中", projects.get(0).status());
        Assertions.assertEquals("重庆江北区", projects.get(0).region());
    }

    @Test
    public void shouldListAdminOrders() {
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setTitle("巡检任务");
        task.setTaskType("INSPECTION");
        Mockito.when(taskMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(task));

        List<ApiDtos.AdminOrderSummaryView> orders = adminProjectService.listAdminOrders();

        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals("巡检任务", orders.get(0).projectName());
    }

    @Test
    public void shouldGetAdminOrderDetail() {
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        order.setCreateTime(LocalDateTime.now());
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setTitle("巡检任务");
        task.setTaskType("INSPECTION");
        Mockito.when(taskMapper.selectById(101L)).thenReturn(task);

        ApiDtos.AdminOrderDetailView detail = adminProjectService.getAdminOrderDetail(501L);

        Assertions.assertEquals("巡检任务", detail.projectName());
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
