import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const component = await readFile(
  new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url),
  'utf8',
)
const rentalSigningComponent = await readFile(
  new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url),
  'utf8',
)

async function listVueFiles(directory) {
  const files = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) files.push(...await listVueFiles(path))
    else if (entry.isFile() && entry.name.endsWith('.vue')) files.push(path)
  }
  return files
}

test('工作台弹窗仅在按下遮罩时关闭，拖选到遮罩外不会误关闭', () => {
  const modalBackdropClasses = [
    'process-setup-backdrop',
    'rental-checklist-modal',
    'operations-center-modal',
    'operations-work-order-modal',
    'operations-renewal-modal',
  ]

  for (const className of modalBackdropClasses) {
    const tag = component.match(new RegExp(`<div[^>]*class="${className}"[^>]*>`))?.[0] || ''
    assert.ok(tag, `未找到 ${className} 弹窗遮罩`)
    assert.match(tag, /@pointerdown\.self=/, `${className} 应在指针按下时判断遮罩`)
    assert.doesNotMatch(tag, /@click\.self=/, `${className} 不应使用会被拖拽误触的 click.self`)
  }
})

test('附件签约的资料弹窗从内容区向外拖拽时不会误关闭', () => {
  const modalStates = [
    'otrFormOpen',
    'rentalAppointmentFormOpen',
    'authorizationFormOpen',
    'pmaFormOpen',
    'financeDocumentOpen',
  ]

  for (const state of modalStates) {
    const tag = rentalSigningComponent.match(new RegExp(`<div[^>]*v-if="${state}"[^>]*>`))?.[0] || ''
    assert.ok(tag, `未找到 ${state} 弹窗遮罩`)
    assert.match(tag, /@pointerdown\.self=/, `${state} 应在指针按下时判断遮罩`)
    assert.doesNotMatch(tag, /@click\.self=/, `${state} 不应使用会被拖拽误触的 click.self`)
  }
})

test('所有二级界面禁止使用会被拖拽误触的 click.self 遮罩关闭事件', async () => {
  const sourceRoot = fileURLToPath(new URL('../src/', import.meta.url))
  const violations = []

  for (const file of await listVueFiles(sourceRoot)) {
    const source = await readFile(file, 'utf8')
    if (source.includes('@click.self=')) violations.push(relative(sourceRoot, file))
  }

  assert.deepEqual(violations, [], `以下二级界面仍使用 click.self：${violations.join('、')}`)
})
