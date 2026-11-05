import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { VillageVO, VillageTreeVO, VillageSaveDTO } from '@/types/system'

/** 村委会分页查询入参 */
export interface VillageQuery extends PageQuery {
  name?: string
  secretary?: string
  director?: string
  nodeCode?: string
}

/** 乡镇/村/组区划树 */
export function getVillageTree() {
  return get<Result<VillageTreeVO[]>>('/api/base/village/tree')
}

/** 村委会分页查询 */
export function getVillagePage(params: VillageQuery) {
  return get<Result<PageVO<VillageVO>>>('/api/base/village/page', { params })
}

/** 区划详情（按区划编码查询） */
export function getVillageDetail(code: string) {
  return get<Result<VillageVO>>(`/api/base/village/detail/${encodeURIComponent(code)}`)
}

/** 新增区划（含添加下级） */
export function createVillage(data: VillageSaveDTO) {
  return post<Result<number>>('/api/base/village', data)
}

/** 编辑区划 */
export function updateVillage(data: VillageSaveDTO) {
  return put<Result<void>>('/api/base/village', data)
}

/** 删除区划 */
export function deleteVillage(code: string) {
  return del<Result<void>>(`/api/base/village/${encodeURIComponent(code)}`)
}
