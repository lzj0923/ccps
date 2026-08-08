import fs from 'node:fs';
import path from 'node:path';

const source = fs.readFileSync(path.resolve('frontend/src/components/AdminProjectManagementWorkspace.vue'), 'utf8');
const assertions = [
  [source.includes('table-layout:fixed'), '建案表格必须使用固定列布局，避免内容撑宽'],
  [source.includes('class="actions-col"') && source.includes('.project-list-card .actions-col{width:15%}'), '操作列必须预留足够且不过量的宽度'],
  [source.includes('.project-list-card th:last-child,.project-list-card td:last-child{min-width:174px;text-align:right}'), '操作列必须保证按钮完整显示并右对齐'],
  [source.includes('class="project-row-actions"') && !source.includes('<div class="row-actions">'), '按钮组不能复用全局单图标按钮类 row-actions'],
  [source.includes('.project-row-actions{display:flex;align-items:center;justify-content:flex-end') && source.includes('border:0;background:transparent;box-shadow:none'), '按钮组本身不能出现外框或背景'],
  [source.includes('.address-cell{max-width:0;overflow:hidden'), '长地址必须省略显示，不能撑宽表格'],
  [source.includes('min-width:1120px'), '窄屏必须保留最小可读宽度和横向滚动']
];

const failure = assertions.find(([passed]) => !passed);
if (failure) {
  console.error(`FAIL: ${failure[1]}`);
  process.exit(1);
}

console.log('PASS: 建案表格不会在桌面宽度下溢出，操作按钮完整可见');
