import { get } from './request'
import type { Result } from '@/types/common'

/** 后端健康检查返回数据 */
export interface HealthData {
  status: string
  service: string
  time: string
}

/** 健康检查 */
export function healthCheck() {
  return get<Result<HealthData>>('/api/test/health')
}
