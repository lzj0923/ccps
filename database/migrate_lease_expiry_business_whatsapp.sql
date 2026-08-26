USE ccps_property_management;

INSERT INTO notification_rules
  (code, name, event_type, days_before, channels, recipient_role, enabled, created_by)
VALUES
  ('LEASE_EXPIRY_BUSINESS_30D', '租约结束前一个月通知业务人员', 'lease_expiry', 30,
   JSON_ARRAY('whatsapp'), 'business', 1, NULL)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  event_type = 'lease_expiry',
  days_before = 30,
  channels = JSON_ARRAY('whatsapp'),
  recipient_role = 'business',
  enabled = 1;
