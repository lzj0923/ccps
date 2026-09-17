import test from 'node:test';
import assert from 'node:assert/strict';
import {
  findFirstEmptyRequiredControl,
  findFirstInvalidControl,
  missingRequiredFields,
  revealFirstInvalidControl,
} from '../src/utils/supplementFormValidation.js';

test('returns required fields in visual form order', () => {
  const required = { owner: 'ownerLabel', witness: 'witnessLabel', date: 'dateLabel' };
  assert.deepEqual(missingRequiredFields({ owner: 'Jane', witness: ' ', date: '' }, required), [
    { field: 'witness', labelKey: 'witnessLabel' },
    { field: 'date', labelKey: 'dateLabel' },
  ]);
});

test('ignores format validity and finds only the first empty required control', () => {
  const calls = [];
  const controls = [
    { disabled: true, required: true, value: '', checkValidity: () => false },
    { disabled: false, required: true, value: '1', checkValidity: () => false },
    { disabled: false, required: false, value: '', checkValidity: () => false },
    {
      disabled: false,
      required: true,
      value: ' ',
      checkValidity: () => false,
      scrollIntoView: options => calls.push(['scroll', options]),
      focus: options => calls.push(['focus', options]),
    },
  ];
  const form = { querySelectorAll: () => controls };

  assert.equal(findFirstEmptyRequiredControl(form), controls[3]);
  assert.equal(findFirstInvalidControl(form), controls[3]);
  assert.equal(revealFirstInvalidControl(form), controls[3]);
  assert.deepEqual(calls, [
    ['scroll', { behavior: 'smooth', block: 'center', inline: 'nearest' }],
    ['focus', { preventScroll: true }],
  ]);
});

test('returns null when every required control contains a value even if its format is invalid', () => {
  const form = { querySelectorAll: () => [{ disabled: false, required: true, value: 'x', checkValidity: () => false }] };
  assert.equal(findFirstEmptyRequiredControl(form), null);
  assert.equal(findFirstInvalidControl(form), null);
  assert.equal(revealFirstInvalidControl(form), null);
});
