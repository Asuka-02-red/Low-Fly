package com.lowaltitude.reststop.server.service;

import com.lowaltitude.reststop.server.api.ApiDtos;
import com.lowaltitude.reststop.server.common.BizException;
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

public class CourseServiceTest {

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
    public void shouldDecreaseCourseSeatOnEnroll() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = new CourseEntity();
        course.setId(2001L);
        course.setTitle("民航法规基础课");
        course.setCourseType("OFFLINE");
        course.setStatus("OPEN");
        course.setSeatAvailable(18);
        course.setEnrollCount(0);
        Mockito.when(courseMapper.selectById(2001L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);
        Mockito.doAnswer(invocation -> {
            CourseEnrollmentEntity enrollment = invocation.getArgument(0);
            enrollment.setId(3001L);
            return 1;
        }).when(courseEnrollmentMapper).insert(ArgumentMatchers.any(CourseEnrollmentEntity.class));

        ApiDtos.EnrollmentResult result = courseService.enrollCourse(pilot, 2001L);

        Assertions.assertTrue(result.seatAvailable() < 18);
    }

    @Test
    public void shouldExposeEnrollmentStateInCourseDetail() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = new CourseEntity();
        course.setId(2001L);
        course.setTitle("民航法规基础课");
        course.setCourseType("OFFLINE");
        course.setStatus("OPEN");
        course.setSeatTotal(30);
        course.setSeatAvailable(18);
        course.setEnrollCount(4);
        course.setBrowseCount(12);
        Mockito.when(courseMapper.selectById(2001L)).thenReturn(course);
        CourseEnrollmentEntity enrollment = new CourseEnrollmentEntity();
        enrollment.setCourseId(2001L);
        enrollment.setUserId(1L);
        enrollment.setEnrollmentNo("ENR2001");
        enrollment.setStatus("ENROLLED");
        Mockito.when(courseEnrollmentMapper.selectOne(ArgumentMatchers.any())).thenReturn(enrollment);

        ApiDtos.CourseDetailView detail = courseService.getCourseDetail(pilot, 2001L);

        Assertions.assertTrue(detail.enrolled());
        Assertions.assertEquals("ENR2001", detail.enrollmentNo());
        Assertions.assertEquals("ENROLLED", detail.enrollmentStatus());
    }

    @Test
    public void shouldAllowInstitutionManagingCourses() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        UserAccountEntity owner = buildUser(3L, "institution_demo", "INSTITUTION");
        owner.setCompanyName("示范培训机构");
        Mockito.when(userAccountMapper.selectById(3L)).thenReturn(owner);
        Mockito.doAnswer(invocation -> {
            CourseEntity course = invocation.getArgument(0);
            course.setId(4001L);
            return 1;
        }).when(courseMapper).insert(ArgumentMatchers.any(CourseEntity.class));

        ApiDtos.CourseManageView created = courseService.createCourse(
                institution,
                new ApiDtos.CourseManageRequest(
                        "空域法规速训",
                        "面向机构教员的法规训练营",
                        "覆盖法规、案例复盘和带班规范。",
                        "OFFLINE",
                        24,
                        BigDecimal.valueOf(299),
                        "DRAFT"
                )
        );

        Assertions.assertEquals(4001L, created.id());
        Assertions.assertEquals("OFFLINE", created.learningMode());
        Assertions.assertEquals("DRAFT", created.status());
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
