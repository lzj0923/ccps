import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/components/SidebarNav.vue', import.meta.url), 'utf8')

test('管理端側邊欄使用固定 CCPS 品牌字樣，不把 Logo 當作可翻譯文案', () => {
  assert.match(source, /<div class="crest"[^>]*>CC<\/div>/)
  assert.doesNotMatch(source, /t_c5a976de7b52/)
})
