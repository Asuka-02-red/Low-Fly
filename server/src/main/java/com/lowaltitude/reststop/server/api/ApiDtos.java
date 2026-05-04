package com.lowaltitude.reststop.server.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * API 数据传输对象（DTO）集合。
 * <p>
 * 本类采用 Java Record 集中定义所有 REST 接口的请求体（Request）、响应体（Response）
 * 及视图对象（View），作为 Controller 层与 Service 层之间的数据契约。
 * </p>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>使用 Java 16+ Record 语法，自动生成不可变对象、构造器、getter、equals/hashCode/toString</li>
 *   <li>请求体通过 Jakarta Validation 注解（@NotBlank、@Size、@Pattern 等）声明校验规则，
 *       由 Spring MVC 的 @Valid 或 @Validated 触发校验</li>
 *   <li>视图对象不包含校验注解，仅用于向前端返回数据</li>
 *   <li>私有构造器防止外部实例化本工具类</li>
 * </ul>
 *
 * <h3>模块划分</h3>
 * <ul>
 *   <li>认证模块：LoginRequest / RegisterRequest / RefreshTokenRequest / SessionInfo / AuthPayload</li>
 *   <li>任务模块：TaskRequest / TaskView / TaskDetailView</li>
 *   <li>订单模块：OrderCreateRequest / OrderView / OrderDetailView</li>
 *   <li>支付模块：PaymentRequest / PaymentResult</li>
 *   <li>禁飞区模块：NoFlyZoneView</li>
 *   <li>飞行申请模块：FlightApplicationRequest / FlightApplicationView</li>
 *   <li>反馈工单模块：FeedbackTicketRequest / FeedbackTicketReplyRequest / FeedbackTicketView</li>
 *   <li>消息模块：MessageSendRequest / MessageReadReceiptRequest / MessageReadReceiptResponse /
 *       MessageConversationView / MessageEntryView / MessageThreadView</li>
 *   <li>用户角色视图：PilotProfileView / EnterpriseInfoView</li>
 *   <li>告警模块：AlertView</li>
 *   <li>课程培训模块：CourseView / CourseDetailView / CourseManageRequest / CourseManageView / EnrollmentResult</li>
 *   <li>管理后台仪表盘：DashboardMetric / DashboardActivity / DashboardNotice / DashboardOverview</li>
 *   <li>管理后台天气与适飞：AdminFlightConditionCheck / AdminFlightSuitabilityView / AdminRealtimeWeatherView</li>
 *   <li>管理后台分区摘要：SectionMetric / AdminSectionSummary</li>
 *   <li>管理后台审计：AuditEventView</li>
 *   <li>管理后台用户管理：AdminUserView / AdminUserCreateRequest / AdminUserUpdateRequest</li>
 *   <li>管理后台项目管理：AdminProjectView</li>
 *   <li>管理后台数据分析：AdminTrendPoint / AdminDistributionItem / AdminServiceMetric / AdminAnalyticsView</li>
 *   <li>管理后台系统设置：AdminBasicSettings / AdminSecuritySettings / AdminNotificationRule /
 *       AdminNotificationRulesRequest / AdminSettingsView</li>
 *   <li>管理后台订单管理：AdminOrderSummaryView / AdminOrderDetailView</li>
 * </ul>
 */
public final class ApiDtos {

    /**
     * 私有构造器，防止外部实例化。
     * 本类仅作为 DTO 的命名空间容器，所有内部 Record 均为静态内部类。
     */
    private ApiDtos() {
    }

    // ========================================================================
    // 认证模块（Auth）
    // ========================================================================

