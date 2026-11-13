import { get, put, getBlob } from './request'
import type { PageQuery, PageVO, Result } from '@/types/common'
import type { ApprovalVO } from '@/api/approval'
import type { FundNodeVO } from '@/api/fund'
import type { ConstructionNodeVO } from '@/api/construction'
import type { AcceptanceNodeVO } from '@/api/acceptance'

/** 档案附件 */
export interface ArchiveAttachmentVO {
  id?: string
  /** 附件类型 */
  attachmentType?: string
  /** 原始文件名 */
  fileName?: string
  /** MinIO 对象存储 key */
  fileKey?: string
  /** 存储桶 */
  bucket?: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 备注 */
  remark?: string
  /** 上传时间 */
  createTime?: string
}

/** 档案列表项 */
export interface ArchiveVO {
  /** 档案ID */
  id: string
  /** 立项ID */
  approvalId: string
  /** 档案编号 */
  archiveNo?: string
  /** 存储位置 */
  storageLocation?: string
  /** 归档时间 */
  archiveTime?: string
  /** 归档人 */
  archiveBy?: string
  /** 项目编号 */
  approvalCode?: string
  /** 项目名称 */
  projectName?: string
  /** 项目类别（字典 xiang_mu_lei_bie） */
  projectCategory?: string
  /** 项目类型（字典 xiang_mu_lei_xing） */
  projectType?: string
  /** 项目负责人 */
  leader?: string
}

/** 申报明细（农户/公共基础设施） */
export interface ArchiveDeclareDetailVO {
  id: string
  /** HOUSEHOLD农户 / FACILITY公共基础设施 */
  detailType?: string
  /** 户号/设施编码 */
  code?: string
  /** 户主/设施名称 */
  name?: string
  /** 户主身份证号（仅农户） */
  idCard?: string
  /** 联系电话（仅农户） */
  phone?: string
  /** 户类型（字典 hu_lei_xing）/设施类型（字典 she_shi_lei_xing） */
  typeValue?: string
  /** 所属村组 */
  regionName?: string
  /** 事项说明 */
  remark?: string
  /** 申报金额（元） */
  amount?: number
  /** 明细附件 */
  attachments?: ArchiveAttachmentVO[]
}

/** 事项申报节点 */
export interface ArchiveDeclareVO {
  /** 立项-申报关联ID */
  id: string
  /** 申报单ID */
  declarationId?: string
  /** 申报编号 */
  declareCode?: string
  /** 申报标题 */
  declareName?: string
  /** 申报类型（字典 shen_bao_lei_xing） */
  declareType?: string
  /** 申报类别（字典 shen_bao_lei_bie） */
  declareCategory?: string
  /** 申报金额（元） */
  amount?: number
  /** 申报日期 */
  declarationDate?: string
  /** 所属乡镇 */
  townName?: string
  /** 所属村组 */
  villageName?: string
  /** 备注说明 */
  remark?: string
  /** 申报材料附件 */
  attachments?: ArchiveAttachmentVO[]
  /** 申报明细 */
  details?: ArchiveDeclareDetailVO[]
}

/** 档案查看详情（六页签） */
export interface ArchiveDetailVO {
  archive: ArchiveVO
  approval: ApprovalVO
  approvalAttachments: ArchiveAttachmentVO[]
  declares: ArchiveDeclareVO[]
  fund?: FundNodeVO
  construction?: ConstructionNodeVO
  acceptance?: AcceptanceNodeVO
}

/** 档案查询参数 */
export interface ArchiveQuery extends PageQuery {
  projectName?: string
  /** 项目类别（字典 xiang_mu_lei_bie） */
  projectCategory?: string
  /** 项目类型（字典 xiang_mu_lei_xing） */
  projectType?: string
}

/** 档案编辑入参 */
export interface ArchiveEditDTO {
  id: string
  /** 存储位置 */
  storageLocation?: string
  /** 归档时间 yyyy-MM-dd HH:mm:ss */
  archiveTime: string
}

/** 档案分页查询 */
export function getArchivePage(params: ArchiveQuery) {
  return get<Result<PageVO<ArchiveVO>>>('/api/project/archive/page', { params })
}

/** 档案查看（六页签数据聚合） */
export function getArchiveDetail(id: string) {
  return get<Result<ArchiveDetailVO>>(`/api/project/archive/detail/${id}`)
}

/** 编辑档案位置信息 */
export function updateArchive(data: ArchiveEditDTO) {
  return put<Result<void>>('/api/project/archive', data)
}

/** 导出项目全部资料（压缩包二进制流） */
export function exportArchive(id: string) {
  return getBlob(`/api/project/archive/export/${id}`)
}
