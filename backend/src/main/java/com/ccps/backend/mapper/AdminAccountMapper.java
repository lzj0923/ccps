package com.ccps.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminAccountMapper {

    @Select("""
            SELECT u.id, u.username, u.email, u.display_name AS displayName, u.phone,
                   u.account_type AS accountType, u.status, o.id AS ownerId
            FROM users u
            LEFT JOIN owners o ON o.user_id = u.id
            ORDER BY u.account_type, u.display_name, u.id
            """)
    List<AccountRow> findAll();

    @Select("""
            SELECT u.id, u.username, u.email, u.display_name AS displayName, u.phone,
                   u.account_type AS accountType, u.status, o.id AS ownerId
            FROM users u
            LEFT JOIN owners o ON o.user_id = u.id
            WHERE u.id = #{id}
            """)
    AccountRow findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE username = #{username} AND id <> COALESCE(#{id}, 0)")
    int countUsername(@Param("username") String username, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email} AND id <> COALESCE(#{id}, 0)")
    int countEmail(@Param("email") String email, @Param("id") Long id);

    @Select("SELECT password_hash FROM users WHERE id = #{id}")
    String findPasswordHash(@Param("id") Long id);

    @Insert("""
            INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
            VALUES (#{username}, #{email}, #{passwordHash}, #{displayName}, #{phone}, #{accountType}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AccountRecord account);

    @Update("""
            UPDATE users
            SET username = #{username}, email = #{email}, password_hash = #{passwordHash},
                display_name = #{displayName}, phone = #{phone}, account_type = #{accountType}, status = #{status}
            WHERE id = #{id}
            """)
    int update(AccountRecord account);

    @Update("UPDATE users SET status = 'inactive' WHERE id = #{id}")
    int deactivate(@Param("id") Long id);

    class AccountRow {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String phone;
        private String accountType;
        private String status;
        private Long ownerId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    }

    class AccountRecord extends AccountRow {
        private String passwordHash;

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    }
}
