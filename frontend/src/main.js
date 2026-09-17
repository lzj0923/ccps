import { createApp } from 'vue';
import App from './App.vue';
import { i18n, localizeLegacyTree, regionDisplayName, translateLegacyTemplate, translateLegacyText } from './i18n';
import { isNativeApp, setupNativeApp } from './nativeApp';
import '../styles.css';
import '../tokens.css';

async function bootstrap() {
  await setupNativeApp();
  if (!isNativeApp) await import('./admin-theme.css');
  const app = createApp(App);
  app.config.globalProperties.$lt = translateLegacyText;
  app.config.globalProperties.$ltf = translateLegacyTemplate;
  app.config.globalProperties.$localizeTree = localizeLegacyTree;
  app.config.globalProperties.$regionName = regionDisplayName;
  app.use(i18n).mount('#app');
}

bootstrap();
