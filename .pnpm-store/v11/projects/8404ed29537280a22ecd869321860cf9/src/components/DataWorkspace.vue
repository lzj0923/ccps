<template>
  <section class="content-grid">
    <div class="panel table-panel">
      <div class="panel-head">
        <div><h2>{{ currentModule.table }}</h2><span>{{ filteredRows.length }} records</span></div>
        <div class="view-tabs">
          <button :class="{ active: viewMode === 'list' }" title="List" @click="viewMode = 'list'"><List :size="14" :stroke-width="1.9" /><span>List</span></button>
          <button :class="{ active: viewMode === 'kanban' }" title="Board" @click="viewMode = 'kanban'"><Kanban :size="14" :stroke-width="1.9" /><span>Board</span></button>
          <button :class="{ active: viewMode === 'timeline' }" title="Timeline" @click="viewMode = 'timeline'"><Clock3 :size="14" :stroke-width="1.9" /><span>Timeline</span></button>
        </div>
      </div>
      <div v-if="viewMode === 'kanban'" class="kanban">
        <article v-for="column in kanbanColumns" :key="column.name" class="kanban-col">
          <h3>{{ column.name }} <span>{{ column.rows.length }}</span></h3>
          <div v-for="row in column.rows" :key="row.join('-')" class="kanban-card" @click="selectedIndex = filteredRows.indexOf(row)">
            <b>{{ row[0] }}</b><span>{{ row[1] || row[2] }}</span><small>{{ row[row.length - 1] }}</small>
          </div>
        </article>
      </div>
      <div v-else-if="viewMode === 'timeline'" class="timeline">
        <article v-for="(row, index) in filteredRows" :key="row.join('-')" @click="selectedIndex = index">
          <b>{{ row[0] }}</b><span>{{ row[1] || currentModule.name }}</span><small>{{ row[row.length - 1] }}</small>
        </article>
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr><th v-for="head in currentHeaders" :key="head">{{ head }}</th><th>Actions</th></tr></thead>
          <tbody>
            <tr v-for="(row, rowIndex) in filteredRows" :key="row.join('-')" :class="{ selected: rowIndex === selectedIndex }" @click="selectedIndex = rowIndex">
              <td v-for="(cell, cellIndex) in row" :key="cellIndex" :class="moneyClass(cell, row)">
                <span v-if="cellIndex === 0 && currentModule.avatar" class="avatar">{{ initials(cell) }}</span>
                <span v-if="isStatus(cell)" class="tag" :class="tagClass(cell)">{{ cell }}</span>
                <template v-else>{{ cell }}</template>
              </td>
              <td><button class="row-actions" title="More actions" @click.stop="openModal('Handle')"><MoreHorizontal :size="16" :stroke-width="1.9" /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>{{ filteredRows.length }} records</span><div><button disabled>&lt;</button><button class="active">1</button><button>2</button><button>3</button><button>&gt;</button></div></div>
    </div>
    <aside class="panel detail-panel">
      <div class="detail-card">
        <div class="profile"><div class="big-avatar">{{ initials(detailName) }}</div><div><h3>{{ detailName }}</h3><p>{{ detailSubline }}</p></div><span class="tag" :class="tagClass(detailStatus)">{{ detailStatus }}</span></div>
        <div class="detail-actions"><button v-for="action in currentModule.quickActions" :key="action" @click="showToast(action)">{{ action }}</button></div>
        <div v-for="(block, index) in currentModule.detailBlocks" :key="block.title" class="detail-section">
          <h4><span class="num">{{ index + 1 }}</span>{{ block.title }}</h4>
          <div v-for="item in block.items" :key="item.label" class="kv"><span>{{ item.label }}</span><b>{{ item.value }}</b></div>
        </div>
        <div class="detail-section"><h4><span class="num">3</span>Payment progress</h4><div class="kv"><span>Total</span><strong>RM {{ detailTotal }}</strong></div><div class="kv"><span>Pending</span><strong class="money-red">RM {{ detailPending }}</strong></div><div class="progress"><i :style="{ width: detailProgress + '%' }"></i></div></div>
      </div>
    </aside>
  </section>
</template>

<script>
import pageBridge from '../pageBridge';
import { Clock3, Kanban, List, MoreHorizontal } from '@lucide/vue';
export default { mixins: [pageBridge], components: { Clock3, Kanban, List, MoreHorizontal } };
</script>
