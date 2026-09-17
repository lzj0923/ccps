// Native HTTP may not honor AbortController. Bound session restoration without
// depending on transport cancellation; a late response cannot change the result.
export async function restoreOwnerSession(fetchSession, timeoutMs = 6000) {
  let timer;
  try {
    return await Promise.race([
      fetchSession('owner'),
      new Promise(resolve => { timer = setTimeout(() => resolve(null), timeoutMs); })
    ]);
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}
