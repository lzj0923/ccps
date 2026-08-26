import { Capacitor, SystemBars, SystemBarsStyle } from '@capacitor/core';

export const isNativeApp = Capacitor.isNativePlatform();

export async function setupNativeApp() {
  if (!isNativeApp) return;

  const platform = Capacitor.getPlatform();
  document.documentElement.classList.add('capacitor-native', `capacitor-${platform}`);
  window.__CCPS_NATIVE_APP__ = true;

  if (window.location.pathname.startsWith('/admin') || window.location.pathname === '/login') {
    window.history.replaceState({}, '', '/owner');
  }

  const { App } = await import('@capacitor/app');

  try {
    await SystemBars.setStyle({ style: SystemBarsStyle.Light });
    await SystemBars.show();
  } catch {
    // 部分模拟器不支持状态栏设置，不影响业务页面启动。
  }

  if (platform === 'android') {
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
