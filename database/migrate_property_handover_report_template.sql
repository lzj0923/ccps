USE ccps_property_management;

ALTER TABLE property_handover_reports
  ADD COLUMN content_json MEDIUMTEXT NULL AFTER remarks;
