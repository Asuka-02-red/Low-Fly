package com.lowaltitude.reststop.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 每日统计报表实体。
 * <p>
 * 对应 report_daily_summary 表，存储按日汇总的运营统计数据，
 * 包括任务数、订单数、支付金额、告警数及培训数。
 * </p>
 */
@TableName("report_daily_summary")
public class ReportDailySummaryEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("task_count")
    private Integer taskCount;

    @TableField("order_count")
    private Integer orderCount;

    @TableField("payment_amount")
    private BigDecimal paymentAmount;

    @TableField("alert_count")
    private Integer alertCount;

    @TableField("training_count")
    private Integer trainingCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public Integer getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(Integer alertCount) {
        this.alertCount = alertCount;
    }

    public Integer getTrainingCount() {
        return trainingCount;
    }

    public void setTrainingCount(Integer trainingCount) {
        this.trainingCount = trainingCount;
    }
}
