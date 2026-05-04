package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.NoFlyZoneEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.NoFlyZoneMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final UserAccountMapper userAccountMapper;
    private final NoFlyZoneMapper noFlyZoneMapper;
    private final AuditLogService auditLogService;

    public TaskService(
            TaskMapper taskMapper,
            UserAccountMapper userAccountMapper,
            NoFlyZoneMapper noFlyZoneMapper,
            AuditLogService auditLogService
    ) {
        this.taskMapper = taskMapper;
        this.userAccountMapper = userAccountMapper;
        this.noFlyZoneMapper = noFlyZoneMapper;
        this.auditLogService = auditLogService;
    }

    public List<ApiDtos.TaskView> listTasks(SessionUser user) {
        LambdaQueryWrapper<TaskEntity> query = new LambdaQueryWrapper<TaskEntity>()
                .orderByDesc(TaskEntity::getUpdateTime)
                .orderByDesc(TaskEntity::getId);
        if (user.role() == RoleType.ENTERPRISE) {
            query.eq(TaskEntity::getEnterpriseId, user.id());
        } else if (user.role() == RoleType.PILOT) {
            query.notIn(TaskEntity::getStatus, List.of("CANCELLED", "CLOSED"));
        }
        List<TaskEntity> tasks = taskMapper.selectList(query);
        Map<Long, UserAccountEntity> owners = findUsersByIds(tasks.stream()
                .map(TaskEntity::getEnterpriseId)
                .collect(Collectors.toSet()));
        return tasks.stream()
                .map(task -> {
                    UserAccountEntity owner = owners.get(task.getEnterpriseId());
                    return toTaskView(task, owner);
                })
                .toList();
    }

    public ApiDtos.TaskDetailView getTaskDetail(Long taskId) {
        TaskEntity task = getTaskById(taskId);
        UserAccountEntity owner = getUserById(task.getEnterpriseId());
        BigDecimal routeStartLatitude = task.getLatitude().subtract(new BigDecimal("0.018"));
        BigDecimal routeStartLongitude = task.getLongitude().subtract(new BigDecimal("0.022"));
        return new ApiDtos.TaskDetailView(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getDescription(),
                task.getLocation(),
                PlatformUtils.formatDateTime(task.getDeadline()),
                task.getLatitude(),
                task.getLongitude(),
                routeStartLatitude,
                routeStartLongitude,
                PlatformUtils.buildOperationRadius(task.getTaskType()),
                task.getBudget(),
                task.getStatus(),
                PlatformUtils.displayName(owner)
        );
    }

    @Transactional
    public ApiDtos.TaskView createTask(SessionUser user, ApiDtos.TaskRequest request) {
        PlatformUtils.ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = new TaskEntity();
        task.setEnterpriseId(user.id());
        task.setTaskType(request.taskType());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setLocation(request.location());
        task.setDeadline(PlatformUtils.parseTaskDeadline(request.deadline()));
        task.setLatitude(request.latitude());
        task.setLongitude(request.longitude());
        task.setBudget(request.budget());
        task.setStatus("REVIEWING");
        task.setVersion(0);
        taskMapper.insert(task);
        audit(user, "TASK", String.valueOf(task.getId()), "CREATE", task.getTitle());
        return toTaskView(task, getUserById(user.id()));
    }

    @Transactional
    public ApiDtos.TaskView updateTask(SessionUser user, Long taskId, ApiDtos.TaskRequest request) {
        PlatformUtils.ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = getTaskById(taskId);
        ensureTaskOwnership(user, task);
        task.setTaskType(request.taskType());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setLocation(request.location());
        task.setDeadline(PlatformUtils.parseTaskDeadline(request.deadline()));
        task.setLatitude(request.latitude());
        task.setLongitude(request.longitude());
        task.setBudget(request.budget());
        taskMapper.updateById(task);
        audit(user, "TASK", String.valueOf(task.getId()), "UPDATE", task.getTitle());
        return toTaskView(task, getUserById(task.getEnterpriseId()));
    }

    @Transactional
    public ApiDtos.TaskView publishTask(SessionUser user, Long taskId) {
        PlatformUtils.ensureRole(user, RoleType.ENTERPRISE);
        TaskEntity task = getTaskById(taskId);
        ensureTaskOwnership(user, task);
        task.setStatus("REVIEWING");
        taskMapper.updateById(task);
        audit(user, "TASK", String.valueOf(task.getId()), "REPUBLISH", task.getTitle());
        return toTaskView(task, getUserById(task.getEnterpriseId()));
    }

    public List<ApiDtos.NoFlyZoneView> listZones() {
        return noFlyZoneMapper.selectList(new LambdaQueryWrapper<NoFlyZoneEntity>()
                        .orderByAsc(NoFlyZoneEntity::getId))
                .stream()
                .map(zone -> new ApiDtos.NoFlyZoneView(
                        zone.getId(),
                        zone.getName(),
                        zone.getZoneType(),
                        zone.getCenterLat(),
                        zone.getCenterLng(),
                        zone.getRadius() == null ? 0 : zone.getRadius(),
                        zone.getDescription()))
                .toList();
    }

    public ApiDtos.FlightApplicationView submitFlightApplication(SessionUser user, ApiDtos.FlightApplicationRequest request) {
        PlatformUtils.ensureRole(user, RoleType.PILOT);
        String applicationNo = "FLY" + System.currentTimeMillis();
        audit(user, "COMPLIANCE", applicationNo, "APPLY", request.location());
        return new ApiDtos.FlightApplicationView(applicationNo, "REVIEWING", request.location(), "审批结果将在 15 分钟内通过站内信反馈");
    }

    TaskEntity getTaskById(Long id) {
        TaskEntity task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(404, "任务不存在");
        }
        return task;
    }

    Map<Long, UserAccountEntity> findUsersByIds(Set<Long> ids) {
        Set<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(UserAccountEntity::getId, Function.identity()));
    }

    UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private void ensureTaskOwnership(SessionUser user, TaskEntity task) {
        if (user.role() != RoleType.ADMIN && !Objects.equals(task.getEnterpriseId(), user.id())) {
            throw new BizException(403, "无权操作该任务");
        }
    }

    private ApiDtos.TaskView toTaskView(TaskEntity task, UserAccountEntity owner) {
        return new ApiDtos.TaskView(
                task.getId(),
                task.getTitle(),
                task.getTaskType(),
                task.getLocation(),
                PlatformUtils.formatDateTime(task.getDeadline()),
                task.getBudget(),
                task.getStatus(),
                PlatformUtils.displayName(owner));
    }

    void audit(SessionUser actor, String bizType, String bizId, String eventType, String payload) {
        Long actorUserId = actor == null ? null : actor.id();
        String actorRole = actor == null ? null : actor.role().name();
        auditLogService.record(
                RequestIdContext.get(),
                actorUserId,
                actorRole,
                bizType,
                bizId,
                eventType,
                payload);
    }
}
