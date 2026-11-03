export type DocumentStatus =
  | "UPLOADED"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED"

export interface RagDocument {
  id: string
  fileName: string
  contentType: string
  fileSize: number
  status: DocumentStatus
  createdAt: string
}

export interface SourceInfo {
  fileName: string
  page: unknown
  documentId: string | null
}

export interface ChatResponse {
  answer: string
  sources: SourceInfo[]
}