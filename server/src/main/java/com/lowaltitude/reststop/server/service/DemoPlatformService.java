package com.lowaltitude.reststop.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.AdminNotificationRuleEntity;
import com.lowaltitude.reststop.server.entity.AdminSettingEntity;
import com.lowaltitude.reststop.server.entity.BizOrderEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.FeedbackTicketEntity;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
import com.lowaltitude.reststop.server.entity.PaymentOrderEntity;
import com.lowaltitude.reststop.server.entity.ReportDailySummaryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.AdminNotificationRuleMapper;
import com.lowaltitude.reststop.server.mapper.AdminSettingMapper;
import com.lowaltitude.reststop.server.mapper.BizOrderMapper;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.FeedbackTicketMapper;
import com.lowaltitude.reststop.server.mapper.MessageConversationMapper;
import com.lowaltitude.reststop.server.mapper.MessageEntryMapper;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.PaymentOrderMapper;
import com.lowaltitude.reststop.server.mapper.ReportDailySummaryMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.security.TokenService;

import jakarta.annotation.PostConstruct;

/**
 * 演示平台核心业务服务（已拆分，保留作历史参考）。
 * <p>
 * 本类已按单一职责原则拆分为以下独立服务：
 * AuthService、TaskService、OrderService、FeedbackService、MessageService、
 * CourseService、AdminDashboardService、AdminUserService、AdminProjectService、
 * AdminSettingsService、DemoDataSeeder、PlatformUtils。
 * </p>
 */
