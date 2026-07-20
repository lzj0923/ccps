<template><div class="toast" :class="{ show: toastVisible }">{{ toastText }}</div></template>

<script>
import pageBridge from '../pageBridge';
export default { mixins: [pageBridge] };
</script>
