import { useState, useRef, useEffect } from "react";
import { NOTES, PLATFORMS } from "../config/workbench";
import type { ChatMessage } from "../types";
import { Chev, Check, Spark, Help, Tick } from "./icons";

type Props = {
  platformId: string;
  onPlatformChange: (id: string) => void;
  messages: ChatMessage[];
  text: string;
  onTextChange: (value: string) => void;
  generating: boolean;
  error: string;
  onGenerate: () => void;
  isCurStepGenerated: boolean; // 新增：当前步骤是否已经生成
};

function formatMessageTime(value?: string) {
  if (!value) return "--:--";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit", hour12: false });
}

export default function ChatPanel({
  platformId,
  onPlatformChange,
  messages,
  text,
  onTextChange,
  generating,
  error,
  onGenerate,
  isCurStepGenerated,
}: Props) {
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const currentPlatform = PLATFORMS.find((platform) => platform.id === platformId);

  // 点击空白关闭下拉逻辑
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, []);

  // 动态按钮文字
  const btnText = isCurStepGenerated ? "重新生成AI回复" : "生成AI回复";

  return (
    <section className="flex min-h-0 flex-col">
      <div className="relative z-30 mb-4 flex items-center gap-4 rounded-2xl border border-slate-200/80 bg-white px-5 py-3.5 shadow-sm">
        <span className="text-[14px] font-medium text-slate-500">服务平台</span>
        <div className="relative w-[360px]" ref={dropdownRef}>
          <button
  type="button"
  onClick={() => setOpen((value) => !value)}
  className="flex w-full items-center justify-between rounded-xl border border-slate-200 px-3 py-2.5 transition-colors hover:border-blue-300"
>
  {currentPlatform ? (
    <span className="flex items-center gap-2.5">
      <span
        className="grid h-7 w-7 place-items-center rounded-lg text-[13px] font-bold text-white"
        style={{ background: currentPlatform.color }}
      >
        {currentPlatform.shortName}
      </span>
      <span className="text-[14px] font-medium text-slate-800">{currentPlatform.name}</span>
    </span>
  ) : (
    <span className="text-[14px] text-slate-400">请选择服务平台</span>
  )}
  <Chev className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
</button>

          {open && (
            <div className="pop absolute left-0 top-[calc(100%+6px)] w-[230px] rounded-2xl border border-slate-100 bg-white p-2 shadow-2xl shadow-slate-300/40">
              {PLATFORMS.map((platform) => (
                <button
                  key={platform.id}
                  type="button"
                  onClick={() => {
                    onPlatformChange(platform.id);
                    setOpen(false);
                  }}
                  className={`flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-[14px] transition-colors ${
                    platform.id === platformId ? "bg-blue-50 text-blue-700" : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <span className="flex items-center gap-2.5">
                    <span
                      className="grid h-7 w-7 place-items-center rounded-lg text-[13px] font-bold text-white"
                      style={{ background: platform.color }}
                    >
                      {platform.shortName}
                    </span>
                    {platform.name}
                  </span>
                  {platform.id === platformId && <Check className="h-4 w-4 text-blue-600" />}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
        {messages.length === 0 && (
          <div className="grid h-full place-items-center text-[13px] text-slate-400">新建对话后，客户问题会显示在这里</div>
        )}
        {messages.map((message) => (
          <div
            key={`${message.id}-${message.dialogRound}`}
            className="ml-auto mb-3 max-w-[78%] rounded-2xl rounded-tr-md bg-blue-50 px-4 py-3 text-[13.5px] leading-relaxed text-slate-700"
          >
            {message.question}
            <div className="mt-1 text-right text-[11px] text-slate-400">{formatMessageTime(message.createdAt)}</div>
          </div>
        ))}
      </div>

      <div className="mt-4 rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-[14px] font-bold text-slate-800">请输入客户问题</span>
          <button type="button" onClick={() => onTextChange("")} className="text-[13px] font-medium text-blue-600 hover:opacity-70">
            清空
          </button>
        </div>
        <p className="mt-1 text-[12.5px] text-slate-400">请简要描述客户提出的问题，AI 将为您生成回复建议</p>

        <div className="relative mt-3">
          <textarea
            value={text}
            onChange={(event) => onTextChange(event.target.value.slice(0, 500))}
            rows={3}
            className="w-full resize-none rounded-xl border border-slate-200 p-3.5 pr-16 text-[13.5px] leading-relaxed outline-none transition-all placeholder:text-slate-400 focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
            placeholder="请输入当前客户的真实问题"
          />
          <span className="pointer-events-none absolute bottom-2.5 right-3 text-[12px] text-slate-400">{text.length}/500</span>
        </div>

        {error && <div className="mt-2.5 rounded-lg bg-red-50 px-3 py-2 text-[12.5px] text-red-600">{error}</div>}

        <div className="mt-2.5 flex items-center gap-1.5 text-[12.5px] text-slate-400">
          <Help className="h-4 w-4" />
          当前生成结果将保存到数据库，并计入当前客服的 Token 用量。
        </div>

        <button
          type="button"
          onClick={onGenerate}
          disabled={generating || !text.trim() || !currentPlatform}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-blue-500 py-3.5 text-[15px] font-semibold text-white shadow-lg shadow-blue-500/25 transition-all hover:bg-blue-600 active:scale-[.99] disabled:opacity-60"
        >
          <Spark className={`h-5 w-5 ${generating ? "spin" : ""}`} />
          {generating ? "正在生成并保存…" : btnText}
        </button>

        <div className="mt-3 space-y-2 rounded-xl bg-blue-50/60 p-3.5 text-[12.5px] text-blue-700/90">
          {NOTES.map((note) => (
            <div key={note} className="flex items-start gap-2">
              <Tick className="mt-0.5 h-4 w-4 shrink-0 text-blue-500" />
              <span>{note}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}