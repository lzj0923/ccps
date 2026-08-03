function normalizeCell(value) {
  return String(value ?? '')
    .replace(/[\r\n\u2028\u2029]+/g, ' ')
    .replace(/\t+/g, ' ');
}

function escapeCell(value) {
  return `"${normalizeCell(value).replace(/"/g, '""')}"`;
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
    wch: Math.min(60, Math.max(12, ...sourceRows.map((row) => row[column].length + 2))),
  }));
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, '匯出資料');
  XLSX.writeFile(workbook, filename.replace(/\.csv$/i, '.xlsx'));
}
import * as XLSX from 'xlsx';
