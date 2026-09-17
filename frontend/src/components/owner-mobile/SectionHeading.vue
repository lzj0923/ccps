<template><section class="owner-section-heading"><div><span>{{ eyebrow }}</span><h2>{{ title }}</h2></div><small v-if="count">{{ count }}</small></section></template>
<script>export default { props: { eyebrow: { type: String, default: '' }, title: { type: String, required: true }, count: { type: String, default: '' } } };</script>
