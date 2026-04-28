package com.example.low_altitudereststop.feature.compliance;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class FlightApplicationWorkflowTest {

    @Test
    public void shouldFilterByStatus() {
        FlightManagementModels.FlightApplicationRecord pending = build("PENDING", false);
        FlightManagementModels.FlightApplicationRecord approved = build("APPROVED", false);
        FlightManagementModels.FlightApplicationRecord rejected = build("REJECTED", false);

        List<FlightManagementModels.FlightApplicationRecord> result = FlightApplicationWorkflow.filter(
                Arrays.asList(pending, approved, rejected),
                FlightApplicationWorkflow.FILTER_APPROVED
        );

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("APPROVED", result.get(0).status);
    }

    @Test
    public void shouldCountOnlySelectedPendingRecords() {
        List<FlightManagementModels.FlightApplicationRecord> result = Arrays.asList(
                build("PENDING", true),
                build("PENDING", false),
                build("APPROVED", true)
        );

        Assert.assertEquals(1, FlightApplicationWorkflow.selectedPendingCount(result));
        Assert.assertEquals("完成企业审核并下发放行", FlightApplicationWorkflow.nextWorkflowStatus("APPROVED"));
        Assert.assertEquals("审核驳回，等待重新提交", FlightApplicationWorkflow.nextWorkflowStatus("REJECTED"));
    }

    private FlightManagementModels.FlightApplicationRecord build(String status, boolean selected) {
        FlightManagementModels.FlightApplicationRecord record = new FlightManagementModels.FlightApplicationRecord();
        record.status = status;
        record.selected = selected;
        return record;
    }
}
