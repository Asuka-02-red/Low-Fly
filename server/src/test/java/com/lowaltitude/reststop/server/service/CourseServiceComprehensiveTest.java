package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import com.lowaltitude.reststop.server.entity.UserAccountEntity;
import com.lowaltitude.reststop.server.mapper.CourseEnrollmentMapper;
import com.lowaltitude.reststop.server.mapper.CourseMapper;
import com.lowaltitude.reststop.server.mapper.UserAccountMapper;
import com.lowaltitude.reststop.server.security.RoleType;
import com.lowaltitude.reststop.server.security.SessionUser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class CourseServiceComprehensiveTest {

    private CourseService courseService;
    private CourseMapper courseMapper;
    private CourseEnrollmentMapper courseEnrollmentMapper;
    private UserAccountMapper userAccountMapper;
    private AuditLogService auditLogService;

    @BeforeEach
    public void setUp() {
        courseMapper = Mockito.mock(CourseMapper.class);
        courseEnrollmentMapper = Mockito.mock(CourseEnrollmentMapper.class);
        userAccountMapper = Mockito.mock(UserAccountMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        courseService = new CourseService(courseMapper, courseEnrollmentMapper, userAccountMapper, auditLogService);
    }

    @Test
    public void shouldUpdateCourse() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "旧标题", "DRAFT");
        course.setPublishUserId(3L);
        course.setSeatTotal(30);
        course.setSeatAvailable(20);
        course.setEnrollCount(5);
        course.setBrowseCount(10);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        UserAccountEntity owner = buildUser(3L, "institution_demo", "INSTITUTION");
        owner.setCompanyName("示范培训机构");
        Mockito.when(userAccountMapper.selectById(3L)).thenReturn(owner);

        ApiDtos.CourseManageView result = courseService.updateCourse(
                institution,
                1L,
                new ApiDtos.CourseManageRequest("新标题", "新摘要", "新内容", "OFFLINE", 24, BigDecimal.valueOf(299), "OPEN")
        );

        Assertions.assertEquals(1L, result.id());
        Assertions.assertEquals("OFFLINE", result.learningMode());
    }

    @Test
    public void shouldRejectUpdateOthersCourse() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "标题", "DRAFT");
        course.setPublishUserId(99L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class, () -> courseService.updateCourse(
                institution,
                1L,
                new ApiDtos.CourseManageRequest("新标题", "新摘要", "新内容", "OFFLINE", 24, BigDecimal.ZERO, "DRAFT")
        ));
    }

    @Test
    public void shouldListManagedCoursesForAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        CourseEntity course = buildCourse(1L, "测试课程", "OPEN");
        Mockito.when(courseMapper.selectList(ArgumentMatchers.any())).thenReturn(java.util.List.of(course));

        java.util.List<ApiDtos.CourseManageView> courses = courseService.listManagedCourses(admin);

        Assertions.assertEquals(1, courses.size());
    }

    @Test
    public void shouldEnrollOnlineCourseWithoutSeatLimit() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手");
        CourseEntity course = buildCourse(1L, "在线课程", "OPEN");
        course.setCourseType("ARTICLE");
        course.setSeatAvailable(0);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.doAnswer(invocation -> {
            CourseEnrollmentEntity enrollment = invocation.getArgument(0);
            enrollment.setId(3001L);
            return 1;
        }).when(courseEnrollmentMapper).insert(ArgumentMatchers.any(CourseEnrollmentEntity.class));

        ApiDtos.EnrollmentResult result = courseService.enrollCourse(pilot, 1L);

        Assertions.assertEquals("SUCCESS", result.status());
    }

    @Test
    public void shouldRejectDeleteOthersCourse() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "测试课程", "DRAFT");
        course.setPublishUserId(99L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class, () -> courseService.deleteCourse(institution, 1L));
    }

    @Test
    public void shouldRejectNonExistentCourse() {
        Mockito.when(courseMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(com.lowaltitude.reststop.server.common.BizException.class, () -> courseService.getCourseDetail(
                new SessionUser(1L, "pilot_demo", RoleType.PILOT, "张飞手"), 999L));
    }

    private CourseEntity buildCourse(Long id, String title, String status) {
        CourseEntity course = new CourseEntity();
        course.setId(id);
        course.setTitle(title);
        course.setSummary("测试摘要");
        course.setContent("测试内容");
        course.setCourseType("ARTICLE");
        course.setInstitutionName("测试机构");
        course.setSeatTotal(30);
        course.setSeatAvailable(20);
        course.setBrowseCount(10);
        course.setEnrollCount(5);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus(status);
        course.setPublishUserId(3L);
        return course;
    }

    private UserAccountEntity buildUser(Long id, String username, String role) {
        UserAccountEntity user = new UserAccountEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("{noop}demo123");
        user.setPhone("13800138000");
        user.setRole(role);
        user.setRealName("测试用户");
        user.setCompanyName("测试企业");
        return user;
    }
}
