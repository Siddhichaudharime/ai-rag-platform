import type {
  RagDocument,
  SourceInfo,
} from "./types"

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ??
  "http://localhost:8080"

async function request<T>(
  url: string,
  options?: RequestInit,
): Promise<T> {
  const response = await fetch(
    `${API_BASE_URL}${url}`,
    options,
  )

  const text = await response.text()

  let data: unknown = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = text
    }
  }

  if (!response.ok) {
    const message =
      typeof data === "object" &&
      data !== null &&
      "message" in data &&
      typeof data.message === "string"
        ? data.message
        : `Request failed (${response.status})`

    throw new Error(message)
  }

  return data as T
}

export function getDocuments(): Promise<RagDocument[]> {
  return request<RagDocument[]>("/api/documents")
}

export function uploadDocument(
  file: File,
): Promise<RagDocument> {
  const formData = new FormData()
  formData.append("file", file)

  return request<RagDocument>(
    "/api/documents/upload",
    {
      method: "POST",
      body: formData,
    },
  )
}

export async function streamChatMessage(
  message: string,
  onToken: (token: string) => void,
  onSources: (sources: SourceInfo[]) => void,
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/chat`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        message,
      }),
    },
  )

  let data: unknown = null

  try {
    data = await response.json()
  } catch {
    throw new Error(
      `Request failed (${response.status})`,
    )
  }

  if (!response.ok) {
    const messageText =
      typeof data === "object" &&
      data !== null &&
      "message" in data &&
      typeof data.message === "string"
        ? data.message
        : `Request failed (${response.status})`

    throw new Error(messageText)
  }

  const result = data as {
    answer?: string
    sources?: SourceInfo[]
  }

  if (result.answer) {
    onToken(result.answer)
  }

  if (Array.isArray(result.sources)) {
    onSources(result.sources)
  }
}