<template>
  <section class="space-manager">
    <header class="space-manager-head">
      <div><strong>出租空间设置</strong><small>整租使用“整套房产”；合租时为每个可独立出租的房间建立一个空间。</small></div>
      <div class="mode-switch"><button :class="{active:mode==='whole_unit'}" @click="changeMode('whole_unit')">整租</button><button :class="{active:mode==='shared'}" @click="changeMode('shared')">合租</button></div>
    </header>
    <p v-if="error" class="space-error">{{ error }}</p>
    <div v-if="loading" class="space-empty">正在读取出租空间…</div>
    <template v-else>
      <div v-if="mode==='shared'" class="space-summary">
        <div><span>合租房间</span><strong>{{ activeRooms.length }} 间</strong></div>
        <div><span>已出租</span><strong>{{ occupiedRooms.length }} 间</strong></div>
        <div><span>仍可出租</span><strong>{{ Math.max(activeRooms.length - occupiedRooms.length, 0) }} 间</strong></div>
        <div><span>出租进度</span><strong>{{ activeRooms.length ? Math.round(occupiedRooms.length / activeRooms.length * 100) : 0 }}%</strong></div>
      </div>
      <div class="space-grid">
        <article v-for="space in spaces" :key="space.id" class="space-card" :class="{disabled:space.status==='disabled'}">
          <div><span class="space-type">{{ space.spaceType === 'whole_unit' ? '整套' : '房间' }}</span><strong>{{ space.spaceName }}</strong><small>{{ space.spaceCode }} · {{ space.areaSqm ? `${space.areaSqm} m²` : '未填面积' }}</small></div>
          <div class="space-state"><b v-if="space.currentLeaseId">{{ space.tenantName || '租客' }}</b><span :class="space.currentLeaseId ? 'occupied' : 'available'">{{ space.currentLeaseId ? `租至 ${space.leaseEnd}` : space.status === 'active' ? '可出租' : '已停用' }}</span></div>
          <div v-if="space.spaceType === 'room'" class="space-actions"><button @click="edit(space)">编辑</button><button class="danger" :disabled="space.currentLeaseId" @click="disable(space)">停用</button></div>
        </article>
      </div>
      <form v-if="mode==='shared'" class="space-form" @submit.prevent="save">
        <h4>{{ editingId ? '编辑房间' : '增加合租房间' }}</h4>
        <label>空间编号<input v-model.trim="form.spaceCode" maxlength="40" placeholder="例如 ROOM-A" required></label>
        <label>显示名称<input v-model.trim="form.spaceName" maxlength="100" placeholder="例如 主卧 A" required></label>
        <label>可住人数<input v-model.number="form.capacity" type="number" min="1" required></label>
        <label>面积（m²）<input v-model.number="form.areaSqm" type="number" min="0" step="0.01"></label>
        <label>建议月租（RM）<input v-model.number="form.recommendedRent" type="number" min="0" step="0.01"></label>
        <div class="space-form-actions"><button v-if="editingId" type="button" @click="reset">取消</button><button class="primary" :disabled="saving">{{ saving ? '保存中…' : editingId ? '保存修改' : '增加房间' }}</button></div>
      </form>
      <div v-else class="space-tip">当前按整套出租。要分别出租房间，请先切换为“合租”，再建立房间。</div>
    </template>
  </section>
</template>

