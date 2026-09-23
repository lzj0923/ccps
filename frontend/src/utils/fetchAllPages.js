/** Follow the server's actual page count, not the requested (possibly capped) size. */
export async function fetchAllPages(fetchPage, filters = {}) {
  const first = await fetchPage({ ...filters, page: 1, pageSize: 100 });
  const rows = [...(first.rows || [])];
  const pages = Number(first.page?.totalPages || 1);
  for (let page = 2; page <= pages; page++) {
    const result = await fetchPage({ ...filters, page, pageSize: 100 });
    rows.push(...(result.rows || []));
  }
  return rows;
}
