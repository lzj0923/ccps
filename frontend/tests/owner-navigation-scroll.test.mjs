import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const read = file => readFileSync(new URL(file, import.meta.url), 'utf8');
const source = read('../src/components/owner-mobile/OwnerMobileApp.vue');
const watcherSource = source.slice(source.indexOf('  watch: ') + 9, source.indexOf('\n  mounted()')).trim().replace(/,$/, '');
const actions = read('../src/composables/dashboardActions.js');
const selectSource = actions.slice(actions.indexOf('    selectModule(id)'), actions.indexOf('    initials(name)'));

function scenario() {
  let top = 720;
  const scrollCalls = [];
  const pending = [];
  const fakeWindow = { scrollTo(options) { top = options.top; scrollCalls.push(options); } };
  const watchers = new Function('window', `return (${watcherSource})`)(fakeWindow);
  const select = new Function('navigate', 'resolveRoute', 'routeForModule', `return ({${selectSource}}).selectModule`)(() => {}, () => ({mode:'owner'}), id => '/owner/' + id);
  const vm = { detailRequest:0, detailView:'', selectedProperty:null, routeLegacyModule(){}, loadNotifications(){}, $nextTick(callback){ pending.push(callback); } };
  let currentId = 'myProperties';
  Object.defineProperty(vm, 'currentId', { get: () => currentId, set(value) { const previous = currentId; currentId = value; watchers.currentId.handler.call(vm, value, previous); } });
  return { vm, select, scrollCalls, get top(){ return top; }, scrollDown(){top=720;}, async render(){ for(const callback of pending.splice(0)) await callback(); } };
}

test('首页下滑后切换所有底部导航，目标页面渲染后回到顶部', async () => {
  const run = scenario();
  for (const module of ['ownerProjects','ownerRentalHub','ownerMore','ownerNotice','myProperties']) {
    run.scrollDown();
    run.select.call(run.vm, module);
    assert.equal(run.top, 720, '不能在旧页面渲染期间重置滚动');
    await run.render();
    assert.equal(run.top, 0, `${module} 不应继承上一页720px的滚动位置`);
  }
  assert.equal(run.scrollCalls.length, 5);
  assert.ok(run.scrollCalls.every(call => call.behavior === 'instant'));
});

test('快捷入口或路由直接改变模块也会回顶，而普通数据刷新不滚动', async () => {
  const run = scenario();
  run.vm.currentId = 'ownerRentalHub';
  await run.render();
  assert.equal(run.top, 0);
  run.scrollDown();
  run.vm.notifications = [];
  await run.render();
  assert.equal(run.top, 720);
});

test('快速连续切页只执行最终目标页面的回顶', async () => {
  const run = scenario();
  run.select.call(run.vm, 'ownerProjects');
  run.select.call(run.vm, 'ownerRentalHub');
  run.select.call(run.vm, 'ownerMore');
  await run.render();
  assert.equal(run.vm.currentId, 'ownerMore');
  assert.equal(run.top, 0);
  assert.equal(run.scrollCalls.length, 1);
});