    /**
     * 用户登录请求体。
     * <p>对应接口：POST /api/auth/login</p>
     *
     * @param username 用户名，不可为空
     * @param password 密码，不可为空
     */
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /**
     * 用户注册请求体。
     * <p>对应接口：POST /api/auth/register</p>
     *
     * @param username    用户名，4~50字符，不可为空
     * @param password    密码，8~64字符，不可为空（要求包含大小写字母+数字+特殊字符）
     * @param phone       手机号，必须符合中国大陆11位手机号格式（1开头+10位数字）
     * @param role        用户角色，最长20字符，不可为空（如：PILOT、ENTERPRISE、ADMIN）
     * @param realName    真实姓名，可选
     * @param companyName 所属企业名称，可选（企业用户必填）
     */
    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @NotBlank @Size(max = 20) String role,
            String realName,
            String companyName
    ) {
    }

    /**
     * 刷新令牌请求体。
     * <p>对应接口：POST /api/auth/refresh-token</p>
     * <p>当 Access Token 过期时，前端使用 Refresh Token 换取新的 Access Token，
     * 避免用户频繁重新登录。</p>
     *
     * @param refreshToken 刷新令牌，不可为空
     */
    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    /**
     * 会话用户信息视图。
     * <p>登录成功后返回的用户基本信息，嵌入在 AuthPayload 中一起返回给前端。</p>
     *
     * @param id          用户主键ID
     * @param username    用户名
     * @param role        角色编码（PILOT / ENTERPRISE / ADMIN）
     * @param realName    真实姓名
     * @param companyName 所属企业名称
     */
    public record SessionInfo(Long id, String username, String role, String realName, String companyName) {
    }

    /**
     * 认证成功后的完整载荷。
     * <p>登录和刷新令牌接口的响应体。</p>
     *
     * @param token        JWT Access Token，用于后续请求的 Authorization: Bearer {token}
     * @param refreshToken Refresh Token，用于在 Access Token 过期后换取新令牌
     * @param userInfo     当前登录用户的会话信息
     */
    public record AuthPayload(String token, String refreshToken, SessionInfo userInfo) {
    }

    // ========================================================================
    // 任务模块（Task）
    // ========================================================================

    /**
     * 创建任务请求体。
     * <p>对应接口：POST /api/platform/tasks</p>
     * <p>企业用户发布飞行任务，飞手可接单执行。任务类型包括航拍、巡检、物流等。</p>
     *
     * @param taskType   任务类型，不可为空（如：AERIAL_PHOTOGRAPHY / INSPECTION / LOGISTICS）
     * @param title      任务标题，最长100字符，不可为空
     * @param description 任务详细描述，不可为空
     * @param location   任务执行地点，最长255字符，不可为空
     * @param deadline   截止时间，最长16字符（格式：yyyy-MM-dd HH:mm），不可为空
     * @param latitude   任务执行纬度，范围 [-90.0, 90.0]，不可为空
     * @param longitude  任务执行经度，范围 [-180.0, 180.0]，不可为空
     * @param budget     任务预算金额，最小0.01，不可为空
     */
    public record TaskRequest(
            @NotBlank String taskType,
            @NotBlank @Size(max = 100) String title,
            @NotBlank String description,
            @NotBlank @Size(max = 255) String location,
            @NotBlank @Size(max = 16) String deadline,
            @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
            @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
            @NotNull @DecimalMin("0.01") BigDecimal budget
    ) {
    }

    /**
     * 任务列表项视图。
     * <p>用于任务列表页展示，仅包含摘要字段，不包含详细描述。</p>
     *
     * @param id        任务主键ID
     * @param title     任务标题
     * @param taskType  任务类型
     * @param location  执行地点
     * @param deadline  截止时间
     * @param budget    预算金额
     * @param status    任务状态（PENDING / ASSIGNED / IN_PROGRESS / COMPLETED / CANCELLED）
     * @param ownerName 发布者姓名
     */
    public record TaskView(Long id, String title, String taskType, String location, String deadline, BigDecimal budget, String status, String ownerName) {
    }

    /**
     * 任务详情视图。
     * <p>用于任务详情页展示，包含完整信息，包括航线起点坐标和作业半径。</p>
     *
     * @param id                    任务主键ID
     * @param title                 任务标题
     * @param taskType              任务类型
     * @param description           任务详细描述
     * @param location              执行地点
     * @param deadline              截止时间
     * @param latitude              任务执行纬度
     * @param longitude             任务执行经度
     * @param routeStartLatitude    航线起点纬度，用于飞行路径规划
     * @param routeStartLongitude   航线起点经度，用于飞行路径规划
     * @param operationRadiusMeters 作业半径（单位：米），定义无人机飞行覆盖范围
     * @param budget                预算金额
     * @param status                任务状态
     * @param ownerName             发布者姓名
     */
    public record TaskDetailView(
            Long id,
            String title,
            String taskType,
            String description,
            String location,
            String deadline,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal routeStartLatitude,
            BigDecimal routeStartLongitude,
            Integer operationRadiusMeters,
            BigDecimal budget,
            String status,
            String ownerName
    ) {
    }

    // ========================================================================
    // 订单模块（Order）
    // ========================================================================

    /**
     * 创建订单请求体。
     * <p>对应接口：POST /api/platform/orders</p>
     * <p>飞手接受任务后，系统自动生成关联订单。</p>
     *
     * @param taskId 关联的任务ID，不可为空
     */
    public record OrderCreateRequest(@NotNull Long taskId) {
    }

    /**
     * 订单列表项视图。
     * <p>用于订单列表页展示，包含订单核心字段。</p>
     *
     * @param id           订单主键ID
     * @param orderNo      订单编号（系统自动生成的唯一业务编号）
     * @param taskId       关联任务ID
     * @param pilotId      飞手用户ID
     * @param enterpriseId 企业用户ID
     * @param amount       订单金额
     * @param status       订单状态（PENDING / PAID / IN_PROGRESS / COMPLETED / CANCELLED / REFUNDED）
     */
    public record OrderView(Long id, String orderNo, Long taskId, Long pilotId, Long enterpriseId, BigDecimal amount, String status) {
    }

    /**
     * 订单详情视图。
     * <p>用于订单详情页展示，包含完整的订单和关联任务信息。</p>
     *
     * @param id              订单主键ID
     * @param orderNo         订单编号
     * @param status          订单状态
     * @param amount          订单金额
     * @param taskId          关联任务ID
     * @param taskTitle       关联任务标题
     * @param taskType        关联任务类型
     * @param location        执行地点
     * @param pilotName       飞手姓名
     * @param enterpriseName  企业名称
     * @param contactName     联系人姓名
     * @param contactPhone    联系人电话
     * @param paymentChannel  支付渠道（ALIPAY / WECHAT / BANK_TRANSFER）
     * @param paymentStatus   支付状态（UNPAID / PAID / REFUNDING / REFUNDED）
     * @param createdAt       创建时间
     * @param appointmentTime 预约执行时间
     * @param remark          备注信息
     */
    public record OrderDetailView(
            Long id,
            String orderNo,
            String status,
            BigDecimal amount,
            Long taskId,
            String taskTitle,
            String taskType,
            String location,
            String pilotName,
            String enterpriseName,
            String contactName,
            String contactPhone,
            String paymentChannel,
            String paymentStatus,
            String createdAt,
            String appointmentTime,
            String remark
    ) {
    }

    // ========================================================================
    // 支付模块（Payment）
    // ========================================================================

    /**
     * 支付请求体。
     * <p>对应接口：POST /api/platform/payments</p>
     * <p>企业用户对指定订单发起支付。</p>
     *
     * @param orderId 订单ID，不可为空
     * @param channel 支付渠道，最长20字符，不可为空（ALIPAY / WECHAT / BANK_TRANSFER）
     */
    public record PaymentRequest(@NotNull Long orderId, @NotBlank @Size(max = 20) String channel) {
    }

    /**
     * 支付结果视图。
     * <p>支付请求处理后返回的结果信息。</p>
     *
     * @param tradeNo 支付流水号（第三方支付平台返回的交易号）
     * @param status  支付状态（SUCCESS / FAILED / PENDING）
     * @param amount  实际支付金额
     */
    public record PaymentResult(String tradeNo, String status, BigDecimal amount) {
    }

    // ========================================================================
    // 禁飞区模块（NoFlyZone）
    // ========================================================================

    /**
     * 禁飞区视图。
     * <p>用于在地图上展示禁飞区域信息，飞手在执行任务前需避开禁飞区。</p>
     *
     * @param id          禁飞区主键ID
     * @param name        禁飞区名称
     * @param zoneType    区域类型（PERMANENT / TEMPORARY / RESTRICTED）
     * @param centerLat   圆心纬度
     * @param centerLng   圆心经度
     * @param radius      禁飞半径（单位：米）
     * @param description 禁飞区描述（含禁飞原因、生效时间等）
     */
    public record NoFlyZoneView(
            Long id,
            String name,
            String zoneType,
            BigDecimal centerLat,
            BigDecimal centerLng,
            int radius,
            String description
    ) {
    }

    // ========================================================================
    // 飞行申请模块（FlightApplication）
    // ========================================================================

    /**
     * 飞行申请请求体。
     * <p>对应接口：POST /api/platform/flight-applications</p>
     * <p>飞手在执行飞行任务前，需向平台提交飞行申请，经审批后方可执行。</p>
     *
     * @param location   飞行地点，最长255字符，不可为空
     * @param flightTime 飞行时间，最长50字符，不可为空
     * @param purpose    飞行目的，最长255字符，不可为空
     */
    public record FlightApplicationRequest(
            @NotBlank @Size(max = 255) String location,
            @NotBlank @Size(max = 50) String flightTime,
            @NotBlank @Size(max = 255) String purpose
    ) {
    }

    /**
     * 飞行申请视图。
     * <p>提交飞行申请后返回的审批状态信息。</p>
     *
     * @param applicationNo 申请编号（系统自动生成）
     * @param status        审批状态（PENDING / APPROVED / REJECTED）
     * @param location      飞行地点
     * @param approvalHint  审批提示信息（如驳回原因或审批通过备注）
     */
    public record FlightApplicationView(String applicationNo, String status, String location, String approvalHint) {
    }

    // ========================================================================
    // 反馈工单模块（FeedbackTicket）
    // ========================================================================

    /**
     * 提交反馈工单请求体。
     * <p>对应接口：POST /api/platform/feedback-tickets</p>
     * <p>用户可提交问题反馈或建议，管理员在后台进行回复处理。</p>
     *
     * @param contact 联系方式，最长100字符，可选
     * @param detail  反馈详情，10~2000字符，不可为空
     */
    public record FeedbackTicketRequest(
            @Size(max = 100) String contact,
            @NotBlank @Size(min = 10, max = 2000) String detail
    ) {
    }

    /**
     * 管理员回复反馈工单请求体。
     * <p>对应接口：PUT /api/admin/feedback-tickets/{ticketId}</p>
     *
     * @param status 工单状态，最长20字符，不可为空（OPEN / REPLIED / CLOSED）
     * @param reply  管理员回复内容，最长1000字符，可选
     */
    public record FeedbackTicketReplyRequest(
            @NotBlank @Size(max = 20) String status,
            @Size(max = 1000) String reply
    ) {
    }

    /**
     * 反馈工单视图。
     * <p>用于展示工单完整信息，包括提交者信息、处理状态和管理员回复。</p>
     *
     * @param id            工单主键ID
     * @param ticketNo      工单编号（系统自动生成，格式如 TK-20250101-0001）
     * @param submitterName 提交者姓名
     * @param submitterRole 提交者角色
     * @param contact       联系方式
     * @param detail        反馈详情
     * @param status        工单状态（OPEN / REPLIED / CLOSED）
     * @param reply         管理员回复内容
     * @param createTime    创建时间
     * @param updateTime    最后更新时间
     * @param closedTime    关闭时间（仅状态为CLOSED时有值）
     */
    public record FeedbackTicketView(
            Long id,
            String ticketNo,
            String submitterName,
            String submitterRole,
            String contact,
            String detail,
            String status,
            String reply,
            String createTime,
            String updateTime,
            String closedTime
    ) {
    }

    // ========================================================================
    // 消息模块（Message）
    // ========================================================================

    /**
     * 发送消息请求体。
     * <p>对应接口：POST /api/platform/messages</p>
     * <p>在会话中发送一条新消息，消息归属到当前用户与对方的会话中。</p>
     *
     * @param content 消息内容，1~1000字符，不可为空
     */
    public record MessageSendRequest(
            @NotBlank @Size(min = 1, max = 1000) String content
    ) {
    }

    /**
     * 消息已读回执请求体。
     * <p>对应接口：POST /api/platform/messages/read-receipt</p>
     * <p>客户端批量上报已读消息ID，服务端更新消息的已读状态。</p>
     *
     * @param msgIds 已读消息ID列表，1~200条，不可为空
     */
    public record MessageReadReceiptRequest(
            @NotNull @Size(min = 1, max = 200) List<Long> msgIds
    ) {
    }

    /**
     * 消息已读回执响应体。
     * <p>返回已读回执同步结果。</p>
     *
     * @param successCount 成功标记已读的消息数量
     * @param syncedMsgIds 成功同步的消息ID列表
     */
    public record MessageReadReceiptResponse(
            int successCount,
            List<Long> syncedMsgIds
    ) {
    }

    /**
     * 消息会话视图。
     * <p>用于消息列表页展示，每个会话代表飞手与企业之间的一次对话。</p>
     *
     * @param id                  会话主键ID
     * @param title               会话标题
     * @param subtitle            会话副标题
     * @param counterpartName     对方用户姓名
     * @param counterpartUid      对方用户UID
     * @param counterpartRole     对方用户角色
     * @param pilotUid            飞手UID
     * @param enterpriseUid       企业UID
     * @param lastMessagePreview  最后一条消息的预览文本（截断显示）
     * @param lastMessageTime     最后一条消息的时间
     * @param unreadCount         未读消息数量
     */
    public record MessageConversationView(
            Long id,
            String title,
            String subtitle,
            String counterpartName,
            String counterpartUid,
            String counterpartRole,
            String pilotUid,
            String enterpriseUid,
            String lastMessagePreview,
            String lastMessageTime,
            int unreadCount
    ) {
    }

    /**
     * 消息条目视图。
     * <p>用于聊天界面展示单条消息，包含发送者信息和已读状态。</p>
     *
     * @param id            消息主键ID
     * @param senderId      发送者用户ID
     * @param pilotUid      所属飞手UID
     * @param enterpriseUid 所属企业UID
     * @param senderName    发送者姓名
     * @param senderRole    发送者角色
     * @param content       消息内容
     * @param createTime    发送时间
     * @param isRead        是否已读
     * @param mine          是否为当前登录用户发送的消息（前端据此区分气泡左右位置）
     */
    public record MessageEntryView(
            Long id,
            Long senderId,
            String pilotUid,
            String enterpriseUid,
            String senderName,
            String senderRole,
            String content,
            String createTime,
            boolean isRead,
            boolean mine
    ) {
    }

    /**
     * 消息会话线程视图。
     * <p>进入某个会话后，返回该会话的完整消息列表。</p>
     *
     * @param conversationId 会话ID
     * @param title          会话标题
     * @param subtitle       会话副标题
     * @param pilotUid       飞手UID
     * @param enterpriseUid  企业UID
     * @param messages       该会话下的所有消息条目列表
     */
    public record MessageThreadView(
            Long conversationId,
            String title,
            String subtitle,
            String pilotUid,
            String enterpriseUid,
            List<MessageEntryView> messages
    ) {
    }

    // ========================================================================
    // 用户角色视图
    // ========================================================================

    /**
     * 飞手档案视图。
     * <p>展示飞手的基本身份信息。</p>
     *
     * @param uid  飞手唯一标识
     * @param name 飞手姓名
     */
    public record PilotProfileView(String uid, String name) {
    }

    /**
     * 企业信息视图。
     * <p>展示企业的基本身份信息。</p>
     *
     * @param uid         企业唯一标识
     * @param companyName 企业名称
     */
    public record EnterpriseInfoView(String uid, String companyName) {
    }

    // ========================================================================
    // 告警模块（Alert）
    // ========================================================================

    /**
     * 告警视图。
     * <p>用于展示系统告警信息，如禁飞区变更、天气预警、设备异常等。</p>
     *
     * @param id         告警主键ID
     * @param level      告警级别（INFO / WARNING / CRITICAL）
     * @param content    告警内容描述
     * @param status     告警状态（ACTIVE / ACKNOWLEDGED / RESOLVED）
     * @param createTime 告警产生时间
     */
    public record AlertView(Long id, String level, String content, String status, LocalDateTime createTime) {
    }

    // ========================================================================
    // 课程培训模块（Course）
    // ========================================================================

    /**
     * 课程列表项视图。
     * <p>用于课程列表页展示，包含课程摘要信息。</p>
     *
     * @param id               课程主键ID
     * @param title            课程标题
     * @param summary          课程摘要
     * @param learningMode     学习模式（ONLINE / OFFLINE / HYBRID）
     * @param institutionName  培训机构名称
     * @param seatAvailable    剩余可用座位数
     * @param browseCount      浏览次数
     * @param enrollCount      已报名人数
     * @param price            课程价格
     * @param status           课程状态（DRAFT / PUBLISHED / ARCHIVED）
     */
    public record CourseView(
            Long id,
            String title,
            String summary,
            String learningMode,
            String institutionName,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status
    ) {
    }

    /**
     * 课程详情视图。
     * <p>用于课程详情页展示，包含完整课程信息及当前用户的报名状态。</p>
     *
     * @param id               课程主键ID
     * @param title            课程标题
     * @param summary          课程摘要
     * @param content          课程详细内容（富文本）
     * @param learningMode     学习模式
     * @param institutionName  培训机构名称
     * @param seatTotal        总座位数
     * @param seatAvailable    剩余可用座位数
     * @param browseCount      浏览次数
     * @param enrollCount      已报名人数
     * @param price            课程价格
     * @param status           课程状态
     * @param enrolled         当前用户是否已报名
     * @param enrollmentNo     报名编号（已报名时返回）
     * @param enrollmentStatus 报名状态（ENROLLED / CANCELLED / COMPLETED）
     */
    public record CourseDetailView(
            Long id,
            String title,
            String summary,
            String content,
            String learningMode,
            String institutionName,
            int seatTotal,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status,
            boolean enrolled,
            String enrollmentNo,
            String enrollmentStatus
    ) {
    }

    /**
     * 管理员创建/编辑课程请求体。
     * <p>对应接口：POST/PUT /api/admin/courses</p>
     *
     * @param title        课程标题，最长100字符，不可为空
     * @param summary      课程摘要，最长255字符，不可为空
     * @param content      课程详细内容，不可为空
     * @param learningMode 学习模式，最长20字符，不可为空（ONLINE / OFFLINE / HYBRID）
     * @param seatTotal    总座位数，最小1，不可为空
     * @param price        课程价格，最小0.00，不可为空
     * @param status       课程状态，最长20字符，不可为空（DRAFT / PUBLISHED / ARCHIVED）
     */
    public record CourseManageRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 255) String summary,
            @NotBlank String content,
            @NotBlank @Size(max = 20) String learningMode,
            @NotNull @Min(1) Integer seatTotal,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @NotBlank @Size(max = 20) String status
    ) {
    }

    /**
     * 管理员课程管理视图。
     * <p>用于管理后台课程列表展示。</p>
     *
     * @param id            课程主键ID
     * @param title         课程标题
     * @param summary       课程摘要
     * @param learningMode  学习模式
     * @param seatTotal     总座位数
     * @param seatAvailable 剩余可用座位数
     * @param browseCount   浏览次数
     * @param enrollCount   已报名人数
     * @param price         课程价格
     * @param status        课程状态
     */
    public record CourseManageView(
            Long id,
            String title,
            String summary,
            String learningMode,
            int seatTotal,
            int seatAvailable,
            int browseCount,
            int enrollCount,
            BigDecimal price,
            String status
    ) {
    }

    /**
     * 课程报名结果视图。
     * <p>用户报名课程后返回的报名确认信息。</p>
     *
     * @param enrollmentNo  报名编号（系统自动生成）
     * @param status        报名状态（ENROLLED / CANCELLED / COMPLETED）
     * @param courseTitle   报名课程标题
     * @param seatAvailable 报名后剩余可用座位数
     */
    public record EnrollmentResult(String enrollmentNo, String status, String courseTitle, int seatAvailable) {
    }

    // ========================================================================
    // 管理后台仪表盘模块（Dashboard）
    // ========================================================================

    /**
     * 仪表盘指标项。
     * <p>用于展示仪表盘中的关键指标卡片，如"今日订单数"、"活跃飞手数"等。</p>
     *
     * @param label  指标名称
     * @param value  指标值（字符串形式，便于前端直接展示，如"128"、"98.5%"）
     * @param trend  趋势方向（UP / DOWN / FLAT）
     * @param status 状态标识（info / success / warning / danger，用于前端配色）
     */
    public record DashboardMetric(String label, String value, String trend, String status) {
    }

    /**
     * 仪表盘活动项。
     * <p>用于展示最近的平台活动动态，如"飞手张三完成了巡检任务"。</p>
     *
     * @param title   活动标题
     * @param content 活动内容
     * @param time    活动时间
     * @param tag     活动标签（用于前端分类展示，如"任务"、"订单"、"告警"）
     */
    public record DashboardActivity(String title, String content, String time, String tag) {
    }

    /**
     * 仪表盘通知项。
     * <p>用于展示系统通知公告，如"系统维护通知"、"政策更新"等。</p>
     *
     * @param title 通知标题
     * @param level 通知级别（info / warning / critical）
     * @param time  通知时间
     */
    public record DashboardNotice(String title, String level, String time) {
    }

    /**
     * 仪表盘概览视图。
     * <p>管理后台首页的完整数据概览，包含指标、活动、通知及图表数据。</p>
     *
     * @param metrics             核心业务指标列表（如订单数、飞手数、营收等）
     * @param deviceStats         设备状态指标列表（如在线设备数、离线设备数等）
     * @param activities          最近活动列表
     * @param notices             系统通知列表
     * @param projectDistribution 项目分布数据（用于饼图展示）
     * @param progressTrend       进度趋势数据（用于折线图展示）
     */
    public record DashboardOverview(
            List<DashboardMetric> metrics,
            List<DashboardMetric> deviceStats,
            List<DashboardActivity> activities,
            List<DashboardNotice> notices,
            List<AdminDistributionItem> projectDistribution,
            List<AdminTrendPoint> progressTrend
    ) {
    }

    // ========================================================================
    // 管理后台天气与适飞评估模块
    // ========================================================================

    /**
     * 飞行条件检查项。
     * <p>展示单项飞行条件的检查结果，如"风速 3.2m/s ≤ 8m/s ✓"。</p>
     *
     * @param label       检查项名称（如"风速"、"能见度"、"降水概率"）
     * @param currentValue 当前实际值
     * @param threshold    安全阈值
     * @param passed       是否通过检查
     */
    public record AdminFlightConditionCheck(String label, String currentValue, String threshold, boolean passed) {
    }

    /**
     * 飞行适飞性评估视图。
     * <p>综合各项天气条件，给出飞行适飞性评估结论。</p>
     *
     * @param result         评估结论（适宜飞行 / 不适宜飞行 / 条件飞行）
     * @param level          评估等级（SAFE / CAUTION / DANGER）
     * @param summary        评估摘要
     * @param checks         各项飞行条件检查列表
     * @param conditionNotes 条件备注列表（补充说明影响飞行的天气因素）
     * @param recommendations 建议措施列表（如"建议降低飞行高度"、"建议推迟起飞"）
     */
    public record AdminFlightSuitabilityView(
            String result,
            String level,
            String summary,
            List<AdminFlightConditionCheck> checks,
            List<String> conditionNotes,
            List<String> recommendations
    ) {
    }

    /**
     * 管理后台实时天气视图。
     * <p>整合高德天气/和风天气API数据，提供实时天气和适飞评估。</p>
     *
     * @param serviceName             天气服务名称（如"高德天气"、"和风天气"）
     * @param locationName            位置名称
     * @param adcode                  行政区划代码
     * @param weather                 天气现象描述（如"晴"、"多云"、"小雨"）
     * @param reportTime              天气数据报告时间
     * @param fetchedAt               服务端获取时间
     * @param refreshInterval         刷新间隔说明
     * @param temperature             温度（摄氏度）
     * @param humidity                湿度（百分比）
     * @param windDirection           风向（如"北风"、"东北风"）
     * @param windPower               风力等级（如"3级"）
     * @param windSpeed               风速（m/s）
     * @param visibility              能见度（km）
     * @param precipitationProbability 降水概率（百分比）
     * @param precipitationIntensity  降水强度（mm/h）
     * @param thunderstormRisk        雷暴风险等级（低 / 中 / 高）
     * @param sourceNote              数据来源说明
     * @param suitability             飞行适飞性评估结果
     */
    public record AdminRealtimeWeatherView(
            String serviceName,
            String locationName,
            String adcode,
            String weather,
            String reportTime,
            String fetchedAt,
            String refreshInterval,
            double temperature,
            int humidity,
            String windDirection,
            String windPower,
            double windSpeed,
            double visibility,
            int precipitationProbability,
            double precipitationIntensity,
            String thunderstormRisk,
            String sourceNote,
            AdminFlightSuitabilityView suitability
    ) {
    }

    // ========================================================================
    // 管理后台分区摘要模块
    // ========================================================================

    /**
     * 分区指标项。
     * <p>与 DashboardMetric 结构相同，用于各业务分区的指标展示。</p>
     *
     * @param label  指标名称
     * @param value  指标值
     * @param trend  趋势方向
     * @param status 状态标识
     */
    public record SectionMetric(String label, String value, String trend, String status) {
    }

    /**
     * 管理后台分区摘要视图。
     * <p>每个业务分区（如任务管理、合规管理、培训中心等）的概要信息。</p>
     *
     * @param sectionKey  分区唯一标识（如 tasks / compliance / training）
     * @param title       分区标题
     * @param headline    分区头条/摘要文字
     * @param buttonLabel 操作按钮文案（如"查看全部"、"立即管理"）
     * @param metrics     分区指标列表
     * @param highlights  分区亮点/关键信息列表
     */
    public record AdminSectionSummary(
            String sectionKey,
            String title,
            String headline,
            String buttonLabel,
            List<SectionMetric> metrics,
            List<String> highlights
    ) {
    }

    // ========================================================================
    // 管理后台审计模块（Audit）
    // ========================================================================

    /**
     * 审计事件视图。
     * <p>记录系统中所有关键操作的审计日志，用于安全审计和问题追溯。</p>
     *
     * @param requestId   请求唯一ID（由 RequestIdFilter 生成，用于链路追踪）
     * @param actorUserId 操作者用户ID（null表示系统自动操作）
     * @param actorRole   操作者角色
     * @param bizType     业务类型（如 USER / TASK / ORDER / COURSE）
     * @param bizId       业务对象ID
     * @param eventType   事件类型（如 CREATE / UPDATE / DELETE / LOGIN）
     * @param payload     事件载荷（JSON字符串，包含操作详情）
     * @param createTime  事件发生时间
     */
    public record AuditEventView(
            String requestId,
            Long actorUserId,
            String actorRole,
            String bizType,
            String bizId,
            String eventType,
            String payload,
            LocalDateTime createTime
    ) {
    }

    // ========================================================================
    // 管理后台用户管理模块
    // ========================================================================

    /**
     * 管理后台用户视图。
     * <p>用于管理后台用户列表/详情展示。</p>
     *
     * @param id                   用户ID（字符串形式）
     * @param username             用户名
     * @param email                邮箱地址
     * @param name                 真实姓名
     * @param organization         所属组织/企业
     * @param phone                手机号
     * @param status               账号状态（0-禁用 / 1-正常）
     * @param createTime           创建时间
     * @param roleNames            角色名称列表
     * @param permissionGroupName  权限组名称
     * @param roleCode             角色编码（PILOT / ENTERPRISE / ADMIN）
     */
    public record AdminUserView(
            String id,
            String username,
            String email,
            String name,
            String organization,
            String phone,
            String status,
            String createTime,
            List<String> roleNames,
            String permissionGroupName,
            String roleCode
    ) {
    }

    /**
     * 管理员创建用户请求体。
     * <p>对应接口：POST /api/admin/users</p>
     *
     * @param username    用户名，4~50字符，不可为空
     * @param password    密码，8~64字符，不可为空
     * @param phone       手机号，必须符合中国大陆11位手机号格式
     * @param email       邮箱，最长100字符，可选
     * @param role        角色，最长20字符，不可为空
     * @param realName    真实姓名，最长20字符，不可为空
     * @param companyName 企业名称，最长100字符，可选
     * @param status      账号状态，最小0，不可为空（0-禁用 / 1-正常）
     */
    public record AdminUserCreateRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(max = 100) String email,
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 20) String realName,
            @Size(max = 100) String companyName,
            @NotNull @Min(0) Integer status
    ) {
    }

    /**
     * 管理员更新用户请求体。
     * <p>对应接口：PUT /api/admin/users/{userId}</p>
     * <p>与 AdminUserCreateRequest 的区别：password 字段可选（不传则不修改密码）。</p>
     *
     * @param username    用户名，4~50字符，不可为空
     * @param password    新密码，8~64字符，可选（不传则保持原密码不变）
     * @param phone       手机号，必须符合中国大陆11位手机号格式
     * @param email       邮箱，最长100字符，可选
     * @param role        角色，最长20字符，不可为空
     * @param realName    真实姓名，最长20字符，不可为空
     * @param companyName 企业名称，最长100字符，可选
     * @param status      账号状态，最小0，不可为空
     */
    public record AdminUserUpdateRequest(
            @NotBlank @Size(min = 4, max = 50) String username,
            @Size(min = 8, max = 64) String password,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
            @Size(max = 100) String email,
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 20) String realName,
            @Size(max = 100) String companyName,
            @NotNull @Min(0) Integer status
    ) {
    }

    // ========================================================================
    // 管理后台项目管理模块
    // ========================================================================

    /**
     * 管理后台项目视图。
     * <p>用于管理后台项目列表展示，包含项目完整状态信息。</p>
     *
     * @param id                  项目ID
     * @param name                项目名称
     * @param owner               项目负责人
     * @param region              项目所在区域
     * @param status              项目状态（PLANNING / IN_PROGRESS / COMPLETED / ARCHIVED）
     * @param progress            项目进度（0~100百分比）
     * @param budget              项目预算
     * @param complianceStatus    合规状态（COMPLIANT / NON_COMPLIANT / PENDING_REVIEW）
     * @param riskLevel           风险等级（LOW / MEDIUM / HIGH / CRITICAL）
     * @param trainingCompletion  培训完成率（0~100百分比）
     * @param paymentStatus       付款状态（UNPAID / PARTIAL / PAID）
     * @param updatedAt           最后更新时间
     */
    public record AdminProjectView(
            String id,
            String name,
            String owner,
            String region,
            String status,
            int progress,
            BigDecimal budget,
            String complianceStatus,
            String riskLevel,
            int trainingCompletion,
            String paymentStatus,
            String updatedAt
    ) {
    }

    // ========================================================================
    // 管理后台数据分析模块（Analytics）
    // ========================================================================

    /**
     * 趋势数据点。
     * <p>用于折线图展示，每个点代表一个时间维度的数值。</p>
     *
     * @param label 时间标签（如"1月"、"Q1"、"2025-01"）
     * @param value 对应数值
     */
    public record AdminTrendPoint(String label, int value) {
    }

    /**
     * 分布数据项。
     * <p>用于饼图/环形图展示，每个项代表一个分类的数值。</p>
     *
     * @param name  分类名称
     * @param value 分类数值
     */
    public record AdminDistributionItem(String name, int value) {
    }

    /**
     * 服务性能指标。
     * <p>用于展示各服务的性能数据。</p>
     *
     * @param label        服务名称
     * @param response     平均响应时间（毫秒）
     * @param availability 可用率（0.0~1.0，如0.999表示99.9%可用）
     */
    public record AdminServiceMetric(String label, int response, double availability) {
    }

    /**
     * 管理后台数据分析视图。
     * <p>整合业务指标、性能指标及各类图表数据，用于数据分析页面。</p>
     *
     * @param businessMetrics    业务指标列表（如营收、订单量、用户增长等）
     * @param performanceMetrics 性能指标列表（如系统响应时间、吞吐量等）
     * @param revenueTrend       营收趋势数据（折线图）
     * @param userActivity       用户活跃度趋势数据（折线图）
     * @param projectHealth      项目健康度分布数据（饼图）
     * @param servicePerformance 服务性能数据（表格展示）
     * @param operatorLoad       运营人员负载分布数据（饼图）
     */
    public record AdminAnalyticsView(
            List<SectionMetric> businessMetrics,
            List<SectionMetric> performanceMetrics,
            List<AdminTrendPoint> revenueTrend,
            List<AdminTrendPoint> userActivity,
            List<AdminDistributionItem> projectHealth,
            List<AdminServiceMetric> servicePerformance,
            List<AdminDistributionItem> operatorLoad
    ) {
    }

    // ========================================================================
    // 管理后台系统设置模块（Settings）
    // ========================================================================

    /**
     * 基本设置。
     * <p>管理后台的系统基本配置项。</p>
     *
     * @param stationName           驿站名称，最长100字符，不可为空
     * @param serviceHotline        服务热线电话，最长30字符，不可为空
     * @param defaultRegion         默认区域，最长50字符，不可为空
     * @param mobileDashboardEnabled 是否启用移动端仪表盘
     */
    public record AdminBasicSettings(
            @NotBlank @Size(max = 100) String stationName,
            @NotBlank @Size(max = 30) String serviceHotline,
            @NotBlank @Size(max = 50) String defaultRegion,
            boolean mobileDashboardEnabled
    ) {
    }

    /**
     * 安全设置。
     * <p>管理后台的安全策略配置项。</p>
     *
     * @param passwordValidityDays 密码有效期天数，最小1，不可为空
     * @param loginRetryLimit      登录重试次数限制，最小1，不可为空（超过后锁定账号）
     * @param ipWhitelist          IP白名单，最长500字符，可选（逗号分隔的IP/CIDR列表）
     * @param mfaRequired          是否强制启用多因素认证（MFA）
     */
    public record AdminSecuritySettings(
            @NotNull @Min(1) Integer passwordValidityDays,
            @NotNull @Min(1) Integer loginRetryLimit,
            @Size(max = 500) String ipWhitelist,
            boolean mfaRequired
    ) {
    }

    /**
     * 通知规则。
     * <p>定义管理后台的告警通知规则，如触发条件、通知渠道等。</p>
     *
     * @param id      规则ID（新建时为null，更新时必传）
     * @param name    规则名称，最长100字符，不可为空
     * @param channel 通知渠道，最长20字符，不可为空（EMAIL / SMS / WECHAT / DINGTALK）
     * @param enabled 是否启用
     * @param trigger 触发条件表达式，最长255字符，不可为空
     */
    public record AdminNotificationRule(
            String id,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String channel,
            boolean enabled,
            @NotBlank @Size(max = 255) String trigger
    ) {
    }

    /**
     * 通知规则批量更新请求体。
     * <p>对应接口：PUT /api/admin/settings/notifications</p>
     *
     * @param rules 通知规则列表，不可为空
     */
    public record AdminNotificationRulesRequest(@NotNull List<AdminNotificationRule> rules) {
    }

    /**
     * 系统设置视图。
     * <p>管理后台设置页面的完整数据结构，包含基本设置、安全设置和通知规则。</p>
     *
     * @param basic         基本设置
     * @param security      安全设置
     * @param notifications 通知规则列表
     */
    public record AdminSettingsView(
            AdminBasicSettings basic,
            AdminSecuritySettings security,
            List<AdminNotificationRule> notifications
    ) {
    }

    // ========================================================================
    // 管理后台订单管理模块
    // ========================================================================

    /**
     * 管理后台订单摘要视图。
     * <p>用于管理后台订单列表展示。</p>
     *
     * @param id            订单ID
     * @param orderNo       订单编号
     * @param projectName   关联项目名称
     * @param amount        订单金额
     * @param status        订单状态
     * @param createTime    创建时间
     * @param paymentMethod 支付方式
     * @param details       订单详情摘要
     */
    public record AdminOrderSummaryView(
            String id,
            String orderNo,
            String projectName,
            BigDecimal amount,
            String status,
            String createTime,
            String paymentMethod,
            String details
    ) {
    }

    /**
     * 管理后台订单详情视图。
     * <p>用于管理后台订单详情展示，结构与摘要视图一致（可按需扩展更多字段）。</p>
     *
     * @param id            订单ID
     * @param orderNo       订单编号
     * @param projectName   关联项目名称
     * @param amount        订单金额
     * @param status        订单状态
     * @param createTime    创建时间
     * @param paymentMethod 支付方式
     * @param details       订单详情
     */
    public record AdminOrderDetailView(
            String id,
            String orderNo,
            String projectName,
            BigDecimal amount,
            String status,
            String createTime,
            String paymentMethod,
            String details
    ) {
    }
}
