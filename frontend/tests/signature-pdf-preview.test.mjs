import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {parse,compileTemplate} from '@vue/compiler-sfc';
import {parse as babelParse} from '@babel/parser';
const source=readFileSync(new URL('../src/components/SignaturePdfPreview.vue',import.meta.url),'utf8');
const {descriptor}=parse(source);
const node=babelParse(descriptor.script.content,{sourceType:'module'}).program.body.find(n=>n.type==='ExportDefaultDeclaration').declaration;
function fixture(getDocument){
 const options=new Function('markRaw','getDocument','return '+descriptor.script.content.slice(node.start,node.end))(x=>x,getDocument);
 const page={...options.data(),src:'/api/public/signatures/test/document#page=2',initialPage:2,$nextTick:async()=>{},$refs:{canvas:{},scroll:{scrollTop:0}}};
 for(const [name,fn]of Object.entries(options.methods))page[name]=fn.bind(page);
 return {page,options};
}
test('signing page no longer depends on native PDF iframe',()=>{
 const signing=readFileSync(new URL('../src/pages/SignaturePage.vue',import.meta.url),'utf8');
 assert.doesNotMatch(signing,/<iframe/);assert.match(signing,/<SignaturePdfPreview[^>]*:src="previewDocumentUrl"[^>]*:initial-page="previewPage"/);
 assert.deepEqual(compileTemplate({source:descriptor.template.content,filename:'SignaturePdfPreview.vue',id:'test'}).errors,[]);
});
test('document load renders target page locally and removes URL anchor',async()=>{
 let url;const {page}=fixture(opts=>{url=opts.url;return {promise:Promise.resolve({numPages:3}),destroy:async()=>{}}});
 let rendered=0;page.renderPage=async()=>{rendered++;page.loading=false;};await page.loadDocument();
 assert.equal(page.pageCount,3);assert.equal(page.pageNumber,2);assert.equal(rendered,1);assert.equal(url,'/api/public/signatures/test/document');
});
test('failed PDF produces recoverable error rather than blank loading forever',async()=>{
 let fails=true;const {page}=fixture(()=>({promise:fails?Promise.reject(Error('503')):Promise.resolve({numPages:1}),destroy:async()=>{}}));
 page.renderPage=async()=>{page.loading=false;};await page.loadDocument();assert.equal(page.error,true);assert.equal(page.loading,false);
 fails=false;await page.loadDocument();assert.equal(page.error,false);assert.equal(page.pageCount,1);
});
test('replaced PDF ignores stale async load and unmount invalidates work',async()=>{
 let release,call=0;const {page,options}=fixture(()=>({promise:++call===1?new Promise(r=>release=r):Promise.resolve({numPages:2}),destroy:async()=>{}}));
 page.renderPage=async()=>{};const old=page.loadDocument();await page.loadDocument();release({numPages:99});await old;assert.equal(page.pageCount,2);
 const before=page.revision;options.beforeUnmount.call(page);assert.equal(page.revision,before+1);
});
