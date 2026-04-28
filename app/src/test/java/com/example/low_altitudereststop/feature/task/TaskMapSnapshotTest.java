package com.example.low_altitudereststop.feature.task;

import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import org.junit.Assert;
import org.junit.Test;

public class TaskMapSnapshotTest {

    @Test
    public void shouldBuildMockTaskDetailWithCoordinatesAndOwner() {
        PlatformModels.TaskDetailView detail = AppScenarioMapper.findTaskDetail(88001L, null, null);

        Assert.assertNotNull(detail);
        Assert.assertEquals("长江沿线桥梁巡检补测项目", detail.title);
        Assert.assertNotNull(detail.latitude);
        Assert.assertNotNull(detail.longitude);
        Assert.assertEquals("企业调度中心", detail.ownerName);
    }

    @Test
    public void shouldBuildRouteSnapshotFromMockDetail() {
        PlatformModels.TaskDetailView detail = AppScenarioMapper.findTaskDetail(88002L, null, null);

        TaskDetailActivity.TaskMapSnapshot snapshot = TaskDetailActivity.TaskMapSnapshot.fromDetail(detail);

        Assert.assertNotNull(snapshot);
        Assert.assertEquals(4, snapshot.routePoints.size());
        Assert.assertTrue(snapshot.radiusMeters > 0);
        Assert.assertTrue(snapshot.distanceLabel().endsWith("km"));
    }
}
