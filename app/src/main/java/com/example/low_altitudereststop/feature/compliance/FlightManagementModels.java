package com.example.low_altitudereststop.feature.compliance;

import java.math.BigDecimal;

/**
 * 飞行管理数据模型类，定义飞行申请和禁飞区的核心数据结构。
 * <p>
 * 包含FlightApplicationRecord（飞行申请记录）和NoFlyZoneRecord（禁飞区记录）
 * 两个内部类，分别用于申请审批和禁飞区管理业务。
 * </p>
 */
public final class FlightManagementModels {

    private FlightManagementModels() {
    }

    public static final class FlightApplicationRecord {
        public String applicationNo;
        public String applicantName;
        public String applicantCompany;
        public String projectName;
        public String location;
        public String flightTime;
        public String purpose;
        public String status;
        public String workflowStatus;
        public String approvalOpinion;
        public String updatedAt;
        public boolean selected;
    }

    public static final class NoFlyZoneRecord {
        public String id;
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
}
