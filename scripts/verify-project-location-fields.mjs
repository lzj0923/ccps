import fs from 'node:fs';
import path from 'node:path';

const component = fs.readFileSync(path.resolve('frontend/src/components/AdminProjectManagementWorkspace.vue'), 'utf8');
const locations = fs.readFileSync(path.resolve('frontend/src/data/malaysiaLocations.js'), 'utf8');
const mapper = fs.readFileSync(path.resolve('backend/src/main/java/com/ccps/backend/mapper/AdminProjectMapper.java'), 'utf8');

const assertions = [
  [!component.includes('<datalist'), '地址选择不能使用不可控样式的原生 datalist'],
  [component.includes("$t('projectManagement.state')"), '建案表单必须包含州属选择'],
  [component.includes('v-for="area in areaOptions"'), '城市/县区必须随州属联动'],
  [component.includes("$t('projectManagement.detailedAddress')"), '建案表单必须包含详细地址'],
  [component.includes('state: source.state?.trim() || null'), '前端必须提交州属字段'],
  [locations.includes("state: 'Johor'") && locations.includes("state: 'Kuala Lumpur'"), '马来西亚地区数据必须包含常用州属'],
  [mapper.includes('state_name AS state'), '后端必须读取独立州属字段'],
  [mapper.includes('state_name, city, country_code'), '后端必须保存州属、城市/县区和详细地址']
];

const failure = assertions.find(([passed]) => !passed);
if (failure) {
  console.error(`FAIL: ${failure[1]}`);
  process.exit(1);
}

console.log('PASS: 建案地址支持州属、城市/县区和详细地址');