@Deprecated
// @Service
public class DemoPlatformService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TokenService tokenService;
    private final UserAccountMapper userAccountMapper;
    private final TaskMapper taskMapper;
    private final BizOrderMapper bizOrderMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final NoFlyZoneMapper noFlyZoneMapper;
    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final FeedbackTicketMapper feedbackTicketMapper;
    private final MessageConversationMapper messageConversationMapper;
    private final MessageEntryMapper messageEntryMapper;
    private final AdminSettingMapper adminSettingMapper;
    private final AdminNotificationRuleMapper adminNotificationRuleMapper;
    private final ReportDailySummaryMapper reportDailySummaryMapper;
    private final RefreshTokenStore refreshTokenStore;
    private final AlertService alertService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public DemoPlatformService(
            TokenService tokenService,
            UserAccountMapper userAccountMapper,
            TaskMapper taskMapper,
            BizOrderMapper bizOrderMapper,
            PaymentOrderMapper paymentOrderMapper,
            NoFlyZoneMapper noFlyZoneMapper,
            CourseMapper courseMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            FeedbackTicketMapper feedbackTicketMapper,
            MessageConversationMapper messageConversationMapper,
            MessageEntryMapper messageEntryMapper,
            AdminSettingMapper adminSettingMapper,
            AdminNotificationRuleMapper adminNotificationRuleMapper,
            ReportDailySummaryMapper reportDailySummaryMapper,
            RefreshTokenStore refreshTokenStore,
            AlertService alertService,
            AuditLogService auditLogService
    ) {
        this.tokenService = tokenService;
        this.userAccountMapper = userAccountMapper;
        this.taskMapper = taskMapper;
        this.bizOrderMapper = bizOrderMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.noFlyZoneMapper = noFlyZoneMapper;
        this.courseMapper = courseMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.feedbackTicketMapper = feedbackTicketMapper;
        this.messageConversationMapper = messageConversationMapper;
        this.messageEntryMapper = messageEntryMapper;
        this.adminSettingMapper = adminSettingMapper;
        this.adminNotificationRuleMapper = adminNotificationRuleMapper;
        this.reportDailySummaryMapper = reportDailySummaryMapper;
        this.refreshTokenStore = refreshTokenStore;
        this.alertService = alertService;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    void init() {
        alertService.seedDefaultsIfEmpty();
        seedDemoCoursesIfNecessary();
        seedDemoConversationsIfNecessary();
        audit(null, null, "SYSTEM", "boot", "INIT", "比赛演示数据已载入");
    }

    public ApiDtos.AuthPayload login(ApiDtos.LoginRequest request) {
        UserAccountEntity user = findUserByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(401, "用户名或密码错误");
        }
        return buildAuthPayload(user);
    }

    @Transactional
    public ApiDtos.AuthPayload register(ApiDtos.RegisterRequest request) {
        if (findUserByUsername(request.username()) != null) {
            throw new BizException(400, "用户名已存在");
        }
        if (findUserByPhone(request.phone()) != null) {
            throw new BizException(400, "手机号已存在");
        }
        RoleType role = parseRole(request.role());
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(role.name());
        user.setRealName(isBlank(request.realName()) ? request.username() : request.realName().trim());
        user.setCompanyName(normalizeNullable(request.companyName()));
        user.setStatus(1);
        user.setVersion(0);
        userAccountMapper.insert(user);
        audit(user.getId(), role.name(), "USER", String.valueOf(user.getId()), "REGISTER", "role=" + role.name());
        return buildAuthPayload(user);
    }

    public ApiDtos.AuthPayload refresh(ApiDtos.RefreshTokenRequest request) {
        Long userId = refreshTokenStore.requireUserIdByRefreshToken(request.refreshToken());
        UserAccountEntity user = getUserById(userId);
        SessionUser sessionUser = toSessionUser(user);
        return new ApiDtos.AuthPayload(
                tokenService.createToken(sessionUser),
                request.refreshToken(),
                toSessionInfo(user)
        );
    }

    public ApiDtos.SessionInfo currentUser(SessionUser user) {
        return toSessionInfo(getUserById(user.id()));
    }

    public List<ApiDtos.TaskView> listTasks(SessionUser user) {
        LambdaQueryWrapper<TaskEntity> query = new LambdaQueryWrapper<TaskEntity>()
                .orderByDesc(TaskEntity::getUpdateTime)
                .orderByDesc(TaskEntity::getId);
        if (user.role() == RoleType.ENTERPRISE) {
            query.eq(TaskEntity::getEnterpriseId, user.id());
        } else if (user.role() == RoleType.PILOT) {
            query.notIn(TaskEntity::getStatus, List.of("CANCELLED", "CLOSED"));
        }
        List<TaskEntity> tasks = taskMapper.selectList(query);
        Map<Long, UserAccountEntity> owners = findUsersByIds(tasks.stream()
                .map(TaskEntity::getEnterpriseId)
                .collect(Collectors.toSet()));
        return tasks.stream()
                .map(task -> {
                    UserAccountEntity owner = owners.get(task.getEnterpriseId());
                    return toTaskView(task, owner);
                })
                .toList();
    }

    public ApiDtos.TaskDetailView getTaskDetail(Long taskId) {
        TaskEntity task = getTaskById(taskId);
        UserAccountEntity owner = getUserById(task.getEnterpriseId());
        BigDecimal routeStartLatitude = task.getLatitude().subtract(new BigDecimal("0.018"));
        BigDecimal routeStartLongitude = task.getLongitude().subtract(new BigDecimal("0.022"));
        return new ApiDtos.TaskDetailView(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getDescription(),
                task.getLocation(),
                formatDateTime(task.getDeadline()),
                task.getLatitude(),
                task.getLongitude(),
                routeStartLatitude,
                routeStartLongitude,
                buildOperationRadius(task),
                task.getBudget(),
                task.getStatus(),
                displayName(owner)
        );
    }

    @Transactional
    public ApiDtos.TaskView createTask(SessionUser user, ApiDtos.TaskRequest request) {
        ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = new TaskEntity();
        task.setEnterpriseId(user.id());
        task.setTaskType(request.taskType());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setLocation(request.location());
        task.setDeadline(parseTaskDeadline(request.deadline()));
        task.setLatitude(request.latitude());
        task.setLongitude(request.longitude());
        task.setBudget(request.budget());
        task.setStatus("REVIEWING");
        task.setVersion(0);
        taskMapper.insert(task);
        audit(user, "TASK", String.valueOf(task.getId()), "CREATE", task.getTitle());
        return toTaskView(task, getUserById(user.id()));
    }

    @Transactional
    public ApiDtos.TaskView updateTask(SessionUser user, Long taskId, ApiDtos.TaskRequest request) {
        ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = getTaskById(taskId);
        ensureTaskOwnership(user, task);
        task.setTaskType(request.taskType());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setLocation(request.location());
        task.setDeadline(parseTaskDeadline(request.deadline()));
        task.setLatitude(request.latitude());
        task.setLongitude(request.longitude());
        task.setBudget(request.budget());
        taskMapper.updateById(task);
        audit(user, "TASK", String.valueOf(task.getId()), "UPDATE", task.getTitle());
        return toTaskView(task, getUserById(task.getEnterpriseId()));
    }

    @Transactional
    public ApiDtos.TaskView publishTask(SessionUser user, Long taskId) {
        ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = getTaskById(taskId);
        ensureTaskOwnership(user, task);
        task.setStatus("REVIEWING");
        taskMapper.updateById(task);
        audit(user, "TASK", String.valueOf(task.getId()), "REPUBLISH", task.getTitle());
        return toTaskView(task, getUserById(task.getEnterpriseId()));
    }

    @Transactional
    public ApiDtos.OrderView createOrder(SessionUser user, ApiDtos.OrderCreateRequest request) {
        ensureRole(user, RoleType.PILOT);
        TaskEntity task = getTaskById(request.taskId());
        getUserById(user.id());
        getUserById(task.getEnterpriseId());

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

        TaskEntity task = getTaskById(order.getTaskId());
        UserAccountEntity pilot = getUserById(order.getPilotId());
        UserAccountEntity enterprise = getUserById(order.getEnterpriseId());
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
                displayName(pilot),
                displayName(enterprise),
                displayName(enterprise),
                safePhone(enterprise),
                payment == null ? "待支付" : payment.getChannel(),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                formatTime(createdAt),
                formatTime(createdAt.plusDays(1)),
                buildOrderRemark(task)
        );
    }

    public List<ApiDtos.NoFlyZoneView> listZones() {
        return noFlyZoneMapper.selectList(new LambdaQueryWrapper<NoFlyZoneEntity>()
                        .orderByAsc(NoFlyZoneEntity::getId))
                .stream()
                .map(zone -> new ApiDtos.NoFlyZoneView(
                        zone.getId(),
                        zone.getName(),
                        zone.getZoneType(),
                        zone.getCenterLat(),
                        zone.getCenterLng(),
                        zone.getRadius() == null ? 0 : zone.getRadius(),
                        zone.getDescription()))
                .toList();
    }

    public ApiDtos.FlightApplicationView submitFlightApplication(SessionUser user, ApiDtos.FlightApplicationRequest request) {
        ensureRole(user, RoleType.PILOT);
        String applicationNo = "FLY" + System.currentTimeMillis();
        audit(user, "COMPLIANCE", applicationNo, "APPLY", request.location());
        return new ApiDtos.FlightApplicationView(applicationNo, "REVIEWING", request.location(), "审批结果将在 15 分钟内通过站内信反馈");
    }

    @Transactional
    public ApiDtos.FeedbackTicketView createFeedbackTicket(SessionUser user, ApiDtos.FeedbackTicketRequest request) {
        FeedbackTicketEntity ticket = new FeedbackTicketEntity();
        ticket.setTicketNo("FBK" + System.currentTimeMillis());
        ticket.setSubmitUserId(user.id());
        ticket.setSubmitUserRole(user.role().name());
        ticket.setContact(normalizeNullable(request.contact()));
        ticket.setDetail(request.detail().trim());
        ticket.setStatus("OPEN");
        feedbackTicketMapper.insert(ticket);
        audit(user, "FEEDBACK", ticket.getTicketNo(), "CREATE", ticket.getDetail());
        return toFeedbackTicketView(ticket, displayName(getUserById(user.id())));
    }

    public List<ApiDtos.FeedbackTicketView> listMyFeedbackTickets(SessionUser user) {
        return feedbackTicketMapper.selectList(new LambdaQueryWrapper<FeedbackTicketEntity>()
                        .eq(FeedbackTicketEntity::getSubmitUserId, user.id())
                        .orderByDesc(FeedbackTicketEntity::getCreateTime)
                        .orderByDesc(FeedbackTicketEntity::getId))
                .stream()
                .map(ticket -> toFeedbackTicketView(ticket, displayName(getUserById(user.id()))))
                .toList();
    }

    public List<ApiDtos.MessageConversationView> listMessageConversations(SessionUser user) {
        List<MessageConversationEntity> conversations = findConversationsForUser(user);
        Map<Long, UserAccountEntity> users = findUsersByIds(conversations.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getEnterpriseId(), item.getPilotId()))
                .collect(Collectors.toSet()));

        return conversations.stream()
                .map(conversation -> {
                    UserAccountEntity counterpart = resolveCounterpart(user, conversation, users);
                    MessageEntryEntity latest = findLatestMessage(conversation.getId());
                    int unreadCount = countCounterpartMessages(conversation.getId(), user.id());
                    String pilotUid = toUid(conversation.getPilotId());
                    String enterpriseUid = toUid(conversation.getEnterpriseId());
                    return new ApiDtos.MessageConversationView(
                            conversation.getId(),
                            conversationTitle(conversation, counterpart),
                            conversationSubtitle(conversation),
                            displayName(counterpart),
                            toUid(counterpart == null ? null : counterpart.getId()),
                            displayRole(counterpart == null ? null : counterpart.getRole()),
                            pilotUid,
                            enterpriseUid,
                            latest == null ? "暂无消息，立即发起协同沟通。" : latest.getContent(),
                            formatDateTime(latest == null ? conversation.getLastMessageTime() : latest.getCreateTime()),
                            unreadCount);
                })
                .toList();
    }

    public ApiDtos.MessageThreadView getMessageThread(SessionUser user, Long conversationId) {
        MessageConversationEntity conversation = getConversation(conversationId);
        ensureConversationAccess(user, conversation);
        Map<Long, UserAccountEntity> users = findUsersByIds(Set.of(conversation.getEnterpriseId(), conversation.getPilotId()));
        UserAccountEntity counterpart = resolveCounterpart(user, conversation, users);
        List<ApiDtos.MessageEntryView> messages = messageEntryMapper.selectList(new LambdaQueryWrapper<MessageEntryEntity>()
                        .eq(MessageEntryEntity::getConversationId, conversationId)
                        .orderByAsc(MessageEntryEntity::getCreateTime)
                        .orderByAsc(MessageEntryEntity::getId))
                .stream()
                .map(entry -> toMessageEntryView(entry, users.get(entry.getSenderUserId()), user.id()))
                .toList();
        return new ApiDtos.MessageThreadView(
                conversation.getId(),
                conversationTitle(conversation, counterpart),
                conversationSubtitle(conversation),
                toUid(conversation.getPilotId()),
                toUid(conversation.getEnterpriseId()),
                messages);
    }

    public ApiDtos.MessageReadReceiptResponse syncReadReceipts(SessionUser user, ApiDtos.MessageReadReceiptRequest request) {
        List<Long> syncedIds = new ArrayList<>();
        for (Long msgId : request.msgIds()) {
            if (msgId == null) {
                continue;
            }
            MessageEntryEntity entry = messageEntryMapper.selectById(msgId);
            if (entry == null) {
                continue;
            }
            MessageConversationEntity conversation = getConversation(entry.getConversationId());
            ensureConversationAccess(user, conversation);
            syncedIds.add(msgId);
        }
        audit(user, "MESSAGE", String.valueOf(user.id()), "READ_RECEIPT", "count=" + syncedIds.size());
        return new ApiDtos.MessageReadReceiptResponse(syncedIds.size(), syncedIds);
    }

    @Transactional
    public ApiDtos.MessageThreadView sendMessage(SessionUser user, Long conversationId, ApiDtos.MessageSendRequest request) {
        MessageConversationEntity conversation = getConversation(conversationId);
        ensureConversationAccess(user, conversation);
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setConversationId(conversationId);
        entry.setSenderUserId(user.id());
        entry.setSenderRole(user.role().name());
        entry.setContent(request.content().trim());
        entry.setCreateTime(LocalDateTime.now());
        messageEntryMapper.insert(entry);
        conversation.setLastMessageTime(entry.getCreateTime());
        messageConversationMapper.updateById(conversation);
        audit(user, "MESSAGE", String.valueOf(conversationId), "SEND", request.content().trim());
        return getMessageThread(user, conversationId);
    }

    public ApiDtos.PilotProfileView getPilotProfile(SessionUser user, String uid) {
        UserAccountEntity target = requireUserByUid(uid);
        if (RoleType.valueOf(target.getRole()) != RoleType.PILOT) {
            throw new BizException(404, "飞手不存在");
        }
        return new ApiDtos.PilotProfileView(toUid(target.getId()), displayName(target));
    }

    public ApiDtos.EnterpriseInfoView getEnterpriseInfo(SessionUser user, String uid) {
        UserAccountEntity target = requireUserByUid(uid);
        if (RoleType.valueOf(target.getRole()) != RoleType.ENTERPRISE) {
            throw new BizException(404, "企业不存在");
        }
        return new ApiDtos.EnterpriseInfoView(toUid(target.getId()), defaultIfBlank(target.getCompanyName(), displayName(target)));
    }

    public List<ApiDtos.AlertView> listAlerts() {
        return alertService.listAlerts();
    }

    public List<ApiDtos.CourseView> listCourses() {
        return courseMapper.selectList(new LambdaQueryWrapper<CourseEntity>()
                        .eq(CourseEntity::getStatus, "OPEN")
                        .orderByDesc(CourseEntity::getId))
                .stream()
                .map(this::toCourseView)
                .toList();
    }

    @Transactional
    public ApiDtos.CourseDetailView getCourseDetail(SessionUser user, Long courseId) {
        CourseEntity course = getCourse(courseId);
        if (!"OPEN".equalsIgnoreCase(course.getStatus()) && !canManageCourse(user, course)) {
            throw new BizException(403, "当前课程未发布");
        }
        CourseEnrollmentEntity enrollment = findActiveEnrollment(courseId, user.id());
        if (!canManageCourse(user, course)) {
            course.setBrowseCount(safeInt(course.getBrowseCount()) + 1);
            courseMapper.updateById(course);
        }
        return toCourseDetailView(course, enrollment);
    }

    public List<ApiDtos.CourseManageView> listManagedCourses(SessionUser user) {
        ensureCourseManager(user);
        LambdaQueryWrapper<CourseEntity> query = new LambdaQueryWrapper<CourseEntity>()
                .orderByDesc(CourseEntity::getId);
        if (user.role() != RoleType.ADMIN) {
            query.eq(CourseEntity::getPublishUserId, user.id());
        }
        return courseMapper.selectList(query).stream()
                .map(this::toCourseManageView)
                .toList();
    }

    @Transactional
    public ApiDtos.CourseManageView createCourse(SessionUser user, ApiDtos.CourseManageRequest request) {
        ensureCourseManager(user);
        CourseEntity course = buildCourseEntity(user, null, request);
        courseMapper.insert(course);
        audit(user, "COURSE", String.valueOf(course.getId()), "CREATE", course.getTitle());
        return toCourseManageView(course);
    }

    @Transactional
    public ApiDtos.CourseManageView updateCourse(SessionUser user, Long courseId, ApiDtos.CourseManageRequest request) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        CourseEntity updated = buildCourseEntity(user, course, request);
        updated.setId(course.getId());
        updated.setCreateTime(course.getCreateTime());
        updated.setBrowseCount(safeInt(course.getBrowseCount()));
        updated.setEnrollCount(safeInt(course.getEnrollCount()));
        updated.setPublishUserId(course.getPublishUserId());
        updated.setInstitutionName(course.getInstitutionName());
        updated.setSeatAvailable(resolveSeatAvailable(course, request.seatTotal()));
        courseMapper.updateById(updated);
        audit(user, "COURSE", String.valueOf(updated.getId()), "UPDATE", updated.getTitle());
        return toCourseManageView(updated);
    }

    @Transactional
    public ApiDtos.CourseManageView publishCourse(SessionUser user, Long courseId) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        course.setStatus("OPEN");
        courseMapper.updateById(course);
        audit(user, "COURSE", String.valueOf(course.getId()), "PUBLISH", course.getTitle());
        return toCourseManageView(course);
    }

    @Transactional
    public void deleteCourse(SessionUser user, Long courseId) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        courseMapper.deleteById(courseId);
        courseEnrollmentMapper.delete(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId));
        audit(user, "COURSE", String.valueOf(courseId), "DELETE", course.getTitle());
    }

    @Transactional
    public synchronized ApiDtos.EnrollmentResult enrollCourse(SessionUser user, Long courseId) {
        if (user.role() == RoleType.ADMIN) {
            throw new BizException(403, "管理员不可直接报名课程");
        }
        CourseEntity course = getCourse(courseId);
        if (!"OPEN".equalsIgnoreCase(course.getStatus())) {
            throw new BizException(400, "课程未发布，暂不可报名");
        }
        if (courseEnrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getUserId, user.id())
                .eq(CourseEnrollmentEntity::getStatus, "ENROLLED")) > 0) {
            throw new BizException(400, "您已报名该课程");
        }
        int seatAvailable = safeInt(course.getSeatAvailable());
        if ("OFFLINE".equalsIgnoreCase(course.getCourseType()) && seatAvailable <= 0) {
            throw new BizException(400, "课程余量不足");
        }
        if ("OFFLINE".equalsIgnoreCase(course.getCourseType())) {
            course.setSeatAvailable(seatAvailable - 1);
        }
        course.setEnrollCount(safeInt(course.getEnrollCount()) + 1);
        courseMapper.updateById(course);

        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(user.id());
        enrollment.setUserRole(user.role().name());
        enrollment.setEnrollmentNo("ENR" + System.currentTimeMillis());
        enrollment.setStatus("ENROLLED");
        courseEnrollmentMapper.insert(enrollment);

        audit(user, "TRAINING", String.valueOf(course.getId()), "ENROLL", "user=" + user.id());
        return new ApiDtos.EnrollmentResult(enrollment.getEnrollmentNo(), "SUCCESS", course.getTitle(), safeInt(course.getSeatAvailable()));
    }

    public ApiDtos.DashboardOverview adminOverview() {
        long totalUsers = countUsers();
        long enabledUsers = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .gt(UserAccountEntity::getStatus, 0));
        long adminUsers = countUsersByRole(RoleType.ADMIN);
        long totalTasks = countTasks();
        long reviewingTasks = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long totalOrders = countOrders();
        long paidOrders = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        long openCourses = countOpenCourses();
        long auditCount = auditLogService.count();
        long openAlerts = alertService.countOpenAlerts();
        long highAlerts = alertService.countHighRiskAlerts();

        List<ReportDailySummaryEntity> reports = loadRecentReports(6);
        List<TaskEntity> tasks = taskMapper.selectList(new QueryWrapper<TaskEntity>()
                .select("id", "title", "location", "status", "update_time")
                .orderByDesc("update_time", "id"));

        List<ApiDtos.DashboardMetric> metrics = List.of(
                new ApiDtos.DashboardMetric("用户总量", String.valueOf(totalUsers), "启用账号 " + enabledUsers, "success"),
                new ApiDtos.DashboardMetric("任务总量", String.valueOf(totalTasks), "待审核 " + reviewingTasks, "info"),
                new ApiDtos.DashboardMetric("订单总量", String.valueOf(totalOrders), "已支付 " + paidOrders, "success"),
                new ApiDtos.DashboardMetric("高风险告警", String.valueOf(highAlerts), "未关闭 " + openAlerts, highAlerts > 0 ? "warning" : "success")
        );

        List<ApiDtos.DashboardMetric> deviceStats = List.of(
                new ApiDtos.DashboardMetric("启用账号", String.valueOf(enabledUsers), "管理员 " + adminUsers, "success"),
                new ApiDtos.DashboardMetric("开放课程", String.valueOf(openCourses), "培训资源已发布", "info"),
                new ApiDtos.DashboardMetric("审计日志", String.valueOf(auditCount), "关键操作留痕", "warning")
        );

        List<ApiDtos.DashboardActivity> activities = auditLogService.listRecent().stream()
                .limit(5)
                .map(item -> new ApiDtos.DashboardActivity(
                        item.bizType() + " / " + item.eventType(),
                        item.payload(),
                        formatDateTime(item.createTime()),
                        defaultIfBlank(item.actorRole(), "SYSTEM")))
                .toList();

        List<ApiDtos.DashboardNotice> notices = List.of(
                new ApiDtos.DashboardNotice("待审核任务 " + reviewingTasks + " 个，请尽快处理。", reviewingTasks > 0 ? "中" : "低", "实时"),
                new ApiDtos.DashboardNotice("高风险告警 " + highAlerts + " 条，建议优先跟进。", highAlerts > 0 ? "高" : "低", "实时"),
                new ApiDtos.DashboardNotice("已支付订单 " + paidOrders + " 笔，平台经营数据已同步。", "低", "实时")
        );

        List<ApiDtos.AdminDistributionItem> projectDistribution = tasks.stream()
                .collect(Collectors.groupingBy(task -> defaultIfBlank(task.getLocation(), "未分类"), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(entry -> new ApiDtos.AdminDistributionItem(entry.getKey(), entry.getValue().intValue()))
                .toList();

        List<ApiDtos.AdminTrendPoint> progressTrend = reports.stream()
                .map(report -> new ApiDtos.AdminTrendPoint(
                        report.getStatDate().getMonthValue() + "/" + report.getStatDate().getDayOfMonth(),
                        Math.min(100, safeInt(report.getTaskCount()) * 8 + safeInt(report.getOrderCount()) * 6 + safeInt(report.getTrainingCount()) * 3)))
                .toList();

        return new ApiDtos.DashboardOverview(
                metrics,
                deviceStats,
                activities,
                notices,
                projectDistribution,
                progressTrend
        );
    }

    public List<ApiDtos.AdminSectionSummary> adminSectionSummaries() {
        long userCount = countUsers();
        long adminUserCount = countUsersByRole(RoleType.ADMIN);
        long taskCount = countTasks();
        long pendingTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long orderCount = countOrders();
        long paidOrderCount = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        long zoneCount = noFlyZoneMapper.selectCount(new LambdaQueryWrapper<>());
        long openAlertCount = alertService.countOpenAlerts();
        long highAlertCount = alertService.countHighRiskAlerts();
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<>());
        long openCourseCount = countOpenCourses();
        long enabledNotificationCount = 2L;
        long auditEventCount = auditLogService.count();

        return List.of(
                new ApiDtos.AdminSectionSummary(
                        "overview",
                        "系统概览",
                        "总览当前平台状态。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("用户", String.valueOf(userCount), "在线管理员 " + adminUserCount, "success"),
                                new ApiDtos.SectionMetric("任务", String.valueOf(taskCount), "待审核 " + pendingTaskCount, "info"),
                                new ApiDtos.SectionMetric("告警", String.valueOf(openAlertCount), "高风险 " + highAlertCount, "warning")
                        ),
                        List.of("待处理高风险告警 " + highAlertCount + " 条", "当前功能开关 3 项生效")
                ),
                new ApiDtos.AdminSectionSummary(
                        "users",
                        "用户管理",
                        "管理账号、角色与权限。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("账号", String.valueOf(userCount), "管理员 " + adminUserCount, "success"),
                                new ApiDtos.SectionMetric("角色", "4", "覆盖后台岗位", "info"),
                                new ApiDtos.SectionMetric("审计", String.valueOf(auditEventCount), "最近 24h 有更新", "warning")
                        ),
                        List.of("支持角色分配", "支持权限组配置")
                ),
                new ApiDtos.AdminSectionSummary(
                        "projects",
                        "项目管理",
                        "跟踪项目与交付状态。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("任务", String.valueOf(taskCount), "待审核 " + pendingTaskCount, "info"),
                                new ApiDtos.SectionMetric("订单", String.valueOf(orderCount), "已支付 " + paidOrderCount, "success"),
                                new ApiDtos.SectionMetric("禁飞区", String.valueOf(zoneCount), "合规校验已启用", "warning")
                        ),
                        List.of("支持状态流转", "支持合规与支付联动")
                ),
                new ApiDtos.AdminSectionSummary(
                        "analytics",
                        "数据分析",
                        "查看经营与性能数据。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("订单", String.valueOf(orderCount), "支付成功 " + paidOrderCount, "success"),
                                new ApiDtos.SectionMetric("课程", String.valueOf(courseCount), "开放 " + openCourseCount, "info"),
                                new ApiDtos.SectionMetric("告警", String.valueOf(openAlertCount), "闭环持续跟踪", "warning")
                        ),
                        List.of("含经营图表", "含性能图表")
                ),
                new ApiDtos.AdminSectionSummary(
                        "settings",
                        "系统设置",
                        "维护基础参数与安全策略。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("策略", "2", "基础参数与安全策略", "info"),
                                new ApiDtos.SectionMetric("通知", "3", "启用 " + enabledNotificationCount, "success"),
                                new ApiDtos.SectionMetric("白名单", "2", "当前已生效", "warning")
                        ),
                        List.of("支持基础参数保存", "支持通知规则维护")
                ),
                new ApiDtos.AdminSectionSummary(
                        "logs",
                        "日志管理",
                        "查看操作留痕与导出记录。",
                        "进入子页面",
                        List.of(
                                new ApiDtos.SectionMetric("日志", String.valueOf(auditEventCount), "全链路留痕", "success"),
                                new ApiDtos.SectionMetric("高风险", String.valueOf(highAlertCount), "需重点复核", "warning"),
                                new ApiDtos.SectionMetric("导出", "CSV", "支持筛选导出", "info")
                        ),
                        List.of("支持查询", "支持导出")
                )
        );
    }

    public List<ApiDtos.AuditEventView> listAuditEvents() {
        return auditLogService.listRecent();
    }

    public List<ApiDtos.AdminUserView> listAdminUsers() {
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountEntity>()
                        .orderByDesc(UserAccountEntity::getCreateTime)
                        .orderByDesc(UserAccountEntity::getId))
                .stream()
                .map(this::toAdminUserView)
                .toList();
    }

    @Transactional
    public ApiDtos.AdminUserView createAdminUser(SessionUser admin, ApiDtos.AdminUserCreateRequest request) {
        ensureRole(admin, RoleType.ADMIN);
        validateUserUniqueness(request.username(), request.phone(), normalizeNullable(request.email()), null);

        RoleType role = parseRole(request.role());
        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone().trim());
        user.setEmail(normalizeNullable(request.email()));
        user.setRole(role.name());
        user.setRealName(request.realName().trim());
        user.setCompanyName(normalizeNullable(request.companyName()));
        user.setStatus(request.status());
        user.setVersion(0);
        userAccountMapper.insert(user);
        audit(admin, "ADMIN_USER", String.valueOf(user.getId()), "CREATE", "username=" + user.getUsername());
        return toAdminUserView(getUserById(user.getId()));
    }

    @Transactional
    public ApiDtos.AdminUserView updateAdminUser(SessionUser admin, Long userId, ApiDtos.AdminUserUpdateRequest request) {
        ensureRole(admin, RoleType.ADMIN);
        UserAccountEntity user = getUserById(userId);
        validateAdminOperationTarget(user);
        validateUserUniqueness(request.username(), request.phone(), normalizeNullable(request.email()), userId);

        RoleType role = parseRole(request.role());
        user.setUsername(request.username().trim());
        if (!isBlank(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        user.setPhone(request.phone().trim());
        user.setEmail(normalizeNullable(request.email()));
        user.setRole(role.name());
        user.setRealName(request.realName().trim());
        user.setCompanyName(normalizeNullable(request.companyName()));
        user.setStatus(request.status());
        userAccountMapper.updateById(user);
        audit(admin, "ADMIN_USER", String.valueOf(user.getId()), "UPDATE", "username=" + user.getUsername());
        return toAdminUserView(getUserById(userId));
    }

    @Transactional
    public void deleteAdminUser(SessionUser admin, Long userId) {
        ensureRole(admin, RoleType.ADMIN);
        UserAccountEntity user = getUserById(userId);
        validateAdminOperationTarget(user);
        userAccountMapper.deleteById(userId);
        audit(admin, "ADMIN_USER", String.valueOf(userId), "DELETE", "username=" + user.getUsername());
    }

    public List<ApiDtos.AdminProjectView> listAdminProjects() {
        List<TaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                .orderByDesc(TaskEntity::getUpdateTime)
                .orderByDesc(TaskEntity::getId));
        Map<Long, UserAccountEntity> owners = findUsersByIds(tasks.stream()
                .map(TaskEntity::getEnterpriseId)
                .collect(Collectors.toSet()));
        Map<Long, List<BizOrderEntity>> ordersByTaskId = groupOrdersByTaskId(tasks);

        return tasks.stream()
                .map(task -> {
                    List<BizOrderEntity> taskOrders = ordersByTaskId.getOrDefault(task.getId(), List.of());
                    return new ApiDtos.AdminProjectView(
                            String.valueOf(task.getId()),
                            task.getTitle(),
                            displayName(owners.get(task.getEnterpriseId())),
                            task.getLocation(),
                            mapAdminProjectStatus(task.getStatus()),
                            estimateProjectProgress(task.getStatus()),
                            defaultBudget(task.getBudget()),
                            taskOrders.isEmpty() && "REVIEWING".equalsIgnoreCase(task.getStatus()) ? "待复核" : "正常",
                            estimateRiskLevel(task, taskOrders),
                            estimateTrainingCompletion(task),
                            resolvePaymentStatus(taskOrders),
                            formatDateTime(task.getUpdateTime() != null ? task.getUpdateTime() : task.getCreateTime()));
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
        TaskEntity task = getTaskById(order.getTaskId());
        PaymentOrderEntity payment = findLatestPayment(order.getId());
        return toAdminOrderDetailView(order, task, payment);
    }

    public ApiDtos.AdminAnalyticsView adminAnalytics() {
        List<ReportDailySummaryEntity> reports = loadRecentReports(6);
        List<ApiDtos.AdminProjectView> projects = listAdminProjects();
        long totalUsers = countUsers();
        long enabledUsers = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .gt(UserAccountEntity::getStatus, 0));
        long paidOrderCount = bizOrderMapper.selectCount(new LambdaQueryWrapper<BizOrderEntity>()
                .eq(BizOrderEntity::getStatus, "PAID"));
        BigDecimal totalRevenue = bizOrderMapper.selectList(new LambdaQueryWrapper<BizOrderEntity>()
                        .eq(BizOrderEntity::getStatus, "PAID"))
                .stream()
                .map(BizOrderEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowRiskCount = projects.stream().filter(item -> "低".equals(item.riskLevel())).count();
        long mediumRiskCount = projects.stream().filter(item -> "中".equals(item.riskLevel())).count();
        long highRiskCount = projects.stream().filter(item -> "高".equals(item.riskLevel())).count();
        long openFeedbackCount = feedbackTicketMapper.selectCount(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .ne(FeedbackTicketEntity::getStatus, "CLOSED"));
        long processingFeedbackCount = feedbackTicketMapper.selectCount(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .eq(FeedbackTicketEntity::getStatus, "PROCESSING"));
        long pendingTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "REVIEWING"));
        long openAlertCount = alertService.listAlerts().stream()
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.status()))
                .count();

        int projectHealthPercent = projects.isEmpty()
                ? 100
                : BigDecimal.valueOf(lowRiskCount * 100.0 / projects.size())
                .setScale(1, RoundingMode.HALF_UP)
                .intValue();
        int userActivityPercent = totalUsers == 0
                ? 0
                : BigDecimal.valueOf(enabledUsers * 100.0 / totalUsers)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        int avgResponse = 120 + (int) Math.min(180, openAlertCount * 8 + openFeedbackCount * 6 + pendingTaskCount * 5);
        double availability = Math.max(96.5d, 99.95d - openAlertCount * 0.18d - openFeedbackCount * 0.05d);
        int feedbackCycle = 18 + (int) Math.min(45, openFeedbackCount * 4 + processingFeedbackCount * 2);

        return new ApiDtos.AdminAnalyticsView(
                List.of(
                        new ApiDtos.SectionMetric("月度营收", "¥ " + totalRevenue.divide(BigDecimal.valueOf(10000), 1, RoundingMode.HALF_UP) + " 万", "已支付订单 " + paidOrderCount + " 笔", "success"),
                        new ApiDtos.SectionMetric("项目健康度", projectHealthPercent + "%", "低风险项目 " + lowRiskCount + " 个", "success"),
                        new ApiDtos.SectionMetric("用户活跃度", userActivityPercent + "%", "启用账号 " + enabledUsers + " / " + totalUsers, "info")
                ),
                List.of(
                        new ApiDtos.SectionMetric("平均响应时间", avgResponse + " ms", "待处理告警 " + openAlertCount + " 条", "success"),
                        new ApiDtos.SectionMetric("系统可用性", String.format("%.2f%%", availability), "基于近期告警与工单估算", "success"),
                        new ApiDtos.SectionMetric("工单闭环时长", feedbackCycle + " 分钟", "未关闭工单 " + openFeedbackCount + " 条", openFeedbackCount > 0 ? "warning" : "info")
                ),
                reports.stream()
                        .map(item -> new ApiDtos.AdminTrendPoint(item.getStatDate().format(DateTimeFormatter.ofPattern("MM-dd")), item.getPaymentAmount().divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP).intValue()))
                        .toList(),
                reports.stream()
                        .map(item -> new ApiDtos.AdminTrendPoint(item.getStatDate().format(DateTimeFormatter.ofPattern("MM-dd")), safeInt(item.getTaskCount()) + safeInt(item.getOrderCount()) + safeInt(item.getTrainingCount())))
                        .toList(),
                List.of(
                        new ApiDtos.AdminDistributionItem("健康项目", (int) lowRiskCount),
                        new ApiDtos.AdminDistributionItem("需关注项目", (int) mediumRiskCount),
                        new ApiDtos.AdminDistributionItem("高风险项目", (int) highRiskCount)
                ),
                buildServicePerformanceMetrics(reports, openFeedbackCount, openAlertCount),
                List.of(
                        new ApiDtos.AdminDistributionItem("反馈工单", (int) openFeedbackCount),
                        new ApiDtos.AdminDistributionItem("权限复核", (int) pendingTaskCount),
                        new ApiDtos.AdminDistributionItem("风险告警", (int) openAlertCount),
                        new ApiDtos.AdminDistributionItem("项目跟进", Math.max(1, projects.size()))
                )
        );
    }

    public ApiDtos.AdminSettingsView getAdminSettings() {
        return new ApiDtos.AdminSettingsView(
                new ApiDtos.AdminBasicSettings(
                        settingValue("basic.stationName", "低空驿站一站式数字化服务平台"),
                        settingValue("basic.serviceHotline", "400-820-2026"),
                        settingValue("basic.defaultRegion", "深圳南山"),
                        parseBooleanSetting("basic.mobileDashboardEnabled", true)
                ),
                new ApiDtos.AdminSecuritySettings(
                        parseIntSetting("security.passwordValidityDays", 90),
                        parseIntSetting("security.loginRetryLimit", 5),
                        settingValue("security.ipWhitelist", ""),
                        parseBooleanSetting("security.mfaRequired", true)
                ),
                adminNotificationRuleMapper.selectList(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                                .orderByAsc(AdminNotificationRuleEntity::getId))
                        .stream()
                        .map(this::toAdminNotificationRule)
                        .toList()
        );
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminBasicSettings(SessionUser admin, ApiDtos.AdminBasicSettings request) {
        ensureRole(admin, RoleType.ADMIN);
        saveSetting("basic.stationName", request.stationName());
        saveSetting("basic.serviceHotline", request.serviceHotline());
        saveSetting("basic.defaultRegion", request.defaultRegion());
        saveSetting("basic.mobileDashboardEnabled", String.valueOf(request.mobileDashboardEnabled()));
        audit(admin, "ADMIN_SETTINGS", "basic", "UPDATE", "station=" + request.stationName());
        return getAdminSettings();
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminSecuritySettings(SessionUser admin, ApiDtos.AdminSecuritySettings request) {
        ensureRole(admin, RoleType.ADMIN);
        saveSetting("security.passwordValidityDays", String.valueOf(request.passwordValidityDays()));
        saveSetting("security.loginRetryLimit", String.valueOf(request.loginRetryLimit()));
        saveSetting("security.ipWhitelist", defaultIfBlank(request.ipWhitelist(), ""));
        saveSetting("security.mfaRequired", String.valueOf(request.mfaRequired()));
        audit(admin, "ADMIN_SETTINGS", "security", "UPDATE", "retryLimit=" + request.loginRetryLimit());
        return getAdminSettings();
    }

    @Transactional
    public ApiDtos.AdminSettingsView saveAdminNotificationRules(SessionUser admin, ApiDtos.AdminNotificationRulesRequest request) {
        ensureRole(admin, RoleType.ADMIN);
        List<ApiDtos.AdminNotificationRule> rules = request.rules() == null ? List.of() : request.rules();
        Set<String> ruleKeys = rules.stream()
                .map(rule -> defaultIfBlank(rule.id(), "rule-" + Math.abs(rule.name().hashCode())))
                .collect(Collectors.toSet());
        if (!ruleKeys.isEmpty()) {
            adminNotificationRuleMapper.delete(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                    .notIn(AdminNotificationRuleEntity::getRuleKey, ruleKeys));
        }
        for (ApiDtos.AdminNotificationRule rule : rules) {
            upsertNotificationRule(rule);
        }
        audit(admin, "ADMIN_SETTINGS", "notifications", "UPDATE", "rules=" + rules.size());
        return getAdminSettings();
    }

    public List<ApiDtos.FeedbackTicketView> listAdminFeedbackTickets() {
        List<FeedbackTicketEntity> tickets = feedbackTicketMapper.selectList(new LambdaQueryWrapper<FeedbackTicketEntity>()
                .orderByDesc(FeedbackTicketEntity::getCreateTime)
                .orderByDesc(FeedbackTicketEntity::getId));
        Map<Long, UserAccountEntity> owners = findUsersByIds(tickets.stream()
                .map(FeedbackTicketEntity::getSubmitUserId)
                .collect(Collectors.toSet()));
        return tickets.stream()
                .map(ticket -> toFeedbackTicketView(ticket, displayName(owners.get(ticket.getSubmitUserId()))))
                .toList();
    }

    @Transactional
    public ApiDtos.FeedbackTicketView replyFeedbackTicket(SessionUser admin, Long ticketId, ApiDtos.FeedbackTicketReplyRequest request) {
        ensureRole(admin, RoleType.ADMIN);
        FeedbackTicketEntity ticket = getFeedbackTicket(ticketId);
        ticket.setReply(normalizeNullable(request.reply()));
        ticket.setStatus(normalizeStatus(request.status()));
        if ("CLOSED".equals(ticket.getStatus())) {
            ticket.setCloseTime(LocalDateTime.now());
        } else {
            ticket.setCloseTime(null);
        }
        feedbackTicketMapper.updateById(ticket);
        audit(admin, "FEEDBACK", ticket.getTicketNo(), "REPLY", defaultIfBlank(ticket.getReply(), ticket.getStatus()));
        String submitterName = displayName(getUserById(ticket.getSubmitUserId()));
        return toFeedbackTicketView(ticket, submitterName);
    }

    private ApiDtos.AuthPayload buildAuthPayload(UserAccountEntity user) {
        SessionUser sessionUser = toSessionUser(user);
        String refreshToken = refreshTokenStore.issueToken(user);
        audit(user.getId(), user.getRole(), "AUTH", user.getUsername(), "LOGIN", user.getRole());
        return new ApiDtos.AuthPayload(
                tokenService.createToken(sessionUser),
                refreshToken,
                toSessionInfo(user)
        );
    }

    private void ensureRole(SessionUser user, RoleType expected) {
        if (user.role() != expected && user.role() != RoleType.ADMIN) {
            throw new BizException(403, "当前角色无权执行该操作");
        }
    }

    private RoleType parseRole(String role) {
        try {
            return RoleType.valueOf(role.toUpperCase());
        } catch (Exception ex) {
            throw new BizException(400, "角色不合法");
        }
    }

    private void audit(SessionUser actor, String bizType, String bizId, String eventType, String payload) {
        Long actorUserId = actor == null ? null : actor.id();
        String actorRole = actor == null ? null : actor.role().name();
        audit(actorUserId, actorRole, bizType, bizId, eventType, payload);
    }

    private void audit(Long actorUserId, String actorRole, String bizType, String bizId, String eventType, String payload) {
        auditLogService.record(
                RequestIdContext.get(),
                actorUserId,
                actorRole,
                bizType,
                bizId,
                eventType,
                payload);
    }

    private void seedDemoCoursesIfNecessary() {
        seedCourseIfMissing(
                "低空巡检项目经理训练营",
                "低空驿站企业学院",
                2L,
                "OFFLINE",
                "适合企业项目负责人，聚焦排班、风险协同与回款节奏。",
                "课程大纲：项目启动会模板、飞手排班与设备准备清单、风险处理与回款节奏管理。\n产出模板：调度看板、风险沟通表、项目复盘模板。",
                30,
                18,
                new BigDecimal("1999"),
                126,
                32);
        seedCourseIfMissing(
                "飞手夜航与应急处置专项课",
                "低空驿站飞行学院",
                3L,
                "ARTICLE",
                "适合飞手，覆盖夜航检查、应急返航和现场沟通模板。",
                "课程内容：夜航前设备检查、异常返航与失联处置 SOP、与企业调度员的关键口令模板。",
                999,
                999,
                BigDecimal.ZERO,
                218,
                56);
        seedCourseIfMissing(
                "企业与飞手协同沟通实战课",
                "低空驿站协同中心",
                2L,
                "ARTICLE",
                "通过真实场景演练项目确认、设备交接和复盘反馈。",
                "章节安排：任务确认、设备交接、项目复盘。\n课程亮点：适合企业和飞手共同学习，方便在消息中心直接演练协同话术。",
                999,
                999,
                new BigDecimal("99"),
                164,
                41);
    }

    private void seedCourseIfMissing(
            String title,
            String institutionName,
            Long publishUserId,
            String courseType,
            String summary,
            String content,
            int seatTotal,
            int seatAvailable,
            BigDecimal price,
            int browseCount,
            int enrollCount
    ) {
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<CourseEntity>()
                .eq(CourseEntity::getTitle, title));
        if (count != null && count > 0) {
            return;
        }
        CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setInstitutionName(institutionName);
        course.setPublishUserId(publishUserId);
        course.setCourseType(courseType);
        course.setSummary(summary);
        course.setContent(content);
        course.setSeatTotal(seatTotal);
        course.setSeatAvailable(seatAvailable);
        course.setPrice(price);
        course.setStatus("OPEN");
        course.setBrowseCount(browseCount);
        course.setEnrollCount(enrollCount);
        course.setCreateTime(LocalDateTime.now());
        courseMapper.insert(course);
    }

    private void seedDemoConversationsIfNecessary() {
        UserAccountEntity pilot = findUserByUsername("pilot_demo");
        UserAccountEntity enterprise = findUserByUsername("enterprise_demo");
        if (pilot == null || enterprise == null) {
            return;
        }
        MessageConversationEntity existing = messageConversationMapper.selectOne(new LambdaQueryWrapper<MessageConversationEntity>()
                .eq(MessageConversationEntity::getEnterpriseId, enterprise.getId())
                .eq(MessageConversationEntity::getPilotId, pilot.getId())
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        TaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getEnterpriseId, enterprise.getId())
                .orderByAsc(TaskEntity::getId)
                .last("limit 1"));
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setEnterpriseId(enterprise.getId());
        conversation.setPilotId(pilot.getId());
        conversation.setTaskId(task == null ? null : task.getId());
        conversation.setTitle(task == null ? "企业与飞手协同沟通" : task.getTitle() + " 协同沟通");
        conversation.setLastMessageTime(LocalDateTime.now());
        messageConversationMapper.insert(conversation);
        seedMessageEntry(conversation.getId(), enterprise, "欢迎加入本次项目沟通群，今天先确认巡检航线和到场时间。");
        seedMessageEntry(conversation.getId(), pilot, "已收到，我会在今晚 19:00 前确认绕飞方案并同步设备状态。");
        seedMessageEntry(conversation.getId(), enterprise, "好的，如需调整任务窗口请直接在这里留言，我会实时跟进。");
    }

    private void seedMessageEntry(Long conversationId, UserAccountEntity sender, String content) {
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setConversationId(conversationId);
        entry.setSenderUserId(sender.getId());
        entry.setSenderRole(sender.getRole());
        entry.setContent(content);
        entry.setCreateTime(LocalDateTime.now());
        messageEntryMapper.insert(entry);
    }

    private UserAccountEntity findUserByUsername(String username) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getUsername, username)
                .last("limit 1"));
    }

    private UserAccountEntity findUserByPhone(String phone) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getPhone, phone)
                .last("limit 1"));
    }

    private UserAccountEntity findUserByEmail(String email) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getEmail, email)
                .last("limit 1"));
    }

    private UserAccountEntity getUserByUsername(String username) {
        UserAccountEntity user = findUserByUsername(username);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private TaskEntity getTaskById(Long id) {
        TaskEntity task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(404, "任务不存在");
        }
        return task;
    }

    private void ensureTaskOwnership(SessionUser user, TaskEntity task) {
        if (user.role() != RoleType.ADMIN && !Objects.equals(task.getEnterpriseId(), user.id())) {
            throw new BizException(403, "无权操作该任务");
        }
    }

    private FeedbackTicketEntity getFeedbackTicket(Long id) {
        FeedbackTicketEntity ticket = feedbackTicketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException(404, "工单不存在");
        }
        return ticket;
    }

    private MessageConversationEntity getConversation(Long id) {
        MessageConversationEntity conversation = messageConversationMapper.selectById(id);
        if (conversation == null) {
            throw new BizException(404, "会话不存在");
        }
        return conversation;
    }

    private List<MessageConversationEntity> findConversationsForUser(SessionUser user) {
        if (user.role() == RoleType.ENTERPRISE) {
            return messageConversationMapper.selectList(new LambdaQueryWrapper<MessageConversationEntity>()
                    .eq(MessageConversationEntity::getEnterpriseId, user.id())
                    .orderByDesc(MessageConversationEntity::getLastMessageTime)
                    .orderByDesc(MessageConversationEntity::getId));
        }
        if (user.role() == RoleType.PILOT) {
            return messageConversationMapper.selectList(new LambdaQueryWrapper<MessageConversationEntity>()
                    .eq(MessageConversationEntity::getPilotId, user.id())
                    .orderByDesc(MessageConversationEntity::getLastMessageTime)
                    .orderByDesc(MessageConversationEntity::getId));
        }
        return List.of();
    }

    private void ensureConversationAccess(SessionUser user, MessageConversationEntity conversation) {
        boolean accessible = user.role() == RoleType.ADMIN
                || Objects.equals(conversation.getEnterpriseId(), user.id())
                || Objects.equals(conversation.getPilotId(), user.id());
        if (!accessible) {
            throw new BizException(403, "当前用户无权访问该会话");
        }
    }

    private UserAccountEntity resolveCounterpart(SessionUser user, MessageConversationEntity conversation, Map<Long, UserAccountEntity> users) {
        Long counterpartId = Objects.equals(conversation.getEnterpriseId(), user.id())
                ? conversation.getPilotId()
                : conversation.getEnterpriseId();
        return users.get(counterpartId);
    }

    private MessageEntryEntity findLatestMessage(Long conversationId) {
        return messageEntryMapper.selectOne(new LambdaQueryWrapper<MessageEntryEntity>()
                .eq(MessageEntryEntity::getConversationId, conversationId)
                .orderByDesc(MessageEntryEntity::getCreateTime)
                .orderByDesc(MessageEntryEntity::getId)
                .last("limit 1"));
    }

    private int countCounterpartMessages(Long conversationId, Long currentUserId) {
        Long count = messageEntryMapper.selectCount(new LambdaQueryWrapper<MessageEntryEntity>()
                .eq(MessageEntryEntity::getConversationId, conversationId)
                .ne(MessageEntryEntity::getSenderUserId, currentUserId));
        return count == null ? 0 : count.intValue();
    }

    private ApiDtos.MessageEntryView toMessageEntryView(MessageEntryEntity entry, UserAccountEntity sender, Long currentUserId) {
        MessageConversationEntity conversation = getConversation(entry.getConversationId());
        return new ApiDtos.MessageEntryView(
                entry.getId(),
                entry.getSenderUserId(),
                toUid(conversation.getPilotId()),
                toUid(conversation.getEnterpriseId()),
                displayName(sender),
                displayRole(entry.getSenderRole()),
                entry.getContent(),
                formatDateTime(entry.getCreateTime()),
                Objects.equals(entry.getSenderUserId(), currentUserId),
                Objects.equals(entry.getSenderUserId(), currentUserId));
    }

    private String conversationTitle(MessageConversationEntity conversation, UserAccountEntity counterpart) {
        String counterpartName = displayName(counterpart);
        String title = defaultIfBlank(conversation.getTitle(), "协同沟通");
        if (counterpartName.equals("-")) {
            return title;
        }
        return counterpartName + " / " + title;
    }

    private String conversationSubtitle(MessageConversationEntity conversation) {
        if (conversation.getTaskId() == null) {
            return "企业与飞手实时协同";
        }
        try {
            TaskEntity task = getTaskById(conversation.getTaskId());
            return defaultIfBlank(task.getLocation(), "协同沟通") + " · " + defaultIfBlank(task.getTaskType(), "任务沟通");
        } catch (BizException ignored) {
            return "企业与飞手实时协同";
        }
    }

    private Map<Long, UserAccountEntity> findUsersByIds(Collection<Long> ids) {
        Set<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, Function.identity()));
    }

    private SessionUser toSessionUser(UserAccountEntity user) {
        return new SessionUser(user.getId(), user.getUsername(), RoleType.valueOf(user.getRole()), displayName(user));
    }

    private ApiDtos.SessionInfo toSessionInfo(UserAccountEntity user) {
        return new ApiDtos.SessionInfo(user.getId(), user.getUsername(), user.getRole(), user.getRealName(), user.getCompanyName());
    }

    private ApiDtos.OrderView toOrderView(BizOrderEntity order) {
        return new ApiDtos.OrderView(order.getId(), order.getOrderNo(), order.getTaskId(), order.getPilotId(), order.getEnterpriseId(), order.getAmount(), order.getStatus());
    }

    private boolean canAccessOrder(SessionUser user, BizOrderEntity order) {
        return user.role() == RoleType.ADMIN
                || Objects.equals(order.getPilotId(), user.id())
                || Objects.equals(order.getEnterpriseId(), user.id());
    }

    private PaymentOrderEntity findLatestPayment(Long orderId) {
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getBizOrderId, orderId)
                .orderByDesc(PaymentOrderEntity::getId)
                .last("limit 1"));
    }

    private String formatTime(LocalDateTime time) {
        return TIME_FORMATTER.format(time);
    }

    private String formatDateTime(LocalDateTime time) {
        if (time == null) {
            return "-";
        }
        return TIME_FORMATTER.format(time);
    }

    private LocalDateTime parseTaskDeadline(String deadline) {
        try {
            return LocalDateTime.parse(deadline, TIME_FORMATTER);
        } catch (Exception ex) {
            throw new BizException(400, "任务截止时间格式不正确");
        }
    }

    private ApiDtos.TaskView toTaskView(TaskEntity task, UserAccountEntity owner) {
        return new ApiDtos.TaskView(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getLocation(),
                formatDateTime(task.getDeadline()),
                task.getBudget(),
                task.getStatus(),
                displayName(owner));
    }

    private ApiDtos.FeedbackTicketView toFeedbackTicketView(FeedbackTicketEntity ticket, String submitterName) {
        return new ApiDtos.FeedbackTicketView(
                ticket.getId(),
                ticket.getTicketNo(),
                submitterName,
                displayRole(ticket.getSubmitUserRole()),
                defaultIfBlank(ticket.getContact(), "未填写"),
                ticket.getDetail(),
                mapFeedbackStatus(ticket.getStatus()),
                defaultIfBlank(ticket.getReply(), "待客服回复"),
                formatDateTime(ticket.getCreateTime()),
                formatDateTime(ticket.getUpdateTime()),
                ticket.getCloseTime() == null ? "-" : formatDateTime(ticket.getCloseTime()));
    }

    private String mapFeedbackStatus(String status) {
        if ("PROCESSING".equalsIgnoreCase(status)) {
            return "处理中";
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            return "已关闭";
        }
        return "待处理";
    }

    private String normalizeStatus(String status) {
        if ("已关闭".equals(status) || "CLOSED".equalsIgnoreCase(status)) {
            return "CLOSED";
        }
        if ("处理中".equals(status) || "PROCESSING".equalsIgnoreCase(status)) {
            return "PROCESSING";
        }
        return "OPEN";
    }

    private BigDecimal defaultBudget(BigDecimal budget) {
        return budget == null ? BigDecimal.ZERO : budget;
    }

    private String defaultIfBlank(String text, String fallback) {
        return isBlank(text) ? fallback : text.trim();
    }

    private ApiDtos.AdminUserView toAdminUserView(UserAccountEntity user) {
        return new ApiDtos.AdminUserView(
                String.valueOf(user.getId()),
                user.getUsername(),
                defaultIfBlank(user.getEmail(), "未配置"),
                displayName(user),
                defaultIfBlank(user.getCompanyName(), "低空驿站运营中心"),
                defaultIfBlank(user.getPhone(), "未配置"),
                mapAdminUserStatus(user.getStatus()),
                formatDateTime(user.getCreateTime()),
                List.of(displayRole(user.getRole())),
                permissionGroupName(user.getRole()),
                defaultIfBlank(user.getRole(), "未定义"));
    }

    private String mapAdminUserStatus(Integer status) {
        if (status == null) {
            return "待审核";
        }
        if (status <= 0) {
            return "停用";
        }
        return "启用";
    }

    private String displayRole(String role) {
        if (isBlank(role)) {
            return "未定义角色";
        }
        try {
            return switch (RoleType.valueOf(role)) {
                case ADMIN -> "管理员";
                case ENTERPRISE -> "企业";
                case PILOT -> "飞手";
                case INSTITUTION -> "机构";
            };
        } catch (IllegalArgumentException ex) {
            return role;
        }
    }

    private String permissionGroupName(String role) {
        if (isBlank(role)) {
            return "未分配";
        }
        return switch (RoleType.valueOf(role)) {
            case ADMIN -> "全量管控组";
            case ENTERPRISE -> "项目运营组";
            case PILOT -> "执行协同组";
            case INSTITUTION -> "培训管理组";
        };
    }

    private void validateUserUniqueness(String username, String phone, String email, Long excludeUserId) {
        String normalizedUsername = username.trim();
        String normalizedPhone = phone.trim();
        String normalizedEmail = normalizeNullable(email);

        UserAccountEntity sameUsername = findUserByUsername(normalizedUsername);
        if (sameUsername != null && !Objects.equals(sameUsername.getId(), excludeUserId)) {
            throw new BizException(400, "用户名已存在");
        }

        UserAccountEntity samePhone = findUserByPhone(normalizedPhone);
        if (samePhone != null && !Objects.equals(samePhone.getId(), excludeUserId)) {
            throw new BizException(400, "手机号已存在");
        }

        if (!isBlank(normalizedEmail)) {
            UserAccountEntity sameEmail = findUserByEmail(normalizedEmail);
            if (sameEmail != null && !Objects.equals(sameEmail.getId(), excludeUserId)) {
                throw new BizException(400, "邮箱已存在");
            }
        }
    }

    private void validateAdminOperationTarget(UserAccountEntity user) {
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BizException(400, "默认管理员账号不允许修改或删除");
        }
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

    private String mapAdminProjectStatus(String status) {
        if ("REVIEWING".equalsIgnoreCase(status)) {
            return "规划中";
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return "执行中";
        }
        if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return "已完成";
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return "已暂停";
        }
        return "规划中";
    }

    private int estimateProjectProgress(String status) {
        if ("REVIEWING".equalsIgnoreCase(status)) {
            return 20;
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return 68;
        }
        if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return 100;
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return 56;
        }
        return 32;
    }

    private String estimateRiskLevel(TaskEntity task, List<BizOrderEntity> taskOrders) {
        if ("REVIEWING".equalsIgnoreCase(task.getStatus())) {
            return "中";
        }
        if (taskOrders.isEmpty()) {
            return "中";
        }
        return "低";
    }

    private int estimateTrainingCompletion(TaskEntity task) {
        if ("MAPPING".equalsIgnoreCase(task.getTaskType())) {
            return 72;
        }
        if ("INSPECTION".equalsIgnoreCase(task.getTaskType())) {
            return 88;
        }
        return 60;
    }

    private String resolvePaymentStatus(List<BizOrderEntity> taskOrders) {
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

    private String buildOrderRemark(TaskEntity task) {
        if ("MAPPING".equalsIgnoreCase(task.getTaskType())) {
            return "请提前校准测绘设备，并在预约时间前 30 分钟到场。";
        }
        if ("INSPECTION".equalsIgnoreCase(task.getTaskType())) {
            return "请优先完成重点航段巡检，并同步回传关键风险点照片。";
        }
        return "请按预约时间到场执行，并在完工后及时回传结果。";
    }

    private int buildOperationRadius(TaskEntity task) {
        if ("MAPPING".equalsIgnoreCase(task.getTaskType())) {
            return 1200;
        }
        if ("INSPECTION".equalsIgnoreCase(task.getTaskType())) {
            return 900;
        }
        return 700;
    }

    private Map<Long, TaskEntity> findTasksByIds(Set<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                        .in(TaskEntity::getId, taskIds))
                .stream()
                .collect(Collectors.toMap(TaskEntity::getId, Function.identity()));
    }

    private Map<Long, PaymentOrderEntity> findLatestPayments(Set<Long> orderIds) {
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

    private ApiDtos.AdminOrderSummaryView toAdminOrderSummaryView(BizOrderEntity order, TaskEntity task, PaymentOrderEntity payment) {
        String paymentMethod = payment == null ? "待支付" : paymentChannelLabel(payment.getChannel());
        return new ApiDtos.AdminOrderSummaryView(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                task == null ? "未知项目" : task.getTitle(),
                defaultBudget(order.getAmount()),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                formatDateTime(order.getCreateTime()),
                paymentMethod,
                task == null ? "暂无任务详情" : buildOrderRemark(task)
        );
    }

    private ApiDtos.AdminOrderDetailView toAdminOrderDetailView(BizOrderEntity order, TaskEntity task, PaymentOrderEntity payment) {
        String paymentMethod = payment == null ? "待支付" : paymentChannelLabel(payment.getChannel());
        return new ApiDtos.AdminOrderDetailView(
                String.valueOf(order.getId()),
                order.getOrderNo(),
                task.getTitle(),
                defaultBudget(order.getAmount()),
                "PAID".equalsIgnoreCase(order.getStatus()) ? "已支付" : "待支付",
                formatDateTime(order.getCreateTime()),
                paymentMethod,
                buildOrderRemark(task)
        );
    }

    private List<ReportDailySummaryEntity> loadRecentReports(int limit) {
        List<ReportDailySummaryEntity> reports = reportDailySummaryMapper.selectList(new LambdaQueryWrapper<ReportDailySummaryEntity>()
                .orderByAsc(ReportDailySummaryEntity::getStatDate));
        if (reports.isEmpty()) {
            return buildFallbackReports(limit);
        }
        if (reports.size() <= limit) {
            return reports;
        }
        return reports.subList(reports.size() - limit, reports.size());
    }

    private List<ReportDailySummaryEntity> buildFallbackReports(int limit) {
        List<ReportDailySummaryEntity> result = new ArrayList<>();
        long taskCount = countTasks();
        long orderCount = countOrders();
        long openAlertCount = alertService.listAlerts().stream()
                .filter(item -> !"RESOLVED".equalsIgnoreCase(item.status()))
                .count();
        long openCourseCount = countOpenCourses();
        BigDecimal paidAmount = bizOrderMapper.selectList(new LambdaQueryWrapper<BizOrderEntity>()
                        .eq(BizOrderEntity::getStatus, "PAID"))
                .stream()
                .map(BizOrderEntity::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (int i = limit - 1; i >= 0; i--) {
            ReportDailySummaryEntity item = new ReportDailySummaryEntity();
            item.setStatDate(LocalDate.now().minusDays(i));
            item.setTaskCount((int) taskCount);
            item.setOrderCount((int) orderCount);
            item.setPaymentAmount(paidAmount);
            item.setAlertCount((int) openAlertCount);
            item.setTrainingCount((int) openCourseCount);
            result.add(item);
        }
        return result;
    }

    private List<ApiDtos.AdminServiceMetric> buildServicePerformanceMetrics(
            List<ReportDailySummaryEntity> reports,
            long openFeedbackCount,
            long openAlertCount
    ) {
        ReportDailySummaryEntity latest = reports.isEmpty() ? null : reports.get(reports.size() - 1);
        int baseTaskCount = latest == null ? (int) countTasks() : safeInt(latest.getTaskCount());
        int baseOrderCount = latest == null ? (int) countOrders() : safeInt(latest.getOrderCount());
        int baseTrainingCount = latest == null ? (int) countOpenCourses() : safeInt(latest.getTrainingCount());
        int baseAlertCount = latest == null ? (int) openAlertCount : safeInt(latest.getAlertCount());
        return List.of(
                new ApiDtos.AdminServiceMetric("认证服务", 120 + Math.min(60, (int) countUsers()), Math.max(97.8d, 99.96d - openAlertCount * 0.08d)),
                new ApiDtos.AdminServiceMetric("项目服务", 160 + Math.min(80, baseTaskCount * 4), Math.max(97.2d, 99.92d - baseAlertCount * 0.12d)),
                new ApiDtos.AdminServiceMetric("订单服务", 150 + Math.min(90, baseOrderCount * 5), Math.max(97.0d, 99.88d - baseAlertCount * 0.10d)),
                new ApiDtos.AdminServiceMetric("工单服务", 140 + Math.min(100, (int) openFeedbackCount * 9 + baseTrainingCount), Math.max(96.8d, 99.85d - openFeedbackCount * 0.14d))
        );
    }

    private String settingValue(String key, String fallback) {
        AdminSettingEntity entity = adminSettingMapper.selectById(key);
        return entity == null || isBlank(entity.getSettingValue()) ? fallback : entity.getSettingValue().trim();
    }

    private boolean parseBooleanSetting(String key, boolean fallback) {
        String value = settingValue(key, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private int parseIntSetting(String key, int fallback) {
        try {
            return Integer.parseInt(settingValue(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void saveSetting(String key, String value) {
        AdminSettingEntity entity = new AdminSettingEntity();
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        entity.setUpdateTime(LocalDateTime.now());
        adminSettingMapper.insertOrUpdate(entity);
    }

    private void upsertNotificationRule(ApiDtos.AdminNotificationRule rule) {
        String ruleKey = defaultIfBlank(rule.id(), "rule-" + Math.abs(rule.name().hashCode()));
        AdminNotificationRuleEntity entity = adminNotificationRuleMapper.selectOne(new LambdaQueryWrapper<AdminNotificationRuleEntity>()
                .eq(AdminNotificationRuleEntity::getRuleKey, ruleKey)
                .last("limit 1"));
        if (entity == null) {
            entity = new AdminNotificationRuleEntity();
            entity.setRuleKey(ruleKey);
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setName(rule.name().trim());
        entity.setChannel(rule.channel().trim());
        entity.setEnabled(rule.enabled() ? 1 : 0);
        entity.setTriggerDesc(rule.trigger().trim());
        entity.setUpdateTime(LocalDateTime.now());
        adminNotificationRuleMapper.insertOrUpdate(entity);
    }

    private ApiDtos.AdminNotificationRule toAdminNotificationRule(AdminNotificationRuleEntity entity) {
        return new ApiDtos.AdminNotificationRule(
                entity.getRuleKey(),
                entity.getName(),
                entity.getChannel(),
                safeInt(entity.getEnabled()) > 0,
                entity.getTriggerDesc()
        );
    }

    private String paymentChannelLabel(String channel) {
        if ("ALIPAY".equalsIgnoreCase(channel)) {
            return "支付宝";
        }
        if ("WECHAT".equalsIgnoreCase(channel) || "WECHAT_PAY".equalsIgnoreCase(channel)) {
            return "微信支付";
        }
        if ("BANK_TRANSFER".equalsIgnoreCase(channel)) {
            return "银行转账";
        }
        return defaultIfBlank(channel, "待支付");
    }

    private CourseEntity getCourse(Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        return course;
    }

    private void ensureCourseManager(SessionUser user) {
        if (user.role() != RoleType.ENTERPRISE && user.role() != RoleType.INSTITUTION && user.role() != RoleType.ADMIN) {
            throw new BizException(403, "当前角色无权管理课程");
        }
    }

    private void ensureCourseOwnership(SessionUser user, CourseEntity course) {
        if (!canManageCourse(user, course)) {
            throw new BizException(403, "无权操作该课程");
        }
    }

    private boolean canManageCourse(SessionUser user, CourseEntity course) {
        return user.role() == RoleType.ADMIN || Objects.equals(course.getPublishUserId(), user.id());
    }

    private ApiDtos.CourseView toCourseView(CourseEntity course) {
        return new ApiDtos.CourseView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCourseType(),
                course.getInstitutionName(),
                safeInt(course.getSeatAvailable()),
                safeInt(course.getBrowseCount()),
                safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus()
        );
    }

    private ApiDtos.CourseDetailView toCourseDetailView(CourseEntity course, CourseEnrollmentEntity enrollment) {
        return new ApiDtos.CourseDetailView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getContent(),
                course.getCourseType(),
                course.getInstitutionName(),
                safeInt(course.getSeatTotal()),
                safeInt(course.getSeatAvailable()),
                safeInt(course.getBrowseCount()),
                safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus(),
                enrollment != null,
                enrollment == null ? null : enrollment.getEnrollmentNo(),
                enrollment == null ? null : enrollment.getStatus()
        );
    }

    private CourseEnrollmentEntity findActiveEnrollment(Long courseId, Long userId) {
        if (courseId == null || userId == null) {
            return null;
        }
        return courseEnrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getUserId, userId)
                .orderByDesc(CourseEnrollmentEntity::getId)
                .last("limit 1"));
    }

    private UserAccountEntity requireUserByUid(String uid) {
        if (isBlank(uid)) {
            throw new BizException(404, "用户不存在");
        }
        if (uid.chars().allMatch(Character::isDigit)) {
            return getUserById(Long.parseLong(uid));
        }
        return getUserByUsername(uid.trim());
    }

    private String toUid(Long userId) {
        return userId == null ? "" : String.valueOf(userId);
    }

    private ApiDtos.CourseManageView toCourseManageView(CourseEntity course) {
        return new ApiDtos.CourseManageView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCourseType(),
                safeInt(course.getSeatTotal()),
                safeInt(course.getSeatAvailable()),
                safeInt(course.getBrowseCount()),
                safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus()
        );
    }

    private CourseEntity buildCourseEntity(SessionUser user, CourseEntity original, ApiDtos.CourseManageRequest request) {
        CourseEntity course = original == null ? new CourseEntity() : original;
        course.setTitle(request.title().trim());
        course.setSummary(request.summary().trim());
        course.setContent(request.content().trim());
        course.setCourseType(normalizeCourseType(request.learningMode()));
        course.setSeatTotal(Math.max(0, request.seatTotal()));
        course.setSeatAvailable(resolveSeatAvailable(original, request.seatTotal()));
        course.setPrice(request.price());
        course.setStatus(normalizeCourseStatus(request.status()));
        course.setInstitutionName(resolveInstitutionName(user));
        course.setPublishUserId(original == null ? user.id() : original.getPublishUserId());
        if (original == null) {
            course.setBrowseCount(0);
            course.setEnrollCount(0);
        }
        return course;
    }

    private int resolveSeatAvailable(CourseEntity original, Integer seatTotal) {
        int total = Math.max(0, seatTotal == null ? 0 : seatTotal);
        if (original == null) {
            return total;
        }
        int oldTotal = safeInt(original.getSeatTotal());
        int oldAvailable = safeInt(original.getSeatAvailable());
        int used = Math.max(0, oldTotal - oldAvailable);
        return Math.max(0, total - used);
    }

    private String resolveInstitutionName(SessionUser user) {
        UserAccountEntity owner = getUserById(user.id());
        if (!isBlank(owner.getCompanyName())) {
            return owner.getCompanyName();
        }
        return displayName(owner);
    }

    private String normalizeCourseType(String learningMode) {
        if ("OFFLINE".equalsIgnoreCase(learningMode)) {
            return "OFFLINE";
        }
        return "ARTICLE";
    }

    private String normalizeCourseStatus(String status) {
        if ("OPEN".equalsIgnoreCase(status)) {
            return "OPEN";
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            return "CLOSED";
        }
        return "DRAFT";
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String displayName(UserAccountEntity user) {
        if (user == null || isBlank(user.getRealName())) {
            return user == null ? "-" : user.getUsername();
        }
        return user.getRealName();
    }

    private String safePhone(UserAccountEntity user) {
        return user == null || isBlank(user.getPhone()) ? "-" : user.getPhone();
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private long countUsers() {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countUsersByRole(RoleType role) {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getRole, role.name()));
    }

    private long countTasks() {
        return taskMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countOrders() {
        return bizOrderMapper.selectCount(new LambdaQueryWrapper<>());
    }

    private long countOpenCourses() {
        return courseMapper.selectCount(new LambdaQueryWrapper<CourseEntity>()
                .eq(CourseEntity::getStatus, "OPEN"));
    }
}
