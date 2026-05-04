package com.lowaltitude.reststop.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowaltitude.reststop.server.entity.CourseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 培训课程Mapper接口。
 * <p>
 * 基于 MyBatis-Plus 的 BaseMapper，提供对 course 表的CRUD操作。
 * </p>
 */
@Mapper
public interface CourseMapper extends BaseMapper<CourseEntity> {
}
