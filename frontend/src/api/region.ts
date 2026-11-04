import { get } from './request'
import type { Result } from '@/types/common'
import type { RegionVO } from '@/types/system'

/** 行政区划列表 */
export function getRegionList() {
  return get<Result<RegionVO[]>>('/api/system/region/list')
}
