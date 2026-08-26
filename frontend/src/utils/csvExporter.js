function normalizeCell(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : '';
  if (typeof value === 'boolean') return value;
  return String(value ?? '')
    .replace(/[\r\n\u2028\u2029]+/g, ' ')
    .replace(/\t+/g, ' ');
}

function escapeCell(value) {
  return `"${String(normalizeCell(value)).replace(/"/g, '""')}"`;
}

function displayWidth(value) {
  return [...String(value ?? '')].reduce((width, character) =>
    width + (/[ᄀ-ᅟ⺀-꓏가-힣豈-﫿︐-﹯＀-￯]/u.test(character) ? 2 : 1), 0);
}

export function serializeCsv(headers, rows = []) {
  const columns = Array.isArray(headers) ? headers.length : 0;
  const allRows = [headers, ...rows].map((row) => {
    const source = Array.isArray(row) ? row : [];
    return Array.from({ length: columns }, (_, index) => escapeCell(source[index]));
  });

  return allRows.map((row) => row.join(',')).join('\r\n') + '\r\n';
}

export function downloadCsv(filename, headers, rows = []) {
  const sourceRows = [headers, ...rows].map((row) => {
    const source = Array.isArray(row) ? row : [];
    return Array.from({ length: headers.length }, (_, index) => normalizeCell(source[index]));
  });
  const worksheet = XLSX.utils.aoa_to_sheet(sourceRows);
  worksheet['!cols'] = headers.map((_, column) => ({
    wch: Math.min(60, Math.max(12, ...sourceRows.map((row) => displayWidth(row[column]) + 2))),
  }));
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, '匯出資料');
  XLSX.writeFile(workbook, filename.replace(/\.csv$/i, '.xlsx'));
}

export function downloadPaymentReport(filename, headers, rows = []) {
  const sourceRows = [headers, ...rows].map((row) => {
    const source = Array.isArray(row) ? row : [];
    return Array.from({ length: headers.length }, (_, index) => normalizeCell(source[index]));
  });
  const signatureStart = sourceRows.length + 3;
  const signatureRows = [
    [],
    [],
    ['签名栏位：'],
    [],
    ['____________________', '', '', '', '____________________'],
    ['名字', '', '', '', '名字'],
    ['行政部门', '', '', '', '行政主管'],
    [],
    [],
    ['____________________', '', '', '', '____________________'],
    ['名字', '', '', '', '名字'],
    ['财务部门', '', '', '', '财务主管'],
  ];
  const worksheet = XLSX.utils.aoa_to_sheet(sourceRows);
  XLSX.utils.sheet_add_aoa(worksheet, signatureRows, { origin: `C${sourceRows.length + 1}` });
  worksheet['!cols'] = [12, 14, 30, 18, 18, 14, 36, 12, 14, 18, 20, 20, 14, 20]
    .slice(0, headers.length)
    .map((wch) => ({ wch }));
  worksheet['!rows'] = [{ hpt: 30 }, ...rows.map(() => ({ hpt: 24 }))];
  worksheet['!freeze'] = { xSplit: 0, ySplit: 1 };
  worksheet['!autofilter'] = { ref: XLSX.utils.encode_range({ s: { r: 0, c: 0 }, e: { r: Math.max(0, rows.length), c: headers.length - 1 } }) };

  const reservedColumns = Math.min(9, headers.length);
  for (let column = 0; column < headers.length; column += 1) {
    const header = worksheet[XLSX.utils.encode_cell({ r: 0, c: column })];
    if (header) header.s = { font: { bold: true }, alignment: { horizontal: 'center', vertical: 'center', wrapText: true }, fill: { fgColor: { rgb: column < reservedColumns ? 'FFF200' : '4FB3C8' } } };
    for (let row = 1; row <= rows.length; row += 1) {
      const cell = worksheet[XLSX.utils.encode_cell({ r: row, c: column })];
      if (!cell) continue;
      cell.s = { alignment: { vertical: 'top', wrapText: true }, fill: { fgColor: { rgb: column < reservedColumns ? 'FFF86B' : '77C6D6' } } };
      if (column === 7 && typeof cell.v === 'number') cell.z = '#,##0.00';
    }
  }
  const signatureLabel = worksheet[`C${signatureStart}`];
  if (signatureLabel) signatureLabel.s = { font: { bold: true }, alignment: { horizontal: 'left' } };

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, '付款报告');
  XLSX.writeFile(workbook, filename.replace(/\.csv$/i, '.xlsx'), { cellStyles: true });
}
import * as XLSX from 'xlsx';
