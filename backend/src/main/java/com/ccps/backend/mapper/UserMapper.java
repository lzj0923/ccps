package com.ccps.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccps.backend.model.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("""
            SELECT DISTINCT UPPER(r.code)
            FROM user_roles ur
            JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY UPPER(r.code)
            """)
    List<String> findRoleCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT UPPER(p.code)
            FROM user_roles ur
            JOIN role_permissions rp ON rp.role_id = ur.role_id
            JOIN permissions p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
            ORDER BY UPPER(p.code)
            """)
    List<String> findPermissionCodes(@Param("userId") Long userId);
}
