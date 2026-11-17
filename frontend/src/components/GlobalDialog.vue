<template>
  <ElDialog
    v-bind="$attrs"
    :model-value="modelValue"
    @update:model-value="handleUpdate"
  >
    <template v-for="name in forwardSlots" :key="name" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
    <template #footer>
      <slot name="footer" />
      <el-button v-if="!hasFooterAction()" @click="handleUpdate(false)">关闭</el-button>
    </template>
  </ElDialog>
</template>

<script setup lang="ts">
import { ElDialog } from 'element-plus'
import { useSlots } from 'vue'

defineOptions({ name: 'GlobalElDialog', inheritAttrs: false })

defineProps<{ modelValue?: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const slots = useSlots()
/** 除 footer 外的插槽原样转发，footer 由本组件统一渲染 */
const forwardSlots = Object.keys(slots).filter((name) => name !== 'footer')

function handleUpdate(value: boolean): void {
  emit('update:modelValue', value)
}

/** 递归提取插槽节点中的文本，用于判断底部是否已经存在“关闭”按钮 */
function extractText(node: unknown, depth = 0): string {
  if (node == null || depth > 20) return ''
  if (typeof node === 'string') return node
  if (typeof node === 'number') return String(node)
  if (Array.isArray(node)) return node.map((item) => extractText(item, depth + 1)).join('')
  if (typeof node !== 'object') return ''
  const children = (node as { children?: unknown }).children
  if (typeof children === 'string') return children
  if (Array.isArray(children)) return extractText(children, depth + 1)
  if (children && typeof children === 'object') {
    return Object.values(children as Record<string, unknown>)
      .filter((child): child is () => unknown => typeof child === 'function')
      .map((child) => extractText(child(), depth + 1))
      .join('')
  }
  return ''
}

/** 底部已有“关闭/取消/确定/保存/提交/确认”等操作按钮时不追加关闭按钮：
 *  编辑、新增类弹窗保留自身的取消/确定按钮，避免重复；仅查看类弹窗自动补“关闭”按钮 */
function hasFooterAction(): boolean {
  const footer = slots.footer
  if (!footer) return false
  const text = extractText(footer())
  return ['关闭', '取消', '确定', '保存', '提交', '确认'].some((keyword) => text.includes(keyword))
}
</script>
