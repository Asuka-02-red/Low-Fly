package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.AlertRecordEntity;
import com.lowaltitude.reststop.server.mapper.AlertRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 告警服务。
 * <p>
 * 提供告警记录的管理功能，包括初始化种子告警数据、
 * 查询告警列表、统计未关闭告警数及高风险告警数。
 * </p>
 */
@Service
public class AlertService {

    private final AlertRecordMapper alertRecordMapper;

    public AlertService(AlertRecordMapper alertRecordMapper) {
        this.alertRecordMapper = alertRecordMapper;
    }

    public void seedDefaultsIfEmpty() {
        if (alertRecordMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            return;
        }
        createSeedAlert("HIGH", "检测到航线进入限飞区边缘", LocalDateTime.now().minusMinutes(8));
        createSeedAlert("MEDIUM", "风速接近阈值，请关注气象更新", LocalDateTime.now().minusMinutes(20));
    }

    public List<ApiDtos.AlertView> listAlerts() {
        return alertRecordMapper.selectList(new LambdaQueryWrapper<AlertRecordEntity>()
                        .orderByDesc(AlertRecordEntity::getCreateTime))
                .stream()
                .map(entity -> new ApiDtos.AlertView(
                        entity.getId(),
                        entity.getLevel(),
                        entity.getContent(),
                        entity.getStatus(),
                        entity.getCreateTime()))
                .toList();
    }

    public long countOpenAlerts() {
        return alertRecordMapper.selectCount(new LambdaQueryWrapper<AlertRecordEntity>()
                .eq(AlertRecordEntity::getStatus, "OPEN"));
    }

    public long countHighRiskAlerts() {
        return alertRecordMapper.selectCount(new LambdaQueryWrapper<AlertRecordEntity>()
                .eq(AlertRecordEntity::getLevel, "HIGH"));
    }

    private void createSeedAlert(String level, String content, LocalDateTime createTime) {
        AlertRecordEntity entity = new AlertRecordEntity();
        entity.setPilotId(1L);
        entity.setLevel(level);
        entity.setContent(content);
        entity.setStatus("OPEN");
        entity.setCreateTime(createTime);
        alertRecordMapper.insert(entity);
    }
}
