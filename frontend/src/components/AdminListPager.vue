<template>
  <div v-if="total > 0" class="list-pager">
    <span>{{ $t('legacy.t_3b6ef811b85a') }} {{ total }} {{ $t('legacy.t_d0bb9b2b8ea7') }}</span>
    <div>
      <button type="button" :disabled="page <= 1" @click="$emit('update:page', page - 1)">&lt;</button>
      <button v-for="number in pages" :key="number" type="button" :class="{ active:number === page }" @click="$emit('update:page', number)">{{ number }}</button>
      <button type="button" :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">&gt;</button>
      <select :value="pageSize" @change="$emit('update:pageSize', Number($event.target.value))"><option :value="5">{{ $t('legacy.t_404e0d23200e') }}</option><option :value="10">{{ $t('legacy.t_fc6da0e815a1') }}</option><option :value="20">{{ $t('legacy.t_93d673672fa5') }}</option><option :value="50">{{ $t('legacy.t_529930b886d2') }}</option></select>
    </div>
  </div>
</template>
<script>
export default { name:'AdminListPager',props:{page:{type:Number,default:1},pageSize:{type:Number,default:10},total:{type:Number,default:0}},emits:['update:page','update:pageSize'],computed:{totalPages(){return Math.max(1,Math.ceil(this.total/this.pageSize))},pages(){const count=Math.min(5,this.totalPages),start=Math.max(1,Math.min(this.page-2,this.totalPages-count+1));return Array.from({length:count},(_,i)=>start+i)}}};
</script>
<style scoped>
.list-pager{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 4px;color:#607086;font-size:12px}.list-pager>div{display:flex;gap:6px}.list-pager button,.list-pager select{height:32px;min-width:32px;border:1px solid #d7e1ec;border-radius:7px;background:#fff;color:#12385f;padding:0 9px}.list-pager button.active{background:#08969a;border-color:#08969a;color:#fff}.list-pager button:disabled{opacity:.4}.list-pager select{min-width:88px}
</style>
