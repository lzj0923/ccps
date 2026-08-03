export const rentalLeaseTermError = ({
  startDate,
  endDate,
  mandateStartDate,
  mandateEndDate,
} = {}) => {
  if (!startDate || !endDate) return 'lease_term_required';
  if (endDate < startDate) return 'lease_end_before_start';
  if (mandateStartDate && startDate < mandateStartDate) return 'lease_start_before_mandate';
  if (mandateEndDate && endDate > mandateEndDate) return 'lease_end_after_mandate';
  return '';
};
