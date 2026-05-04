package com.lowaltitude.reststop.server.controller;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.ApiResponse;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.service.AlertService;
import com.lowaltitude.reststop.server.service.AuthService;
import com.lowaltitude.reststop.server.service.CourseService;
import com.lowaltitude.reststop.server.service.FeedbackService;
import com.lowaltitude.reststop.server.service.MessageService;
import com.lowaltitude.reststop.server.service.OrderService;
import com.lowaltitude.reststop.server.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlatformController {

    private final AuthService authService;
    private final TaskService taskService;
    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final MessageService messageService;
    private final CourseService courseService;
    private final AlertService alertService;

    public PlatformController(
            AuthService authService,
            TaskService taskService,
            OrderService orderService,
            FeedbackService feedbackService,
            MessageService messageService,
            CourseService courseService,
            AlertService alertService
    ) {
        this.authService = authService;
        this.taskService = taskService;
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.messageService = messageService;
        this.courseService = courseService;
        this.alertService = alertService;
    }

    @GetMapping("/users/me")
    public ApiResponse<ApiDtos.SessionInfo> currentUser(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", authService.currentUser(user));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ApiDtos.TaskView>> listTasks(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", taskService.listTasks(user));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<ApiDtos.TaskDetailView> getTaskDetail(@PathVariable Long taskId) {
        return ApiResponse.success("查询成功", taskService.getTaskDetail(taskId));
    }

    @PostMapping("/tasks")
    public ApiResponse<ApiDtos.TaskView> createTask(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.TaskRequest request) {
        return ApiResponse.success("任务创建成功", taskService.createTask(user, request));
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<ApiDtos.TaskView> updateTask(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long taskId,
            @Valid @RequestBody ApiDtos.TaskRequest request
    ) {
        return ApiResponse.success("任务更新成功", taskService.updateTask(user, taskId, request));
    }

    @PostMapping("/tasks/{taskId}/publish")
    public ApiResponse<ApiDtos.TaskView> publishTask(@AuthenticationPrincipal SessionUser user, @PathVariable Long taskId) {
        return ApiResponse.success("任务重新发布成功", taskService.publishTask(user, taskId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<ApiDtos.OrderView>> listOrders(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", orderService.listOrders(user));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ApiDtos.OrderDetailView> getOrderDetail(@AuthenticationPrincipal SessionUser user, @PathVariable Long orderId) {
        return ApiResponse.success("查询成功", orderService.getOrderDetail(user, orderId));
    }

    @PostMapping("/orders")
    public ApiResponse<ApiDtos.OrderView> createOrder(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.OrderCreateRequest request) {
        return ApiResponse.success("订单创建成功", orderService.createOrder(user, request));
    }

    @PostMapping("/orders/pay")
    public ApiResponse<ApiDtos.PaymentResult> payOrder(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.PaymentRequest request) {
        return ApiResponse.success("支付成功", orderService.payOrder(user, request));
    }

    @GetMapping("/compliance/no-fly-zones")
    public ApiResponse<List<ApiDtos.NoFlyZoneView>> listZones() {
        return ApiResponse.success("查询成功", taskService.listZones());
    }

    @PostMapping("/compliance/flight-applications")
    public ApiResponse<ApiDtos.FlightApplicationView> submitApplication(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.FlightApplicationRequest request
    ) {
        return ApiResponse.success("申请已提交", taskService.submitFlightApplication(user, request));
    }

    @PostMapping("/feedback/tickets")
    public ApiResponse<ApiDtos.FeedbackTicketView> createFeedbackTicket(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.FeedbackTicketRequest request
    ) {
        return ApiResponse.success("工单提交成功", feedbackService.createFeedbackTicket(user, request));
    }

    @GetMapping("/feedback/tickets/mine")
    public ApiResponse<List<ApiDtos.FeedbackTicketView>> listMyFeedbackTickets(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", feedbackService.listMyFeedbackTickets(user));
    }

    @GetMapping("/messages/conversations")
    public ApiResponse<List<ApiDtos.MessageConversationView>> listMessageConversations(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", messageService.listMessageConversations(user));
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public ApiResponse<ApiDtos.MessageThreadView> getMessageThread(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success("查询成功", messageService.getMessageThread(user, conversationId));
    }

    @PostMapping("/message/readReceipt")
    public ApiResponse<ApiDtos.MessageReadReceiptResponse> syncReadReceipts(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.MessageReadReceiptRequest request
    ) {
        return ApiResponse.success("同步成功", messageService.syncReadReceipts(user, request));
    }

    @PostMapping("/messages/conversations/{conversationId}/messages")
    public ApiResponse<ApiDtos.MessageThreadView> sendMessage(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long conversationId,
            @Valid @RequestBody ApiDtos.MessageSendRequest request
    ) {
        return ApiResponse.success("发送成功", messageService.sendMessage(user, conversationId, request));
    }

    @GetMapping("/user/pilot/{uid}/profile")
    public ApiResponse<ApiDtos.PilotProfileView> getPilotProfile(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String uid
    ) {
        return ApiResponse.success("查询成功", messageService.getPilotProfile(user, uid));
    }

    @GetMapping("/enterprise/{uid}/info")
    public ApiResponse<ApiDtos.EnterpriseInfoView> getEnterpriseInfo(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String uid
    ) {
        return ApiResponse.success("查询成功", messageService.getEnterpriseInfo(user, uid));
    }

    @GetMapping("/risk/alerts")
    public ApiResponse<List<ApiDtos.AlertView>> listAlerts() {
        return ApiResponse.success("查询成功", alertService.listAlerts());
    }

    @GetMapping("/training/courses")
    public ApiResponse<List<ApiDtos.CourseView>> listCourses() {
        return ApiResponse.success("查询成功", courseService.listCourses());
    }

    @GetMapping("/training/courses/{courseId}")
    public ApiResponse<ApiDtos.CourseDetailView> getCourseDetail(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("查询成功", courseService.getCourseDetail(user, courseId));
    }

    @GetMapping("/training/courses/manage")
    public ApiResponse<List<ApiDtos.CourseManageView>> listManagedCourses(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", courseService.listManagedCourses(user));
    }

    @PostMapping("/training/courses/manage")
    public ApiResponse<ApiDtos.CourseManageView> createCourse(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.CourseManageRequest request) {
        return ApiResponse.success("课程创建成功", courseService.createCourse(user, request));
    }

    @PutMapping("/training/courses/manage/{courseId}")
    public ApiResponse<ApiDtos.CourseManageView> updateCourse(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody ApiDtos.CourseManageRequest request
    ) {
        return ApiResponse.success("课程更新成功", courseService.updateCourse(user, courseId, request));
    }

    @PostMapping("/training/courses/manage/{courseId}/publish")
    public ApiResponse<ApiDtos.CourseManageView> publishCourse(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("课程发布成功", courseService.publishCourse(user, courseId));
    }

    @DeleteMapping("/training/courses/manage/{courseId}")
    public ApiResponse<Void> deleteCourse(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        courseService.deleteCourse(user, courseId);
        return ApiResponse.success("课程删除成功", null);
    }

    @PostMapping("/training/courses/{courseId}/enroll")
    public ApiResponse<ApiDtos.EnrollmentResult> enroll(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("报名成功", courseService.enrollCourse(user, courseId));
    }
}
