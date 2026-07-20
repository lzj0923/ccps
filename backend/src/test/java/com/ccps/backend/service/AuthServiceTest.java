package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ccps.backend.dto.LoginRequest;
import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.mapper.UserMapper;
import com.ccps.backend.model.User;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String PASSWORD_HASH = "$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW";

    @Mock
    private UserMapper userMapper;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userMapper);
    }

    @Test
    void adminPortalLoginReturnsAdminAsTheActiveRole() {
        User user = activeUser(1L, "admin", "ADMIN");
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<Wrapper<User>>any())).thenReturn(user);
        when(userMapper.findRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        LoginResponse response = service.login(new LoginRequest("admin", "123456", true), "ADMIN");

        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.roles()).containsExactly("ADMIN");
        verify(userMapper).updateById(user);
    }

    @Test
    void adminOnlyAccountCannotLoginThroughOwnerPortal() {
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<Wrapper<User>>any()))
                .thenReturn(activeUser(1L, "admin", "ADMIN"));
        when(userMapper.findRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "123456", false), "OWNER"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void ownerAccountTypeSupportsOwnerLoginWithoutRoleRow() {
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<Wrapper<User>>any()))
                .thenReturn(activeUser(8L, "owner", "OWNER"));
        when(userMapper.findRoleCodes(8L)).thenReturn(List.of());

        LoginResponse response = service.login(new LoginRequest("owner", "123456", false), "OWNER");

        assertThat(response.role()).isEqualTo("OWNER");
        assertThat(response.roles()).containsExactly("OWNER");
    }

    @Test
    void roleRowsCannotOverrideTheAccountPortalType() {
        when(userMapper.selectOne(org.mockito.ArgumentMatchers.<Wrapper<User>>any()))
                .thenReturn(activeUser(9L, "admin", "ADMIN"));
        when(userMapper.findRoleCodes(9L)).thenReturn(List.of("OWNER"));

        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "123456", false), "OWNER"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    private User activeUser(Long id, String username, String accountType) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "username", username);
        ReflectionTestUtils.setField(user, "email", username + "@example.com");
        ReflectionTestUtils.setField(user, "passwordHash", PASSWORD_HASH);
        ReflectionTestUtils.setField(user, "displayName", username);
        ReflectionTestUtils.setField(user, "accountType", accountType);
        ReflectionTestUtils.setField(user, "status", "active");
        return user;
    }
}
