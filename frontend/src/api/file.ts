import { getBlob, post, uploadConfig } from './request'
import type { Result } from '@/types/common'
import type { FileUploadVO } from '@/types/system'

/** 上传单个文件（本地磁盘存储，需登录鉴权） */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<Result<FileUploadVO>>('/api/file/upload', formData, {
    ...uploadConfig,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 获取附件文件流（预览/下载，需登录鉴权） */
export function fetchFileBlob(path: string) {
  return getBlob(`/api/file/preview?path=${encodeURIComponent(path)}`)
}
