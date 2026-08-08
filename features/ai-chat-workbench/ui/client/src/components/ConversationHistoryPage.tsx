import { useCallback, useEffect, useMemo, useState } from "react";
import { platformFor } from "../config/workbench";
import type { Conversation } from "../types";
import { Clock, Search } from "./icons";
import { isHistoryConversation } from "../utils/conversationStatus";

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

function formatTime(value?: string) {
  if (!value) return "--";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
}

export default function ConversationHistoryPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const requestedConversationId = useMemo(
    () => new URLSearchParams(window.location.search).get("conversationId"),
    [],
  );

  const openConversation = useCallback(async (conversation: Conversation) => {
    setSelected(conversation);
    setDetailLoading(true);
    setError("");
    try {
      const response = await fetch(`/api/v1/conversations/${encodeURIComponent(conversation.conversationId)}`, {
        credentials: "include",
      });
      const payload: ApiResponse<Conversation> = await response.json();
      if (!response.ok || payload.code !== 200) {
        throw new Error(payload.message || "对话详情加载失败");
      }
      setSelected(payload.data);
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : "对话详情加载失败");
    } finally {
      setDetailLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    const loadConversations = async () => {
      try {
        const response = await fetch("/api/v1/conversations", {
          credentials: "include",
          signal: controller.signal,
        });
        const payload: ApiResponse<Conversation[]> = await response.json();
        if (!response.ok || payload.code !== 200) {
          throw new Error(payload.message || "对话记录加载失败");
        }
        const loadedConversations = (payload.data || []).filter(isHistoryConversation);
        setConversations(loadedConversations);
        if (requestedConversationId) {
          const requestedConversation = loadedConversations.find(
            (conversation) => conversation.conversationId === requestedConversationId,
          );
          if (requestedConversation) {
            void openConversation(requestedConversation);
          } else {
            setError("未找到指定的对话记录");
          }
        }
      } catch (loadError) {
        if (!controller.signal.aborted) {
          setError(loadError instanceof Error ? loadError.message : "对话记录加载失败");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };

    void loadConversations();
    return () => controller.abort();
  }, [openConversation, requestedConversationId]);

  const filtered = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    if (!normalizedKeyword) return conversations;
    return conversations.filter((conversation) =>
      `${conversation.title} ${conversation.platform}`.toLowerCase().includes(normalizedKeyword),
    );
  }, [conversations, keyword]);

  return (
    <main className="grid min-h-0 flex-1 grid-cols-[360px_minmax(0,1fr)] gap-5 overflow-hidden p-5">
      <section className="flex min-h-0 flex-col rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between px-1">
          <h2 className="text-[16px] font-bold text-slate-800">全部对话</h2>
          <span className="text-[12px] text-slate-400">{conversations.length} 条</span>
        </div>
        <div className="relative mt-3">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-slate-400" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索标题或平台"
            className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-2.5 pl-9 pr-3 text-[13px] outline-none focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-100"
          />
        </div>
        <div className="mt-3 min-h-0 flex-1 space-y-1 overflow-y-auto">
          {loading && <p className="p-4 text-center text-sm text-slate-400">正在加载对话记录…</p>}
          {!loading && filtered.length === 0 && <p className="p-4 text-center text-sm text-slate-400">暂无对话记录</p>}
          {filtered.map((conversation) => (
            <button
              key={conversation.conversationId}
              type="button"
              onClick={() => void openConversation(conversation)}
              className={`w-full rounded-xl px-3 py-3 text-left transition-colors ${
                selected?.conversationId === conversation.conversationId ? "bg-blue-50" : "hover:bg-slate-50"
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <span className="truncate text-[13.5px] font-semibold text-slate-800">{conversation.title || "未命名对话"}</span>
                <span className="shrink-0 text-[11px] text-slate-400">{platformFor(conversation.platform).name}</span>
              </div>
              <div className="mt-2 flex items-center gap-1.5 text-[11.5px] text-slate-400">
                <Clock className="h-3.5 w-3.5" />
                {formatTime(conversation.updatedAt || conversation.createdAt)}
              </div>
            </button>
          ))}
        </div>
      </section>

      <section className="min-h-0 overflow-y-auto rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
        {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
        {!selected && !error && (
          <div className="grid h-full place-items-center text-sm text-slate-400">请选择一条对话查看完整记录</div>
        )}
        {selected && (
          <div>
            <div className="border-b border-slate-100 pb-4">
              <div className="flex items-center gap-3">
                <h2 className="text-xl font-bold text-slate-900">{selected.title || "未命名对话"}</h2>
                <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-600">{platformFor(selected.platform).name}</span>
              </div>
              <p className="mt-2 text-xs text-slate-400">最近更新：{formatTime(selected.updatedAt || selected.createdAt)}</p>
            </div>
            {detailLoading && <p className="py-8 text-center text-sm text-slate-400">正在加载详情…</p>}
            {!detailLoading && (selected.messages || []).length === 0 && (
              <p className="py-8 text-center text-sm text-slate-400">该对话暂无消息</p>
            )}
            {!detailLoading && (selected.messages || []).map((message) => (
              <article key={message.id} className="mt-5 space-y-3 rounded-2xl bg-slate-50 p-5">
                <div>
                  <p className="text-xs font-semibold text-slate-400">客户问题</p>
                  <p className="mt-1 text-sm leading-7 text-slate-700">{message.question}</p>
                </div>
                {message.recommendedScript && (
                  <div className="rounded-xl border border-blue-100 bg-white p-4">
                    <p className="text-xs font-semibold text-blue-600">AI 推荐回复</p>
                    <p className="mt-1 text-sm leading-7 text-slate-700">{message.recommendedScript}</p>
                  </div>
                )}
                <div className="flex gap-4 text-xs text-slate-400">
                  <span>{formatTime(message.createdAt)}</span>
                  {typeof message.totalTokens === "number" && <span>{message.totalTokens} Token</span>}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
