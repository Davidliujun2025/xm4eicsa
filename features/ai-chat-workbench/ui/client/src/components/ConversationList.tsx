import { useMemo, useState } from "react";
import { FILTERS, platformFor } from "../config/workbench";
import type { Conversation } from "../types";
import { Search, Plus } from "./icons";

type Props = {
  conversations: Conversation[];
  activeConversationId?: string;
  loading: boolean;
  onSelect: (conversation: Conversation) => void;
  onNewConversation: () => void;
};

function formatTime(value?: string) {
  if (!value) return "--";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const today = new Date();
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
  }
  return date.toLocaleDateString("zh-CN", { month: "numeric", day: "numeric" });
}

function categoryOf(conversation: Conversation) {
  return conversation.messages?.at(-1)?.customerType || "其他";
}

export default function ConversationList({
  conversations,
  activeConversationId,
  loading,
  onSelect,
  onNewConversation,
}: Props) {
  const [filter, setFilter] = useState<string>("全部");
  const [keyword, setKeyword] = useState("");
  const list = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return conversations.filter((conversation) => {
      const matchesFilter = filter === "全部" || categoryOf(conversation) === filter;
      const matchesKeyword = !normalizedKeyword
        || `${conversation.title} ${conversation.platform}`.toLowerCase().includes(normalizedKeyword);
      return matchesFilter && matchesKeyword;
    });
  }, [conversations, filter, keyword]);

  return (
    <section className="flex min-h-0 flex-col rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between px-1">
        <span className="text-[16px] font-bold text-slate-800">对话列表</span>
        <button
          type="button"
          onClick={onNewConversation}
          className="flex items-center gap-1 text-[13px] font-medium text-blue-600 transition-opacity hover:opacity-70"
        >
          <Plus className="h-4 w-4" />
          新建对话
        </button>
      </div>

      <div className="relative mt-3">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-slate-400" />
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索对话内容"
          className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-2.5 pl-9 pr-3 text-[13px] outline-none transition-all placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-100"
        />
      </div>

      <div className="mt-3 flex gap-1.5">
        {FILTERS.map((item) => (
          <button
            key={item}
            type="button"
            onClick={() => setFilter(item)}
            className={`rounded-full px-3 py-1.5 text-[12.5px] font-medium transition-all duration-200 ${
              filter === item ? "bg-blue-100 text-blue-700" : "text-slate-500 hover:bg-slate-100"
            }`}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="mt-2 min-h-0 flex-1 space-y-0.5 overflow-y-auto pr-1">
        {loading && <p className="px-3 py-8 text-center text-[13px] text-slate-400">正在加载数据库会话…</p>}
        {!loading && list.length === 0 && (
          <p className="px-3 py-8 text-center text-[13px] text-slate-400">暂无符合条件的会话</p>
        )}
        {list.map((conversation) => (
          <button
            key={conversation.conversationId}
            type="button"
            onClick={() => onSelect(conversation)}
            className={`group relative block w-full rounded-xl px-3 py-3 text-left transition-all duration-200 ${
              activeConversationId === conversation.conversationId ? "bg-blue-50/80" : "hover:bg-slate-50"
            }`}
          >
            {activeConversationId === conversation.conversationId && (
              <span className="absolute left-0 top-1/2 h-7 w-[3px] -translate-y-1/2 rounded-r bg-blue-600" />
            )}
            <div className="flex items-start justify-between gap-2">
              <span className="truncate text-[13.5px] font-semibold text-slate-800">{conversation.title || "未命名对话"}</span>
              <span className="shrink-0 text-[11.5px] text-slate-400">
                {formatTime(conversation.updatedAt || conversation.createdAt)}
              </span>
            </div>
            <div className="mt-1.5 flex items-center gap-2 text-[11.5px]">
              <span className="font-medium text-blue-600">{platformFor(conversation.platform).name}</span>
              <span className="text-slate-300">|</span>
              <span className="text-slate-400">{categoryOf(conversation)}</span>
            </div>
          </button>
        ))}
      </div>

      <button
        type="button"
        onClick={() => window.location.assign(`${window.location.pathname}?view=history`)}
        className="mt-2 py-2 text-center text-[13px] font-medium text-blue-600 transition-opacity hover:opacity-70"
      >
        查看全部对话（{conversations.length}）
      </button>
    </section>
  );
}
