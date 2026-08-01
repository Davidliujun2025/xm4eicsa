import { useEffect, useState } from "react";
import { STEPS } from "../config/workbench";
import { useCountUp } from "../hooks/useCountUp";
import type { ChatMessage, TokenInfo } from "../types";
import { Spark, Check, Flag, Arrow } from "./icons";

const STEP_FIELDS: Array<keyof ChatMessage> = [
  "intentRecognition",
  "replyStrategy",
  "recommendedScript",
  "hookGuidance",
  "successClose",
];

type Props = {
  platformName: string;
  messages: ChatMessage[];
  tokenInfo?: TokenInfo;
  generating: boolean;
};

export default function AssistantPanel({ platformName, messages, tokenInfo, generating }: Props) {
  const latestMessage = messages.at(-1);
  const [step, setStep] = useState(1);
  const usedToday = useCountUp(tokenInfo?.usedToday ?? 0);
  const totalLimit = tokenInfo?.totalLimit ?? 0;
  const usagePercent = Math.min(100, Math.max(0, tokenInfo?.usagePercent ?? 0));
  const content = latestMessage?.[STEP_FIELDS[step - 1]];

  useEffect(() => {
    setStep(1);
  }, [generating, latestMessage?.id, latestMessage?.dialogRound]);

  const advance = () => setStep((current) => Math.min(5, current + 1));

  return (
    <section className="flex min-h-0 flex-col gap-4">
      <div className="flex items-center gap-3 rounded-2xl border border-slate-200/80 bg-white px-4 py-3.5 shadow-sm">
        <span className="grid h-7 w-7 place-items-center rounded-full bg-blue-50 text-blue-600">
          <Spark className="h-4 w-4" />
        </span>
        <span className="whitespace-nowrap text-[13px] font-medium text-slate-600">今日 Token 消耗</span>
        <span className="text-[15px] font-bold text-slate-900">{usedToday.toLocaleString()}</span>
        <span className="text-[12px] text-slate-400">/ {totalLimit.toLocaleString()}</span>
        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-slate-100">
          <div className="h-full rounded-full bg-blue-500 transition-all duration-1000" style={{ width: `${usagePercent}%` }} />
        </div>
        <span className="text-[12.5px] font-semibold text-blue-600">{usagePercent.toFixed(0)}%</span>
        <button
          type="button"
          onClick={() => window.location.assign(import.meta.env.VITE_TOKEN_USAGE_URL || "/token-usage/")}
          className="ml-auto flex items-center gap-0.5 whitespace-nowrap text-[12.5px] font-medium text-blue-600 hover:opacity-70"
        >
          查看明细
          <Arrow className="h-3.5 w-3.5" />
        </button>
      </div>

      <div className="flex min-h-0 flex-1 flex-col rounded-2xl border border-slate-200/80 bg-white shadow-sm">
        <div className="flex items-start justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <div className="text-[16px] font-bold text-slate-800">AI智能助手</div>
            <div className="mt-0.5 text-[12.5px] text-slate-400">五步生成法</div>
          </div>
          <Spark className="h-6 w-6 text-blue-500" />
        </div>

        <div className="px-5 py-5">
          <div className="flex items-center">
            {STEPS.map((label, index) => {
              const number = index + 1;
              const completed = Boolean(latestMessage) && number < step;
              const current = number === step;
              return (
                <div key={label} className="flex flex-1 flex-col items-center last:flex-none">
                  <div className="flex w-full items-center">
                    <button
                      type="button"
                      disabled
                      aria-current={current && latestMessage ? "step" : undefined}
                      className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-[12px] font-bold transition-all duration-300 ${
                        completed
                          ? "bg-blue-600 text-white"
                          : current && latestMessage
                            ? "bg-blue-600 text-white ring-4 ring-blue-100"
                            : "bg-white text-slate-400 ring-1 ring-slate-200"
                      }`}
                    >
                      {completed ? <Check className="h-4 w-4" /> : number}
                    </button>
                    {index < STEPS.length - 1 && (
                      <div className={`mx-1 h-[3px] flex-1 rounded-full ${completed ? "bg-blue-600" : "bg-slate-200"}`} />
                    )}
                  </div>
                  <span className={`mt-2 whitespace-nowrap text-[11.5px] ${
                    current && latestMessage ? "font-semibold text-blue-600" : completed ? "text-blue-600" : "text-slate-400"
                  }`}>
                    {label}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-4">
          {!latestMessage ? (
            <div className="grid h-full min-h-48 place-items-center rounded-2xl border border-dashed border-slate-200 text-[13px] text-slate-400">
              请选择数据库会话，或输入客户问题生成回复
            </div>
          ) : (
            <div className={`rounded-2xl border border-slate-100 bg-slate-50/50 p-4 ${generating ? "shimmer" : ""}`}>
              <div className="flex items-center justify-between">
                <span className="text-[16px] font-bold text-slate-800">第{step}步 - {STEPS[step - 1]}</span>
                <span className="rounded-md bg-blue-50 px-2.5 py-1 text-[11.5px] font-medium text-blue-600">适用平台：{platformName}</span>
              </div>

              <div className="mt-3 text-[12.5px] text-slate-400">适用场景与语气</div>
              <div className="mt-2 flex gap-2">
                <span className="rounded-lg bg-blue-100 px-3 py-1.5 text-[12.5px] font-medium text-blue-700">{platformName}</span>
                <span className="rounded-lg bg-emerald-100 px-3 py-1.5 text-[12.5px] font-medium text-emerald-700">
                  {latestMessage.customerType || "专业友好"}
                </span>
              </div>

              <div className="mt-4 text-[12.5px] text-slate-400">{STEPS[step - 1]}</div>
              <div className="mt-2 min-h-24 rounded-xl border border-slate-200 bg-white p-3.5 text-[13px] leading-relaxed text-slate-700">
                {typeof content === "string" && content.trim() ? content : "该步骤暂无生成内容"}
              </div>

              <div className="mt-3 flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-[12.5px] text-emerald-700">
                <Check className="mt-0.5 h-4 w-4 shrink-0" />
                {latestMessage.riskWarning || "未发现高风险表达"}
                {latestMessage.riskSuggestion ? `；${latestMessage.riskSuggestion}` : ""}
              </div>

              <div className="mt-3 flex items-center justify-between">
                <span className="text-[12.5px] text-slate-400">
                  本轮消耗 {latestMessage.totalTokens?.toLocaleString() ?? 0} Token
                </span>
                <button type="button" className="flex items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-1.5 text-[12.5px] font-medium text-blue-600 transition-colors hover:bg-blue-50">
                  <Flag className="h-4 w-4" />
                  收藏到话术库
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="border-t border-slate-100 p-4">
          <button
            type="button"
            onClick={advance}
            disabled={!latestMessage || step >= 5}
            className={`w-full rounded-xl py-3.5 text-[15px] font-semibold text-white shadow-lg transition-all active:scale-[.99] ${
              latestMessage && step >= 5
                ? "bg-emerald-500 shadow-emerald-500/25"
                : "bg-blue-600 shadow-blue-600/25 hover:bg-blue-700 disabled:bg-slate-300 disabled:shadow-none"
            }`}
          >
            {latestMessage && step >= 5 ? "已完成全部步骤 ✓" : "确认，查看下一步骤"}
          </button>
        </div>
      </div>
    </section>
  );
}
