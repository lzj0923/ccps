package com.ccps.backend.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccps.backend.dto.PropertyRequest;
import com.ccps.backend.dto.PropertyResponse;
import com.ccps.backend.exception.ResourceNotFoundException;
import com.ccps.backend.mapper.PropertyMapper;
import com.ccps.backend.model.Property;

@Service
public class PropertyService extends ServiceImpl<PropertyMapper, Property> {

    public List<PropertyResponse> findAllProperties() {
        return list().stream().map(this::toResponse).toList();
    }

    public PropertyResponse findPropertyById(Long id) {
        return toResponse(getRequired(id));
    }

    @Transactional
    public PropertyResponse createProperty(PropertyRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        Property property = new Property(request.name(), request.projectName(), request.address(), request.price(),
                request.area(), request.bedrooms(), request.status(), now, now);
        save(property);
        return toResponse(property);
    }

    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {
        Property property = getRequired(id);
        property.update(request.name(), request.projectName(), request.address(), request.price(), request.area(),
                request.bedrooms(), request.status(), OffsetDateTime.now());
        updateById(property);
        return toResponse(property);
    }

    @Transactional
    public void deleteProperty(Long id) {
        getRequired(id);
        removeById(id);
    }

    private Property getRequired(Long id) {
        Property property = getById(id);
        if (property == null) {
            throw new ResourceNotFoundException("找不到物業：" + id);
        }
        return property;
    }

    private PropertyResponse toResponse(Property property) {
        return new PropertyResponse(property.getId(), property.getName(), property.getProjectName(),
                property.getAddress(), property.getPrice(), property.getArea(), property.getBedrooms(),
                property.getStatus(), property.getCreatedAt(), property.getUpdatedAt());
    }
}
