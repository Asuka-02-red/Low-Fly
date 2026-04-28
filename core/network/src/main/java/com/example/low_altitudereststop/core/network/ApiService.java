package com.example.low_altitudereststop.core.network;

import com.example.low_altitudereststop.core.model.AuthModels;
import com.example.low_altitudereststop.core.model.PlatformModels;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface ApiService {

    @POST("auth/login")
    Call<ApiEnvelope<AuthModels.AuthPayload>> login(@Body AuthModels.LoginRequest request);

    @POST("auth/register")
    Call<ApiEnvelope<AuthModels.AuthPayload>> register(@Body AuthModels.RegisterRequest request);

    @POST("auth/refresh")
    Call<ApiEnvelope<AuthModels.AuthPayload>> refresh(@Body AuthModels.RefreshTokenRequest request);

    @GET("users/me")
    Call<ApiEnvelope<AuthModels.SessionInfo>> me();

    @GET("tasks")
    Call<ApiEnvelope<List<PlatformModels.TaskView>>> listTasks();

    @GET("tasks/{taskId}")
    Call<ApiEnvelope<PlatformModels.TaskDetailView>> getTaskDetail(@Path("taskId") long taskId);

    @POST("tasks")
    Call<ApiEnvelope<PlatformModels.TaskView>> createTask(@Body PlatformModels.TaskRequest request);

    @PUT("tasks/{taskId}")
    Call<ApiEnvelope<PlatformModels.TaskView>> updateTask(@Path("taskId") long taskId, @Body PlatformModels.TaskRequest request);

    @POST("tasks/{taskId}/publish")
    Call<ApiEnvelope<PlatformModels.TaskView>> publishTask(@Path("taskId") long taskId);

    @GET("orders")
    Call<ApiEnvelope<List<PlatformModels.OrderView>>> listOrders();

    @GET("orders/{orderId}")
    Call<ApiEnvelope<PlatformModels.OrderDetailView>> getOrderDetail(@Path("orderId") long orderId);

    @POST("orders")
    Call<ApiEnvelope<PlatformModels.OrderView>> createOrder(@Body PlatformModels.OrderCreateRequest request);

    @POST("orders/pay")
    Call<ApiEnvelope<PlatformModels.PaymentResult>> payOrder(@Body PlatformModels.PaymentRequest request);

    @GET("compliance/no-fly-zones")
    Call<ApiEnvelope<List<PlatformModels.NoFlyZoneView>>> listNoFlyZones();

    @POST("compliance/flight-applications")
    Call<ApiEnvelope<PlatformModels.FlightApplicationView>> submitFlightApplication(@Body PlatformModels.FlightApplicationRequest request);

    @POST("feedback/tickets")
    Call<ApiEnvelope<PlatformModels.FeedbackTicketView>> createFeedbackTicket(@Body PlatformModels.FeedbackTicketRequest request);

    @GET("feedback/tickets/mine")
    Call<ApiEnvelope<List<PlatformModels.FeedbackTicketView>>> listMyFeedbackTickets();

    @GET("messages/conversations")
    Call<ApiEnvelope<List<PlatformModels.MessageConversationView>>> listMessageConversations();

    @GET("messages/conversations/{conversationId}")
    Call<ApiEnvelope<PlatformModels.MessageThreadView>> getMessageThread(@Path("conversationId") long conversationId);

    @POST("message/readReceipt")
    Call<ApiEnvelope<PlatformModels.MessageReadReceiptResponse>> sendReadReceipt(
            @Body PlatformModels.MessageReadReceiptRequest request
    );

    @POST("messages/conversations/{conversationId}/messages")
    Call<ApiEnvelope<PlatformModels.MessageThreadView>> sendMessage(
            @Path("conversationId") long conversationId,
            @Body PlatformModels.MessageSendRequest request
    );

    @GET("user/pilot/{uid}/profile")
    Call<ApiEnvelope<PlatformModels.PilotProfileView>> getPilotProfile(@Path("uid") String uid);

    @GET("enterprise/{uid}/info")
    Call<ApiEnvelope<PlatformModels.EnterpriseInfoView>> getEnterpriseInfo(@Path("uid") String uid);

    @GET("risk/alerts")
    Call<ApiEnvelope<List<PlatformModels.AlertView>>> listAlerts();

    @GET("training/courses")
    Call<ApiEnvelope<List<PlatformModels.CourseView>>> listCourses();

    @GET("training/courses/{courseId}")
    Call<ApiEnvelope<PlatformModels.CourseDetailView>> getCourseDetail(@Path("courseId") long courseId);

    @GET("training/courses/manage")
    Call<ApiEnvelope<List<PlatformModels.CourseManageView>>> listManagedCourses();

    @POST("training/courses/manage")
    Call<ApiEnvelope<PlatformModels.CourseManageView>> createCourse(@Body PlatformModels.CourseManageRequest request);

    @PUT("training/courses/manage/{courseId}")
    Call<ApiEnvelope<PlatformModels.CourseManageView>> updateCourse(@Path("courseId") long courseId, @Body PlatformModels.CourseManageRequest request);

    @POST("training/courses/manage/{courseId}/publish")
    Call<ApiEnvelope<PlatformModels.CourseManageView>> publishCourse(@Path("courseId") long courseId);

    @DELETE("training/courses/manage/{courseId}")
    Call<ApiEnvelope<Void>> deleteCourse(@Path("courseId") long courseId);

    @POST("training/courses/{courseId}/enroll")
    Call<ApiEnvelope<PlatformModels.EnrollmentResult>> enroll(@Path("courseId") long courseId);

    @GET("https://restapi.amap.com/v3/geocode/regeo")
    Call<AmapEnvelope> reverseGeocode(
            @Query("key") String key,
            @Query("location") String location,
            @Query("extensions") String extensions
    );

    @GET("https://restapi.amap.com/v3/weather/weatherInfo")
    Call<AmapEnvelope> getAmapWeather(
            @Query("key") String key,
            @Query("city") String city,
            @Query("extensions") String extensions,
            @Query("output") String output
    );
}

