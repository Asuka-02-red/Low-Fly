package com.example.low_altitudereststop.feature.demo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AppScenarioModels {

    private AppScenarioModels() {
    }

    public static final class ScenarioBundle {
        public long conversationIdPilot;
        public long conversationIdEnterprise;
        public long taskId;
        public long orderId;
        public long courseId;
        public long zoneRemoteId;
        public String orderNo;
        public String applicationNo;
        public Person pilot;
        public Company company;
        public Task task;
        public Order order;
        public Course course;
        public FlightApplication application;
        public Zone zone;
        public WeatherAnchor weatherAnchor;
        public final List<MessageLine> pilotMessages = new ArrayList<>();
        public final List<MessageLine> enterpriseMessages = new ArrayList<>();
    }

    public static final class Person {
        public String uid;
        public String name;
        public String roleTitle;
        public String phone;
    }

    public static final class Company {
        public String uid;
        public String name;
        public String dispatchTitle;
        public String contactName;
        public String contactPhone;
    }

    public static final class Task {
        public String title;
        public String taskType;
        public String description;
        public String location;
        public String deadline;
        public BigDecimal latitude;
        public BigDecimal longitude;
        public BigDecimal routeStartLatitude;
        public BigDecimal routeStartLongitude;
        public Integer operationRadiusMeters;
        public BigDecimal budget;
        public String status;
        public String ownerName;
    }

    public static final class Order {
        public String status;
        public BigDecimal amount;
        public String paymentStatus;
        public String paymentChannel;
        public String createdAt;
        public String appointmentTime;
        public String remark;
    }

    public static final class Course {
        public String title;
        public String summary;
        public String content;
        public String learningMode;
        public String institutionName;
        public int seatTotal;
        public int seatAvailable;
        public int browseCount;
        public int enrollCount;
        public BigDecimal price;
        public String status;
        public boolean enrolled;
        public String enrollmentNo;
        public String enrollmentStatus;
    }

    public static final class FlightApplication {
        public String projectName;
        public String location;
        public String flightTime;
        public String purpose;
        public String status;
        public String workflowStatus;
        public String approvalOpinion;
        public String updatedAt;
    }

    public static final class Zone {
        public String name;
        public String zoneType;
        public BigDecimal centerLat;
        public BigDecimal centerLng;
        public int radius;
        public String effectiveStart;
        public String effectiveEnd;
        public String reason;
        public String description;
        public boolean builtIn;
    }

    public static final class WeatherAnchor {
        public String locationName;
        public String adcode;
        public String seasonHint;
    }

    public static final class MessageLine {
        public long messageId;
        public String senderName;
        public String senderRole;
        public boolean mine;
        public boolean isRead;
        public String content;
        public long timeMillis;
    }
}
