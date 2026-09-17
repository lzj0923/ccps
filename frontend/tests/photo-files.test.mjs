import test from 'node:test';
import assert from 'node:assert/strict';

import { mergeUniquePhotoFiles, removePhotoFile } from '../src/utils/photoFiles.js';

const photo = (name, size, lastModified) => ({ name, size, lastModified, type: 'image/png' });

test('连续选择照片时追加到原列表并按文件信息去重', () => {
  const first = photo('first.png', 100, 1);
  const second = photo('second.png', 200, 2);

  assert.deepEqual(mergeUniquePhotoFiles([first], [second, first]), [first, second]);
});

test('待新增照片可以逐张移除', () => {
  const first = photo('first.png', 100, 1);
  const second = photo('second.png', 200, 2);

  assert.deepEqual(removePhotoFile([first, second], first), [second]);
});
