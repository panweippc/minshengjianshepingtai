import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { DictVO, DictSaveDTO } from '@/types/system'

/** 字典分页查询入参 */
export interface DictQuery extends PageQuery {
  dictType?: string
  description?: string
  dictLabel?: string
}

/** 字典分页查询 */
export function getDictPage(params: DictQuery) {
  return get<Result<PageVO<DictVO>>>('/api/system/dict/page', { params })
}

/** 全部字典类型（筛选下拉用） */
export function getDictTypes() {
  return get<Result<string[]>>('/api/system/dict/types')
}

/** 按字典类型查询字典项（业务下拉联动用，如 qu_hua_lei_xing） */
export function getDictByType(dictType: string) {
  return get<Result<DictVO[]>>(`/api/system/dict/type/${encodeURIComponent(dictType)}`)
}

/** 新增字典 */
export function createDict(data: DictSaveDTO) {
  return post<Result<void>>('/api/system/dict', data)
}

/** 编辑字典 */
export function updateDict(data: DictSaveDTO) {
  return put<Result<void>>('/api/system/dict', data)
}

/** 删除字典 */
export function deleteDict(id: number) {
  return del<Result<void>>(`/api/system/dict/${id}`)
}
