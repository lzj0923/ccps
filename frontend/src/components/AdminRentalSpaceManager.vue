<template>
  <section class="space-manager">
    <header class="space-manager-head">
      <div><strong>{{ $t('legacy.t_127935ea12ee') }}</strong><small>{{ $t('legacy.t_0871ca7c388d') }}</small></div>
      <div class="mode-switch"><button :class="{active:mode==='whole_unit'}" @click="changeMode('whole_unit')">{{ $t('legacy.t_075c9794aabf') }}</button><button :class="{active:mode==='shared'}" @click="changeMode('shared')">{{ $t('legacy.t_99add30d1525') }}</button></div>
    </header>
    <div v-if="error" class="space-error">
      <span>{{ $lt(error) }}</span>
      <button v-if="modeChangeBlocked && activeWholeLease" type="button" @click="openWholeLease(activeWholeLease)">{{ $t('propertyDetail.manageWholeLease') }}</button>
    </div>
    <div v-if="loading" class="space-empty">{{ $t('legacy.t_e709d15a7073') }}</div>
    <template v-else>
      <div v-if="mode==='shared'" class="space-summary">
        <div><span>{{ $t('legacy.t_0d973006434d') }}</span><strong>{{ activeRooms.length }} {{ $t('legacy.t_01b221aaefee') }}</strong></div>
        <div><span>{{ $t('legacy.t_a8f1f363be92') }}</span><strong>{{ occupiedRooms.length }} {{ $t('legacy.t_01b221aaefee') }}</strong></div>
        <div><span>{{ $t('legacy.t_902214209ee5') }}</span><strong>{{ Math.max(activeRooms.length - occupiedRooms.length, 0) }} {{ $t('legacy.t_01b221aaefee') }}</strong></div>
        <div><span>{{ $t('legacy.t_d0c77244a015') }}</span><strong>{{ activeRooms.length ? Math.round(occupiedRooms.length / activeRooms.length * 100) : Number(false) }}%</strong></div>
      </div>
      <div class="space-grid">
        <article v-for="space in spaces" :key="space.id" class="space-card" :class="{disabled:space.status==='disabled'}">
          <div><span class="space-type">{{ space.spaceType === 'whole_unit' ? $t('legacy.t_0a5380076895') : $t('legacy.t_dab2ced9277b') }}</span><strong>{{ space.spaceName }}</strong><small>{{ space.spaceCode }} · {{ space.areaSqm ? `${space.areaSqm} m²` : $t('legacy.t_3492081b4eeb') }}</small></div>
<div class="space-state"><b v-if="space.currentLeaseId">{{ space.tenantName || $t('legacy.t_42c950a2ce7c') }}</b><span :class="space.currentLeaseId ? 'occupied' : 'available'">{{ space.currentLeaseId ? $t('ui.leaseUntil', { date: space.leaseEnd }) : space.status === 'active' ? $t('legacy.t_0bc70dfa0ef8') : $t('legacy.t_6c7dcbb73a59') }}</span></div>
          <div v-if="space.spaceType === 'whole_unit' && space.currentLeaseId" class="space-actions"><button type="button" @click="openWholeLease(space)">{{ $t('propertyDetail.manageWholeLease') }}</button></div>
          <div v-else-if="space.spaceType === 'room'" class="space-actions"><button @click="edit(space)">{{ $t('legacy.t_a7f814c0a40d') }}</button><button class="danger" :disabled="space.currentLeaseId" @click="disable(space)">{{ $t('legacy.t_d989e55188c9') }}</button></div>
        </article>
      </div>
      <form v-if="mode==='shared'" class="space-form" @submit.prevent="save">
        <h4>{{ editingId ? $t('legacy.t_0ea784155a1a') : $t('legacy.t_f7fe6eaa15f5') }}</h4>
        <label>{{ $t('legacy.t_d202b26a1fe8') }}<input v-model.trim="form.spaceCode" maxlength="40" :placeholder="$t('legacy.t_e507c98fb185')" required></label>
        <label>{{ $t('legacy.t_75ae6a8a7dd1') }}<input v-model.trim="form.spaceName" maxlength="100" :placeholder="$t('legacy.t_8accc18b5b94')" required></label>
        <label>{{ $t('legacy.t_c5bd0af409f5') }}<input v-model.number="form.capacity" type="number" min="1" required></label>
        <label>{{ $t('legacy.t_a57270fd4c4f') }}<input v-model.number="form.areaSqm" type="number" min="0" step="0.01"></label>
        <label>{{ $t('legacy.t_578050516df2') }}<input v-model.number="form.recommendedRent" type="number" min="0" step="0.01"></label>
        <div class="space-form-actions"><button v-if="editingId" type="button" @click="reset">{{ $t('legacy.t_4d0b4688c787') }}</button><button class="primary" :disabled="saving">{{ saving ? $t('legacy.t_6644f06197a4') : editingId ? $t('legacy.t_60b4ae9082a3') : $t('legacy.t_fdb013246032') }}</button></div>
      </form>
      <div v-else class="space-tip">{{ $t('legacy.t_3454f69d2dee') }}</div>
    </template>
  </section>
</template>

