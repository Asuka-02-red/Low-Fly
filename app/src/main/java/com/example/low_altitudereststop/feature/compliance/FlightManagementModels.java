package com.example.low_altitudereststop.feature.compliance;

import java.math.BigDecimal;

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
