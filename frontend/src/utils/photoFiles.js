const photoFileKey = file => [
  String(file?.name || ''),
  Number(file?.size || 0),
  Number(file?.lastModified || 0),
  String(file?.type || ''),
].join('::');

export function mergeUniquePhotoFiles(current = [], incoming = []) {
  const merged = [];
  const seen = new Set();
  for (const file of [...current, ...incoming]) {
    if (!file) continue;
    const key = photoFileKey(file);
    if (seen.has(key)) continue;
    seen.add(key);
    merged.push(file);
  }
  return merged;
}

export function removePhotoFile(files = [], target) {
  return files.filter(file => file !== target);
}
