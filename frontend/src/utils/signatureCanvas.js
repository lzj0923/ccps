export function snapshotSignature(canvas) {
  if (!canvas || typeof canvas.toDataURL !== 'function') return '';
  const value = canvas.toDataURL('image/png');
  return typeof value === 'string' && value.startsWith('data:image/png;base64,') ? value : '';
}

export function appendSignaturePoint(path, point) {
  return `${path} L ${Math.round(point.x)} ${Math.round(point.y)}`;
}

export function startSignatureStroke(path, point) {
  const stroke = `M ${Math.round(point.x)} ${Math.round(point.y)}`;
  return path ? `${path} ${stroke}` : stroke;
}

export const signatureSurface = Object.freeze({ preserveAspectRatio: 'none' });

export function nextSignatureRenderKey(value) {
  return Number(value || 0) + 1;
}
