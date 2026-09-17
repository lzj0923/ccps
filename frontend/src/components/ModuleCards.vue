<template>
<section v-if="currentModule.cards?.length" class="owner-card-grid"><article v-for="card in currentModule.cards" :key="card.title" class="owner-data-card"><div class="owner-card-mark">{{ (card.label || card.title || "CC").slice(0, 2).toUpperCase() }}</div><div><span>{{ $lt(card.label) }}</span><h3>{{ $lt(card.title) }}</h3><p>{{ $lt(card.text) }}</p></div><b>{{ $lt(card.value) }}</b></article></section>
</template>

<script>
import pageBridge from "../pageBridge";
export default { mixins: [pageBridge] };
</script>
