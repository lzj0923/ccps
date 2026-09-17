export const cashflowCategories = ['rent', 'deposit', 'management_fee', 'management', 'tax', 'maintenance', 'refund', 'reserve', 'income', 'expense'];

export function hasOwnerPaymentRecords(property) {
  return Number(property?.totalInstallmentCount) > 0 && Number(property?.purchasePrice) > 0;
}
export function hasCompleteOwnerPaymentRecords(properties) {
  return properties.length > 0 && properties.every(hasOwnerPaymentRecords);
}

export function ownerNoticeTabs(categories, total) {
  return [{ key: 'all', label: '全部', count: total }, ...categories.filter(item => item.key !== 'all')];
}

export function cashflowCategory(value, direction) {
  const key = String(value || '').toLowerCase();
  return ({ service_fee: 'management_fee', agency_fee: 'management_fee', management_fee: 'management_fee',
    management: 'management', property_fee: 'management', vat: 'tax', sst: 'tax', tax: 'tax',
    repair: 'maintenance', maintenance: 'maintenance', owner_remittance: 'refund', refund: 'refund',
    deposit: 'deposit', tenant_deposit: 'deposit', security_deposit: 'deposit',
    rent: 'rent', reserve: 'reserve', income: 'income', expense: 'expense' })[key] || (direction === 'income' ? 'income' : 'expense');
}

export function filterCashflows(records, { year, month = '', direction = 'all', category = 'all', search = '' }) {
  const query = search.trim().toLocaleLowerCase();
  return records.filter(item => {
    const date = String(item.occurredOn || '');
    return date.startsWith(`${year}-`) && (!month || Number(date.slice(5, 7)) === Number(month))
      && (direction === 'all' || item.direction === direction)
      && (category === 'all' || cashflowCategory(item.category, item.direction) === category)
      && (!query || String(item.description || '').toLocaleLowerCase().includes(query));
  });
}

export function summarizeCashflows(records) {
  const income = records.filter(item => item.direction === 'income').reduce((sum, item) => sum + Number(item.amount || 0), 0);
  const expense = records.filter(item => item.direction === 'expense').reduce((sum, item) => sum + Number(item.amount || 0), 0);
  return { income, expense, net: income - expense };
}

export function groupCashflowMonths(records, year) {
  return Array.from({ length: 12 }, (_, index) => {
    const month = index + 1;
    const items = filterCashflows(records, { year, month });
    const categories = cashflowCategories.map(category => {
      const subset = items.filter(item => cashflowCategory(item.category, item.direction) === category);
      return { category, ...summarizeCashflows(subset), count: subset.length };
    }).filter(item => item.count);
    return { month, key: `${year}-${String(month).padStart(2, '0')}`, records: items,
      ...summarizeCashflows(items), categories };
  });
}

export function photoStageGroup(value) {
  const stage = String(value || '').toUpperCase();
  if (['BEFORE', 'BEFORE_RENT', 'PRE_RENT', 'MOVE_IN', 'BEFORE_RENTAL'].includes(stage)) return 'before';
  if (['AFTER', 'MOVE_OUT', 'AFTER_RENT', 'AFTER_RENTAL'].includes(stage)) return 'after';
  return 'current';
}

export function filterPropertyPhotos(photos, leaseId = '', stage = 'all') {
  return photos.filter(photo => (!leaseId || String(photo.leaseId) === String(leaseId))
    && (stage === 'all' || photoStageGroup(photo.rentalStage) === stage));
}

export function searchOwnerProperties(properties, search = '', status = 'all') {
  const query = search.trim().toLocaleLowerCase();
  return properties.filter(item => (status === 'all' || item.assetStage === status)
    && (!query || [item.projectName, item.unitNo, item.city, item.tenantName].some(value => String(value || '').toLocaleLowerCase().includes(query))));
}
