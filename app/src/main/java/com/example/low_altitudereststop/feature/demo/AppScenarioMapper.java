package com.example.low_altitudereststop.feature.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.feature.compliance.FlightManagementModels;
import com.example.low_altitudereststop.feature.message.local.EnterpriseProfileEntity;
import com.example.low_altitudereststop.feature.message.local.MessageEntity;
import com.example.low_altitudereststop.feature.message.local.PilotProfileEntity;
import com.example.low_altitudereststop.ui.UserRole;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AppScenarioMapper {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private AppScenarioMapper() {
    }

    @NonNull
    public static List<PlatformModels.MessageConversationView> buildConversationSummaries(@NonNull UserRole role) {
        List<PlatformModels.MessageConversationView> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            AppScenarioModels.MessageLine last = lastLine(role == UserRole.ENTERPRISE ? scenario.enterpriseMessages : scenario.pilotMessages);
            if (last == null) {
                continue;
            }
            PlatformModels.MessageConversationView item = new PlatformModels.MessageConversationView();
            item.id = role == UserRole.ENTERPRISE ? scenario.conversationIdEnterprise : scenario.conversationIdPilot;
            item.title = titleForScenario(scenario);
            item.subtitle = scenario.task.title;
            item.counterpartName = role == UserRole.ENTERPRISE ? scenario.pilot.name : scenario.company.dispatchTitle;
            item.counterpartUid = role == UserRole.ENTERPRISE ? scenario.pilot.uid : scenario.company.uid;
            item.counterpartRole = role == UserRole.ENTERPRISE ? scenario.pilot.roleTitle : "企业";
            item.pilotUid = scenario.pilot.uid;
            item.enterpriseUid = scenario.company.uid;
            item.lastMessagePreview = last.content;
            item.lastMessageTime = formatTime(last.timeMillis);
            item.unreadCount = countUnread(role == UserRole.ENTERPRISE ? scenario.enterpriseMessages : scenario.pilotMessages);
            items.add(item);
        }
        items.sort((left, right) -> safe(right.lastMessageTime).compareTo(safe(left.lastMessageTime)));
        return items;
    }

    @NonNull
    public static List<MessageEntity> buildMessageEntities(@NonNull UserRole role) {
        List<MessageEntity> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            long conversationId = role == UserRole.ENTERPRISE ? scenario.conversationIdEnterprise : scenario.conversationIdPilot;
            List<AppScenarioModels.MessageLine> source = role == UserRole.ENTERPRISE ? scenario.enterpriseMessages : scenario.pilotMessages;
            for (AppScenarioModels.MessageLine line : source) {
                MessageEntity item = new MessageEntity();
                item.msgId = line.messageId;
                item.conversationId = conversationId;
                item.content = safe(line.content);
                item.senderName = safe(line.senderName);
                item.senderRole = safe(line.senderRole);
                item.pilotUid = scenario.pilot.uid;
                item.enterpriseUid = scenario.company.uid;
                item.counterpartTitle = titleForScenario(scenario);
                item.createTimeMillis = line.timeMillis;
                item.createTime = formatTime(line.timeMillis);
                item.mine = line.mine;
                item.isRead = line.isRead || line.mine;
                item.readReceiptPending = false;
                item.receiptRetryCount = 0;
                item.receiptSyncedAt = 0L;
                items.add(item);
            }
        }
        items.sort(Comparator.comparingLong(entity -> entity.createTimeMillis));
        return items;
    }

    @NonNull
    public static List<PilotProfileEntity> buildPilotProfiles() {
        List<PilotProfileEntity> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            PilotProfileEntity item = new PilotProfileEntity();
            item.uid = scenario.pilot.uid;
            item.name = scenario.pilot.name;
            item.updateTimeMillis = System.currentTimeMillis();
            items.add(item);
        }
        return items;
    }

    @NonNull
    public static List<EnterpriseProfileEntity> buildEnterpriseProfiles() {
        List<EnterpriseProfileEntity> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            EnterpriseProfileEntity item = new EnterpriseProfileEntity();
            item.uid = scenario.company.uid;
            item.companyName = scenario.company.name;
            item.updateTimeMillis = System.currentTimeMillis();
            items.add(item);
        }
        return items;
    }

    public static boolean isScenarioConversation(long conversationId) {
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            if (scenario.conversationIdPilot == conversationId || scenario.conversationIdEnterprise == conversationId) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static MessageEntity buildOutgoingMessage(long conversationId, @NonNull String content, @NonNull UserRole role) {
        AppScenarioModels.ScenarioBundle scenario = findScenarioByConversation(conversationId);
        MessageEntity entity = new MessageEntity();
        entity.msgId = System.currentTimeMillis();
        entity.conversationId = conversationId;
        entity.content = content;
        entity.senderName = role == UserRole.ENTERPRISE ? scenario.company.dispatchTitle : scenario.pilot.name;
        entity.senderRole = role == UserRole.ENTERPRISE ? "企业" : "飞手";
        entity.pilotUid = scenario.pilot.uid;
        entity.enterpriseUid = scenario.company.uid;
        entity.counterpartTitle = titleForScenario(scenario);
        entity.createTimeMillis = System.currentTimeMillis();
        entity.createTime = formatTime(entity.createTimeMillis);
        entity.mine = true;
        entity.isRead = true;
        entity.readReceiptPending = false;
        entity.receiptRetryCount = 0;
        entity.receiptSyncedAt = 0L;
        return entity;
    }

    @NonNull
    public static List<PlatformModels.CourseView> buildFallbackCourses(@Nullable List<PlatformModels.CourseView> remote) {
        List<PlatformModels.CourseView> items = new ArrayList<>();
        if (remote != null) {
            items.addAll(remote);
        }
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            items.add(toCourseView(scenario));
        }
        return items;
    }

    @Nullable
    public static PlatformModels.CourseDetailView findCourseDetail(long courseId) {
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            if (scenario.courseId == courseId) {
                return toCourseDetailView(scenario);
            }
        }
        return null;
    }

    @NonNull
    public static List<FlightManagementModels.FlightApplicationRecord> buildFlightApplications() {
        List<FlightManagementModels.FlightApplicationRecord> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            FlightManagementModels.FlightApplicationRecord item = new FlightManagementModels.FlightApplicationRecord();
            item.applicationNo = scenario.applicationNo;
            item.applicantName = scenario.pilot.name;
            item.applicantCompany = scenario.company.name;
            item.projectName = scenario.application.projectName;
            item.location = scenario.application.location;
            item.flightTime = scenario.application.flightTime;
            item.purpose = scenario.application.purpose;
            item.status = scenario.application.status;
            item.workflowStatus = scenario.application.workflowStatus;
            item.approvalOpinion = scenario.application.approvalOpinion;
            item.updatedAt = scenario.application.updatedAt;
            items.add(item);
        }
        items.sort((left, right) -> safe(right.updatedAt).compareTo(safe(left.updatedAt)));
        return items;
    }

    @NonNull
    public static List<FlightManagementModels.NoFlyZoneRecord> buildNoFlyZones() {
        List<FlightManagementModels.NoFlyZoneRecord> items = new ArrayList<>();
        int index = 0;
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            FlightManagementModels.NoFlyZoneRecord item = new FlightManagementModels.NoFlyZoneRecord();
            item.id = index < 2 ? "LOCAL-00" + (index + 1) : "REMOTE-" + scenario.zoneRemoteId;
            item.name = scenario.zone.name;
            item.zoneType = scenario.zone.zoneType;
            item.centerLat = scenario.zone.centerLat;
            item.centerLng = scenario.zone.centerLng;
            item.radius = scenario.zone.radius;
            item.effectiveStart = scenario.zone.effectiveStart;
            item.effectiveEnd = scenario.zone.effectiveEnd;
            item.reason = scenario.zone.reason;
            item.description = scenario.zone.description;
            item.builtIn = scenario.zone.builtIn;
            items.add(item);
            index++;
        }
        items.sort(Comparator.comparing(item -> safe(item.name)));
        return items;
    }

    @NonNull
    public static List<PlatformModels.OrderView> buildFallbackOrders() {
        List<PlatformModels.OrderView> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            PlatformModels.OrderView item = new PlatformModels.OrderView();
            item.id = scenario.orderId;
            item.orderNo = scenario.orderNo;
            item.taskId = scenario.taskId;
            item.amount = scenario.order.amount;
            item.status = scenario.order.status;
            items.add(item);
        }
        return items;
    }

    @NonNull
    public static List<PlatformModels.AlertView> buildFallbackAlerts(@NonNull UserRole role) {
        List<PlatformModels.AlertView> items = new ArrayList<>();
        long baseId = role == UserRole.ENTERPRISE ? 9000L : 8000L;
        int index = 0;
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            PlatformModels.AlertView item = new PlatformModels.AlertView();
            item.id = baseId + index;
            item.level = index % 2 == 0 ? "中风险" : "高风险";
            item.status = index % 2 == 0 ? "待复核" : "需处置";
            if (role == UserRole.ENTERPRISE) {
                item.content = scenario.company.name + " - " + scenario.task.title + " 存在"
                        + (index % 2 == 0 ? "气象窗口波动" : "回款节点临近")
                        + "，请及时跟进项目与订单风险。";
            } else {
                item.content = scenario.pilot.name + " - " + scenario.task.title + " 存在"
                        + (index % 2 == 0 ? "飞行空域限制变化" : "天气条件预警")
                        + "，请谨慎执行任务。";
            }
            item.createTime = formatDateTime(System.currentTimeMillis() - index * 45L * 60L * 1000L);
            items.add(item);
            index++;
            if (items.size() >= 4) {
                break;
            }
        }
        return items;
    }

    @Nullable
    public static PlatformModels.OrderDetailView findOrderDetail(long orderId, @Nullable String orderNo) {
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            if (scenario.orderId == orderId || safe(scenario.orderNo).equals(orderNo)) {
                PlatformModels.OrderDetailView item = new PlatformModels.OrderDetailView();
                item.id = scenario.orderId;
                item.orderNo = scenario.orderNo;
                item.status = scenario.order.status;
                item.amount = scenario.order.amount;
                item.taskId = scenario.taskId;
                item.taskTitle = scenario.task.title;
                item.taskType = scenario.task.taskType;
                item.location = scenario.task.location;
                item.pilotName = scenario.pilot.name;
                item.enterpriseName = scenario.company.name;
                item.contactName = scenario.company.contactName;
                item.contactPhone = scenario.company.contactPhone;
                item.paymentChannel = scenario.order.paymentChannel;
                item.paymentStatus = scenario.order.paymentStatus;
                item.createdAt = scenario.order.createdAt;
                item.appointmentTime = scenario.order.appointmentTime;
                item.remark = scenario.order.remark;
                return item;
            }
        }
        return null;
    }

    @Nullable
    public static PlatformModels.TaskDetailView findTaskDetail(long taskId, @Nullable String title, @Nullable String location) {
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            if (scenario.taskId == taskId || safe(scenario.task.title).equals(title) || safe(scenario.task.location).equals(location)) {
                PlatformModels.TaskDetailView item = new PlatformModels.TaskDetailView();
                item.id = scenario.taskId;
                item.title = scenario.task.title;
                item.taskType = scenario.task.taskType;
                item.description = scenario.task.description;
                item.location = scenario.task.location;
                item.deadline = scenario.task.deadline;
                item.latitude = scenario.task.latitude;
                item.longitude = scenario.task.longitude;
                item.routeStartLatitude = scenario.task.routeStartLatitude;
                item.routeStartLongitude = scenario.task.routeStartLongitude;
                item.operationRadiusMeters = scenario.task.operationRadiusMeters;
                item.budget = scenario.task.budget;
                item.status = scenario.task.status;
                item.ownerName = scenario.task.ownerName;
                return item;
            }
        }
        if (taskId == 88001L || safe(title).contains("桥梁巡检补测") || safe(location).contains("长江大桥")) {
            return buildMockTaskDetail(
                    88001L,
                    "长江沿线桥梁巡检补测项目",
                    "桥梁巡检",
                    "针对重庆南岸区长江大桥沿线开展补测巡检，补采桥体底部和斜拉索高清影像，输出巡检报告与病害标注清单。",
                    "重庆南岸区长江大桥沿线",
                    "2026-04-25 10:30",
                    new BigDecimal("29.53380"),
                    new BigDecimal("106.57640"),
                    new BigDecimal("29.51550"),
                    new BigDecimal("106.55390"),
                    1100,
                    new BigDecimal("5600"),
                    "待派发",
                    "企业调度中心"
            );
        }
        if (taskId == 88002L || safe(title).contains("夜航灯光巡查") || safe(location).contains("数字园区")) {
            return buildMockTaskDetail(
                    88002L,
                    "智慧园区夜航灯光巡查项目",
                    "园区巡查",
                    "围绕两江新区数字园区主干道、停车场和楼宇外立面进行夜航灯光巡查，核对异常照明点位并生成巡查台账。",
                    "重庆两江新区数字园区",
                    "2026-04-25 19:00",
                    new BigDecimal("29.67680"),
                    new BigDecimal("106.60920"),
                    new BigDecimal("29.65820"),
                    new BigDecimal("106.58640"),
                    900,
                    new BigDecimal("4200"),
                    "执行准备",
                    "项目运行组"
            );
        }
        if (taskId == 88003L || safe(title).contains("应急物资投送") || safe(location).contains("仙女山")) {
            return buildMockTaskDetail(
                    88003L,
                    "山地应急物资投送演练项目",
                    "应急保障",
                    "针对仙女山片区复杂地形开展物资投送演练，验证飞行航线、低空补给点和山地起降协同流程。",
                    "重庆武隆仙女山片区",
                    "2026-04-26 08:45",
                    new BigDecimal("29.40060"),
                    new BigDecimal("107.76480"),
                    new BigDecimal("29.38230"),
                    new BigDecimal("107.74210"),
                    1400,
                    new BigDecimal("6800"),
                    "待确认",
                    "低空应急专班"
            );
        }
        return null;
    }

    @NonNull
    public static WeatherDescriptor describeWeatherLocation(double latitude, double longitude) {
        AppScenarioModels.ScenarioBundle closest = null;
        double minDistance = Double.MAX_VALUE;
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            double distance = distanceSquared(latitude, longitude, decimal(scenario.zone.centerLat), decimal(scenario.zone.centerLng));
            if (distance < minDistance) {
                minDistance = distance;
                closest = scenario;
            }
        }
        if (closest == null) {
            return new WeatherDescriptor("重庆低空综合服务空域", "CQ-DEMO-AIR", "基于区域天气模板生成飞行天气评估与建议。");
        }
        String location = closest.weatherAnchor.locationName + " (" + DECIMAL_FORMAT.format(latitude) + ", " + DECIMAL_FORMAT.format(longitude) + ")";
        String note = "基于" + closest.weatherAnchor.locationName + "的区域天气模板和当前时间窗口生成飞行天气评估；"
                + closest.weatherAnchor.seasonHint + "，可用于巡检、建模和活动保障判断。";
        return new WeatherDescriptor(location, closest.weatherAnchor.adcode, note);
    }

    @NonNull
    public static List<PlatformModels.TaskView> buildFallbackTasks() {
        List<PlatformModels.TaskView> items = new ArrayList<>();
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            PlatformModels.TaskView item = new PlatformModels.TaskView();
            item.id = scenario.taskId;
            item.title = scenario.task.title;
            item.taskType = scenario.task.taskType;
            item.location = scenario.task.location;
            item.deadline = scenario.task.deadline;
            item.budget = scenario.task.budget;
            item.status = scenario.task.status;
            item.ownerName = scenario.task.ownerName;
            items.add(item);
        }
        items.add(buildMockTaskView(
                88001L,
                "长江沿线桥梁巡检补测项目",
                "桥梁巡检",
                "重庆南岸区长江大桥沿线",
                "2026-04-25 10:30",
                new BigDecimal("5600"),
                "待派发",
                "企业调度中心"
        ));
        items.add(buildMockTaskView(
                88002L,
                "智慧园区夜航灯光巡查项目",
                "园区巡查",
                "重庆两江新区数字园区",
                "2026-04-25 19:00",
                new BigDecimal("4200"),
                "执行准备",
                "项目运行组"
        ));
        items.add(buildMockTaskView(
                88003L,
                "山地应急物资投送演练项目",
                "应急保障",
                "重庆武隆仙女山片区",
                "2026-04-26 08:45",
                new BigDecimal("6800"),
                "待确认",
                "低空应急专班"
        ));
        return items;
    }

    @NonNull
    private static PlatformModels.TaskDetailView buildMockTaskDetail(
            long id,
            @NonNull String title,
            @NonNull String taskType,
            @NonNull String description,
            @NonNull String location,
            @NonNull String deadline,
            @NonNull BigDecimal latitude,
            @NonNull BigDecimal longitude,
            @NonNull BigDecimal routeStartLatitude,
            @NonNull BigDecimal routeStartLongitude,
            int operationRadiusMeters,
            @NonNull BigDecimal budget,
            @NonNull String status,
            @NonNull String ownerName
    ) {
        PlatformModels.TaskDetailView item = new PlatformModels.TaskDetailView();
        item.id = id;
        item.title = title;
        item.taskType = taskType;
        item.description = description;
        item.location = location;
        item.deadline = deadline;
        item.latitude = latitude;
        item.longitude = longitude;
        item.routeStartLatitude = routeStartLatitude;
        item.routeStartLongitude = routeStartLongitude;
        item.operationRadiusMeters = operationRadiusMeters;
        item.budget = budget;
        item.status = status;
        item.ownerName = ownerName;
        return item;
    }

    @NonNull
    private static PlatformModels.TaskView buildMockTaskView(
            long id,
            @NonNull String title,
            @NonNull String taskType,
            @NonNull String location,
            @NonNull String deadline,
            @NonNull BigDecimal budget,
            @NonNull String status,
            @NonNull String ownerName
    ) {
        PlatformModels.TaskView item = new PlatformModels.TaskView();
        item.id = id;
        item.title = title;
        item.taskType = taskType;
        item.location = location;
        item.deadline = deadline;
        item.budget = budget;
        item.status = status;
        item.ownerName = ownerName;
        return item;
    }

    @NonNull
    private static PlatformModels.CourseView toCourseView(@NonNull AppScenarioModels.ScenarioBundle scenario) {
        PlatformModels.CourseView item = new PlatformModels.CourseView();
        item.id = scenario.courseId;
        item.title = scenario.course.title;
        item.summary = scenario.course.summary;
        item.learningMode = scenario.course.learningMode;
        item.institutionName = scenario.course.institutionName;
        item.seatAvailable = scenario.course.seatAvailable;
        item.browseCount = scenario.course.browseCount;
        item.enrollCount = scenario.course.enrollCount;
        item.price = scenario.course.price;
        item.status = scenario.course.status;
        return item;
    }

    @NonNull
    private static PlatformModels.CourseDetailView toCourseDetailView(@NonNull AppScenarioModels.ScenarioBundle scenario) {
        PlatformModels.CourseDetailView item = new PlatformModels.CourseDetailView();
        item.id = scenario.courseId;
        item.title = scenario.course.title;
        item.summary = scenario.course.summary;
        item.content = scenario.course.content;
        item.learningMode = scenario.course.learningMode;
        item.institutionName = scenario.course.institutionName;
        item.seatTotal = scenario.course.seatTotal;
        item.seatAvailable = scenario.course.seatAvailable;
        item.browseCount = scenario.course.browseCount;
        item.enrollCount = scenario.course.enrollCount;
        item.price = scenario.course.price;
        item.status = scenario.course.status;
        item.enrolled = scenario.course.enrolled;
        item.enrollmentNo = scenario.course.enrollmentNo;
        item.enrollmentStatus = scenario.course.enrollmentStatus;
        return item;
    }

    @NonNull
    private static AppScenarioModels.ScenarioBundle findScenarioByConversation(long conversationId) {
        for (AppScenarioModels.ScenarioBundle scenario : AppScenarioRepository.buildScenarios()) {
            if (scenario.conversationIdPilot == conversationId || scenario.conversationIdEnterprise == conversationId) {
                return scenario;
            }
        }
        return AppScenarioRepository.buildScenarios().get(0);
    }

    @Nullable
    private static AppScenarioModels.MessageLine lastLine(@NonNull List<AppScenarioModels.MessageLine> items) {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(items.size() - 1);
    }

    private static int countUnread(@NonNull List<AppScenarioModels.MessageLine> items) {
        int count = 0;
        for (AppScenarioModels.MessageLine item : items) {
            if (!item.mine && !item.isRead) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    private static String titleForScenario(@NonNull AppScenarioModels.ScenarioBundle scenario) {
        if (scenario.task.title.contains("巡检")) {
            return "任务协同";
        }
        if (scenario.task.title.contains("建模")) {
            return "建模协同";
        }
        if (scenario.task.title.contains("活动")) {
            return "活动保障";
        }
        return "系统通知";
    }

    @NonNull
    private static String formatTime(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(HOUR_MINUTE);
    }

    @NonNull
    private static String formatDateTime(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DATE_TIME);
    }

    private static double distanceSquared(double latitude, double longitude, double anchorLat, double anchorLng) {
        double lat = latitude - anchorLat;
        double lng = longitude - anchorLng;
        return lat * lat + lng * lng;
    }

    private static double decimal(@Nullable BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    public static final class WeatherDescriptor {
        public final String locationName;
        public final String adcode;
        public final String sourceNote;

        public WeatherDescriptor(@NonNull String locationName, @NonNull String adcode, @NonNull String sourceNote) {
            this.locationName = locationName;
            this.adcode = adcode;
            this.sourceNote = sourceNote;
        }
    }
}
