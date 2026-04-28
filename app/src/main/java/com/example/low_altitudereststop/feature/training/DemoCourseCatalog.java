package com.example.low_altitudereststop.feature.training;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import java.util.ArrayList;
import java.util.List;

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
}
