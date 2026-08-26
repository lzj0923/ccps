import { parsePhoneNumberFromString } from 'libphonenumber-js/min';

export const PHONE_COUNTRIES = Object.freeze([
  { code: 'MY', dialCode: '+60', label: '马来西亚', example: '12-345 6789' },
  { code: 'CN', dialCode: '+86', label: '中国大陆', example: '138 0013 8000' },
  { code: 'SG', dialCode: '+65', label: '新加坡', example: '8123 4567' },
  { code: 'HK', dialCode: '+852', label: '中国香港', example: '9123 4567' },
  { code: 'TW', dialCode: '+886', label: '中国台湾', example: '912 345 678' },
  { code: 'ID', dialCode: '+62', label: '印度尼西亚', example: '812 3456 7890' },
  { code: 'TH', dialCode: '+66', label: '泰国', example: '81 234 5678' },
  { code: 'VN', dialCode: '+84', label: '越南', example: '912 345 678' },
  { code: 'PH', dialCode: '+63', label: '菲律宾', example: '917 123 4567' },
  { code: 'KH', dialCode: '+855', label: '柬埔寨', example: '12 345 678' },
  { code: 'MM', dialCode: '+95', label: '缅甸', example: '9 123 456789' },
  { code: 'IN', dialCode: '+91', label: '印度', example: '98765 43210' },
  { code: 'JP', dialCode: '+81', label: '日本', example: '90 1234 5678' },
  { code: 'KR', dialCode: '+82', label: '韩国', example: '10 1234 5678' },
  { code: 'AU', dialCode: '+61', label: '澳大利亚', example: '412 345 678' },
  { code: 'NZ', dialCode: '+64', label: '新西兰', example: '21 123 456' },
  { code: 'US', dialCode: '+1', label: '美国', example: '202 555 0123' },
  { code: 'CA', dialCode: '+1', label: '加拿大', example: '416 555 0123' },
  { code: 'GB', dialCode: '+44', label: '英国', example: '7911 123456' },
  { code: 'FR', dialCode: '+33', label: '法国', example: '6 12 34 56 78' },
  { code: 'DE', dialCode: '+49', label: '德国', example: '151 23456789' },
  { code: 'IT', dialCode: '+39', label: '意大利', example: '312 345 6789' },
  { code: 'ES', dialCode: '+34', label: '西班牙', example: '612 345 678' },
  { code: 'CH', dialCode: '+41', label: '瑞士', example: '78 123 45 67' },
  { code: 'AE', dialCode: '+971', label: '阿联酋', example: '50 123 4567' },
  { code: 'SA', dialCode: '+966', label: '沙特阿拉伯', example: '50 123 4567' },
  { code: 'QA', dialCode: '+974', label: '卡塔尔', example: '3312 3456' },
  { code: 'BR', dialCode: '+55', label: '巴西', example: '11 91234 5678' },
  { code: 'MX', dialCode: '+52', label: '墨西哥', example: '55 1234 5678' },
  { code: 'ZA', dialCode: '+27', label: '南非', example: '71 123 4567' }
]);

export const TENANT_PHONE_COUNTRIES = PHONE_COUNTRIES;

const SUPPORTED_COUNTRIES = new Set(PHONE_COUNTRIES.map((country) => country.code));

export function phoneCountry(code) {
  return PHONE_COUNTRIES.find((country) => country.code === code) || PHONE_COUNTRIES[0];
}

export function tenantPhoneCountry(code) {
  return phoneCountry(code);
}

export function validatePhone(value, countryCode = 'MY', required = false) {
  const raw = String(value || '').trim();
  const country = phoneCountry(countryCode);
  if (!raw) {
    return required
      ? { valid: false, reason: 'required', country: country.code, nationalNumber: '', e164: '' }
      : { valid: true, reason: '', country: country.code, nationalNumber: '', e164: '' };
  }

  const parsed = parsePhoneNumberFromString(raw, country.code);
  if (!parsed || !parsed.isValid()) {
    return { valid: false, reason: 'invalid', country: country.code, nationalNumber: raw, e164: '' };
  }
  if (raw.startsWith('+') && parsed.country !== country.code) {
    return { valid: false, reason: 'country_mismatch', country: country.code, nationalNumber: raw, e164: '' };
  }
  return {
    valid: true,
    reason: '',
    country: parsed.country || country.code,
    nationalNumber: parsed.nationalNumber,
    e164: parsed.number
  };
}

export const validateTenantPhone = validatePhone;

export function splitPhone(value, fallbackCountry = 'MY') {
  const raw = String(value || '').trim();
  const fallback = phoneCountry(fallbackCountry).code;
  if (!raw) return { country: fallback, nationalNumber: '' };

  let parsed = parsePhoneNumberFromString(raw, fallback);
  if ((!parsed || !parsed.isValid()) && /^\d{8,15}$/.test(raw)) {
    parsed = parsePhoneNumberFromString(`+${raw}`);
  }
  if (parsed?.isValid() && SUPPORTED_COUNTRIES.has(parsed.country)) {
    return { country: parsed.country, nationalNumber: parsed.nationalNumber };
  }
  return { country: fallback, nationalNumber: raw };
}

export const splitTenantPhone = splitPhone;
