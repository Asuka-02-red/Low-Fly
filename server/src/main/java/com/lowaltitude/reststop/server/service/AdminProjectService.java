package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminProjectService {

    private final TaskMapper taskMapper;
    private final BizOrderMapper bizOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final NoFlyZoneMapper noFlyZoneMapper;
    private final FeedbackTicketMapper feedbackTicketMapper;
    private final UserAccountMapper userAccountMapper;

    public AdminProjectService(
            TaskMapper taskMapper,
            BizOrderMapper bizOrderMapper,
            PaymentOrderMapper paymentOrderMapper,
            NoFlyZoneMapper noFlyZoneMapper,
            FeedbackTicketMapper feedbackTicketMapper,
            UserAccountMapper userAccountMapper
    ) {
        this.taskMapper = taskMapper;
        this.bizOrderMapper = bizOrderMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.noFlyZoneMapper = noFlyZoneMapper;
        this.feedbackTicketMapper = feedbackTicketMapper;
        this.userAccountMapper = userAccountMapper;
    }

    /**
     * 获取管理员视角的项目列表
     * @return 返回管理员视角的项目视图列表，包含项目详细信息及状态
     */
    public List<ApiDtos.AdminProjectView> listAdminProjects() {
        // 查询所有任务，按更新时间和ID降序排列
        List<TaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                .orderByDesc(TaskEntity::getUpdateTime)
                .orderByDesc(TaskEntity::getId));
        // 根据任务中的企业ID查询所有对应的用户账户信息
        Map<Long, UserAccountEntity> owners = findUsersByIds(tasks.stream()
                .map(TaskEntity::getEnterpriseId)
                .collect(Collectors.toSet()));
        // 按任务ID分组获取所有订单信息
        Map<Long, List<BizOrderEntity>> ordersByTaskId = groupOrdersByTaskId(tasks);

        // 将任务列表转换为管理员视图列表
        return tasks.stream()
                .map(task -> {
                    // 获取当前任务的所有订单
                    List<BizOrderEntity> taskOrders = ordersByTaskId.getOrDefault(task.getId(), List.of());
                    // 创建并返回管理员项目视图对象
                    return new ApiDtos.AdminProjectView(
                            String.valueOf(task.getId()),                    // 任务ID
                            task.getTitle(),                                 // 任务标题
                            PlatformUtils.displayName(owners.get(task.getEnterpriseId())),  // 企业名称
                            task.getLocation(),                              // 任务位置
                            PlatformUtils.mapAdminProjectStatus(task.getStatus()),
                            PlatformUtils.estimateProjectProgress(task.getStatus()),  // 项目进度
                            PlatformUtils.defaultBudget(task.getBudget()),
                            taskOrders.isEmpty() && "REVIEWING".equalsIgnoreCase(task.getStatus()) ? "待复核" : "正常",  // 复核状态
                            PlatformUtils.estimateRiskLevel("REVIEWING".equalsIgnoreCase(task.getStatus()), !taskOrders.isEmpty()),  // 风险等级
                            PlatformUtils.estimateTrainingCompletion(task.getTaskType()),  // 培训完成度
                            resolvePaymentStatus(taskOrders),                // 支付状态
                            PlatformUtils.formatDateTime(task.getUpdateTime() != null ? task.getUpdateTime() : task.getCreateTime()));  // 更新时间
                })
                .toList();
    }

    public List<ApiDtos.AdminOrderSummaryView> listAdminOrders() {
        List<BizOrderEntity> orders = bizOrderMapper.selectList(new LambdaQueryWrapper<BizOrderEntity>()
                .orderByDesc(BizOrderEntity::getCreateTime)
                .orderByDesc(BizOrderEntity::getId));
        Map<Long, TaskEntity> tasks = findTasksByIds(orders.stream()
                .map(BizOrderEntity::getTaskId)
                .collect(Collectors.toSet()));
        Map<Long, PaymentOrderEntity> payments = findLatestPayments(orders.stream()
                .map(BizOrderEntity::getId)
                .collect(Collectors.toSet()));
        return orders.stream()
                .map(order -> toAdminOrderSummaryView(order, tasks.get(order.getTaskId()), payments.get(order.getId())))
                .toList();
    }

    public ApiDtos.AdminOrderDetailView getAdminOrderDetail(Long orderId) {
        BizOrderEntity order = bizOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        TaskEntity task = taskMapper.selectById(order.getTaskId());
        PaymentOrderEntity payment = findLatestPayment(order.getId());
        return toAdminOrderDetailView(order, task, payment);
    }

    long countNoFlyZones() {
        return noFlyZoneMapper.selectCount(new LambdaQueryWrapper<>());
    }

    long countOpenFeedbackTickets() {
        return feedbackTicketMapper.selectCount(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .ne(FeedbackTicketEntity::getStatus, "CLOSED"));
    }

    long countProcessingFeedbackTickets() {
        return feedbackTicketMapper.selectCount(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .eq(FeedbackTicketEntity::getStatus, "PROCESSING"));
    }

    String resolvePaymentStatus(List<BizOrderEntity> taskOrders) {
        if (taskOrders.isEmpty()) {
            return "待结算";
        }
        long paidCount = taskOrders.stream()
                .filter(order -> "PAID".equalsIgnoreCase(order.getStatus()))
                .count();
        if (paidCount == 0) {
            return "待结算";
        }
        if (paidCount == taskOrders.size()) {
            return "已结算";
        }
        return "部分结算";
    }

    private PaymentOrderEntity findLatestPayment(Long orderId) {
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getBizOrderId, orderId)
                .orderByDesc(PaymentOrderEntity::getId)
                .last("limit 1"));
    }

    private Map<Long, List<BizOrderEntity>> groupOrdersByTaskId(List<TaskEntity> tasks) {
        Set<Long> taskIds = tasks.stream()
                .map(TaskEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return bizOrderMapper.selectList(new LambdaQueryWrapper<BizOrderEntity>()
                        .in(BizOrderEntity::getTaskId, taskIds))
                .stream()
                .collect(Collectors.groupingBy(BizOrderEntity::getTaskId));
    }

    Map<Long, TaskEntity> findTasksByIds(Set<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                        .in(TaskEntity::getId, taskIds))
                .stream()
                .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));
    }

    Map<Long, PaymentOrderEntity> findLatestPayments(Set<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderEntity>()
                        .in(PaymentOrderEntity::getBizOrderId, orderIds)
                        .orderByDesc(PaymentOrderEntity::getId))
                .stream()
                .collect(Collectors.toMap(
                        PaymentOrderEntity::getBizOrderId,
                        Function.identity(),
                        (first, ignored) -> first));
    }

    private Map<Long, UserAccountEntity> findUsersByIds(Set<Long> ids) {
        Set<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, Function.identity()));
    }

    ApiDtos.AdminOrderSummaryView toAdminOrderSummaryView(BizOrderEntity order, TaskEntity task, PaymentOrderEntity payment) {
        String paymentMethod = payment == null ? "待支付" : PlatformUtils.paymentChannelLabel(payment.getChannel());
        return new ApiDtos.AdminOrderSummaryView(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                task == null ? "未知项目" : task.getTitle(),
                PlatformUtils.defaultBudget(order.getAmount()),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                PlatformUtils.formatDateTime(order.getCreateTime()),
                paymentMethod,
                task == null ? "暂无任务详情" : PlatformUtils.buildOrderRemark(task.getTaskType())
        );
    }

    ApiDtos.AdminOrderDetailView toAdminOrderDetailView(BizOrderEntity order, TaskEntity task, PaymentOrderEntity payment) {
        String paymentMethod = payment == null ? "待支付" : PlatformUtils.paymentChannelLabel(payment.getChannel());
        return new ApiDtos.AdminOrderDetailView(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                task.getTitle(),
                PlatformUtils.defaultBudget(order.getAmount()),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                PlatformUtils.formatDateTime(order.getCreateTime()),
                paymentMethod,
                PlatformUtils.buildOrderRemark(task.getTaskType())
        );
    }
}
