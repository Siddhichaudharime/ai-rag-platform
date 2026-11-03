import {
  type ChangeEvent,
  type FormEvent,
  useEffect,
  useRef,
  useState,
} from "react"

import {
  getDocuments,
  streamChatMessage,
  uploadDocument,
} from "./api"

import type { RagDocument, SourceInfo } from "./types"

type Message = {
  id: string
  role: "user" | "assistant"
  content: string
  sources?: SourceInfo[]
}

function Icon({
  name,
  size = 18,
}: {
  name: "spark" | "chat" | "file" | "plus" | "send" | "upload" | "clip" | "check" | "search"
  size?: number
}) {
  const p = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.8,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
  }

  if (name === "spark") return (
    <svg {...p}>
      <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />
      <path d="m19 16 .7 2.3L22 19l-2.3.7L19 22l-.7-2.3L16 19l2.3-.7L19 16Z" />
    </svg>
  )

  if (name === "chat") return (
    <svg {...p}>
      <path d="M20 11.5A7.5 7.5 0 0 1 12 19a8.8 8.8 0 0 1-4-1L4 20l1.2-3.5A7.3 7.3 0 0 1 4.5 12 7.5 7.5 0 0 1 12 4.5a7.5 7.5 0 0 1 8 7Z" />
    </svg>
  )

  if (name === "file") return (
    <svg {...p}>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5Z" />
      <path d="M14 3v5h5" />
    </svg>
  )

  if (name === "plus") return (
    <svg {...p}>
      <path d="M12 5v14M5 12h14" />
    </svg>
  )

  if (name === "send") return (
    <svg {...p}>
      <path d="m4 4 16 8-16 8 3-8-3-8Z" />
      <path d="M7 12h13" />
    </svg>
  )

  if (name === "upload") return (
    <svg {...p}>
      <path d="M12 16V4" />
      <path d="m7 9 5-5 5 5" />
      <path d="M5 20h14" />
    </svg>
  )

  if (name === "clip") return (
    <svg {...p}>
      <path d="m20 11-7.5 7.5a5 5 0 0 1-7-7L13 4a3.5 3.5 0 1 1 5 5l-7.5 7.5a2 2 0 1 1-2.8-2.8l7-7" />
    </svg>
  )

  if (name === "check") return (
    <svg {...p}>
      <path d="m5 12 4 4L19 6" />
    </svg>
  )

  return (
    <svg {...p}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m16 16 4 4" />
    </svg>
  )
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function App() {
  const [documents, setDocuments] = useState<RagDocument[]>([])
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "assistant",
      content:
        "Hi! I'm your local document assistant. Upload a file and ask me anything about its contents.",
    },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState("")
  const inputRef = useRef<HTMLInputElement>(null)
  const chatRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    void refreshDocuments()
  }, [])

  useEffect(() => {
    chatRef.current?.scrollTo({
      top: chatRef.current.scrollHeight,
      behavior: "smooth",
    })
  }, [messages, loading])

  async function refreshDocuments() {
    try {
      setDocuments(await getDocuments())
    } catch {
      setError("Could not load documents.")
    }
  }

  async function handleUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ""

    if (!file) return

    const valid = [".pdf", ".docx", ".txt", ".md"].some((x) =>
      file.name.toLowerCase().endsWith(x),
    )

    if (!valid) {
      setError("Supported files: PDF, DOCX, TXT and MD.")
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      setError("Maximum file size is 10 MB.")
      return
    }

    try {
      setError("")
      setUploading(true)
      const result = await uploadDocument(file)

      setDocuments((current) => [
        result,
        ...current.filter((item) => item.id !== result.id),
      ])
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed.")
    } finally {
      setUploading(false)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    const question = input.trim()
    if (!question || loading) return

    setInput("")
    setError("")

    const userId = crypto.randomUUID()
    const assistantId = crypto.randomUUID()

    setMessages((current) => [
      ...current,
      {
        id: userId,
        role: "user",
        content: question,
      },
      {
        id: assistantId,
        role: "assistant",
        content: "",
        sources: [],
      },
    ])

    try {
      setLoading(true)

      await streamChatMessage(
        question,
        (token) => {
          setMessages((current) =>
            current.map((message) =>
              message.id === assistantId
                ? {
                    ...message,
                    content: message.content + token,
                  }
                : message,
            ),
          )
        },
        (sources) => {
          setMessages((current) =>
            current.map((message) =>
              message.id === assistantId
                ? {
                    ...message,
                    sources,
                  }
                : message,
            ),
          )
        },
      )
    } catch (err) {
      setMessages((current) =>
        current.filter((message) => message.id !== assistantId),
      )
      setError(err instanceof Error ? err.message : "Chat failed.")
    } finally {
      setLoading(false)
    }
  }

  function newChat() {
    setMessages([
      {
        id: "welcome",
        role: "assistant",
        content:
          "New chat started. Ask me something about your indexed documents.",
      },
    ])
    setInput("")
    setError("")
  }

  const readyCount = documents.filter(
    (document) => document.status === "COMPLETED",
  ).length

  return (
    <div className="app">
      <aside className="sidebar">
        <div>
          <div className="brand">
            <div className="brand-logo">
              <Icon name="spark" size={19} />
            </div>

            <div>
              <strong>RAG Studio</strong>
              <span>Local document intelligence</span>
            </div>
          </div>

          <button className="new-chat" onClick={newChat}>
            <Icon name="plus" size={16} />
            New chat
            <span>⌘ K</span>
          </button>

          <div className="section-title">WORKSPACE</div>

          <button className="nav active">
            <Icon name="chat" size={16} />
            Document chat
          </button>

          <div className="nav">
            <Icon name="file" size={16} />
            Documents
            <b>{documents.length}</b>
          </div>

          <div className="section-row">
            <span>YOUR FILES</span>
            <button
              className="small-plus"
              onClick={() => inputRef.current?.click()}
            >
              <Icon name="plus" size={14} />
            </button>
          </div>

          <div className="file-list">
            {documents.length === 0 && (
              <div className="empty-files">
                <Icon name="file" size={17} />
                No documents yet
              </div>
            )}

            {documents.map((document) => (
              <div className="file-item" key={document.id}>
                <div className="file-icon">
                  <Icon name="file" size={15} />
                </div>

                <div className="file-info">
                  <strong title={document.fileName}>
                    {document.fileName}
                  </strong>
                  <span>
                    {document.status === "COMPLETED"
                      ? "Ready"
                      : document.status}
                    {" · "}
                    {formatSize(document.fileSize)}
                  </span>
                </div>

                <i
                  className={
                    document.status === "COMPLETED"
                      ? "status ready"
                      : "status"
                  }
                />
              </div>
            ))}
          </div>
        </div>

        <div>
          <div className="system-card">
            <div className="system-head">
              <span>
                <i className="online-dot" />
                Local AI online
              </span>
            </div>

            <div className="model-line">
              <span>Model</span>
              <strong>Qwen3 0.6B</strong>
            </div>

            <small>Ollama · PGVector · PostgreSQL</small>
          </div>

          <div className="workspace-user">
            <div>R</div>
            <span>
              <strong>RAG Workspace</strong>
              Local environment
            </span>
          </div>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div>
            <small>DOCUMENT INTELLIGENCE</small>
            <span>Document chat</span>
          </div>

          <div className="top-actions">
            <div className="pill">
              <i />
              {readyCount} ready
            </div>
            <div className="pill">
              <Icon name="spark" size={13} />
              Local AI
            </div>
          </div>
        </header>

        <div className="content">
          <section className="hero">
            <div className="hero-copy">
              <div className="badge">
                <i />
                RAG ACTIVE
              </div>

              <h1>
                Ask questions.
                <br />
                <em>Get grounded answers.</em>
              </h1>

              <p>
                Search your indexed documents and get precise answers from
                your local AI. Your files stay on your machine.
              </p>

              <div className="hero-metrics">
                <div>
                  <strong>{documents.length}</strong>
                  <span>Documents</span>
                </div>
                <div>
                  <strong>{readyCount}</strong>
                  <span>Indexed</span>
                </div>
                <div>
                  <strong>Local</strong>
                  <span>AI processing</span>
                </div>
              </div>
            </div>

            <div className="orb">
              <div className="orb-ring ring-a" />
              <div className="orb-ring ring-b" />
              <div className="orb-ring ring-c" />
              <div className="orb-core">
                <Icon name="spark" size={25} />
              </div>
            </div>
          </section>

          <section className="chat-card">
            <div className="chat-header">
              <div className="assistant-info">
                <div className="assistant-logo">
                  <Icon name="spark" size={17} />
                </div>
                <div>
                  <strong>AI Document Assistant</strong>
                  <span>Qwen3 0.6B · Local RAG</span>
                </div>
              </div>

              <div className="online">
                <i />
                Online
              </div>
            </div>

            <div className="messages" ref={chatRef}>
              {messages.map((message) => (
                <div
                  className={`message-row ${message.role}`}
                  key={message.id}
                >
                  {message.role === "assistant" && (
                    <div className="message-avatar">
                      <Icon name="spark" size={13} />
                    </div>
                  )}

                  <div className="message-content">
                    <small>
                      {message.role === "assistant"
                        ? "RAG ASSISTANT"
                        : "YOU"}
                    </small>

                    <div className="bubble">
                      {message.content || (
                        <span className="typing">
                          <i />
                          <i />
                          <i />
                        </span>
                      )}
                    </div>

                    {message.sources &&
                      message.sources.length > 0 && (
                        <div className="sources">
                          <label>GROUNDED IN</label>

                          {Array.from(
                            new Map(
                              message.sources.map((source, index) => [
                                `${source.fileName}-${source.page ?? index}`,
                                source,
                              ]),
                            ).values(),
                          ).map((source, index) => (
                            <div
                              className="source"
                              key={`${source.documentId ?? "source"}-${index}`}
                            >
                              <div className="source-icon">
                                <Icon name="file" size={13} />
                              </div>

                              <span>
                                <strong>{source.fileName}</strong>
                                <small>
                                  {source.page !== null &&
                                  source.page !== undefined
                                    ? `Page ${String(source.page)}`
                                    : "Indexed document"}
                                </small>
                              </span>

                              <Icon name="check" size={12} />
                            </div>
                          ))}
                        </div>
                      )}
                  </div>
                </div>
              ))}

              {loading && (
                <div className="generating">
                  <i />
                  Generating grounded answer
                </div>
              )}
            </div>

            {error && (
              <div className="error">
                <span>!</span>
                {error}
                <button onClick={() => setError("")}>×</button>
              </div>
            )}

            <form className="composer" onSubmit={handleSubmit}>
              <textarea
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (
                    event.key === "Enter" &&
                    !event.shiftKey
                  ) {
                    event.preventDefault()
                    event.currentTarget.form?.requestSubmit()
                  }
                }}
                placeholder="Ask anything about your documents..."
                rows={1}
              />

              <button
                type="button"
                className="clip"
                onClick={() => inputRef.current?.click()}
                aria-label="Attach document"
              >
                <Icon name="clip" size={16} />
              </button>

              <button
                className="send"
                type="submit"
                disabled={!input.trim() || loading}
                aria-label="Send message"
              >
                <Icon name="send" size={15} />
              </button>
            </form>

            <div className="composer-footer">
              <span>Enter to send · Shift + Enter for new line</span>
              <span>
                <Icon name="spark" size={10} />
                Grounded responses only
              </span>
            </div>
          </section>

          <section className="upload-card">
            <div className="upload-icon">
              <Icon name="upload" size={18} />
            </div>

            <div className="upload-copy">
              <strong>
                {uploading
                  ? "Indexing document..."
                  : "Add another document"}
              </strong>
              <span>
                PDF, DOCX, TXT or MD · maximum 10 MB
              </span>
            </div>

            <button
              className="upload-button"
              onClick={() => inputRef.current?.click()}
              disabled={uploading}
            >
              {uploading ? "Processing..." : "Choose file"}
              {!uploading && <span>→</span>}
            </button>
          </section>

          <div className="suggestions">
            <span>Try asking</span>
            <button onClick={() => setInput("Summarize this document")}>
              Summarize this document
            </button>
            <button onClick={() => setInput("What are the key points?")}>
              What are the key points?
            </button>
            <button
              onClick={() => setInput("What technologies are mentioned?")}
            >
              What technologies are mentioned?
            </button>
          </div>
        </div>
      </main>

      <input
        ref={inputRef}
        hidden
        type="file"
        accept=".pdf,.docx,.txt,.md"
        onChange={handleUpload}
      />
    </div>
  )
}

export default App