package com.lowaltitude.reststop.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;

@Service
public class OrderService {

    private final BizOrderMapper bizOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final TaskService taskService;
    private final AuditLogService auditLogService;

    public OrderService(
            BizOrderMapper bizOrderMapper,
            PaymentOrderMapper paymentOrderMapper,
            TaskService taskService,
            AuditLogService auditLogService
    ) {
        this.bizOrderMapper = bizOrderMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.taskService = taskService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ApiDtos.OrderView createOrder(SessionUser user, ApiDtos.OrderCreateRequest request) {
        PlatformUtils.ensureRole(user, RoleType.PILOT);
        TaskEntity task = taskService.getTaskById(request.taskId());
        taskService.getUserById(user.id());
        taskService.getUserById(task.getEnterpriseId());

        BizOrderEntity order = new BizOrderEntity();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setTaskId(task.getId());
        order.setPilotId(user.id());
        order.setEnterpriseId(task.getEnterpriseId());
        order.setAmount(task.getBudget());
        order.setStatus("PENDING_PAYMENT");
        order.setVersion(0);
        bizOrderMapper.insert(order);
        audit(user, "ORDER", order.getOrderNo(), "CREATE", "taskId=" + task.getId());
        return toOrderView(order);
    }

    @Transactional
    public ApiDtos.PaymentResult payOrder(SessionUser user, ApiDtos.PaymentRequest request) {
        BizOrderEntity order = bizOrderMapper.selectById(request.orderId());
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (user.role() != RoleType.ADMIN && !Objects.equals(order.getPilotId(), user.id())) {
            throw new BizException(403, "无权支付该订单");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BizException(400, "订单状态不允许支付");
        }

        PaymentOrderEntity payment = new PaymentOrderEntity();
        payment.setBizOrderId(order.getId());
        payment.setTradeNo("PAY" + System.currentTimeMillis());
        payment.setChannel(request.channel());
        payment.setAmount(order.getAmount());
        payment.setStatus("SUCCESS");
        payment.setCallbackPayload("demo-payment-success");
        paymentOrderMapper.insert(payment);

        order.setStatus("PAID");
        bizOrderMapper.updateById(order);
        audit(user, "PAYMENT", order.getOrderNo(), "PAY_SUCCESS", "channel=" + request.channel());
        return new ApiDtos.PaymentResult(payment.getTradeNo(), order.getStatus(), order.getAmount());
    }

    public List<ApiDtos.OrderView> listOrders(SessionUser user) {
        LambdaQueryWrapper<BizOrderEntity> query = new LambdaQueryWrapper<BizOrderEntity>()
                .orderByAsc(BizOrderEntity::getId);
        if (user.role() == RoleType.PILOT) {
            query.eq(BizOrderEntity::getPilotId, user.id());
        } else if (user.role() == RoleType.ENTERPRISE) {
            query.eq(BizOrderEntity::getEnterpriseId, user.id());
        }
        return bizOrderMapper.selectList(query).stream()
                .map(this::toOrderView)
                .toList();
    }

    public ApiDtos.OrderDetailView getOrderDetail(SessionUser user, Long orderId) {
        BizOrderEntity order = bizOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!canAccessOrder(user, order)) {
            throw new BizException(403, "无权查看该订单");
        }

        TaskEntity task = taskService.getTaskById(order.getTaskId());
        UserAccountEntity pilot = taskService.getUserById(order.getPilotId());
        UserAccountEntity enterprise = taskService.getUserById(order.getEnterpriseId());
        PaymentOrderEntity payment = findLatestPayment(order.getId());
        LocalDateTime createdAt = order.getCreateTime() == null ? LocalDateTime.now() : order.getCreateTime();
        return new ApiDtos.OrderDetailView(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getAmount(),
                order.getTaskId(),
                task.getTitle(),
                task.getTaskType(),
                task.getLocation(),
                PlatformUtils.displayName(pilot),
                PlatformUtils.displayName(enterprise),
                PlatformUtils.displayName(enterprise),
                PlatformUtils.safePhone(enterprise),
                payment == null ? "待支付" : payment.getChannel(),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                PlatformUtils.formatTime(createdAt),
                PlatformUtils.formatTime(createdAt.plusDays(1)),
                PlatformUtils.buildOrderRemark(task.getTaskType())
        );
    }

    PaymentOrderEntity findLatestPayment(Long orderId) {
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getBizOrderId, orderId)
                .orderByDesc(PaymentOrderEntity::getId)
                .last("limit 1"));
    }

    private boolean canAccessOrder(SessionUser user, BizOrderEntity order) {
        return user.role() == RoleType.ADMIN
                || Objects.equals(order.getPilotId(), user.id())
                || Objects.equals(order.getEnterpriseId(), user.id());
    }

    private ApiDtos.OrderView toOrderView(BizOrderEntity order) {
        return new ApiDtos.OrderView(order.getId(), order.getOrderNo(), order.getTaskId(), order.getPilotId(), order.getEnterpriseId(), order.getAmount(), order.getStatus());
    }

    void audit(SessionUser actor, String bizType, String bizId, String eventType, String payload) {
        Long actorUserId = actor == null ? null : actor.id();
        String actorRole = actor == null ? null : actor.role().name();
        auditLogService.record(
                RequestIdContext.get(),
                actorUserId,
                actorRole,
                bizType,
                bizId,
                eventType,
                payload);
    }
}
