<template>
  <section class="content-grid">
    <div class="panel table-panel">
      <div class="panel-head">
        <div><h2>{{ $lt(currentModule.table) }}</h2><span>{{ $t('ui.records', { count: filteredRows.length }) }}</span></div>
        <div class="view-tabs">
          <button type="button" :class="{ active: viewMode === 'list' }" :title="$t('ui.listView')" :aria-pressed="viewMode === 'list'" @click="viewMode = 'list'"><List :size="14" :stroke-width="1.9" /><span>{{ $t('ui.listView') }}</span></button>
          <button type="button" :class="{ active: viewMode === 'kanban' }" :title="$t('ui.boardView')" :aria-pressed="viewMode === 'kanban'" @click="viewMode = 'kanban'"><Kanban :size="14" :stroke-width="1.9" /><span>{{ $t('ui.boardView') }}</span></button>
          <button type="button" :class="{ active: viewMode === 'timeline' }" :title="$t('ui.timelineView')" :aria-pressed="viewMode === 'timeline'" @click="viewMode = 'timeline'"><Clock3 :size="14" :stroke-width="1.9" /><span>{{ $t('ui.timelineView') }}</span></button>
        </div>
      </div>
      <div v-if="viewMode === 'kanban'" class="kanban">
        <article v-for="column in kanbanColumns" :key="column.name" class="kanban-col">
          <h3>{{ $lt(column.name) }} <span>{{ column.rows.length }}</span></h3>
          <div v-for="row in column.rows" :key="row.join('-')" class="kanban-card" @click="selectedIndex = filteredRows.indexOf(row)">
            <b>{{ $lt(row[0]) }}</b><span>{{ $lt(row[1] || row[2]) }}</span><small>{{ $lt(row[row.length - 1]) }}</small>
          </div>
        </article>
      </div>
      <div v-else-if="viewMode === 'timeline'" class="timeline">
        <article v-for="(row, index) in filteredRows" :key="row.join('-')" @click="selectedIndex = index">
          <b>{{ $lt(row[0]) }}</b><span>{{ $lt(row[1] || currentModule.name) }}</span><small>{{ $lt(row[row.length - 1]) }}</small>
        </article>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th v-for="head in currentHeaders" :key="head">{{ $lt(head) }}</th><th>{{ $t('ui.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="(row, rowIndex) in filteredRows" :key="row.join('-')" :class="{ selected: rowIndex === selectedIndex }" @click="selectedIndex = rowIndex">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex" :class="moneyClass(cell, row)">
                <span v-if="cellIndex === 0 && currentModule.avatar" class="avatar">{{ initials(cell) }}</span>
                <span v-if="isStatus(cell)" class="tag" :class="tagClass(cell)">{{ $lt(cell) }}</span>
                <template v-else>{{ $lt(cell) }}</template>
              </td>
              <td><button type="button" class="row-actions" :title="$t('ui.moreActions')" @click.stop="openModal($t('ui.actions'))"><MoreHorizontal :size="16" :stroke-width="1.9" /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ $t('ui.records', { count: filteredRows.length }) }}</span><div><button type="button" disabled>&lt;</button><button type="button" class="active">{{ $t('legacy.t_356a192b7913') }}</button><button type="button">{{ $t('legacy.t_da4b9237bacc') }}</button><button type="button">{{ $t('legacy.t_77de68daecd8') }}</button><button type="button">&gt;</button></div></div>
    </div>
    <aside class="panel detail-panel">
      <div class="detail-card">
        <div class="profile"><div class="big-avatar">{{ initials(detailName) }}</div><div><h3>{{ $lt(detailName) }}</h3><p>{{ $lt(detailSubline) }}</p></div><span class="tag" :class="tagClass(detailStatus)">{{ $lt(detailStatus) }}</span></div>
        <div class="detail-actions"><button v-for="action in currentModule.quickActions" :key="action" @click="showToast(action)">{{ $lt(action) }}</button></div>
        <div v-for="(block, index) in currentModule.detailBlocks" :key="block.title" class="detail-section">
          <h4><span class="num">{{ index + 1 }}</span>{{ $lt(block.title) }}</h4>
          <div v-for="item in block.items" :key="item.label" class="kv"><span>{{ $lt(item.label) }}</span><b>{{ $lt(item.value) }}</b></div>
        </div>
        <div class="detail-section"><h4><span class="num">{{ $t('legacy.t_77de68daecd8') }}</span>{{ $t('ui.paymentProgress') }}</h4><div class="kv"><span>{{ $t('ui.total') }}</span><strong>{{ $t('legacy.t_5e7b60c626a4') }} {{ detailTotal }}</strong></div><div class="kv"><span>{{ $t('ui.pendingAmount') }}</span><strong class="money-red">{{ $t('legacy.t_5e7b60c626a4') }} {{ detailPending }}</strong></div><div class="progress"><i :style="{ width: detailProgress + '%' }"></i></div></div>
      </div>
    </aside>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { Clock3, Kanban, List, MoreHorizontal } from '@lucide/vue';
export default { mixins: [pageBridge], components: { Clock3, Kanban, List, MoreHorizontal } };
</script>
