import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parse, compileTemplate } from '@vue/compiler-sfc';
import { pendingOwnerSignatures, unseenOwnerSignatures, ownerSignatureStatus } from '../src/utils/ownerSignatures.js';
import { ownerAppMessages } from '../src/i18n/ownerApp.js';
const now = new Date('2026-09-14T10:00:00').getTime();
const task = { id: 1, status: 'pending', canSign: true, expiresAt: '2026-09-15T10:00:00' };
test('only active pending tasks trigger reminders; closed, expired and invalid dates never do', () => {
  const tasks = [task, { ...task, id: 2, status: 'signed' }, { ...task, id: 3, canSign: false }, { ...task, id: 4, expiresAt: '2026-09-13' }, { ...task, id: 5, expiresAt: 'invalid' }];
  assert.deepEqual(pendingOwnerSignatures(tasks, now), [task]);
  assert.equal(ownerSignatureStatus(tasks[3], now), 'expired');
  assert.equal(ownerSignatureStatus(tasks[2], now), 'closed');
  assert.deepEqual(unseenOwnerSignatures(tasks, new Set(['1']), now), []);
  assert.deepEqual(unseenOwnerSignatures([...tasks, { ...task, id: 6 }], new Set(['1']), now).map(t => t.id), [6]);
});
test('all signing components compile and retain review, consent and close controls', () => {
  for (const name of ['owner-mobile/OwnerSignatureCenter', 'owner-mobile/OwnerSigningForm', 'AdminOwnerSignatureDispatch']) {
    const source=readFileSync(new URL(`../src/components/${name}.vue`,import.meta.url),'utf8');
    const { descriptor }=parse(source);
    assert.deepEqual(compileTemplate({source:descriptor.template.content,filename:name+'.vue',id:name}).errors,[]);
  }
  const source=readFileSync(new URL('../src/components/owner-mobile/OwnerSignatureCenter.vue',import.meta.url),'utf8');
  assert.match(source,/!ready \|\| !previewed/); assert.match(source,/document\.querySelector\('dialog\[open\]'\)/);
  assert.match(source,/clearInterval/); assert.match(source,/visibilitychange/); assert.match(source,/appStateChange/);
  const form=readFileSync(new URL('../src/components/owner-mobile/OwnerSigningForm.vue',import.meta.url),'utf8');
  assert.match(form,/this\.distance<20 \|\| !this\.consent/); assert.match(form,/if \(this\.busy\) return/);
});
test('signing text covers simplified Chinese, traditional Chinese and English', () => {
  const keys=Object.keys(ownerAppMessages['zh-CN'].signing);
  for(const locale of ['zh-CN','zh-TW','en']) assert.deepEqual(Object.keys(ownerAppMessages[locale].signing),keys);
});

function centerInstance(fetchTasks) {
  const source=readFileSync(new URL('../src/components/owner-mobile/OwnerSignatureCenter.vue',import.meta.url),'utf8');
  const script=parse(source).descriptor.script.content.replace(/^import .*;$/gm,'').replace('export default','return');
  const component=new Function('fetchOwnerSignatures','pendingOwnerSignatures','unseenOwnerSignatures','ownerSignatureStatus',
    'const PenLine={},ChevronRight={},X={},OwnerDocumentPreview={},OwnerSigningForm={},formatDateTime=()=>"";'+script)(fetchTasks,pendingOwnerSignatures,unseenOwnerSignatures,ownerSignatureStatus);
  const vm={...component.data(),$t:key=>key,blocked:false,$refs:{reminder:{showModal(){vm.shown++;},close(){vm.reminderOpen=false;}}},shown:0};
  for(const [name,method] of Object.entries(component.methods)) vm[name]=method.bind(vm);
  Object.defineProperty(vm,'pending',{get:()=>component.computed.pending.call(vm)});
  return vm;
}
test('polling shows each task once, respects other dialogs, and ignores unmounted responses', async () => {
  const previous=global.document; let otherDialog=false;
  global.document={hidden:false,querySelector:()=>otherDialog?{}:null};
  try {
    let tasks=[{...task,expiresAt:'2099-01-01T00:00:00'}];
    const vm=centerInstance(async()=>tasks);
    await vm.refresh(); assert.equal(vm.shown,1);
    vm.dismiss(); await vm.refresh(); assert.equal(vm.shown,1);
    tasks=[{...tasks[0],id:2}]; otherDialog=true; await vm.refresh(); assert.equal(vm.shown,1);
    otherDialog=false; await vm.refresh(); assert.equal(vm.shown,2);
    let finish; const late=centerInstance(()=>new Promise(resolve=>{finish=resolve;}));
    const request=late.refresh(); late.disposed=true; finish(tasks); await request;
    assert.equal(late.shown,0); assert.deepEqual(late.tasks,[]);
  } finally { global.document=previous; }
});
test('poll failure is recoverable and cannot announce a fake success', async () => {
  const previous=global.document;global.document={hidden:false,querySelector:()=>null};
  try {
    const vm=centerInstance(async()=>{throw new Error('offline');});
    await vm.refresh(); assert.equal(vm.error,'offline'); assert.equal(vm.shown,0); assert.equal(vm.success,false); assert.equal(vm.loading,false);
  } finally {global.document=previous;}
});
