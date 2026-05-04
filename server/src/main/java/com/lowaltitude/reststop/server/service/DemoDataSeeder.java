package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.MessageConversationEntity;
import com.lowaltitude.reststop.server.entity.MessageEntryEntity;
import com.lowaltitude.reststop.server.entity.TaskEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.MessageConversationMapper;
import com.lowaltitude.reststop.server.mapper.MessageEntryMapper;
import com.lowaltitude.reststop.server.mapper.TaskMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class DemoDataSeeder {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final MessageConversationMapper messageConversationMapper;
    private final MessageEntryMapper messageEntryMapper;
    private final TaskMapper taskMapper;
    private final UserAccountMapper userAccountMapper;
    private final AlertService alertService;
    private final AuditLogService auditLogService;

    public DemoDataSeeder(
            CourseMapper courseMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            MessageConversationMapper messageConversationMapper,
            MessageEntryMapper messageEntryMapper,
            TaskMapper taskMapper,
            UserAccountMapper userAccountMapper,
            AlertService alertService,
            AuditLogService auditLogService
    ) {
        this.courseMapper = courseMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.messageConversationMapper = messageConversationMapper;
        this.messageEntryMapper = messageEntryMapper;
        this.taskMapper = taskMapper;
        this.userAccountMapper = userAccountMapper;
        this.alertService = alertService;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    void init() {
        alertService.seedDefaultsIfEmpty();
        seedDemoCoursesIfNecessary();
        seedDemoConversationsIfNecessary();
        auditLogService.record(RequestIdContext.get(), null, null, "SYSTEM", "boot", "INIT", "比赛演示数据已载入");
    }

    private void seedDemoCoursesIfNecessary() {
        seedCourseIfMissing(
                "低空巡检项目经理训练营",
                "低空驿站企业学院",
                2L,
                "OFFLINE",
                "适合企业项目负责人，聚焦排班、风险协同与回款节奏。",
                "课程大纲：项目启动会模板、飞手排班与设备准备清单、风险处理与回款节奏管理。\n产出模板：调度看板、风险沟通表、项目复盘模板。",
                30,
                18,
                new BigDecimal("1999"),
                126,
                32);
        seedCourseIfMissing(
                "飞手夜航与应急处置专项课",
                "低空驿站飞行学院",
                3L,
                "ARTICLE",
                "适合飞手，覆盖夜航检查、应急返航和现场沟通模板。",
                "课程内容：夜航前设备检查、异常返航与失联处置 SOP、与企业调度员的关键口令模板。",
                999,
                999,
                BigDecimal.ZERO,
                218,
                56);
        seedCourseIfMissing(
                "企业与飞手协同沟通实战课",
                "低空驿站协同中心",
                2L,
                "ARTICLE",
                "通过真实场景演练项目确认、设备交接和复盘反馈。",
                "章节安排：任务确认、设备交接、项目复盘。\n课程亮点：适合企业和飞手共同学习，方便在消息中心直接演练协同话术。",
                999,
                999,
                new BigDecimal("99"),
                164,
                41);
    }

    private void seedCourseIfMissing(
            String title,
            String institutionName,
            Long publishUserId,
            String courseType,
            String summary,
            String content,
            int seatTotal,
            int seatAvailable,
            BigDecimal price,
            int browseCount,
            int enrollCount
    ) {
        Long count = courseMapper.selectCount(new LambdaQueryWrapper<CourseEntity>()
                .eq(CourseEntity::getTitle, title));
        if (count != null && count > 0) {
            return;
        }
        CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setInstitutionName(institutionName);
        course.setPublishUserId(publishUserId);
        course.setCourseType(courseType);
        course.setSummary(summary);
        course.setContent(content);
        course.setSeatTotal(seatTotal);
        course.setSeatAvailable(seatAvailable);
        course.setPrice(price);
        course.setStatus("OPEN");
        course.setBrowseCount(browseCount);
        course.setEnrollCount(enrollCount);
        course.setCreateTime(LocalDateTime.now());
        courseMapper.insert(course);
    }

    private void seedDemoConversationsIfNecessary() {
        UserAccountEntity pilot = findUserByUsername("pilot_demo");
        UserAccountEntity enterprise = findUserByUsername("enterprise_demo");
        if (pilot == null || enterprise == null) {
            return;
        }
        MessageConversationEntity existing = messageConversationMapper.selectOne(new LambdaQueryWrapper<MessageConversationEntity>()
                .eq(MessageConversationEntity::getEnterpriseId, enterprise.getId())
                .eq(MessageConversationEntity::getPilotId, pilot.getId())
                .last("limit 1"));
        if (existing != null) {
            return;
        }
        TaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getEnterpriseId, enterprise.getId())
                .orderByAsc(TaskEntity::getId)
                .last("limit 1"));
        MessageConversationEntity conversation = new MessageConversationEntity();
        conversation.setEnterpriseId(enterprise.getId());
        conversation.setPilotId(pilot.getId());
        conversation.setTaskId(task == null ? null : task.getId());
        conversation.setTitle(task == null ? "企业与飞手协同沟通" : task.getTitle() + " 协同沟通");
        conversation.setLastMessageTime(LocalDateTime.now());
        messageConversationMapper.insert(conversation);
        seedMessageEntry(conversation.getId(), enterprise, "欢迎加入本次项目沟通群，今天先确认巡检航线和到场时间。");
        seedMessageEntry(conversation.getId(), pilot, "已收到，我会在今晚 19:00 前确认绕飞方案并同步设备状态。");
        seedMessageEntry(conversation.getId(), enterprise, "好的，如需调整任务窗口请直接在这里留言，我会实时跟进。");
    }

    private void seedMessageEntry(Long conversationId, UserAccountEntity sender, String content) {
        MessageEntryEntity entry = new MessageEntryEntity();
        entry.setConversationId(conversationId);
        entry.setSenderUserId(sender.getId());
        entry.setSenderRole(sender.getRole());
        entry.setContent(content);
        entry.setCreateTime(LocalDateTime.now());
        messageEntryMapper.insert(entry);
    }

    private UserAccountEntity findUserByUsername(String username) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getUsername, username)
                .last("limit 1"));
    }
}
