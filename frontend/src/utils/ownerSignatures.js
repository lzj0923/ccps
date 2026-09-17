export function pendingOwnerSignatures(tasks, now = Date.now()) {
  return (Array.isArray(tasks) ? tasks : []).filter(task => task.canSign && task.status === 'pending'
    && Number.isFinite(new Date(task.expiresAt).getTime()) && new Date(task.expiresAt).getTime() > now);
}
export function unseenOwnerSignatures(tasks, seen, now = Date.now()) {
  return pendingOwnerSignatures(tasks, now).filter(task => !seen.has(String(task.id)));
}
export function ownerSignatureStatus(task, now = Date.now()) {
  if (task.status === 'pending' && new Date(task.expiresAt).getTime() <= now) return 'expired';
  if (task.status === 'pending' && !task.canSign) return 'closed';
  return ['pending', 'signed', 'expired'].includes(task.status) ? task.status : 'closed';
}
