import { useState } from "react";
import { CONVS, FILTERS, type Conv } from "../mock/data";
import { Search, Plus } from "./icons";

type Props = {
  platformName: string;
  onSelectConversation: (conv: Conv) => void;
};

export default function ConversationList({ platformName, onSelectConversation }: Props) {
  const [filter, setFilter] = useState<string>("全部");
  const [active, setActive] = useState(0);
  const [searchTerm, setSearchTerm] = useState<string>("");

  const filteredByTag = CONVS.filter((c) => filter === "全部" || c.tag === filter);
  const filtered = filteredByTag.filter((c) => {
    if (!searchTerm.trim()) return true;
    const lower = searchTerm.toLowerCase();
    const matchTitle = c.t.toLowerCase().includes(lower);
    const matchMessages = c.messages?.some((m) => m.content.toLowerCase().includes(lower)) || false;
    return matchTitle || matchMessages;
  });

  const handleSelect = (conv: Conv, index: number) => {
    setActive(index);
    onSelectConversation({
      ...conv,
      platform: conv.platform || platformName,
    });
  };

  return (
    <section className="flex min-h-0 flex-col rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between px-1">
        <span className="text-[16px] font-bold text-slate-800">对话列表</span>
        <button className="flex items-center gap-1 text-[13px] font-medium text-blue-600 transition-opacity hover:opacity-70">
          <Plus className="h-4 w-4" />
          新建对话
        </button>
      </div>

      <div className="relative mt-3">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-slate-400" />
        <input
          placeholder="搜索对话内容"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full rounded-xl border border-slate-200 bg-slate-50/70 py-2.5 pl-9 pr-3 text-[13px] outline-none transition-all placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-100"
        />
      </div>

      <div className="mt-3 flex gap-1.5">
        {FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`rounded-full px-3 py-1.5 text-[12.5px] font-medium transition-all duration-200 ${
              filter === f ? "bg-blue-100 text-blue-700" : "text-slate-500 hover:bg-slate-100"
            }`}
          >
            {f}
          </button>
        ))}
      </div>

      <div className="mt-2 min-h-0 flex-1 space-y-0.5 overflow-y-auto pr-1">
        {filtered.length === 0 ? (
          <div className="py-8 text-center text-[13px] text-slate-400">没有找到匹配的对话</div>
        ) : (
          filtered.map((c, i) => (
            <button
              key={c.t + c.time}
              onClick={() => handleSelect(c, i)}
              className={`group relative block w-full rounded-xl px-3 py-3 text-left transition-all duration-200 ${
                i === active ? "bg-blue-50/80" : "hover:bg-slate-50"
              }`}
            >
              {i === active && <span className="absolute left-0 top-1/2 h-7 w-[3px] -translate-y-1/2 rounded-r bg-blue-600" />}
              <div className="flex items-start justify-between gap-2">
                <span className={`text-[13.5px] font-semibold ${i === active ? "text-slate-900" : "text-slate-700"}`}>
                  {c.t}
                </span>
                <span className="shrink-0 text-[11.5px] text-slate-400">{c.time}</span>
              </div>
              <div className="mt-1.5 flex items-center gap-2 text-[11.5px]">
                <span className="font-medium text-blue-600">{c.platform || platformName}</span>
                <span className="text-slate-300">|</span>
                <span className="text-slate-400">{c.tag}</span>
              </div>
            </button>
          ))
        )}
      </div>

      <button className="mt-2 py-2 text-center text-[13px] font-medium text-blue-600 transition-opacity hover:opacity-70">
        查看全部对话（{CONVS.length}）
      </button>
    </section>
  );
}