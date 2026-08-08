<template>
  <section class="report-layout">
    <div class="panel report-main">
      <div class="report-head">
        <div>
          <h2>{{ $t('reports.archiveTitle') }}</h2>
          <p>{{ $t('reports.archiveHint') }}</p>
        </div>
      </div>
      <div class="definition-strip">
        <button
          v-for="definition in definitions"
          :key="definition.id"
          class="definition-card"
          :class="{ active: selectedType === definition.reportType }"
          :aria-pressed="selectedType === definition.reportType"
          @click="selectedType = definition.reportType"
        >
          <b class="definition-icon">{{ typeIcon(definition.reportType) }}</b>
          <span class="definition-copy"><strong>{{ reportName(definition) }}</strong><small>{{ definition.defaultFormat === 'PDF' ? $t('reports.pdfHint') : $t('reports.xlsxHint') }}</small></span>
          <span class="format-chip">{{ definition.defaultFormat }}</span>
        </button>
      </div>
      <ModuleToolbar />
      <div v-if="loading" class="report-state">{{ $t('reports.loading') }}</div>
      <div v-else-if="errorMessage" class="report-state error">
        <strong>{{ $t('reports.loadFailed') }}</strong><span>{{ errorMessage }}</span
        ><button @click="loadData">{{ $t('reports.retry') }}</button>
      </div>
      <div v-else class="table-wrap report-table">
        <table>
          <thead>
            <tr>
              <th>{{ $t('reports.reportName') }}</th>
              <th>{{ $t('reports.dataPeriod') }}</th>
              <th>{{ $t('reports.exportScope') }}</th>
              <th>{{ $t('reports.format') }}</th>
              <th>{{ $t('reports.recordCount') }}</th>
              <th>{{ $t('ui.status') }}</th>
              <th>{{ $t('reports.createdBy') }}</th>
              <th>{{ $t('reports.completedAt') }}</th>
              <th>{{ $t('ui.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in runs" :key="run.id">
              <td>
                <strong>{{ reportRunName(run) }}</strong
                ><small>#{{ run.id }}</small>
              </td>
              <td>{{ run.dateStart }} ~ {{ run.dateEnd }}</td>
              <td>{{ run.scopeName || run.projectName || $t('reports.allScope') }}</td>
              <td>
                <span class="format-chip">{{ run.outputFormat }}</span>
              </td>
              <td>{{ run.recordCount ?? "—" }}</td>
              <td>
                <span class="report-tag" :class="run.status">{{
                  statusLabel(run.status)
                }}</span>
              </td>
              <td>{{ run.requestedByName || $t('reports.system') }}</td>
              <td>{{ dateTime(run.completedAt) }}</td>
              <td>
                <button
                  v-if="run.downloadable"
                  class="download-button"
                  @click="download(run)"
                >
                  {{ $t('reports.download') }}</button
                ><span
                  v-else-if="run.errorMessage"
                  class="error-hint"
                  :title="run.errorMessage"
                  >{{ $t('reports.viewError') }}</span
                ><span v-else>—</span>
              </td>
            </tr>
            <tr v-if="!filteredRuns.length">
              <td colspan="9" class="empty-cell">{{ $t('reports.noReports') }}</td>
            </tr>
          </tbody>
        </table>
        <AdminListPager
          v-if="filteredRuns.length"
          :page="pageNumber"
          :page-size="pageSize"
          :total="totalRows"
          @update:page="changePage"
          @update:page-size="changePageSize"
        />
      </div>
    </div>
    <aside class="panel report-detail">
      <template v-if="selectedDefinition"
        ><div class="detail-title">
          <div class="report-icon">
            {{ typeIcon(selectedDefinition.reportType) }}
          </div>
          <div>
            <h3>{{ reportName(selectedDefinition) }}</h3>
            <p>{{ $t('reports.defaultFormat') }} · {{ selectedDefinition.defaultFormat }}</p>
          </div>
          <span class="report-tag completed">{{ $t('reports.enabled') }}</span>
        </div>
        <button class="generate-button" @click="openDialog(selectedDefinition)">
          {{ $t('reports.generateThisReport') }}
        </button>
        <section class="detail-section">
          <h4>{{ $t('reports.settings') }}</h4>
          <dl>
            <div>
              <dt>{{ $t('reports.reportType') }}</dt>
              <dd>{{ typeLabel(selectedDefinition.reportType) }}</dd>
            </div>
            <div>
              <dt>{{ $t('reports.defaultFormat') }}</dt>
              <dd>{{ selectedDefinition.defaultFormat }}</dd>
            </div>
            <div>
              <dt>{{ $t('reports.schedule') }}</dt>
              <dd>{{ selectedDefinition.scheduleCron || $t('reports.notConfigured') }}</dd>
            </div>
            <div>
              <dt>{{ $t('ui.latestUpdate') }}</dt>
              <dd>{{ dateTime(selectedDefinition.updatedAt) }}</dd>
            </div>
          </dl>
        </section>
        <section class="detail-section">
          <h4>{{ $t('reports.dataDefinition') }}</h4>
          <p>{{ definitionHint(selectedDefinition.reportType) }}</p>
        </section>
        <section class="detail-section">
          <h4>{{ $t('reports.recentRuns') }}</h4>
          <article
            v-for="run in typeRuns.slice(0, 5)"
            :key="run.id"
            class="recent-run"
          >
            <div>
              <strong>{{ run.dateStart }} ~ {{ run.dateEnd }}</strong
              ><small
                >{{ $t('ui.records', { count: run.recordCount ?? 0 }) }} · {{ run.outputFormat }}</small
              >
            </div>
            <span class="report-tag" :class="run.status">{{
              statusLabel(run.status)
            }}</span>
          </article>
          <p v-if="!typeRuns.length">{{ $t('reports.noRecords') }}</p>
        </section>
      </template>
    </aside>
    <dialog ref="reportDialog" class="report-dialog">
      <form @submit.prevent="generate">
        <header>
          <div>
            <h3>{{ $t('reports.generate') }}</h3>
            <p>{{ $t('reports.generationHint') }}</p>
          </div>
          <button type="button" @click="closeDialog">×</button>
        </header>
        <div class="dialog-body">
          <label
            >{{ $t('reports.reportType') }}<select
              v-model="form.reportType"
              @change="syncDefaultFormat"
            >
              <option
                v-for="definition in definitions"
                :key="definition.id"
                :value="definition.reportType"
              >
                {{ reportName(definition) }}
              </option>
            </select></label
          >
          <div class="form-grid">
            <label
              >{{ $t('reports.startDate') }}<input
                v-model="form.dateStart"
                type="date"
                required /></label
            ><label
              >{{ $t('reports.endDate') }}<input v-model="form.dateEnd" type="date" required
            /></label>
          </div>
          <div class="form-grid">
            <label v-if="scopeTypes.length > 1"
              >{{ $t('reports.exportScope') }}<select
                v-model="form.scopeType"
                @change="form.scopeId = null"
              >
                <option v-for="scopeType in scopeTypes" :key="scopeType" :value="scopeType">
                  {{ scopeType === 'all' ? $t('reports.allData') : scopeTypeLabel(scopeType) }}
                </option>
              </select></label
            ><label v-if="form.scopeType !== 'all'" :class="{ 'scope-person-select': scopeTypes.length === 1 }"
              >{{ $t('reports.select') }} {{ scopeTypeLabel(form.scopeType)
              }}<select v-model.number="form.scopeId" required>
                <option :value="null" disabled>
                  {{ $t('reports.select') }} {{ scopeTypeLabel(form.scopeType) }}
                </option>
                <option
                  v-for="option in scopeOptions"
                  :key="option.id"
                  :value="option.id"
                >
                  {{ option.label }}
                </option>
              </select></label
            >
          </div>
          <fieldset>
            <legend>{{ $t('reports.outputFormat') }}</legend>
            <label
              ><input
                v-model="form.outputFormat"
                type="radio"
                value="XLSX"
              /><span
                ><b>{{ $t('legacy.t_3de1af4c1dcb') }}</b><small>{{ $t('reports.xlsxHint') }}</small></span
              ></label
            ><label
              ><input
                v-model="form.outputFormat"
                type="radio"
                value="PDF"
              /><span
                ><b>{{ $t('legacy.t_d613d88cb2d8') }}</b><small>{{ $t('reports.pdfHint') }}</small></span
              ></label
            >
          </fieldset>
          <p v-if="dialogError" class="dialog-error">{{ dialogError }}</p>
        </div>
        <menu>
          <button type="button" @click="closeDialog">{{ $t('ui.cancel') }}</button
          ><button class="save-button" :disabled="generating">
            {{ generating ? $t('reports.generating') : $t('reports.generate') }}
          </button>
        </menu>
      </form>
    </dialog>
  </section>
</template>
<script>
import {
  downloadAdminReport,
  fetchAdminReports,
  generateAdminReport,
} from "../services/propertyApi";
import AdminListPager from "./AdminListPager.vue";
import ModuleToolbar from "./ModuleToolbar.vue";
const monthRange = () => {
  const d = new Date(),
    y = d.getFullYear(),
    m = String(d.getMonth() + 1).padStart(2, "0"),
    last = new Date(y, d.getMonth() + 1, 0).getDate();
  return [`${y}-${m}-01`, `${y}-${m}-${last}`];
};
export default {
  inject: ["page"],
  components: { AdminListPager, ModuleToolbar },
  data() {
    const [start, end] = monthRange();
    return {
      loading: true,
      errorMessage: "",
      definitions: [],
      runs: [],
      projects: [],
      owners: [],
      tenants: [],
      units: [],
      selectedType: "property_payment",
      form: {
        reportType: "property_payment",
        dateStart: start,
        dateEnd: end,
        scopeType: "all",
        scopeId: null,
        outputFormat: "XLSX",
      },
      generating: false,
      dialogError: "",
      pageNumber: 1,
      pageSize: 10,
      totalRows: 0,
      searchTimer: null,
    };
  },
  computed: {
    selectedDefinition() {
      return (
        this.definitions.find((d) => d.reportType === this.selectedType) ||
        this.definitions[0] ||
        null
      );
    },
    typeRuns() {
      return this.runs.filter(
        (r) => r.definitionId === this.selectedDefinition?.id,
      );
    },
    keyword() {
      return String(this.page.globalSearch || this.page.moduleSearch || "")
        .trim()
        .toLowerCase();
    },
    scopeOptions() {
      if (this.form.scopeType === "project")
        return this.projects.map((x) => ({ id: x.id, label: x.name }));
      if (this.form.scopeType === "owner")
        return this.owners.map((x) => ({ id: x.id, label: x.name }));
      if (this.form.scopeType === "tenant")
        return this.tenants.map((x) => ({ id: x.id, label: x.name }));
      if (this.form.scopeType === "unit")
        return this.units.map((x) => ({
          id: x.id,
          label: `${x.projectName} · ${x.unitNo}`,
        }));
      return [];
    },
    scopeTypes() {
      if (this.form.reportType === "owner_statement") return ["owner"];
      if (this.form.reportType === "tenant_statement") return ["tenant"];
      return ["all", "project", "owner", "unit"];
    },
    filteredRuns() {
      return this.runs;
    },
    createNonce() {
      return this.page.adminReportCreateNonce;
    },
    refreshNonce() {
      return this.page.adminReportRefreshNonce;
    },
  },
  watch: {
    selectedType() {
      this.pageNumber = 1;
    },
    keyword() {
      this.scheduleLoad();
    },
    "page.projectFilter"() {
      this.scheduleLoad();
    },
    "page.statusFilter"() {
      this.scheduleLoad();
    },
    createNonce(v, p) {
      if (v > p) this.openDialog(this.selectedDefinition);
    },
    refreshNonce(v, p) {
      if (v > p) this.loadData();
    },
  },
  mounted() {
    this.page.projectFilter = "全部建案";
    this.page.statusFilter = "全部狀態";
    this.loadData();
  },
  methods: {
    async loadData() {
      this.loading = true;
      this.errorMessage = "";
      try {
        const r = await fetchAdminReports({
          page: this.pageNumber,
          pageSize: this.pageSize,
          keyword: this.keyword,
          project: this.page.projectFilter,
          status: this.page.statusFilter,
        });
        this.definitions = r.definitions || [];
        this.runs = r.runs || [];
        this.projects = r.projects || [];
        this.owners = r.owners || [];
        this.tenants = r.tenants || [];
        this.units = r.units || [];
        this.totalRows = Number(r.page?.totalRows || 0);
        if (r.page?.page && r.page.page !== this.pageNumber)
          this.pageNumber = r.page.page;
        this.page.adminReportProjects = this.projects.map((p) => p.name);
        if (!this.definitions.some((d) => d.reportType === this.selectedType))
          this.selectedType = this.definitions[0]?.reportType || "";
        this.setMetrics(r.summary || {});
        this.setExport();
      } catch (e) {
        this.errorMessage = e.message || this.$t('reports.loadFailed');
        this.page.adminReportMetrics = null;
      } finally {
        this.loading = false;
      }
    },
    scheduleLoad() {
      clearTimeout(this.searchTimer);
      this.pageNumber = 1;
      this.searchTimer = setTimeout(() => this.loadData(), 250);
    },
    changePage(value) {
      if (value === this.pageNumber) return;
      this.pageNumber = value;
      this.loadData();
    },
    changePageSize(value) {
      if (value === this.pageSize) return;
      this.pageSize = value;
      this.pageNumber = 1;
      this.loadData();
    },
    setMetrics(s) {
      this.page.adminReportMetrics = [
        {
          label: this.$t('reports.availableReports'),
          value: this.$t('ui.records', { count: Number(s.definitionCount || 0) }),
          delta: this.$t('reports.databaseDefinitions'),
          trend: "up",
        },
        {
          label: this.$t('reports.generatedThisMonth'),
          value: this.$t('ui.records', { count: Number(s.generatedThisMonth || 0) }),
          delta: 'PDF / XLSX',
          trend: "up",
        },
        {
          label: this.$t('reports.completed'),
          value: this.$t('ui.records', { count: Number(s.completedCount || 0) }),
          delta: this.$t('reports.downloadableArchive'),
          trend: "up",
        },
        {
          label: this.$t('reports.failed'),
          value: this.$t('ui.records', { count: Number(s.failedCount || 0) }),
          delta: this.$t('reports.retainFailureReason'),
          trend: Number(s.failedCount) ? "down" : "up",
        },
      ];
    },
    setExport() {
      this.page.adminReportExportHeaders = [
        this.$t('reports.reportName'),
        this.$t('reports.startDate'),
        this.$t('reports.endDate'),
        this.$t('reports.exportScope'),
        this.$t('reports.format'),
        this.$t('reports.recordCount'),
        this.$t('ui.status'),
        this.$t('reports.createdBy'),
        this.$t('reports.completedAt'),
        this.$t('reports.viewError'),
      ];
      this.page.adminReportExportRows = this.runs.map((r) => [
        this.reportRunName(r),
        r.dateStart,
        r.dateEnd,
        r.scopeName || r.projectName || this.$t('reports.allScope'),
        r.outputFormat,
        r.recordCount ?? "",
        this.statusLabel(r.status),
        r.requestedByName || "",
        this.dateTime(r.completedAt),
        r.errorMessage || "",
      ]);
    },
    openDialog(definition) {
      const [start, end] = monthRange();
      const d = definition || this.selectedDefinition;
      this.form = {
        reportType: d?.reportType || "property_payment",
        dateStart: start,
        dateEnd: end,
        scopeType: this.defaultScopeType(d?.reportType),
        scopeId: null,
        outputFormat: d?.defaultFormat || "XLSX",
      };
      this.dialogError = "";
      this.$refs.reportDialog?.showModal();
    },
    closeDialog() {
      if (!this.generating) this.$refs.reportDialog?.close();
    },
    syncDefaultFormat() {
      const d = this.definitions.find(
        (x) => x.reportType === this.form.reportType,
      );
      if (d) this.form.outputFormat = d.defaultFormat;
      this.form.scopeType = this.defaultScopeType(this.form.reportType);
      this.form.scopeId = null;
    },
    async generate() {
      this.generating = true;
      this.dialogError = "";
      try {
        const payload = {
          reportType: this.form.reportType,
          dateStart: this.form.dateStart,
          dateEnd: this.form.dateEnd,
          projectId:
            this.form.scopeType === "project" ? this.form.scopeId : null,
          ownerId: this.form.scopeType === "owner" ? this.form.scopeId : null,
          tenantId: this.form.scopeType === "tenant" ? this.form.scopeId : null,
          unitId: this.form.scopeType === "unit" ? this.form.scopeId : null,
          outputFormat: this.form.outputFormat,
        };
        const run = await generateAdminReport(payload);
        if (run.status === "failed") {
          this.dialogError = run.errorMessage || this.$t('reports.generationFailed');
          return;
        }
        this.$refs.reportDialog?.close();
        this.selectedType = this.form.reportType;
        await this.loadData();
        this.page.showToast(
          this.$t('reports.generated', { count: Number(run.recordCount || 0) }),
        );
      } catch (e) {
        this.dialogError = e.message || this.$t('reports.generationFailed');
      } finally {
        this.generating = false;
      }
    },
    async download(run) {
      try {
        const f = await downloadAdminReport(run.id);
        const u = URL.createObjectURL(f.blob),
          a = document.createElement("a");
        a.href = u;
        a.download = this.downloadName(run);
        a.click();
        URL.revokeObjectURL(u);
      } catch (e) {
        this.page.showToast(e.message || this.$t('reports.downloadFailed'));
      }
    },
    scopeTypeLabel(v) {
      return this.$t({ project: 'reports.byProject', owner: 'reports.byOwner', tenant: 'reports.byTenant', unit: 'reports.byUnit' }[v] || 'reports.exportScope');
    },
    defaultScopeType(reportType) {
      if (reportType === 'owner_statement') return 'owner';
      if (reportType === 'tenant_statement') return 'tenant';
      return 'all';
    },
    downloadName(run) {
      const scope = run.scopeName || run.projectName || this.$t('reports.allScope');
      const period = run.dateStart && run.dateEnd ? `${run.dateStart}-${run.dateEnd}` : run.dateStart || run.dateEnd || this.$t('reports.notConfigured');
      const clean = (value) => String(value || this.$t('reports.reportName')).replace(/[\\/:*?"<>|\r\n]/g, "_").trim();
      return `${clean(this.reportRunName(run))}_${clean(scope)}_${period}.${String(run.outputFormat || "XLSX").toLowerCase()}`;
    },
    typeIcon(v) {
      return (
        {
          property_payment: "款",
          rent_collection: "租",
          income_expense: "收",
          maintenance: "修",
          reserve: "金",
          reserve_refund: "返",
          finance: "財",
          sync: "同",
          owner_statement: "主",
          tenant_statement: "客",
        }[v] || "表"
      );
    },
    typeLabel(v) {
      const key = `reports.${v}`;
      return this.$t(key) === key ? v : this.$t(key);
    },
    reportName(definition) {
      return this.typeLabel(definition?.reportType) || definition?.name || '—';
    },
    reportRunName(run) {
      return this.typeLabel(run?.reportType) || run?.reportName || '—';
    },
    definitionHint(v) {
      const key = `reports.hint_${v}`;
      return this.$t(key) === key ? this.$t('reports.hint_default') : this.$t(key);
    },
    statusLabel(v) {
      return (
        {
          completed: this.$t('reports.completed'),
          failed: this.$t('reports.failed'),
          processing: this.$t('reports.processing'),
          queued: this.$t('reports.pending'),
        }[v] || v
      );
    },
    dateTime(v) {
      return v ? String(v).replace("T", " ").slice(0, 16) : "—";
    },
  },
};
</script>
<style scoped>
.report-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
  gap: 16px;
  min-height: 560px;
}
.report-main,
.report-detail {
  min-width: 0;
}
.report-main {
  overflow: hidden;
}
.report-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px 10px;
}
.report-head h2 {
  margin: 0 0 4px;
  font-size: 18px;
}
.report-head p {
  margin: 0;
  color: #718096;
  font-size: 12px;
}
.format-chip {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 5px;
  background: #eaf2fb;
  color: #0a4b80;
  font-size: 11px;
  font-weight: 700;
}
.definition-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 9px;
  padding: 7px 18px 18px;
}
.definition-card {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
  min-height: 78px;
  border: 1px solid #dce4ed;
  border-radius: 10px;
  background: linear-gradient(145deg, #fff, #fbfdff);
  padding: 12px;
  text-align: left;
  transition: border-color .16s ease, box-shadow .16s ease, transform .16s ease;
  cursor: pointer;
}
.definition-card:hover {
  border-color: #9fc1df;
  box-shadow: 0 6px 16px #12355d12;
  transform: translateY(-1px);
}
.definition-card.active {
  border-color: #d49718;
  background: #fff9eb;
  box-shadow: inset 3px 0 #d49718, 0 7px 18px #d4971820;
}
.definition-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #0a3d72;
  color: #fff;
  font-size: 15px;
}
.definition-card.active .definition-icon {
  background: #c98300;
}
.definition-copy {
  min-width: 0;
}
.definition-copy strong,
.definition-copy small {
  display: block;
}
.definition-copy strong {
  color: #092f5d;
  font-size: 14px;
  line-height: 1.35;
}
.definition-copy small {
  display: -webkit-box;
  margin-top: 4px;
  overflow: hidden;
  color: #718096;
  font-size: 11px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.report-main :deep(.toolbar) {
  width: auto;
  margin: 0;
  padding: 13px 18px;
  gap: 8px;
  border-width: 1px 0 0;
  border-radius: 0;
  background: #f8fbfc;
  box-shadow: none;
}
.report-main :deep(.toolbar .search-box) {
  flex: 1 1 280px;
  min-width: 240px;
}
.report-main :deep(.toolbar select) {
  width: 160px;
  min-width: 140px;
  flex: 0 1 160px;
}
.report-main :deep(.toolbar .primary-btn),
.report-main :deep(.toolbar .ghost-btn) {
  min-width: auto;
  padding-inline: 13px;
}
.report-table {
  overflow: auto;
  border-top: 1px solid #e3ebf1;
}
.report-table table {
  min-width: 1000px;
}
.report-table td strong,
.report-table td small {
  display: block;
}
.report-table td small {
  margin-top: 3px;
  color: #718096;
}
.report-tag {
  display: inline-flex;
  padding: 4px 8px;
  border: 1px solid;
  border-radius: 6px;
  font-size: 11px;
}
.report-tag.completed {
  color: #168348;
  background: #edf9f1;
  border-color: #b9e4c7;
}
.report-tag.processing,
.report-tag.queued {
  color: #a56600;
  background: #fff7e5;
  border-color: #eed08c;
}
.report-tag.failed {
  color: #c5313b;
  background: #fff0f1;
  border-color: #efbbc0;
}
.download-button {
  border: 1px solid #cbd7e4;
  border-radius: 6px;
  background: #fff;
  padding: 5px 9px;
  color: #0a477c;
}
.error-hint {
  color: #c5313b;
}
.empty-cell {
  text-align: center;
  color: #718096;
  padding: 35px !important;
}
.report-detail {
  position: sticky;
  top: 16px;
  align-self: start;
  padding: 18px;
  overflow: hidden;
}
.detail-title {
  display: grid;
  grid-template-columns: 48px 1fr auto;
  align-items: center;
  gap: 10px;
  margin: -18px -18px 0;
  padding: 18px;
  border-bottom: 1px solid #e0e9ef;
  background: linear-gradient(145deg, #f7fbfc, #fff);
}
.report-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #0a3d72;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}
.detail-title h3 {
  margin: 0 0 4px;
  font-size: 16px;
}
.detail-title p {
  margin: 0;
  color: #718096;
  font-size: 11px;
}
.generate-button {
  width: 100%;
  margin: 16px 0;
  border: 0;
  border-radius: 9px;
  background: linear-gradient(180deg, #efad00, #cf8900);
  color: #fff;
  padding: 12px;
  font-weight: 700;
  box-shadow: 0 7px 16px #cf890026;
  cursor: pointer;
}
.detail-section {
  padding: 15px 0;
  border-top: 1px solid #e2e8ef;
}
.detail-section h4 {
  margin: 0 0 11px;
}
.detail-section dl {
  margin: 0;
}
.detail-section dl div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 6px 0;
}
.detail-section dt {
  color: #718096;
}
.detail-section dd {
  margin: 0;
  text-align: right;
  font-weight: 600;
}
.detail-section p {
  color: #65758a;
  font-size: 12px;
  line-height: 1.65;
}
.recent-run {
  display: flex;
  justify-content: space-between;
  gap: 7px;
  padding: 8px 0;
  border-bottom: 1px dashed #dfe6ed;
}
.recent-run strong,
.recent-run small {
  display: block;
}
.recent-run small {
  margin-top: 3px;
  color: #718096;
  font-size: 10px;
}
.report-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 280px;
  color: #718096;
}
.report-state.error strong,
.dialog-error {
  color: #c5313b;
}
.report-dialog {
  width: min(640px, calc(100vw - 32px));
  max-width: calc(100vw - 32px);
  max-height: calc(100dvh - 32px);
  overflow: auto;
  box-sizing: border-box;
  padding: 0;
  border: 0;
  border-radius: 12px;
  box-shadow: 0 22px 70px #10233b42;
}
.report-dialog::backdrop {
  background: #0a172a80;
}
.report-dialog form {
  margin: 0;
  min-width: 0;
}
.report-dialog header {
  display: flex;
  justify-content: space-between;
  padding: 20px 22px;
  border-bottom: 1px solid #e1e7ee;
}
.report-dialog h3 {
  margin: 0 0 5px;
}
.report-dialog header p {
  margin: 0;
  color: #718096;
  font-size: 12px;
}
.report-dialog header button {
  border: 0;
  background: transparent;
  font-size: 26px;
}
.dialog-body {
  display: grid;
  gap: 14px;
  padding: 20px 22px;
  min-width: 0;
  overflow-x: hidden;
}
.dialog-body > label,
.form-grid label {
  display: grid;
  gap: 6px;
  font-weight: 600;
  min-width: 0;
}
.dialog-body select,
.dialog-body input[type="date"] {
  border: 1px solid #ccd7e3;
  border-radius: 7px;
  padding: 10px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.scope-person-select {
  grid-column: 1 / -1;
}
.dialog-body fieldset {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
  margin: 0;
  border: 1px solid #dce4ed;
  border-radius: 8px;
  padding: 12px;
}
.dialog-body fieldset label {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 8px;
  padding: 8px;
  min-width: 0;
  border: 1px solid #e5ebf2;
  border-radius: 6px;
  cursor: pointer;
}
.dialog-body fieldset b,
.dialog-body fieldset small {
  display: block;
  min-width: 0;
}
.dialog-body fieldset small {
  margin-top: 3px;
  color: #718096;
  font-size: 10px;
  overflow-wrap: anywhere;
}
.report-dialog menu {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin: 0;
  padding: 14px 22px;
  background: #f6f8fb;
}
.report-dialog menu button {
  border: 1px solid #ccd7e3;
  border-radius: 7px;
  background: #fff;
  padding: 9px 16px;
}
.report-dialog menu .save-button {
  background: #cf8900;
  border-color: #cf8900;
  color: #fff;
}
.report-dialog menu button:disabled {
  opacity: 0.5;
}
@media (max-width: 1200px) {
  .report-layout {
    grid-template-columns: 1fr;
  }
  .report-detail {
    position: static;
  }
}
@media (min-width: 1500px) {
  .definition-strip {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
@media (max-width: 760px) {
  .definition-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .report-main :deep(.toolbar .search-box) {
    flex-basis: 100%;
  }
}
@media (max-width: 600px) {
  .definition-strip {
    grid-template-columns: 1fr;
  }
  .form-grid,
  .dialog-body fieldset {
    grid-template-columns: 1fr;
  }
}
</style>
