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
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class CourseServiceExtendedTest {

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
    public void shouldListOpenCourses() {
        CourseEntity course = buildCourse(1L, "测试课程", "OPEN");
        Mockito.when(courseMapper.selectList(ArgumentMatchers.any())).thenReturn(List.of(course));

        List<ApiDtos.CourseView> courses = courseService.listCourses();

        Assertions.assertEquals(1, courses.size());
        Assertions.assertEquals("测试课程", courses.get(0).title());
    }

    @Test
    public void shouldRejectEnrollForAdmin() {
        SessionUser admin = new SessionUser(4L, "admin", RoleType.ADMIN, "管理员");
        Assertions.assertThrows(BizException.class, () -> courseService.enrollCourse(admin, 1L));
    }

    @Test
    public void shouldRejectEnrollForNonOpenCourse() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = buildCourse(1L, "草稿课程", "DRAFT");
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        Assertions.assertThrows(BizException.class, () -> courseService.enrollCourse(pilot, 1L));
    }

    @Test
    public void shouldRejectDuplicateEnrollment() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = buildCourse(1L, "测试课程", "OPEN");
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(1L);

        Assertions.assertThrows(BizException.class, () -> courseService.enrollCourse(pilot, 1L));
    }

    @Test
    public void shouldRejectEnrollWhenNoSeats() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = buildCourse(1L, "满员课程", "OPEN");
        course.setCourseType("OFFLINE");
        course.setSeatAvailable(0);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectCount(ArgumentMatchers.any())).thenReturn(0L);

        Assertions.assertThrows(BizException.class, () -> courseService.enrollCourse(pilot, 1L));
    }

    @Test
    public void shouldPublishCourse() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "测试课程", "DRAFT");
        course.setPublishUserId(3L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        ApiDtos.CourseManageView result = courseService.publishCourse(institution, 1L);

        Assertions.assertEquals("OPEN", result.status());
    }

    @Test
    public void shouldDeleteCourse() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "测试课程", "DRAFT");
        course.setPublishUserId(3L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        Assertions.assertDoesNotThrow(() -> courseService.deleteCourse(institution, 1L));
    }

    @Test
    public void shouldRejectNonManagerCreatingCourse() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        Assertions.assertThrows(BizException.class, () -> courseService.createCourse(
                pilot,
                new ApiDtos.CourseManageRequest("课程", "摘要", "内容", "OFFLINE", 30, BigDecimal.ZERO, "DRAFT")
        ));
    }

    @Test
    public void shouldGetCourseDetailForUnpublishedAsManager() {
        SessionUser institution = new SessionUser(3L, "institution_demo", RoleType.INSTITUTION, "培训机构");
        CourseEntity course = buildCourse(1L, "草稿课程", "DRAFT");
        course.setPublishUserId(3L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);
        Mockito.when(courseEnrollmentMapper.selectOne(ArgumentMatchers.any())).thenReturn(null);

        ApiDtos.CourseDetailView detail = courseService.getCourseDetail(institution, 1L);
        Assertions.assertEquals("草稿课程", detail.title());
    }

    @Test
    public void shouldRejectViewUnpublishedCourseForNonManager() {
        SessionUser pilot = new SessionUser(1L, "pilot_demo", RoleType.PILOT, "陈伶");
        CourseEntity course = buildCourse(1L, "草稿课程", "DRAFT");
        course.setPublishUserId(3L);
        Mockito.when(courseMapper.selectById(1L)).thenReturn(course);

        Assertions.assertThrows(BizException.class, () -> courseService.getCourseDetail(pilot, 1L));
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
}
