export function nativePreviewEnabled(development, search, storage) {
  if (!development) return false;
  const flag = new URLSearchParams(search).get('nativePreview');
  try {
    if (flag === '1' || flag === '0') storage.setItem('ccps-owner-native-preview', flag);
    return flag === '1' || (flag !== '0' && storage.getItem('ccps-owner-native-preview') === '1');
  } catch { return flag === '1'; }
}
