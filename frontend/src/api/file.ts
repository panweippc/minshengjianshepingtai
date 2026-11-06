import { get, post, uploadConfig } from './request'
import type { Result } from '@/types/common'

/** 文件上传结果（后端 FileUploadVO） */
export interface FileUploadVO {
  fileName: string
  fileKey: string
  bucket: string
  fileSize: number
  fileHash: string
}

/** 上传文件到正式归档桶（大文件单独超时） */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<Result<FileUploadVO>>('/api/file/upload', formData, uploadConfig)
}

/** 获取时效签名URL（download=true 强制下载，false 浏览器内联预览） */
export function getPresignUrl(params: {
  bucket: string
  fileKey: string
  fileName?: string
  download?: boolean
}) {
  return get<Result<string>>('/api/file/presign', { params })
}
