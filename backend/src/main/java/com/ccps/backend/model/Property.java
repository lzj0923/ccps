package com.ccps.backend.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("properties")
public class Property {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField("project_name")
    private String projectName;
    private String address;
    private BigDecimal price;
    private Integer area;
    private Integer bedrooms;

    private PropertyStatus status;
    @TableField("created_at")
    private OffsetDateTime createdAt;
    @TableField("updated_at")
    private OffsetDateTime updatedAt;

    protected Property() {
    }

    public Property(String name, String projectName, String address, BigDecimal price, Integer area,
                    Integer bedrooms, PropertyStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.name = name;
        this.projectName = projectName;
        this.address = address;
        this.price = price;
        this.area = area;
        this.bedrooms = bedrooms;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getProjectName() { return projectName; }
    public String getAddress() { return address; }
    public BigDecimal getPrice() { return price; }
    public Integer getArea() { return area; }
    public Integer getBedrooms() { return bedrooms; }
    public PropertyStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void update(String name, String projectName, String address, BigDecimal price, Integer area,
                       Integer bedrooms, PropertyStatus status, OffsetDateTime updatedAt) {
        this.name = name;
        this.projectName = projectName;
        this.address = address;
        this.price = price;
        this.area = area;
        this.bedrooms = bedrooms;
        this.status = status;
        this.updatedAt = updatedAt;
    }
}
