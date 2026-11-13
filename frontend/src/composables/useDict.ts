import { reactive, onMounted } from 'vue'
import { getDictByType } from '@/api/dict'
import type { DictVO } from '@/types/system'

/**
 * 字典缓存 composable：一次性预加载常用字典类型，避免每个页面重复调后端。
 * 页面里用：const { dictOptions, dictLabel } = useDict()
 * 支持延迟按需加载（首次访问某个 dictType 才请求，之后缓存复用）。
 */
const dictMap = reactive<Record<string, DictVO[]>>({})
const loadingMap = reactive<Record<string, boolean>>({})
const fetchedTypes = new Set<string>()

async function ensureLoaded(type: string) {
  if (fetchedTypes.has(type) || loadingMap[type]) return
  loadingMap[type] = true
  try {
    const res: any = await getDictByType(type)
    dictMap[type] = res?.data || []
    fetchedTypes.add(type)
  } catch {
    dictMap[type] = []
  } finally {
    loadingMap[type] = false
  }
}

/** 首次访问某个字典类型时触发加载（Promise 不阻塞） */
function preload(types: string[]) {
  types.forEach(t => { if (!fetchedTypes.has(t)) ensureLoaded(t) })
}

export function useDict(preloadTypes?: string[]) {
  if (preloadTypes && preloadTypes.length) preload(preloadTypes)

  /** 返回某个字典类型的全部字典项（按排序字段排序） */
  function dictOptions(type: string): DictVO[] {
    ensureLoaded(type)
    return dictMap[type] || []
  }

  /** 字典值 → 中文 label（找不到返回原值） */
  function dictLabel(type: string, value?: string | null): string {
    if (!value) return ''
    const list = dictOptions(type)
    const hit = list.find((item: any) => item.dictValue === value)
    return hit?.dictLabel || value
  }

  return { dictOptions, dictLabel, preload }
}

/**
 * 应用启动时预加载全量常用字典（main.ts / App.vue 里调一次）
 * 避免每个页面首次打开都有一次字典请求。
 */
export function preloadCommonDicts() {
  const common = [
    'xiang_mu_lei_bie',
    'xiang_mu_lei_xing',
    'zi_jin_lei_xing',
    'xia_bo_lei_xing',
    'zi_jin_lai_yuan',
    'shen_bao_lei_bie',
    'shen_bao_lei_xing',
    'fu_jian_lei_xing',
    'li_xiang_fu_jian_lei_xing',
    'shi_gong_fu_jian_lei_xing',
    'sheng_chan_tou_zi_qian_shu'
  ]
  preload(common)
  onMounted(() => { preload(common) })
}
