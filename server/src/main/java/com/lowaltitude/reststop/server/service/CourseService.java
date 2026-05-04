package com.lowaltitude.reststop.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RequestIdContext;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditLogService auditLogService;

    public CourseService(
            CourseMapper courseMapper,
            CourseEnrollmentMapper courseEnrollmentMapper,
            UserAccountMapper userAccountMapper,
            AuditLogService auditLogService
    ) {
        this.courseMapper = courseMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditLogService = auditLogService;
    }

    public List<ApiDtos.CourseView> listCourses() {
        return courseMapper.selectList(new LambdaQueryWrapper<CourseEntity>()
                        .eq(CourseEntity::getStatus, "OPEN")
                        .orderByDesc(CourseEntity::getId))
                .stream()
                .map(this::toCourseView)
                .toList();
    }

    @Transactional
    public ApiDtos.CourseDetailView getCourseDetail(SessionUser user, Long courseId) {
        CourseEntity course = getCourse(courseId);
        if (!"OPEN".equalsIgnoreCase(course.getStatus()) && !canManageCourse(user, course)) {
            throw new BizException(403, "当前课程未发布");
        }
        CourseEnrollmentEntity enrollment = findActiveEnrollment(courseId, user.id());
        if (!canManageCourse(user, course)) {
            course.setBrowseCount(PlatformUtils.safeInt(course.getBrowseCount()) + 1);
            courseMapper.updateById(course);
        }
        return toCourseDetailView(course, enrollment);
    }

    public List<ApiDtos.CourseManageView> listManagedCourses(SessionUser user) {
        ensureCourseManager(user);
        LambdaQueryWrapper<CourseEntity> query = new LambdaQueryWrapper<CourseEntity>()
                .orderByDesc(CourseEntity::getId);
        if (user.role() != RoleType.ADMIN) {
            query.eq(CourseEntity::getPublishUserId, user.id());
        }
        return courseMapper.selectList(query).stream()
                .map(this::toCourseManageView)
                .toList();
    }

    @Transactional
    public ApiDtos.CourseManageView createCourse(SessionUser user, ApiDtos.CourseManageRequest request) {
        ensureCourseManager(user);
        CourseEntity course = buildCourseEntity(user, null, request);
        courseMapper.insert(course);
        audit(user, "COURSE", String.valueOf(course.getId()), "CREATE", course.getTitle());
        return toCourseManageView(course);
    }

    @Transactional
    public ApiDtos.CourseManageView updateCourse(SessionUser user, Long courseId, ApiDtos.CourseManageRequest request) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        CourseEntity updated = buildCourseEntity(user, course, request);
        updated.setId(course.getId());
        updated.setCreateTime(course.getCreateTime());
        updated.setBrowseCount(PlatformUtils.safeInt(course.getBrowseCount()));
        updated.setEnrollCount(PlatformUtils.safeInt(course.getEnrollCount()));
        updated.setPublishUserId(course.getPublishUserId());
        updated.setInstitutionName(course.getInstitutionName());
        updated.setSeatAvailable(PlatformUtils.resolveSeatAvailable(course.getSeatTotal(), course.getSeatAvailable(), request.seatTotal()));
        courseMapper.updateById(updated);
        audit(user, "COURSE", String.valueOf(updated.getId()), "UPDATE", updated.getTitle());
        return toCourseManageView(updated);
    }

    @Transactional
    public ApiDtos.CourseManageView publishCourse(SessionUser user, Long courseId) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        course.setStatus("OPEN");
        courseMapper.updateById(course);
        audit(user, "COURSE", String.valueOf(course.getId()), "PUBLISH", course.getTitle());
        return toCourseManageView(course);
    }

    @Transactional
    public void deleteCourse(SessionUser user, Long courseId) {
        ensureCourseManager(user);
        CourseEntity course = getCourse(courseId);
        ensureCourseOwnership(user, course);
        courseMapper.deleteById(courseId);
        courseEnrollmentMapper.delete(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId));
        audit(user, "COURSE", String.valueOf(courseId), "DELETE", course.getTitle());
    }

    @Transactional
    public synchronized ApiDtos.EnrollmentResult enrollCourse(SessionUser user, Long courseId) {
        if (user.role() == RoleType.ADMIN) {
            throw new BizException(403, "管理员不可直接报名课程");
        }
        CourseEntity course = getCourse(courseId);
        if (!"OPEN".equalsIgnoreCase(course.getStatus())) {
            throw new BizException(400, "课程未发布，暂不可报名");
        }
        if (courseEnrollmentMapper.selectCount(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getUserId, user.id())
                .eq(CourseEnrollmentEntity::getStatus, "ENROLLED")) > 0) {
            throw new BizException(400, "您已报名该课程");
        }
        int seatAvailable = PlatformUtils.safeInt(course.getSeatAvailable());
        if ("OFFLINE".equalsIgnoreCase(course.getCourseType()) && seatAvailable <= 0) {
            throw new BizException(400, "课程余量不足");
        }
        if ("OFFLINE".equalsIgnoreCase(course.getCourseType())) {
            course.setSeatAvailable(seatAvailable - 1);
        }
        course.setEnrollCount(PlatformUtils.safeInt(course.getEnrollCount()) + 1);
        courseMapper.updateById(course);

        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setCourseId(courseId);
        enrollment.setUserId(user.id());
        enrollment.setUserRole(user.role().name());
        enrollment.setEnrollmentNo("ENR" + System.currentTimeMillis());
        enrollment.setStatus("ENROLLED");
        courseEnrollmentMapper.insert(enrollment);

        audit(user, "TRAINING", String.valueOf(course.getId()), "ENROLL", "user=" + user.id());
        return new ApiDtos.EnrollmentResult(enrollment.getEnrollmentNo(), "SUCCESS", course.getTitle(), PlatformUtils.safeInt(course.getSeatAvailable()));
    }

    CourseEntity getCourse(Long courseId) {
        CourseEntity course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException(404, "课程不存在");
        }
        return course;
    }

    private void ensureCourseManager(SessionUser user) {
        if (user.role() != RoleType.ENTERPRISE && user.role() != RoleType.INSTITUTION && user.role() != RoleType.ADMIN) {
            throw new BizException(403, "当前角色无权管理课程");
        }
    }

    private void ensureCourseOwnership(SessionUser user, CourseEntity course) {
        if (!canManageCourse(user, course)) {
            throw new BizException(403, "无权操作该课程");
        }
    }

    private boolean canManageCourse(SessionUser user, CourseEntity course) {
        return user.role() == RoleType.ADMIN || Objects.equals(course.getPublishUserId(), user.id());
    }

    CourseEnrollmentEntity findActiveEnrollment(Long courseId, Long userId) {
        if (courseId == null || userId == null) {
            return null;
        }
        return courseEnrollmentMapper.selectOne(new LambdaQueryWrapper<CourseEnrollmentEntity>()
                .eq(CourseEnrollmentEntity::getCourseId, courseId)
                .eq(CourseEnrollmentEntity::getUserId, userId)
                .orderByDesc(CourseEnrollmentEntity::getId)
                .last("limit 1"));
    }

    private CourseEntity buildCourseEntity(SessionUser user, CourseEntity original, ApiDtos.CourseManageRequest request) {
        CourseEntity course = original == null ? new CourseEntity() : original;
        course.setTitle(request.title().trim());
        course.setSummary(request.summary().trim());
        course.setContent(request.content().trim());
        course.setCourseType(PlatformUtils.normalizeCourseType(request.learningMode()));
        course.setSeatTotal(Math.max(0, request.seatTotal()));
        course.setSeatAvailable(PlatformUtils.resolveSeatAvailable(original == null ? null : original.getSeatTotal(), original == null ? null : original.getSeatAvailable(), request.seatTotal()));
        course.setPrice(request.price());
        course.setStatus(PlatformUtils.normalizeCourseStatus(request.status()));
        course.setInstitutionName(resolveInstitutionName(user));
        course.setPublishUserId(original == null ? user.id() : original.getPublishUserId());
        if (original == null) {
            course.setBrowseCount(0);
            course.setEnrollCount(0);
        }
        return course;
    }

    private String resolveInstitutionName(SessionUser user) {
        UserAccountEntity owner = getUserById(user.id());
        if (!PlatformUtils.isBlank(owner.getCompanyName())) {
            return owner.getCompanyName();
        }
        return PlatformUtils.displayName(owner);
    }

    UserAccountEntity getUserById(Long id) {
        UserAccountEntity user = userAccountMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }

    private ApiDtos.CourseView toCourseView(CourseEntity course) {
        return new ApiDtos.CourseView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCourseType(),
                course.getInstitutionName(),
                PlatformUtils.safeInt(course.getSeatAvailable()),
                PlatformUtils.safeInt(course.getBrowseCount()),
                PlatformUtils.safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus()
        );
    }

    private ApiDtos.CourseDetailView toCourseDetailView(CourseEntity course, CourseEnrollmentEntity enrollment) {
        return new ApiDtos.CourseDetailView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getContent(),
                course.getCourseType(),
                course.getInstitutionName(),
                PlatformUtils.safeInt(course.getSeatTotal()),
                PlatformUtils.safeInt(course.getSeatAvailable()),
                PlatformUtils.safeInt(course.getBrowseCount()),
                PlatformUtils.safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus(),
                enrollment != null,
                enrollment == null ? null : enrollment.getEnrollmentNo(),
                enrollment == null ? null : enrollment.getStatus()
        );
    }

    private ApiDtos.CourseManageView toCourseManageView(CourseEntity course) {
        return new ApiDtos.CourseManageView(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCourseType(),
                PlatformUtils.safeInt(course.getSeatTotal()),
                PlatformUtils.safeInt(course.getSeatAvailable()),
                PlatformUtils.safeInt(course.getBrowseCount()),
                PlatformUtils.safeInt(course.getEnrollCount()),
                course.getPrice(),
                course.getStatus()
        );
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