<script>
import { createAdminRentalSpace, disableAdminRentalSpace, fetchAdminRentalSpaces, updateAdminRentalMode, updateAdminRentalSpace } from '../services/propertyApi';
const emptyForm=()=>({spaceCode:'',spaceName:'',capacity:1,areaSqm:null,recommendedRent:null,status:'active'});
export default{
  name:'AdminRentalSpaceManager',props:{unitId:{type:[String,Number],required:true},initialMode:{type:String,default:'whole_unit'}},
  emits:['edit-lease'],
  data(){return{spaces:[],mode:this.initialMode||'whole_unit',loading:false,saving:false,error:'',modeChangeBlocked:false,editingId:null,form:emptyForm()}},
  computed:{activeRooms(){return this.spaces.filter(x=>x.spaceType==='room'&&x.status==='active')},occupiedRooms(){return this.activeRooms.filter(x=>x.currentLeaseId)},activeWholeLease(){return this.spaces.find(x=>x.spaceType==='whole_unit'&&x.currentLeaseId)||null}},
  mounted(){this.load()},
  methods:{
    async load(){this.loading=true;this.error='';this.modeChangeBlocked=false;try{this.spaces=await fetchAdminRentalSpaces(this.unitId)||[];if(this.spaces.some(x=>x.spaceType==='room'&&x.status==='active'))this.mode='shared'}catch(e){this.error=e.message||'出租空间读取失败'}finally{this.loading=false}},
    async changeMode(mode){if(mode===this.mode)return;this.error='';this.modeChangeBlocked=false;try{await updateAdminRentalMode(this.unitId,mode);this.mode=mode;this.reset()}catch(e){this.error=e.message||'出租方式切换失败';this.modeChangeBlocked=mode==='shared'&&e.status===409}},
    openWholeLease(space){if(space?.currentLeaseId)this.$emit('edit-lease',space.currentLeaseId)},
    edit(space){this.editingId=space.id;this.form={spaceCode:space.spaceCode,spaceName:space.spaceName,capacity:Number(space.capacity||1),areaSqm:space.areaSqm,recommendedRent:space.recommendedRent,status:space.status||'active'}},
    reset(){this.editingId=null;this.form=emptyForm()},
    async save(){this.saving=true;this.error='';try{const payload={...this.form,areaSqm:this.form.areaSqm||null,recommendedRent:this.form.recommendedRent||null};if(this.editingId)await updateAdminRentalSpace(this.unitId,this.editingId,payload);else await createAdminRentalSpace(this.unitId,payload);this.mode='shared';this.reset();await this.load()}catch(e){this.error=e.message||'出租空间保存失败'}finally{this.saving=false}},
    async disable(space){if(!confirm(this.$ltf`确定停用“${space.spaceName}”？历史租约会继续保留。`))return;this.error='';try{await disableAdminRentalSpace(this.unitId,space.id);await this.load()}catch(e){this.error=e.message||'出租空间停用失败'}}
  }
};
</script>

<style scoped>
.space-manager{display:grid;gap:18px}.space-manager-head{display:flex;justify-content:space-between;align-items:center;padding:18px;border:1px solid #d8e6ec;border-radius:12px;background:#fff}.space-manager-head div:first-child{display:grid;gap:5px}.space-manager-head small{color:#6d8193}.mode-switch{display:flex;padding:3px;border-radius:9px;background:#edf4f6}.mode-switch button{border:0;background:transparent;padding:9px 18px;border-radius:7px}.mode-switch button.active{background:#078f94;color:#fff}.space-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.space-summary>div{display:grid;gap:6px;padding:14px 16px;border:1px solid #d8e6ec;border-radius:10px;background:#fff}.space-summary span{color:#6f8494;font-size:12px}.space-summary strong{color:#0b526f;font-size:20px}.space-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:12px}.space-card{display:grid;grid-template-columns:1fr auto;gap:14px;padding:16px;border:1px solid #d7e5eb;border-radius:12px;background:#fff}.space-card>div:first-child,.space-state{display:grid;gap:5px}.space-type{width:max-content;padding:3px 8px;border-radius:999px;background:#e8f6f6;color:#067b80;font-size:12px}.space-card small{color:#7a8d9d}.space-state{text-align:right}.space-state span{font-size:12px}.space-state .available{color:#16855f}.space-state .occupied{color:#bd7c00}.space-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px}.space-actions button,.space-form-actions button{padding:7px 14px;border:1px solid #cbdbe3;border-radius:7px;background:#fff}.space-actions .danger{color:#c9423c;border-color:#efc4c1}.space-form{display:grid;grid-template-columns:repeat(5,minmax(130px,1fr));gap:12px;padding:18px;border:1px solid #cfe2e8;border-radius:12px;background:#f8fcfd}.space-form h4{grid-column:1/-1;margin:0}.space-form label{display:grid;gap:6px;color:#38546a;font-size:13px}.space-form input{padding:9px 10px;border:1px solid #c8d9e2;border-radius:7px}.space-form-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px}.space-form-actions .primary{background:#078f94;color:#fff;border-color:#078f94}.space-error,.space-tip,.space-empty{padding:13px;border-radius:9px;background:#fff5f3;color:#bf4239}.space-error{display:flex;align-items:center;justify-content:space-between;gap:12px}.space-error button{flex:0 0 auto;border:1px solid #e6aaa5;border-radius:7px;background:#fff;padding:7px 12px;color:#a82e28;font-weight:700}.space-tip,.space-empty{background:#f1f7f8;color:#587082}.space-card.disabled{opacity:.6}@media(max-width:1000px){.space-form,.space-summary{grid-template-columns:repeat(2,1fr)}}
</style>
