import { get, post, put, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { FacilityVO, FacilityDetailVO, FacilitySaveDTO, RegionFacilityCountVO } from '@/types/system'

/** 公共基础设施分页查询入参 */
export interface FacilityQuery extends PageQuery {
  facilityCode?: string
  facilityName?: string
  facilityType?: string
  nodeCode?: string
}

/** 公共基础设施分页查询 */
export function getFacilityPage(params: FacilityQuery) {
  return get<Result<PageVO<FacilityVO>>>('/api/base/facility/page', { params })
}

/** 设施详情（含附件和关联项目） */
export function getFacilityDetail(id: string) {
  return get<Result<FacilityDetailVO>>(`/api/base/facility/detail/${id}`)
}

/** 各区划设施数量统计（左侧区划树徽标） */
export function getFacilityRegionCounts() {
  return get<Result<RegionFacilityCountVO[]>>('/api/base/facility/region-counts')
}

/** 新增设施 */
export function createFacility(data: FacilitySaveDTO) {
  return post<Result<number>>('/api/base/facility', data)
}

/** 编辑设施 */
export function updateFacility(data: FacilitySaveDTO) {
  return put<Result<void>>('/api/base/facility', data)
}

/** 删除设施 */
export function deleteFacility(id: string) {
  return del<Result<void>>(`/api/base/facility/${id}`)
}
