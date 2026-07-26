import { useState, useEffect } from "react";
import { PLATFORMS, NOTES } from "../mock/data";
import { Chev, Check, Spark, Help, Tick } from "./icons";

type Conv = {
  t: string;
  time: string;
  tag: string;
  platform?: string;
  messages?: { role: "user" | "assistant"; content: string; time?: string }[];
};

type Props = {
  selectedConversation: Conv | null;
  onNewConversation: () => void;
  platformId?: string;
  onPlatformChange?: (id: string) => void;
  regen: boolean;
  onRegen: () => void;
};

export default function ChatPanel({ 
  selectedConversation, 
  onNewConversation,
  platformId: initialPlatformId, 
  onPlatformChange, 
  regen, 
  onRegen 
}: Props) {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [text, setText] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  // 调试：监听选中对话变化
  useEffect(() => {
    console.log("💬 ChatPanel 收到 selectedConversation:", selectedConversation);
  }, [selectedConversation]);

  const cur = PLATFORMS.find((p) => p.id === selectedId);

  const handleSelect = (id: string) => {
    setSelectedId(id);
    setOpen(false);
    if (onPlatformChange) {
      onPlatformChange(id);
    }
  };

  const handleGenerate = () => {
    if (!text.trim() || isGenerating) return;
    setIsGenerating(true);
    if (onRegen) {
      onRegen();
    }
    setTimeout(() => {
      setIsGenerating(false);
    }, 2000);
  };

  const renderMessages = () => {
    // 如果存在选中的对话且有消息，则显示
    if (selectedConversation && selectedConversation.messages && selectedConversation.messages.length > 0) {
      return selectedConversation.messages.map((msg, idx) => (
        <div
          key={idx}
          className={`max-w-[78%] rounded-2xl px-4 py-3 text-[13.5px] leading-relaxed ${
            msg.role === "user"
              ? "ml-auto rounded-tr-md bg-blue-50 text-slate-700"
              : "mr-auto rounded-tl-md bg-slate-100 text-slate-700"
          }`}
        >
          {msg.content}
          {msg.time && <div className="mt-1 text-right text-[11px] text-slate-400">{msg.time}</div>}
        </div>
      ));
    }

    // 否则显示默认示例
    return (
      <div className="ml-auto max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-4 py-3 text-[13.5px] leading-relaxed text-slate-700">
        您好，请问你们的这款蓝牙耳机支持七天无理由退货吗？我刚刚收到，还没拆封，想确认一下是否支持无理由退货，谢谢！
        <div className="mt-1 text-right text-[11px] text-slate-400">10:24</div>
      </div>
    );
  };

  return (
    <section className="flex min-h-0 flex-col">
      {/* 服务平台行 */}
      <div className="relative z-30 mb-4 flex items-center gap-4 rounded-2xl border border-slate-200/80 bg-white px-5 py-3.5 shadow-sm">
        <span className="whitespace-nowrap text-[14px] font-medium text-slate-500">服务平台</span>
        <div className="relative w-[360px]">
          <button
            onClick={() => setOpen((o) => !o)}
            className="flex w-full items-center justify-between rounded-xl border border-slate-200 px-3 py-2.5 transition-colors hover:border-blue-300"
          >
            <span className="flex items-center gap-2.5">
              {cur ? (
                <>
                  <span className="grid h-7 w-7 place-items-center rounded-lg text-[13px] font-bold text-white" style={{ background: cur.c }}>
                    {cur.t}
                  </span>
                  <span className="text-[14px] font-medium text-slate-800">{cur.name}</span>
                </>
              ) : (
                <span className="text-[14px] font-medium text-slate-400">请选择服务平台</span>
              )}
            </span>
            <Chev className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
          </button>

          {open && (
            <div className="pop absolute left-0 top-[calc(100%+6px)] w-[230px] rounded-2xl border border-slate-100 bg-white p-2 shadow-2xl shadow-slate-300/40">
              {PLATFORMS.map((p) => (
                <button
                  key={p.id}
                  onClick={() => handleSelect(p.id)}
                  className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-[14px] transition-colors ${
                    p.id === selectedId ? "bg-blue-50 text-blue-700" : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <span className="flex items-center gap-2.5">
                    <span className="grid h-7 w-7 place-items-center rounded-lg text-[13px] font-bold text-white" style={{ background: p.c }}>
                      {p.t}
                    </span>
                    {p.name}
                  </span>
                  {p.id === selectedId && <Check className="h-4 w-4 text-blue-600" />}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 聊天流 */}
      <div className="min-h-0 flex-1 space-y-3 overflow-y-auto rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
        {renderMessages()}
      </div>

      {/* 输入区 */}
      <div className="mt-4 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-[14px] font-bold text-slate-800">请输入客户问题</span>
          <button onClick={() => setText("")} className="text-[13px] font-medium text-blue-600 hover:opacity-70">
            清空
          </button>
        </div>
        <p className="mt-1 text-[12.5px] text-slate-400">请简要描述客户提出的问题，AI 将为您生成回复建议</p>

        <div className="relative mt-3">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value.slice(0, 500))}
            rows={3}
            className="w-full resize-none rounded-xl border border-slate-200 p-3.5 pr-16 text-[13.5px] leading-relaxed outline-none transition-all placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
            placeholder="例如：客户询问蓝牙耳机是否支持七天无理由退货…"
          />
          <span className="pointer-events-none absolute bottom-2.5 right-3 text-[12px] text-slate-400">{text.length}/500</span>
        </div>

        <div className="mt-2.5 flex items-center gap-1.5 text-[12.5px] text-slate-400">
          <Help className="h-4 w-4" />
          可从当前会话中选择最近 5 条消息，或按 Ctrl + / 快捷提取关键信息。
        </div>

        <button
          onClick={handleGenerate}
          disabled={!text.trim() || isGenerating}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-blue-500 py-3.5 text-[15px] font-semibold text-white shadow-lg shadow-blue-500/25 transition-all hover:bg-blue-600 active:scale-[.99] disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <Spark className={`h-5 w-5 ${isGenerating ? "spin" : ""}`} />
          {isGenerating ? "AI正在生成中..." : "生成AI回复"}
        </button>

        <div className="mt-3 space-y-2 rounded-xl bg-blue-50/60 p-3.5 text-[12.5px] text-blue-700/90">
          {NOTES.map((s) => (
            <div key={s} className="flex items-start gap-2">
              <Tick className="mt-0.5 h-4 w-4 shrink-0 text-blue-500" />
              <span>{s}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}