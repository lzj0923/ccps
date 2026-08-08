import fs from 'node:fs';
import path from 'node:path';

const source = fs.readFileSync(path.resolve('frontend/src/components/AdminOwnersWorkspace.vue'), 'utf8');
const assertions = [
  [!source.includes('<option value="__new__">'), '新增房产不能包含临时新增建案选项'],
  [!source.includes('newProjectConfirmDialog'), '新增房产不能弹出第二套建案创建流程'],
  [!source.includes("projectId === '__new__'"), '房产表单不能保留临时建案分支'],
  [source.includes('projectId: Number(this.propertyCreateForm.projectId)'), '新增房产必须提交已有建案 ID'],
  [source.includes("this.propertyCreateError = '請選擇已有建案，並填寫單位編號'"), '校验必须明确要求选择已有建案']
];

const failure = assertions.find(([passed]) => !passed);
if (failure) {
  console.error(`FAIL: ${failure[1]}`);
  process.exit(1);
}

console.log('PASS: 新增房产只允许选择建案主档中的已有建案');
