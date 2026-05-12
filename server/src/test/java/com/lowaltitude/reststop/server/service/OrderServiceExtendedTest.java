package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OrderServiceExtendedTest {

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
    public void shouldRejectNonPilotCreatingOrder() {
        SessionUser enterprise = new SessionUser(2L, "enterprise_demo", RoleType.ENTERPRISE, "李企业");
        Assertions.assertThrows(BizException.class, () -> orderService.createOrder(
                enterprise,
                new ApiDtos.OrderCreateRequest(101L)
        ));
    }

    @Test
    public void shouldRejectPayForNonOwner() {
        SessionUser otherPilot = new SessionUser(99L, "other_pilot", RoleType.PILOT, "其他飞手");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setPilotId(1L);
        order.setStatus("PENDING_PAYMENT");
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);

        Assertions.assertThrows(BizException.class, () -> orderService.payOrder(
                otherPilot,
                new ApiDtos.PaymentRequest(501L, "ALIPAY")
        ));
    }

    @Test
    public void shouldRejectPayForAlreadyPaidOrder() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setPilotId(1L);
        order.setStatus("PAID");
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);

        Assertions.assertThrows(BizException.class, () -> orderService.payOrder(
                pilot,
                new ApiDtos.PaymentRequest(501L, "ALIPAY")
        ));
    }

    @Test
    public void shouldRejectPayForNonExistentOrder() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        Mockito.when(bizOrderMapper.selectById(999L)).thenReturn(null);

        Assertions.assertThrows(BizException.class, () -> orderService.payOrder(
                pilot,
                new ApiDtos.PaymentRequest(999L, "ALIPAY")
        ));
    }

    @Test
    public void shouldRejectDetailForUnauthorizedUser() {
        SessionUser otherPilot = new SessionUser(99L, "other_pilot", RoleType.PILOT, "其他飞手");
        BizOrderEntity order = new BizOrderEntity();
        order.setId(501L);
        order.setPilotId(1L);
        order.setEnterpriseId(2L);
        Mockito.when(bizOrderMapper.selectById(501L)).thenReturn(order);

        Assertions.assertThrows(BizException.class, () -> orderService.getOrderDetail(otherPilot, 501L));
    }
}
