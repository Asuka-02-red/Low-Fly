package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.CourseEnrollmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程报名记录Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 course_enrollment 表的CRUD操作。
 * </p>
 */
@Mapper
public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollmentEntity> {
}
