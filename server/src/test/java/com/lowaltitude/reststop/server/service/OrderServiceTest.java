package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OrderServiceTest {

    private OrderService orderService;
    private BizOrderMapper bizOrderMapper;
    private PaymentOrderMapper paymentOrderMapper;
    private TaskService taskService;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        bizOrderMapper = Mockito.mock(BizOrderMapper.class);
        paymentOrderMapper = Mockito.mock(PaymentOrderMapper.class);
        taskService = Mockito.mock(TaskService.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        orderService = new OrderService(bizOrderMapper, paymentOrderMapper, taskService, auditLogService);
    }

    @Test
    public void shouldCreateOrderForPilot() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        TaskEntity task = new TaskEntity();
        task.setId(101L);
        task.setEnterpriseId(2L);
        task.setBudget(BigDecimal.valueOf(800));
        Mockito.when(taskService.getTaskById(101L)).thenReturn(task);
        Mockito.when(taskService.getUserById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));
        Mockito.when(taskService.getUserById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));
        Mockito.doAnswer(invocation -> {
            BizOrderEntity order = invocation.getArgument(0);
            order.setId(501L);
            return 1;
        }).when(bizOrderMapper).insert(ArgumentMatchers.any(BizOrderEntity.class));

        ApiDtos.OrderView order = orderService.createOrder(pilot, new ApiDtos.OrderCreateRequest(101L));

        Assertions.assertEquals(501L, order.id());
        Assertions.assertEquals("PENDING_PAYMENT", order.status());
    }

    @Test
    public void shouldPayOrder() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PENDING_PAYMENT");
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);
        Mockito.doAnswer(invocation -> {
            PaymentOrderEntity payment = invocation.getArgument(0);
            payment.setTradeNo("PAY123");
            return 1;
        }).when(paymentOrderMapper).insert(ArgumentMatchers.any(PaymentOrderEntity.class));

        ApiDtos.PaymentResult result = orderService.payOrder(pilot, new ApiDtos.PaymentRequest(501L, "ALIPAY"));

        Assertions.assertEquals("PAID", result.status());
        Assertions.assertEquals("PAY123", result.tradeNo());
    }

    @Test
    public void shouldListOrdersForPilot() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));

        List<ApiDtos.OrderView> orders = orderService.listOrders(pilot);

        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals(501L, orders.get(0).id());
    }

    @Test
    public void shouldGetOrderDetail() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
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
        task.setLocation("重庆");
        Mockito.when(taskService.getTaskById(101L)).thenReturn(task);
        Mockito.when(taskService.getUserById(1L)).thenReturn(buildUser(1L, "pilot_demo", "PILOT"));
        Mockito.when(taskService.getUserById(2L)).thenReturn(buildUser(2L, "enterprise_demo", "ENTERPRISE"));

        ApiDtos.OrderDetailView detail = orderService.getOrderDetail(pilot, 501L);

        Assertions.assertEquals(501L, detail.id());
        Assertions.assertEquals("巡检任务", detail.taskTitle());
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
