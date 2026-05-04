package com.lowaltitude.reststop.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 禁飞区实体。
 * <p>
 * 对应 no_fly_zone 表，存储禁飞区域信息，
 * 包括区域名称、类型、中心点经纬度、半径及描述。
 * </p>
 */
@TableName("no_fly_zone")
public class NoFlyZoneEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("zone_type")
    private String zoneType;

    @TableField("center_lat")
    private java.math.BigDecimal centerLat;

    @TableField("center_lng")
    private java.math.BigDecimal centerLng;

    @TableField("radius")
    private Integer radius;

    @TableField("description")
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getZoneType() {
        return zoneType;
    }

    public void setZoneType(String zoneType) {
        this.zoneType = zoneType;
    }

    public java.math.BigDecimal getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(java.math.BigDecimal centerLat) {
        this.centerLat = centerLat;
    }

    public java.math.BigDecimal getCenterLng() {
        return centerLng;
    }

    public void setCenterLng(java.math.BigDecimal centerLng) {
        this.centerLng = centerLng;
    }

    public Integer getRadius() {
        return radius;
    }

    public void setRadius(Integer radius) {
        this.radius = radius;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
