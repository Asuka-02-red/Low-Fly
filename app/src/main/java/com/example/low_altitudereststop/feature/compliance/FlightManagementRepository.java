package com.example.low_altitudereststop.feature.compliance;

import android.content.Context;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.example.low_altitudereststop.core.trace.OperationLogStore;
import com.example.low_altitudereststop.feature.demo.AppScenarioMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 飞行管理本地数据仓库，负责飞行申请和禁飞区数据的持久化与查询。
 * <p>
 * 基于FileCache实现JSON序列化存储，提供飞行申请的增删改查、
 * 状态批量更新、禁飞区的搜索/新增/编辑/删除、远程禁飞区数据合并，
 * 以及操作审计日志记录。数据为空时自动填充演示种子数据。
 * </p>
 */
public class FlightManagementRepository {

    private static final String APPLICATION_CACHE = "flight_applications_manage.json";
    private static final String ZONE_CACHE = "flight_no_fly_zone_manage.json";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FileCache cache;
    private final OperationLogStore operationLogStore;
    private final Gson gson = new Gson();

    public FlightManagementRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.cache = new FileCache(appContext);
        this.operationLogStore = new OperationLogStore(appContext);
    }

    public List<FlightManagementModels.FlightApplicationRecord> listApplications() {
        Type type = new TypeToken<List<FlightManagementModels.FlightApplicationRecord>>() {
        }.getType();
        List<FlightManagementModels.FlightApplicationRecord> records = readList(APPLICATION_CACHE, type);
        if (records != null && !records.isEmpty()) {
            return sortApplications(records);
        }
        List<FlightManagementModels.FlightApplicationRecord> seeded = buildSeedApplications();
        writeApplications(seeded);
        return sortApplications(seeded);
    }

    public List<FlightManagementModels.FlightApplicationRecord> updateApplicationStatus(
            List<String> applicationNos,
            String targetStatus,
            String opinion
    ) {
        List<FlightManagementModels.FlightApplicationRecord> records = listApplications();
        String workflowStatus = FlightApplicationWorkflow.nextWorkflowStatus(targetStatus);
        String finalOpinion = blankToDefault(opinion, "企业端已完成审核");
        int changedCount = 0;
        for (FlightManagementModels.FlightApplicationRecord record : records) {
            if (record == null || applicationNos == null || !applicationNos.contains(record.applicationNo)) {
                continue;
            }
            record.status = targetStatus;
            record.workflowStatus = workflowStatus;
            record.approvalOpinion = finalOpinion;
            record.updatedAt = nowText();
            record.selected = false;
            changedCount++;
        }
        writeApplications(records);
        appendAudit(
                "FLIGHT_APPLICATION_" + FlightApplicationWorkflow.normalizeFilter(targetStatus),
                "count=" + changedCount + ", opinion=" + finalOpinion
        );
        return sortApplications(records);
    }

    public List<FlightManagementModels.NoFlyZoneRecord> listZones(String keyword) {
        List<FlightManagementModels.NoFlyZoneRecord> zones = ensureZones();
        if (keyword == null || keyword.trim().isEmpty()) {
            return zones;
        }
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<FlightManagementModels.NoFlyZoneRecord> filtered = new ArrayList<>();
        for (FlightManagementModels.NoFlyZoneRecord zone : zones) {
            if (zone == null) {
                continue;
            }
            String haystack = safe(zone.name) + "|" + safe(zone.reason) + "|" + safe(zone.description) + "|" + safe(zone.zoneType);
            if (haystack.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(zone);
            }
        }
        return filtered;
    }

    public FlightManagementModels.NoFlyZoneRecord getZone(String zoneId) {
        for (FlightManagementModels.NoFlyZoneRecord zone : ensureZones()) {
            if (zone != null && safe(zone.id).equals(zoneId)) {
                return zone;
            }
        }
        return null;
    }

    public List<FlightManagementModels.NoFlyZoneRecord> saveZone(FlightManagementModels.NoFlyZoneRecord zone) {
        List<FlightManagementModels.NoFlyZoneRecord> zones = ensureZones();
        boolean updated = false;
        if (zone.id == null || zone.id.trim().isEmpty()) {
            zone.id = "ZONE-" + System.currentTimeMillis();
        }
        for (int i = 0; i < zones.size(); i++) {
            FlightManagementModels.NoFlyZoneRecord current = zones.get(i);
            if (current != null && safe(current.id).equals(zone.id)) {
                zones.set(i, zone);
                updated = true;
                break;
            }
        }
        if (!updated) {
            zones.add(0, zone);
        }
        writeZones(zones);
        appendAudit(updated ? "NO_FLY_ZONE_UPDATE" : "NO_FLY_ZONE_CREATE", zone.id + "," + safe(zone.name));
        return zones;
    }

    public List<FlightManagementModels.NoFlyZoneRecord> deleteZone(String zoneId) {
        List<FlightManagementModels.NoFlyZoneRecord> zones = ensureZones();
        List<FlightManagementModels.NoFlyZoneRecord> result = new ArrayList<>();
        for (FlightManagementModels.NoFlyZoneRecord zone : zones) {
            if (zone == null || safe(zone.id).equals(zoneId)) {
                continue;
            }
            result.add(zone);
        }
        writeZones(result);
        appendAudit("NO_FLY_ZONE_DELETE", safe(zoneId));
        return result;
    }

    public void mergeRemoteZones(List<PlatformModels.NoFlyZoneView> remoteZones) {
        if (remoteZones == null || remoteZones.isEmpty()) {
            return;
        }
        Map<String, FlightManagementModels.NoFlyZoneRecord> merged = new LinkedHashMap<>();
        for (FlightManagementModels.NoFlyZoneRecord zone : ensureZones()) {
            if (zone != null) {
                merged.put(safe(zone.id), zone);
            }
        }
        for (PlatformModels.NoFlyZoneView remote : remoteZones) {
            if (remote == null || remote.id == null) {
                continue;
            }
            String zoneId = "REMOTE-" + remote.id;
            FlightManagementModels.NoFlyZoneRecord local = merged.get(zoneId);
            if (local == null) {
                local = new FlightManagementModels.NoFlyZoneRecord();
                local.id = zoneId;
                local.builtIn = true;
            }
            local.name = blankToDefault(remote.name, "系统禁飞区");
            local.zoneType = blankToDefault(remote.zoneType, "FORBIDDEN");
            local.centerLat = remote.centerLat == null ? BigDecimal.valueOf(29.56301) : remote.centerLat;
            local.centerLng = remote.centerLng == null ? BigDecimal.valueOf(106.55156) : remote.centerLng;
            local.radius = Math.max(remote.radius, 1000);
            local.reason = blankToDefault(remote.description, "平台同步禁飞区");
            local.description = blankToDefault(remote.description, "来源于平台禁飞区同步");
            if (local.effectiveStart == null) {
                local.effectiveStart = "2026-04-01 00:00";
            }
            if (local.effectiveEnd == null) {
                local.effectiveEnd = "2026-12-31 23:59";
            }
            merged.put(zoneId, local);
        }
        writeZones(new ArrayList<>(merged.values()));
    }

    private List<FlightManagementModels.NoFlyZoneRecord> ensureZones() {
        Type type = new TypeToken<List<FlightManagementModels.NoFlyZoneRecord>>() {
        }.getType();
        List<FlightManagementModels.NoFlyZoneRecord> zones = readList(ZONE_CACHE, type);
        if (zones != null && !zones.isEmpty()) {
            zones.sort(Comparator.comparing(zone -> safe(zone.name)));
            return zones;
        }
        List<FlightManagementModels.NoFlyZoneRecord> seeded = buildSeedZones();
        writeZones(seeded);
        return seeded;
    }

    private void writeApplications(List<FlightManagementModels.FlightApplicationRecord> records) {
        cache.write(APPLICATION_CACHE, gson.toJson(records));
    }

    private void writeZones(List<FlightManagementModels.NoFlyZoneRecord> zones) {
        zones.sort(Comparator.comparing(zone -> safe(zone.name)));
        cache.write(ZONE_CACHE, gson.toJson(zones));
    }

    private <T> List<T> readList(String name, Type type) {
        try {
            String json = cache.read(name);
            if (json == null || json.trim().isEmpty()) {
                return null;
            }
            return gson.fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<FlightManagementModels.FlightApplicationRecord> sortApplications(
            List<FlightManagementModels.FlightApplicationRecord> records
    ) {
        records.sort((left, right) -> safe(right.updatedAt).compareTo(safe(left.updatedAt)));
        return records;
    }

    private List<FlightManagementModels.FlightApplicationRecord> buildSeedApplications() {
        return new ArrayList<>(AppScenarioMapper.buildFlightApplications());
    }

    private List<FlightManagementModels.NoFlyZoneRecord> buildSeedZones() {
        return new ArrayList<>(AppScenarioMapper.buildNoFlyZones());
    }

    private void appendAudit(String tag, String message) {
        operationLogStore.appendAudit(tag, message);
    }

    private String nowText() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
