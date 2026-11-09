import { get, del } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { OperLogVO } from '@/types/system'

/** 操作日志分页查询 */
export function getLogPage(params: PageQuery & { username?: string; module?: string }) {
  return get<Result<PageVO<OperLogVO>>>('/api/system/log/page', { params })
}

/** 批量删除日志 */
export function deleteLogs(ids: string[]) {
  return del<Result<void>>('/api/system/log/batch', { data: ids })
}

/** 清空日志 */
export function clearLogs() {
  return del<Result<void>>('/api/system/log/clear')
}
