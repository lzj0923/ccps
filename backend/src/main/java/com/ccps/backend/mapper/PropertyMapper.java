package com.ccps.backend.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccps.backend.model.Property;

@Mapper
public interface PropertyMapper extends BaseMapper<Property> {
}
