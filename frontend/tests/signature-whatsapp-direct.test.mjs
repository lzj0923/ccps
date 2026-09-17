import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {parse,compileTemplate} from '@vue/compiler-sfc';
import {parse as babelParse} from '@babel/parser';
import {validatePhone} from '../src/utils/tenantPhone.js';
const source=readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue',import.meta.url),'utf8');
const {descriptor}=parse(source);
const ast=babelParse(descriptor.script.content,{sourceType:'module'});
const component=ast.program.body.find(n=>n.type==='ExportDefaultDeclaration').declaration;
const methods=component.properties.find(n=>n.key.name==='methods');
const method=methods.value.properties.find(n=>n.key.name==='sendSigningWhatsApp');
const body=descriptor.script.content.slice(method.start,method.end);
function fixture(state={status:'ready',available:true,phone:'+60123456789'},failure=false){
 const sent=[]; const get=async()=>state;
 const post=async(...args)=>{sent.push(args);if(failure)throw Error('timeout');return {status:'sent'};};
 const fn=new Function('fetchAdminSignatureWhatsApp','sendAdminSignatureWhatsApp','validatePhone',`return ({${body}}).sendSigningWhatsApp`)(get,post,validatePhone);
 const page={signingWhatsAppBusyId:null,signingDeliveryError:'',$t:k=>k};
 return {page,sent,send:link=>fn.call(page,link)};
}
const link=()=>({requestId:5,signingUrl:'https://sign.example/sign/token',signerName:'Alice'});
test('direct sends mapped signer once, no manual wa.me',async()=>{const f=fixture(),l=link();await f.send(l);await f.send(l);assert.equal(f.sent.length,1);assert.equal(f.sent[0][2],'+60123456789');assert.equal(l.whatsappStatus,'sent');assert.doesNotMatch(source,/whatsAppSigningUrl/);});
test('missing number requires explicit international phone',async()=>{const f=fixture({status:'ready',available:true,phone:''}),l=link();await f.send(l);assert.equal(f.sent.length,0);assert.equal(l.whatsappNeedsPhone,true);l.deliveryPhone='+8618981712596';await f.send(l);assert.equal(f.sent.length,1);});
test('already sent, unavailable and busy do not send',async()=>{for(const state of [{status:'sent',available:true},{status:'ready',available:false}]){const f=fixture(state);await f.send(link());assert.equal(f.sent.length,0);}const f=fixture();f.page.signingWhatsAppBusyId=99;await f.send(link());assert.equal(f.sent.length,0);});
test('unknown result blocks repeated clicks',async()=>{const f=fixture(undefined,true),l=link();await f.send(l);await f.send(l);assert.equal(f.sent.length,1);assert.equal(l.whatsappStatus,'unknown');assert.equal(f.page.signingWhatsAppBusyId,null);});
test('known local phone is retained and needs country only',async()=>{const f=fixture({status:'ready',available:true,phone:'18981712596'}),l=link();await f.send(l);assert.equal(l.deliveryPhone,'18981712596');assert.equal(f.sent.length,0);l.deliveryCountry='CN';await f.send(l);assert.equal(f.sent[0][2],'+8618981712596');});
test('Vue template compiles with accessible busy/result and labelled phone',()=>{const compiled=compileTemplate({source:descriptor.template.content,filename:'AdminRentalSigningWorkspace.vue',id:'signature-test'});assert.deepEqual(compiled.errors,[]);assert.match(source,/:aria-busy="signingWhatsAppBusyId/);assert.match(source,/whatsappRecipientPhone/);assert.match(source,/role="status"/);});
