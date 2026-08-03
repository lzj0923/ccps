<template>
  <label class="language-switcher" :title="$t('language.label')">
    <span>{{ $t('legacy.t_1d56b27a5e87') }}</span>
    <select v-model="locale" :aria-label="$t('language.label')">
      <option v-for="option in locales" :key="option.code" :value="option.code">{{ option.label }}</option>
    </select>
  </label>
</template>

<script>
import { i18n, setLocale, supportedLocales } from '../i18n';

export default {
  computed: {
    locales() { return supportedLocales; },
    locale: { get() { return i18n.global.locale.value; }, set(value) { setLocale(value); } }
  }
};
</script>

<style scoped>
.language-switcher{display:inline-flex;align-items:center;gap:5px;height:38px;box-sizing:border-box;border:1px solid #d7e2ec;border-radius:7px;background:#fff;padding:0 8px;color:#30506a;font-size:12px;font-weight:700}.language-switcher>span{display:grid;place-items:center;width:18px;height:18px;border-radius:50%;background:#eef5f8;color:#0a5969;font-size:11px}.language-switcher select{min-width:78px;border:0;outline:0;background:transparent;color:inherit;font:inherit;cursor:pointer}
</style>
