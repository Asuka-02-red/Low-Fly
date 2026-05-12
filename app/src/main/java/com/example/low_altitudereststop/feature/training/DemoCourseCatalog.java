package com.example.low_altitudereststop.feature.training;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * 演示课程目录数据源，提供预置的培训课程数据。
 * <p>
 * 构建包含无人机法规、安全操作、巡检技术、航拍技巧、
 * 维护保养等预置课程数据，用于演示模式或网络不可用时的课程列表展示。
 * </p>
 */
public final class DemoCourseCatalog {

    private DemoCourseCatalog() {
    }

    @NonNull
    public static List<PlatformModels.CourseView> mergeWithDemo(@Nullable List<PlatformModels.CourseView> remote) {
        return AppScenarioMapper.buildFallbackCourses(remote);
    }

    @NonNull
    public static List<PlatformModels.CourseManageView> buildManagedFallback(@Nullable List<PlatformModels.CourseManageView> remote) {
        List<PlatformModels.CourseManageView> items = new ArrayList<>();
        if (remote != null && !remote.isEmpty()) {
            items.addAll(remote);
            return items;
        }
        for (PlatformModels.CourseView course : AppScenarioMapper.buildFallbackCourses(null)) {
            if (course == null) {
                continue;
            }
            PlatformModels.CourseManageView manageView = new PlatformModels.CourseManageView();
            manageView.id = course.id;
            manageView.title = course.title;
            manageView.summary = course.summary;
            manageView.learningMode = course.learningMode;
            manageView.seatAvailable = course.seatAvailable;
            manageView.seatTotal = Math.max(course.seatAvailable, course.enrollCount + course.seatAvailable);
            manageView.browseCount = course.browseCount;
            manageView.enrollCount = course.enrollCount;
            manageView.price = course.price;
            manageView.status = course.status;
            items.add(manageView);
        }
        return items;
    }

    @Nullable
    public static PlatformModels.CourseDetailView findDemoDetail(long courseId) {
        return AppScenarioMapper.findCourseDetail(courseId);
    }

    @Nullable
    public static PlatformModels.CourseDetailView findFirstDemoDetail() {
        return AppScenarioMapper.findFirstCourseDetail();
    }
}
