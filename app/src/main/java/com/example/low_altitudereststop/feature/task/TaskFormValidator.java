package com.example.low_altitudereststop.feature.task;

import com.example.low_altitudereststop.core.model.PlatformModels;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务表单校验器。
 * <p>
 * 对创建任务表单的标题、描述、地点、截止时间、坐标、预算等字段
 * 进行完整性和合法性校验，返回包含所有字段错误的校验结果，
 * 并提供默认截止时间文本生成。
 * </p>
 */
public final class TaskFormValidator {

    public static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TaskFormValidator() {
    }

    public static ValidationResult validate(FormInput input) {
        FormInput source = input == null ? new FormInput() : input;
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String taskType = textOrDefault(source.taskType, "巡检");
        String title = text(source.title);
        String description = text(source.description);
        String location = text(source.location);
        String deadline = text(source.deadline);
        BigDecimal latitude = parseDecimal(source.latitude, "29.56");
        BigDecimal longitude = parseDecimal(source.longitude, "106.55");
        BigDecimal budget = parseDecimal(source.budget, "1000");

        if (title.isEmpty()) {
            fieldErrors.put("title", "请输入任务标题");
        }
        if (description.isEmpty()) {
            fieldErrors.put("description", "请输入任务描述");
        }
        if (location.isEmpty()) {
            fieldErrors.put("location", "请输入任务地点");
        }
        if (deadline.isEmpty()) {
            fieldErrors.put("deadline", "请输入截止时间");
        } else {
            try {
                LocalDateTime parsedDeadline = LocalDateTime.parse(deadline, DEADLINE_FORMATTER);
                if (!parsedDeadline.isAfter(LocalDateTime.now())) {
                    fieldErrors.put("deadline", "截止时间必须晚于当前时间");
                } else {
                    deadline = DEADLINE_FORMATTER.format(parsedDeadline);
                }
            } catch (DateTimeParseException ex) {
                fieldErrors.put("deadline", "截止时间格式应为 yyyy-MM-dd HH:mm");
            }
        }

        if (latitude == null) {
            fieldErrors.put("latitude", "请输入合法的纬度");
        } else if (latitude.compareTo(new BigDecimal("-90")) < 0 || latitude.compareTo(new BigDecimal("90")) > 0) {
            fieldErrors.put("latitude", "纬度范围应在 -90 到 90 之间");
        }

        if (longitude == null) {
            fieldErrors.put("longitude", "请输入合法的经度");
        } else if (longitude.compareTo(new BigDecimal("-180")) < 0 || longitude.compareTo(new BigDecimal("180")) > 0) {
            fieldErrors.put("longitude", "经度范围应在 -180 到 180 之间");
        }

        if (budget == null) {
            fieldErrors.put("budget", "请输入合法的预算金额");
        } else if (budget.compareTo(BigDecimal.ZERO) <= 0) {
            fieldErrors.put("budget", "预算必须大于 0");
        }

        if (!fieldErrors.isEmpty()) {
            return ValidationResult.failure(fieldErrors);
        }

        PlatformModels.TaskRequest request = new PlatformModels.TaskRequest();
        request.taskType = taskType;
        request.title = title;
        request.description = description;
        request.location = location;
        request.deadline = deadline;
        request.latitude = latitude;
        request.longitude = longitude;
        request.budget = budget;
        return ValidationResult.success(request);
    }

    public static String defaultDeadlineText() {
        return DEADLINE_FORMATTER.format(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private static String textOrDefault(CharSequence value, String defaultValue) {
        String text = text(value);
        return text.isEmpty() ? defaultValue : text;
    }

    private static BigDecimal parseDecimal(CharSequence value, String defaultValue) {
        String text = text(value);
        try {
            return new BigDecimal(text.isEmpty() ? defaultValue : text);
        } catch (Exception ex) {
            return null;
        }
    }

    public static final class FormInput {
        public CharSequence taskType;
        public CharSequence title;
        public CharSequence description;
        public CharSequence location;
        public CharSequence deadline;
        public CharSequence latitude;
        public CharSequence longitude;
        public CharSequence budget;
    }

    public static final class ValidationResult {
        public final PlatformModels.TaskRequest request;
        public final Map<String, String> fieldErrors;

        private ValidationResult(PlatformModels.TaskRequest request, Map<String, String> fieldErrors) {
            this.request = request;
            this.fieldErrors = fieldErrors;
        }

        public static ValidationResult success(PlatformModels.TaskRequest request) {
            return new ValidationResult(request, new LinkedHashMap<>());
        }

        public static ValidationResult failure(Map<String, String> fieldErrors) {
            return new ValidationResult(null, fieldErrors);
        }

        public boolean isValid() {
            return request != null && fieldErrors.isEmpty();
        }

        public String errorFor(String key) {
            return fieldErrors.get(key);
        }
    }
}
