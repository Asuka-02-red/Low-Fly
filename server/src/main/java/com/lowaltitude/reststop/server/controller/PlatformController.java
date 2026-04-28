package com.lowaltitude.reststop.server.controller;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.ApiResponse;
import com.lowaltitude.reststop.server.security.SessionUser;
import com.lowaltitude.reststop.server.service.DemoPlatformService;
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

    private final DemoPlatformService platformService;

    public PlatformController(DemoPlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/users/me")
    public ApiResponse<ApiDtos.SessionInfo> currentUser(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.currentUser(user));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ApiDtos.TaskView>> listTasks(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.listTasks(user));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<ApiDtos.TaskDetailView> getTaskDetail(@PathVariable Long taskId) {
        return ApiResponse.success("查询成功", platformService.getTaskDetail(taskId));
    }

    @PostMapping("/tasks")
    public ApiResponse<ApiDtos.TaskView> createTask(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.TaskRequest request) {
        return ApiResponse.success("任务创建成功", platformService.createTask(user, request));
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<ApiDtos.TaskView> updateTask(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long taskId,
            @Valid @RequestBody ApiDtos.TaskRequest request
    ) {
        return ApiResponse.success("任务更新成功", platformService.updateTask(user, taskId, request));
    }

    @PostMapping("/tasks/{taskId}/publish")
    public ApiResponse<ApiDtos.TaskView> publishTask(@AuthenticationPrincipal SessionUser user, @PathVariable Long taskId) {
        return ApiResponse.success("任务重新发布成功", platformService.publishTask(user, taskId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<ApiDtos.OrderView>> listOrders(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.listOrders(user));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ApiDtos.OrderDetailView> getOrderDetail(@AuthenticationPrincipal SessionUser user, @PathVariable Long orderId) {
        return ApiResponse.success("查询成功", platformService.getOrderDetail(user, orderId));
    }

    @PostMapping("/orders")
    public ApiResponse<ApiDtos.OrderView> createOrder(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.OrderCreateRequest request) {
        return ApiResponse.success("订单创建成功", platformService.createOrder(user, request));
    }

    @PostMapping("/orders/pay")
    public ApiResponse<ApiDtos.PaymentResult> payOrder(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.PaymentRequest request) {
        return ApiResponse.success("支付成功", platformService.payOrder(user, request));
    }

    @GetMapping("/compliance/no-fly-zones")
    public ApiResponse<List<ApiDtos.NoFlyZoneView>> listZones() {
        return ApiResponse.success("查询成功", platformService.listZones());
    }

    @PostMapping("/compliance/flight-applications")
    public ApiResponse<ApiDtos.FlightApplicationView> submitApplication(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.FlightApplicationRequest request
    ) {
        return ApiResponse.success("申请已提交", platformService.submitFlightApplication(user, request));
    }

    @PostMapping("/feedback/tickets")
    public ApiResponse<ApiDtos.FeedbackTicketView> createFeedbackTicket(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.FeedbackTicketRequest request
    ) {
        return ApiResponse.success("工单提交成功", platformService.createFeedbackTicket(user, request));
    }

    @GetMapping("/feedback/tickets/mine")
    public ApiResponse<List<ApiDtos.FeedbackTicketView>> listMyFeedbackTickets(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.listMyFeedbackTickets(user));
    }

    @GetMapping("/messages/conversations")
    public ApiResponse<List<ApiDtos.MessageConversationView>> listMessageConversations(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.listMessageConversations(user));
    }

    @GetMapping("/messages/conversations/{conversationId}")
    public ApiResponse<ApiDtos.MessageThreadView> getMessageThread(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long conversationId
    ) {
        return ApiResponse.success("查询成功", platformService.getMessageThread(user, conversationId));
    }

    @PostMapping("/message/readReceipt")
    public ApiResponse<ApiDtos.MessageReadReceiptResponse> syncReadReceipts(
            @AuthenticationPrincipal SessionUser user,
            @Valid @RequestBody ApiDtos.MessageReadReceiptRequest request
    ) {
        return ApiResponse.success("同步成功", platformService.syncReadReceipts(user, request));
    }

    @PostMapping("/messages/conversations/{conversationId}/messages")
    public ApiResponse<ApiDtos.MessageThreadView> sendMessage(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long conversationId,
            @Valid @RequestBody ApiDtos.MessageSendRequest request
    ) {
        return ApiResponse.success("发送成功", platformService.sendMessage(user, conversationId, request));
    }

    @GetMapping("/user/pilot/{uid}/profile")
    public ApiResponse<ApiDtos.PilotProfileView> getPilotProfile(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String uid
    ) {
        return ApiResponse.success("查询成功", platformService.getPilotProfile(user, uid));
    }

    @GetMapping("/enterprise/{uid}/info")
    public ApiResponse<ApiDtos.EnterpriseInfoView> getEnterpriseInfo(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable String uid
    ) {
        return ApiResponse.success("查询成功", platformService.getEnterpriseInfo(user, uid));
    }

    @GetMapping("/risk/alerts")
    public ApiResponse<List<ApiDtos.AlertView>> listAlerts() {
        return ApiResponse.success("查询成功", platformService.listAlerts());
    }

    @GetMapping("/training/courses")
    public ApiResponse<List<ApiDtos.CourseView>> listCourses() {
        return ApiResponse.success("查询成功", platformService.listCourses());
    }

    @GetMapping("/training/courses/{courseId}")
    public ApiResponse<ApiDtos.CourseDetailView> getCourseDetail(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("查询成功", platformService.getCourseDetail(user, courseId));
    }

    @GetMapping("/training/courses/manage")
    public ApiResponse<List<ApiDtos.CourseManageView>> listManagedCourses(@AuthenticationPrincipal SessionUser user) {
        return ApiResponse.success("查询成功", platformService.listManagedCourses(user));
    }

    @PostMapping("/training/courses/manage")
    public ApiResponse<ApiDtos.CourseManageView> createCourse(@AuthenticationPrincipal SessionUser user, @Valid @RequestBody ApiDtos.CourseManageRequest request) {
        return ApiResponse.success("课程创建成功", platformService.createCourse(user, request));
    }

    @PutMapping("/training/courses/manage/{courseId}")
    public ApiResponse<ApiDtos.CourseManageView> updateCourse(
            @AuthenticationPrincipal SessionUser user,
            @PathVariable Long courseId,
            @Valid @RequestBody ApiDtos.CourseManageRequest request
    ) {
        return ApiResponse.success("课程更新成功", platformService.updateCourse(user, courseId, request));
    }

    @PostMapping("/training/courses/manage/{courseId}/publish")
    public ApiResponse<ApiDtos.CourseManageView> publishCourse(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("课程发布成功", platformService.publishCourse(user, courseId));
    }

    @DeleteMapping("/training/courses/manage/{courseId}")
    public ApiResponse<Void> deleteCourse(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        platformService.deleteCourse(user, courseId);
        return ApiResponse.success("课程删除成功", null);
    }

    @PostMapping("/training/courses/{courseId}/enroll")
    public ApiResponse<ApiDtos.EnrollmentResult> enroll(@AuthenticationPrincipal SessionUser user, @PathVariable Long courseId) {
        return ApiResponse.success("报名成功", platformService.enrollCourse(user, courseId));
    }
}
