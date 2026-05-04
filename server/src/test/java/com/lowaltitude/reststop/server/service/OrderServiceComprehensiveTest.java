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

public class OrderServiceComprehensiveTest {

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
    public void shouldListOrdersForEnterprise() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));

        List<ApiDtos.OrderView> orders = orderService.listOrders(enterprise);

        Assertions.assertEquals(1, orders.size());
    }

    @Test
    public void shouldListOrdersForAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setOrderNo("ORD123");
        order.setTaskId(101L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        order.setAmount(BigDecimal.valueOf(800));
        order.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(order));

        List<ApiDtos.OrderView> orders = orderService.listOrders(admin);

        Assertions.assertEquals(1, orders.size());
    }

    @Test
    public void shouldFindLatestPayment() {
        PaymentOrderEntity payment = new PaymentOrderEntity();
        payment.setId(1L);
        payment.setBizOrderId(501L);
        payment.setTradeNo("PAY123");
        payment.setChannel("ALIPAY");
        payment.setAmount(BigDecimal.valueOf(800));
        payment.setStatus("SUCCESS");
        Mockito.when(paymentOrderMapper.selectOne(ArgumentMatchers.any())).thenReturn(payment);

        PaymentOrderEntity result = orderService.findLatestPayment(501L);

        Assertions.assertEquals("PAY123", result.getTradeNo());
    }

    @Test
    public void shouldGetOrderDetailForAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
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

        ApiDtos.OrderDetailView detail = orderService.getOrderDetail(admin, 501L);

        Assertions.assertEquals(501L, detail.id());
    }

    @Test
    public void shouldRejectNonExistentOrderDetail() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        Mockito.when(bizOrderMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class, () -> orderService.getOrderDetail(pilot, 999L));
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
