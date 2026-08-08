import fs from 'node:fs';
import path from 'node:path';

const frontend = fs.readFileSync(path.resolve('frontend/src/components/AdminOwnersWorkspace.vue'), 'utf8');
const service = fs.readFileSync(path.resolve('backend/src/main/java/com/ccps/backend/service/AdminOwnerService.java'), 'utf8');
const mapper = fs.readFileSync(path.resolve('backend/src/main/java/com/ccps/backend/mapper/AdminOwnerMapper.java'), 'utf8');

const assertions = [
  [!frontend.includes('v-model.trim="ownerForm.ownerNo"') && !frontend.includes("ownerEditingId ? ownerForm.ownerNo : $t('ui.generatedAfterSave')"), '业主弹窗不能显示或编辑编号输入框'],
  [frontend.includes('delete payload.ownerNo'), '前端不能向新增或编辑接口提交业主编号'],
  [service.includes('owner.setOwnerNo(null)') && service.includes('String.format("%06d", owner.getId())'), '后端必须依据数据库主键生成六位业主编号'],
  [service.includes('mapper.assignOwnerNo(owner.getId(), generatedOwnerNo)'), '创建业主后必须回写系统编号'],
  [!mapper.includes('UPDATE owners SET owner_no=#{ownerNo}, full_name='), '编辑接口不能修改系统业主编号'],
  [mapper.includes('int assignOwnerNo('), '数据访问层必须提供一次性编号写入']
];

const failure = assertions.find(([passed]) => !passed);
if (failure) {
  console.error(`FAIL: ${failure[1]}`);
  process.exit(1);
}

console.log('PASS: 业主编号由系统生成且不可手工修改');
