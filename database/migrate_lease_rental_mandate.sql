-- Bind every newly created lease to the rental mandate that started its Rental Cycle.
-- Existing rows remain nullable for history compatibility and are not backfilled by guesswork.
ALTER TABLE leases
  ADD COLUMN rental_mandate_id BIGINT UNSIGNED NULL AFTER tenant_id,
  ADD KEY idx_leases_rental_mandate (rental_mandate_id, status),
  ADD CONSTRAINT fk_leases_rental_mandate FOREIGN KEY (rental_mandate_id) REFERENCES rental_mandates (id);
