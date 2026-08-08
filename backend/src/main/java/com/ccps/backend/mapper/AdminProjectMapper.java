package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminProjectMapper {
    @Select("""
            SELECT COUNT(*) AS total_count,
                   COALESCE(SUM(status = 'active'), 0) AS active_count,
                   COALESCE(SUM(status = 'inactive'), 0) AS inactive_count,
                   (SELECT COUNT(*) FROM units) AS unit_count
            FROM projects
            """)
    SummaryRow findSummary();

    @Select("""
            <script>
            SELECT COUNT(*) FROM projects p
            WHERE 1 = 1
            <if test="keyword != null">
              AND (p.project_code LIKE CONCAT('%', #{keyword}, '%')
                   OR p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.address LIKE CONCAT('%', #{keyword}, '%')
                   OR p.state_name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.city LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null">AND p.status = #{status}</if>
            </script>
            """)
    long countRows(@Param("keyword") String keyword, @Param("status") String status);

    @Select("""
            <script>
            SELECT p.id, p.project_code, p.name, p.address, p.state_name AS state, p.city, p.country_code, p.status,
                   COUNT(DISTINCT u.id) AS unit_count,
                   COUNT(DISTINCT ou.owner_id) AS owner_count,
                   p.created_at, p.updated_at
            FROM projects p
            LEFT JOIN units u ON u.project_id = p.id
            LEFT JOIN owner_units ou ON ou.unit_id = u.id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (p.project_code LIKE CONCAT('%', #{keyword}, '%')
                   OR p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.address LIKE CONCAT('%', #{keyword}, '%')
                   OR p.state_name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.city LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null">AND p.status = #{status}</if>
            GROUP BY p.id, p.project_code, p.name, p.address, p.state_name, p.city, p.country_code, p.status,
                     p.created_at, p.updated_at
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ProjectRow> findRows(@Param("keyword") String keyword, @Param("status") String status,
                              @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT DISTINCT city FROM projects WHERE city IS NOT NULL AND TRIM(city) != '' ORDER BY city")
    List<String> findCities();

    @Select("""
            SELECT p.id, p.project_code, p.name, p.address, p.state_name AS state, p.city, p.country_code, p.status,
                   COUNT(DISTINCT u.id) AS unit_count,
                   COUNT(DISTINCT ou.owner_id) AS owner_count,
                   p.created_at, p.updated_at
            FROM projects p
            LEFT JOIN units u ON u.project_id = p.id
            LEFT JOIN owner_units ou ON ou.unit_id = u.id
            WHERE p.id = #{id}
            GROUP BY p.id, p.project_code, p.name, p.address, p.state_name, p.city, p.country_code, p.status,
                     p.created_at, p.updated_at
            """)
    ProjectRow findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM projects WHERE project_code = #{projectCode} AND (#{excludeId} IS NULL OR id != #{excludeId})")
    int countProjectCode(@Param("projectCode") String projectCode, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM units WHERE project_id = #{projectId}")
    int countUnits(@Param("projectId") Long projectId);

    @Select("""
            SELECT (SELECT COUNT(*) FROM units WHERE project_id = #{projectId})
                 + (SELECT COUNT(*) FROM report_runs WHERE project_id = #{projectId})
            """)
    int countReferences(@Param("projectId") Long projectId);

    @Insert("""
            INSERT INTO projects (project_code, name, address, state_name, city, country_code, status)
            VALUES (#{projectCode}, #{name}, #{address}, #{state}, #{city}, #{countryCode}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectWrite project);

    @Update("""
            UPDATE projects
            SET project_code = #{projectCode}, name = #{name}, address = #{address}, state_name = #{state}, city = #{city},
                country_code = #{countryCode}, status = #{status}
            WHERE id = #{id}
            """)
    int update(ProjectWrite project);

    @Delete("DELETE FROM projects WHERE id = #{id}")
    int delete(@Param("id") Long id);

    class SummaryRow {
        private Long totalCount;
        private Long activeCount;
        private Long inactiveCount;
        private Long unitCount;
        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getActiveCount() { return activeCount; }
        public void setActiveCount(Long activeCount) { this.activeCount = activeCount; }
        public Long getInactiveCount() { return inactiveCount; }
        public void setInactiveCount(Long inactiveCount) { this.inactiveCount = inactiveCount; }
        public Long getUnitCount() { return unitCount; }
        public void setUnitCount(Long unitCount) { this.unitCount = unitCount; }
    }

    class ProjectRow {
        private Long id;
        private String projectCode;
        private String name;
        private String address;
        private String state;
        private String city;
        private String countryCode;
        private String status;
        private Long unitCount;
        private Long ownerCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getUnitCount() { return unitCount; }
        public void setUnitCount(Long unitCount) { this.unitCount = unitCount; }
        public Long getOwnerCount() { return ownerCount; }
        public void setOwnerCount(Long ownerCount) { this.ownerCount = ownerCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    class ProjectWrite {
        private Long id;
        private String projectCode;
        private String name;
        private String address;
        private String state;
        private String city;
        private String countryCode;
        private String status;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
