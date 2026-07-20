USE ccps_property_management;

SELECT u.id, u.username, u.email, u.account_type, u.status,
       GROUP_CONCAT(DISTINCT r.code ORDER BY r.code SEPARATOR ',') AS roles,
       o.id AS owner_id, o.full_name AS owner_name,
       COUNT(DISTINCT ou.id) AS property_count
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
LEFT JOIN owners o ON o.user_id = u.id AND o.status = 'active'
LEFT JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active'
WHERE u.username IN ('admin', 'admin.ops', 'admin.finance', 'owner', 'owner2', 'owner3')
GROUP BY u.id, u.username, u.email, u.account_type, u.status, o.id, o.full_name
ORDER BY u.account_type, u.username;

SELECT COUNT(*) AS invalid_cross_portal_roles
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
WHERE (u.account_type = 'ADMIN' AND r.code = 'OWNER')
   OR (u.account_type = 'OWNER' AND r.code = 'ADMIN');

SELECT COUNT(*) AS admin_linked_owner_profiles
FROM owners o
JOIN users u ON u.id = o.user_id
WHERE u.account_type = 'ADMIN';
