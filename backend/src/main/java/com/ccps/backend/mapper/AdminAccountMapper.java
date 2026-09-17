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
                   sp.employee_no AS employeeNo,sp.department,sp.job_title AS jobTitle,
                   sp.hire_date AS hireDate,sp.leave_date AS leaveDate,sp.employment_status AS employmentStatus,
                   u.account_type AS accountType, u.status, o.id AS ownerId,
                   (SELECT UPPER(r.code) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = u.id AND UPPER(r.code) IN
                    ('SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION')
                    ORDER BY r.id LIMIT 1) AS staffRole
            FROM users u
            LEFT JOIN owners o ON o.user_id = u.id
            LEFT JOIN staff_profiles sp ON sp.user_id=u.id
            ORDER BY u.account_type, u.display_name, u.id
            """)
    List<AccountRow> findAll();

    @Select("""
            SELECT u.id, u.username, u.email, u.display_name AS displayName, u.phone,
                   sp.employee_no AS employeeNo,sp.department,sp.job_title AS jobTitle,
                   sp.hire_date AS hireDate,sp.leave_date AS leaveDate,sp.employment_status AS employmentStatus,
                   u.account_type AS accountType, u.status, o.id AS ownerId,
                   (SELECT UPPER(r.code) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = u.id AND UPPER(r.code) IN
                    ('SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION')
                    ORDER BY r.id LIMIT 1) AS staffRole
            FROM users u
            LEFT JOIN owners o ON o.user_id = u.id
            LEFT JOIN staff_profiles sp ON sp.user_id=u.id
            WHERE u.id = #{id}
            """)
    AccountRow findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE username = #{username} AND id <> COALESCE(#{id}, 0)")
    int countUsername(@Param("username") String username, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email} AND id <> COALESCE(#{id}, 0)")
    int countEmail(@Param("email") String email, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM staff_profiles WHERE employee_no=#{employeeNo} AND user_id<>COALESCE(#{userId},0)")
    int countEmployeeNo(@Param("employeeNo") String employeeNo, @Param("userId") Long userId);

    @Select("SELECT password_hash FROM users WHERE id = #{id}")
    String findPasswordHash(@Param("id") Long id);

    @Select("SELECT user_id FROM owners WHERE id = #{ownerId}")
    Long findOwnerAccountId(@Param("ownerId") Long ownerId);

    @Update("UPDATE users SET username=#{username}, display_name=#{displayName}, phone=#{phone}, status=#{status} WHERE id=#{id}")
    int updateOwnerLogin(@Param("id") Long id, @Param("username") String username,
            @Param("displayName") String displayName, @Param("phone") String phone,
            @Param("status") String status);

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

    @Insert("""
            INSERT INTO staff_profiles
              (user_id,employee_no,department,job_title,hire_date,leave_date,employment_status)
            VALUES (#{userId},#{employeeNo},#{department},#{jobTitle},#{hireDate},#{leaveDate},#{employmentStatus})
            """)
    int insertStaffProfile(@Param("userId") Long userId, @Param("employeeNo") String employeeNo,
            @Param("department") String department, @Param("jobTitle") String jobTitle,
            @Param("hireDate") java.time.LocalDate hireDate, @Param("leaveDate") java.time.LocalDate leaveDate,
            @Param("employmentStatus") String employmentStatus);

    @Update("""
            UPDATE staff_profiles SET employee_no=#{employeeNo},department=#{department},job_title=#{jobTitle},
              hire_date=#{hireDate},leave_date=#{leaveDate},employment_status=#{employmentStatus}
            WHERE user_id=#{userId}
            """)
    int updateStaffProfile(@Param("userId") Long userId, @Param("employeeNo") String employeeNo,
            @Param("department") String department, @Param("jobTitle") String jobTitle,
            @Param("hireDate") java.time.LocalDate hireDate, @Param("leaveDate") java.time.LocalDate leaveDate,
            @Param("employmentStatus") String employmentStatus);

    @Insert("""
            INSERT INTO audit_logs(actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'update_staff_profile','user',#{userId},JSON_OBJECT(),
                    JSON_OBJECT('employeeNo',#{employeeNo},'employmentStatus',#{employmentStatus}))
            """)
    int insertStaffProfileAudit(@Param("actorId") Long actorId, @Param("userId") Long userId,
            @Param("employeeNo") String employeeNo, @Param("employmentStatus") String employmentStatus);

    @Update("UPDATE users SET status = 'inactive' WHERE id = #{id}")
    int deactivate(@Param("id") Long id);

    @org.apache.ibatis.annotations.Delete("""
            DELETE ur FROM user_roles ur JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND UPPER(r.code) IN
            ('ADMIN','OWNER','SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION')
            """)
    int deleteManagedRoles(@Param("userId") Long userId);

    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO user_roles (user_id, role_id)
            SELECT #{userId}, r.id FROM roles r WHERE UPPER(r.code) = UPPER(#{roleCode})
            """)
    int insertRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    class AccountRow {
        private Long id;
        private String username;
        private String email;
        private String displayName;
        private String phone;
        private String accountType;
        private String staffRole;
        private String status;
        private String employeeNo;
        private String department;
        private String jobTitle;
        private java.time.LocalDate hireDate;
        private java.time.LocalDate leaveDate;
        private String employmentStatus;
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
        public String getStaffRole() { return staffRole; }
        public void setStaffRole(String staffRole) { this.staffRole = staffRole; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getEmployeeNo() { return employeeNo; }
        public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
        public java.time.LocalDate getHireDate() { return hireDate; }
        public void setHireDate(java.time.LocalDate hireDate) { this.hireDate = hireDate; }
        public java.time.LocalDate getLeaveDate() { return leaveDate; }
        public void setLeaveDate(java.time.LocalDate leaveDate) { this.leaveDate = leaveDate; }
        public String getEmploymentStatus() { return employmentStatus; }
        public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    }

    class AccountRecord extends AccountRow {
        private String passwordHash;

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    }
}
