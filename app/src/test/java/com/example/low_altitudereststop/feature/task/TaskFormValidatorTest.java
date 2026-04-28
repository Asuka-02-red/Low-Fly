package com.example.low_altitudereststop.feature.task;

import org.junit.Assert;
import org.junit.Test;

public class TaskFormValidatorTest {

    @Test
    public void shouldBuildTaskRequestWhenInputIsValid() {
        TaskFormValidator.FormInput input = new TaskFormValidator.FormInput();
        input.taskType = "INSPECTION";
        input.title = "园区日常巡检";
        input.description = "检查航线点位与作业范围";
        input.location = "重庆渝北区";
        input.deadline = "2099-12-31 18:00";
        input.latitude = "29.56";
        input.longitude = "106.55";
        input.budget = "1200";

        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(input);

        Assert.assertTrue(result.isValid());
        Assert.assertEquals("园区日常巡检", result.request.title);
        Assert.assertEquals("2099-12-31 18:00", result.request.deadline);
    }

    @Test
    public void shouldRejectPastDeadline() {
        TaskFormValidator.FormInput input = new TaskFormValidator.FormInput();
        input.taskType = "INSPECTION";
        input.title = "园区日常巡检";
        input.description = "检查航线点位与作业范围";
        input.location = "重庆渝北区";
        input.deadline = "2020-01-01 10:00";
        input.latitude = "29.56";
        input.longitude = "106.55";
        input.budget = "1200";

        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(input);

        Assert.assertFalse(result.isValid());
        Assert.assertEquals("截止时间必须晚于当前时间", result.errorFor("deadline"));
    }

    @Test
    public void shouldRejectInvalidCoordinatesAndBudget() {
        TaskFormValidator.FormInput input = new TaskFormValidator.FormInput();
        input.taskType = "INSPECTION";
        input.title = "园区日常巡检";
        input.description = "检查航线点位与作业范围";
        input.location = "重庆渝北区";
        input.deadline = "2099-12-31 18:00";
        input.latitude = "99";
        input.longitude = "190";
        input.budget = "0";

        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(input);

        Assert.assertFalse(result.isValid());
        Assert.assertEquals("纬度范围应在 -90 到 90 之间", result.errorFor("latitude"));
        Assert.assertEquals("经度范围应在 -180 到 180 之间", result.errorFor("longitude"));
        Assert.assertEquals("预算必须大于 0", result.errorFor("budget"));
    }
}
