ALTER TABLE leases
  ADD COLUMN rent_calculation_method VARCHAR(30) NOT NULL DEFAULT 'daily_prorated'
    COMMENT '按當月實際承租天數折算' AFTER payment_day;
