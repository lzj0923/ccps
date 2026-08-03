export const toNumber = value => Number(String(value).replace(/,/g, "")) || 0;

export const moneyText = value => toNumber(value).toLocaleString("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
});