<script>
import { createAdminRentalSpace, disableAdminRentalSpace, fetchAdminRentalSpaces, updateAdminRentalMode, updateAdminRentalSpace } from '../services/propertyApi';
const emptyForm=()=>({spaceCode:'',spaceName:'',capacity:1,areaSqm:null,recommendedRent:null,status:'active'});
export default{
  name:'AdminRentalSpaceManager',props:{unitId:{type:[String,Number],required:true},initialMode:{type:String,default:'whole_unit'}},
  data(){return{spaces:[],mode:this.initialMode||'whole_unit',loading:false,saving:false,error:'',editingId:null,form:emptyForm()}},
  computed:{activeRooms(){return this.spaces.filter(x=>x.spaceType==='room'&&x.status==='active')},occupiedRooms(){return this.activeRooms.filter(x=>x.currentLeaseId)}},
  mounted(){this.load()},
  methods:{
    async load(){this.loading=true;this.error='';try{this.spaces=await fetchAdminRentalSpaces(this.unitId)||[];if(this.spaces.some(x=>x.spaceType==='room'&&x.status==='active'))this.mode='shared'}catch(e){this.error=e.message||'出租空间读取失败'}finally{this.loading=false}},
    async changeMode(mode){if(mode===this.mode)return;this.error='';try{await updateAdminRentalMode(this.unitId,mode);this.mode=mode;this.reset()}catch(e){this.error=e.message||'出租方式切换失败'}},
    edit(space){this.editingId=space.id;this.form={spaceCode:space.spaceCode,spaceName:space.spaceName,capacity:Number(space.capacity||1),areaSqm:space.areaSqm,recommendedRent:space.recommendedRent,status:space.status||'active'}},
    reset(){this.editingId=null;this.form=emptyForm()},
    async save(){this.saving=true;this.error='';try{const payload={...this.form,areaSqm:this.form.areaSqm||null,recommendedRent:this.form.recommendedRent||null};if(this.editingId)await updateAdminRentalSpace(this.unitId,this.editingId,payload);else await createAdminRentalSpace(this.unitId,payload);this.mode='shared';this.reset();await this.load()}catch(e){this.error=e.message||'出租空间保存失败'}finally{this.saving=false}},
    async disable(space){if(!confirm(`确定停用“${space.spaceName}”？历史租约会继续保留。`))return;this.error='';try{await disableAdminRentalSpace(this.unitId,space.id);await this.load()}catch(e){this.error=e.message||'出租空间停用失败'}}
  }
};
</script>

<style scoped>
.space-manager{display:grid;gap:18px}.space-manager-head{display:flex;justify-content:space-between;align-items:center;padding:18px;border:1px solid #d8e6ec;border-radius:12px;background:#fff}.space-manager-head div:first-child{display:grid;gap:5px}.space-manager-head small{color:#6d8193}.mode-switch{display:flex;padding:3px;border-radius:9px;background:#edf4f6}.mode-switch button{border:0;background:transparent;padding:9px 18px;border-radius:7px}.mode-switch button.active{background:#078f94;color:#fff}.space-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.space-summary>div{display:grid;gap:6px;padding:14px 16px;border:1px solid #d8e6ec;border-radius:10px;background:#fff}.space-summary span{color:#6f8494;font-size:12px}.space-summary strong{color:#0b526f;font-size:20px}.space-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:12px}.space-card{display:grid;grid-template-columns:1fr auto;gap:14px;padding:16px;border:1px solid #d7e5eb;border-radius:12px;background:#fff}.space-card>div:first-child,.space-state{display:grid;gap:5px}.space-type{width:max-content;padding:3px 8px;border-radius:999px;background:#e8f6f6;color:#067b80;font-size:12px}.space-card small{color:#7a8d9d}.space-state{text-align:right}.space-state span{font-size:12px}.space-state .available{color:#16855f}.space-state .occupied{color:#bd7c00}.space-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px}.space-actions button,.space-form-actions button{padding:7px 14px;border:1px solid #cbdbe3;border-radius:7px;background:#fff}.space-actions .danger{color:#c9423c;border-color:#efc4c1}.space-form{display:grid;grid-template-columns:repeat(5,minmax(130px,1fr));gap:12px;padding:18px;border:1px solid #cfe2e8;border-radius:12px;background:#f8fcfd}.space-form h4{grid-column:1/-1;margin:0}.space-form label{display:grid;gap:6px;color:#38546a;font-size:13px}.space-form input{padding:9px 10px;border:1px solid #c8d9e2;border-radius:7px}.space-form-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px}.space-form-actions .primary{background:#078f94;color:#fff;border-color:#078f94}.space-error,.space-tip,.space-empty{padding:13px;border-radius:9px;background:#fff5f3;color:#bf4239}.space-tip,.space-empty{background:#f1f7f8;color:#587082}.space-card.disabled{opacity:.6}@media(max-width:1000px){.space-form,.space-summary{grid-template-columns:repeat(2,1fr)}}
</style>
