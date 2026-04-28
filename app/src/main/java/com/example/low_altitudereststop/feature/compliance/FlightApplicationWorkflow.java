package com.example.low_altitudereststop.feature.compliance;

import java.util.ArrayList;
import java.util.List;

public final class FlightApplicationWorkflow {

    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_PENDING = "PENDING";
    public static final String FILTER_APPROVED = "APPROVED";
    public static final String FILTER_REJECTED = "REJECTED";

    private FlightApplicationWorkflow() {
    }

    public static List<FlightManagementModels.FlightApplicationRecord> filter(
            List<FlightManagementModels.FlightApplicationRecord> source,
            String filter
    ) {
        List<FlightManagementModels.FlightApplicationRecord> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        String targetFilter = normalizeFilter(filter);
        for (FlightManagementModels.FlightApplicationRecord record : source) {
            if (record == null) {
                continue;
            }
            if (FILTER_ALL.equals(targetFilter) || targetFilter.equals(normalizeFilter(record.status))) {
                result.add(record);
            }
        }
        return result;
    }

    public static int selectedPendingCount(List<FlightManagementModels.FlightApplicationRecord> records) {
        int count = 0;
        if (records == null) {
            return 0;
        }
        for (FlightManagementModels.FlightApplicationRecord record : records) {
            if (record != null && record.selected && FILTER_PENDING.equals(normalizeFilter(record.status))) {
                count++;
            }
        }
        return count;
    }

    public static String normalizeFilter(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return FILTER_ALL;
        }
        return filter.trim().toUpperCase();
    }

    public static String nextWorkflowStatus(String targetStatus) {
        String normalized = normalizeFilter(targetStatus);
        if (FILTER_APPROVED.equals(normalized)) {
            return "完成企业审核并下发放行";
        }
        if (FILTER_REJECTED.equals(normalized)) {
            return "审核驳回，等待重新提交";
        }
        return "待企业审核";
    }
}
