import { Capacitor, SystemBars, SystemBarsStyle } from '@capacitor/core';

import { nativePreviewEnabled } from './utils/nativePreview';
const isNativePreview = nativePreviewEnabled(import.meta.env.DEV, window.location.search, window.sessionStorage);
export const isNativeApp = Capacitor.isNativePlatform() || isNativePreview;

export async function setupNativeApp() {
  if (!isNativeApp) return;

  await import('./native-app.css');

  const platform = isNativePreview ? 'web' : Capacitor.getPlatform();
  document.documentElement.classList.add('capacitor-native', `capacitor-${platform}`);
  window.__CCPS_NATIVE_APP__ = true;

  if (window.location.pathname.startsWith('/admin') || window.location.pathname === '/login') {
    window.history.replaceState({}, '', '/owner');
  }

  const { App } = isNativePreview ? { App: null } : await import('@capacitor/app');

  try {
    if (!isNativePreview) {
      await SystemBars.setStyle({ style: SystemBarsStyle.Light });
      await SystemBars.show();
    }
  } catch {
    // 部分模拟器不支持状态栏设置，不影响业务页面启动。
  }

  if (platform === 'android' && App) {
    await App.addListener('backButton', async () => {
      const path = window.location.pathname.replace(/\/$/, '') || '/';
      const isOwnerRoot = path === '/' || path === '/owner' || path === '/owner/login';
      if (!isOwnerRoot && window.history.length > 1) {
        window.history.back();
        return;
      }
      await App.minimizeApp();
    });
  }
}
