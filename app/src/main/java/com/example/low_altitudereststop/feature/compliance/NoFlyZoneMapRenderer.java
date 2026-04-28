package com.example.low_altitudereststop.feature.compliance;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;
import com.example.low_altitudereststop.core.model.PlatformModels;
import com.example.low_altitudereststop.core.storage.FileCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class NoFlyZoneMapRenderer {

    public static final String ZONE_REMOTE_CACHE = "zones.json";
    public static final LatLng DEFAULT_CENTER = new LatLng(29.56301, 106.55156);
    private static final float DEFAULT_ZOOM = 9.5f;
    private static final int CAMERA_PADDING = 120;
    private static final int MAP_FORBIDDEN_STROKE = 0xFFDC2626;
    private static final int MAP_FORBIDDEN_FILL = 0x44FCA5A5;
    private static final int MAP_RESTRICTED_STROKE = 0xFF2563EB;
    private static final int MAP_RESTRICTED_FILL = 0x443BA3FF;

    private NoFlyZoneMapRenderer() {
    }

    public static void configureBaseMap(@NonNull AMap map, boolean showZoomControls) {
        map.getUiSettings().setZoomControlsEnabled(showZoomControls);
        map.getUiSettings().setCompassEnabled(true);
        map.setMapType(AMap.MAP_TYPE_SATELLITE);
        map.setMinZoomLevel(3f);
        map.setMaxZoomLevel(18f);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
    }

    public static void cacheRemoteZones(@NonNull Context context, @Nullable List<PlatformModels.NoFlyZoneView> zones) {
        new FileCache(context).write(ZONE_REMOTE_CACHE, new Gson().toJson(zones == null ? List.of() : zones));
    }

    @NonNull
    public static List<PlatformModels.NoFlyZoneView> readCachedRemoteZones(@NonNull Context context) {
        try {
            String json = new FileCache(context).read(ZONE_REMOTE_CACHE);
            if (json == null || json.trim().isEmpty()) {
                return List.of();
            }
            Type type = new TypeToken<List<PlatformModels.NoFlyZoneView>>() {
            }.getType();
            List<PlatformModels.NoFlyZoneView> zones = new Gson().fromJson(json, type);
            return zones == null ? List.of() : zones;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public static void renderLocalZones(@NonNull AMap map, @Nullable List<FlightManagementModels.NoFlyZoneRecord> zones) {
        map.clear();
        List<RenderZone> renderZones = new ArrayList<>();
        if (zones != null) {
            for (FlightManagementModels.NoFlyZoneRecord zone : zones) {
                RenderZone renderZone = fromLocal(zone);
                if (renderZone != null) {
                    renderZones.add(renderZone);
                }
            }
        }
        renderStandalone(map, renderZones);
    }

    public static void renderRemoteZones(@NonNull AMap map, @Nullable List<PlatformModels.NoFlyZoneView> zones) {
        map.clear();
        List<RenderZone> renderZones = new ArrayList<>();
        if (zones != null) {
            for (PlatformModels.NoFlyZoneView zone : zones) {
                RenderZone renderZone = fromRemote(zone);
                if (renderZone != null) {
                    renderZones.add(renderZone);
                }
            }
        }
        renderStandalone(map, renderZones);
    }

    public static int addRemoteZoneOverlays(
            @NonNull AMap map,
            @Nullable List<PlatformModels.NoFlyZoneView> zones,
            @Nullable LatLngBounds.Builder boundsBuilder
    ) {
        int count = 0;
        if (zones == null) {
            return 0;
        }
        for (PlatformModels.NoFlyZoneView zone : zones) {
            RenderZone renderZone = fromRemote(zone);
            if (renderZone == null) {
                continue;
            }
            drawZone(map, renderZone);
            if (boundsBuilder != null) {
                boundsBuilder.include(renderZone.center);
            }
            count++;
        }
        return count;
    }

    private static void renderStandalone(@NonNull AMap map, @NonNull List<RenderZone> zones) {
        if (zones.isEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
            return;
        }
        LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
        for (RenderZone zone : zones) {
            drawZone(map, zone);
            boundsBuilder.include(zone.center);
        }
        if (zones.size() == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(zones.get(0).center, 10.5f));
            return;
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), CAMERA_PADDING));
    }

    private static void drawZone(@NonNull AMap map, @NonNull RenderZone zone) {
        map.addMarker(new MarkerOptions()
                .position(zone.center)
                .title(zone.name)
                .snippet(zone.description)
                .icon(BitmapDescriptorFactory.defaultMarker(zone.markerHue)));
        map.addCircle(new CircleOptions()
                .center(zone.center)
                .radius(zone.radius)
                .strokeWidth(4f)
                .strokeColor(zone.strokeColor)
                .fillColor(zone.fillColor));
    }

    @Nullable
    private static RenderZone fromLocal(@Nullable FlightManagementModels.NoFlyZoneRecord zone) {
        if (zone == null || zone.centerLat == null || zone.centerLng == null) {
            return null;
        }
        return buildZone(zone.name, zone.zoneType, zone.centerLat, zone.centerLng, zone.radius, zone.reason == null ? zone.description : zone.reason);
    }

    @Nullable
    private static RenderZone fromRemote(@Nullable PlatformModels.NoFlyZoneView zone) {
        if (zone == null || zone.centerLat == null || zone.centerLng == null) {
            return null;
        }
        return buildZone(zone.name, zone.zoneType, zone.centerLat, zone.centerLng, zone.radius, zone.description);
    }

    @Nullable
    private static RenderZone buildZone(
            @Nullable String name,
            @Nullable String zoneType,
            @Nullable BigDecimal lat,
            @Nullable BigDecimal lng,
            int radius,
            @Nullable String description
    ) {
        if (lat == null || lng == null) {
            return null;
        }
        boolean forbidden = "FORBIDDEN".equalsIgnoreCase(zoneType);
        return new RenderZone(
                new LatLng(lat.doubleValue(), lng.doubleValue()),
                name == null || name.trim().isEmpty() ? "禁飞区" : name,
                description == null ? "" : description,
                Math.max(radius, 100),
                forbidden ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_AZURE,
                forbidden ? MAP_FORBIDDEN_STROKE : MAP_RESTRICTED_STROKE,
                forbidden ? MAP_FORBIDDEN_FILL : MAP_RESTRICTED_FILL
        );
    }

    private static final class RenderZone {
        final LatLng center;
        final String name;
        final String description;
        final int radius;
        final float markerHue;
        final int strokeColor;
        final int fillColor;

        RenderZone(LatLng center, String name, String description, int radius, float markerHue, int strokeColor, int fillColor) {
            this.center = center;
            this.name = name;
            this.description = description;
            this.radius = radius;
            this.markerHue = markerHue;
            this.strokeColor = strokeColor;
            this.fillColor = fillColor;
        }
    }
}
