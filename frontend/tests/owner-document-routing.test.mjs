import test from 'node:test';
import assert from 'node:assert/strict';
import { matchesOwnerDocumentCategory, matchesOwnerDocumentProperty, ownerDocumentFilePath, ownerDocumentKey } from '../src/utils/ownerDocuments.js';

test('合同与普通文件即使数字ID相同也不会选中或下载错来源', () => {
  const photo = { id: 9, source: 'document' };
  const contract = { id: 9, source: 'property_contract' };
  assert.notEqual(ownerDocumentKey(photo),ownerDocumentKey(contract));
  assert.match(ownerDocumentFilePath(9,false,contract.source),/source=property_contract$/);
  assert.throws(()=>ownerDocumentFilePath(9,false,'admin'));
});
test('付款凭证和收据都能按收据凭证查询，收支归档、财务和维修附件不会漏掉', () => {
  for(const category of ['proof','receipt']) assert.ok(matchesOwnerDocumentCategory({category},'proof'));
  for(const category of ['cashflow','finance','maintenance']) assert.ok(matchesOwnerDocumentCategory({category},'cashflow'));
  assert.equal(matchesOwnerDocumentCategory({category:'photo'},'cashflow'),false);
});
test('优先使用归属ID区分同名房号，仍兼容旧文件名称和业主通用文件', () => {
  const property={ownerUnitId:11,projectName:'A',unitNo:'102'};
  assert.equal(matchesOwnerDocumentProperty({ownerUnitId:12,projectName:'A',unitNo:'102'},property),false);
  assert.ok(matchesOwnerDocumentProperty({ownerUnitId:11},property));
  assert.ok(matchesOwnerDocumentProperty({projectName:'A',unitNo:'102'},property));
  assert.ok(matchesOwnerDocumentProperty({},property));
});
