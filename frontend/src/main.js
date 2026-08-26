import { createApp } from 'vue';
import App from './App.vue';
import { i18n } from './i18n';
import { setupNativeApp } from './nativeApp';
import '../styles.css';
import './admin-theme.css';
import '../tokens.css';
import './native-app.css';

setupNativeApp();
createApp(App).use(i18n).mount('#app');
