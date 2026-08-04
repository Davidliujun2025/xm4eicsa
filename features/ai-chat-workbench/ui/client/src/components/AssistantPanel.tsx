import { useEffect, useState } from "react";
import { STEPS } from "../config/workbench";
import { useCountUp } from "../hooks/useCountUp";
import type { ChatMessage, TokenInfo } from "../types";
import { Spark, Check, Flag } from "./icons";
import { workbenchApi } from "../services/workbenchApi";

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
  step: number;
  onNextStep: () => void;
  onJumpStep: (targetStep: number) => void;
  onContentChange?: (text: string) => void;
};

export default function AssistantPanel({
  platformName,
  messages,
  tokenInfo,
  generating,
  step,
  onNextStep,
  onJumpStep,
  onContentChange,
}: Props) {
  const latestMessage = messages.at(-1);
  const usedToday = useCountUp(tokenInfo?.usedToday ?? 0);
  const totalLimit = tokenInfo?.totalLimit ?? 0;
  const usagePercent = Math.min(100, Math.max(0, tokenInfo?.usagePercent ?? 0));
  const originContent = latestMessage?.[STEP_FIELDS[step - 1]];
  const [editContent, setEditContent] = useState("");
  const MAX_CONTENT_LENGTH = 500;

  const [favoriteMessage, setFavoriteMessage] = useState("");
  const [favoriteError, setFavoriteError] = useState("");
  const [favoriting, setFavoriting] = useState(false);

  const advance = () => onNextStep();

  useEffect(() => {
    const text = typeof originContent === "string" ? originContent : "";
    setEditContent(text);
  }, [originContent, latestMessage?.id, step]);

  useEffect(() => {
    setFavoriteMessage("");
    setFavoriteError("");
  }, [latestMessage?.id, step]);

  const buildFavoriteTags = () => {
    const candidates = [
      platformName,
      latestMessage?.customerType,
      STEPS[step - 1],
    ];

    return [...new Set(
      candidates
        .map((item) => item?.trim())
        .filter((item): item is string => Boolean(item))
        .map((item) => item.slice(0, 5)),
    )];
  };

  const buildGeneratedAt = () => {
    if (!latestMessage?.createdAt) return undefined;
    const parsed = new Date(latestMessage.createdAt);
    if (Number.isNaN(parsed.getTime())) return undefined;
    return parsed.toISOString();
  };

  const handleFavorite = async () => {
    if (!latestMessage || typeof editContent !== "string" || !editContent.trim() || favoriting) return;

    setFavoriting(true);
    setFavoriteMessage("");
    setFavoriteError("");

    try {
      const response = await workbenchApi.togglePersonalFavorite({
        sourceTalkId: `msg-${latestMessage.id}-round-${latestMessage.dialogRound}-step-${step}`,
        content: editContent.trim(),
        scenario: `${platformName}-${latestMessage.customerType || "通用"}-${STEPS[step - 1]}`.slice(0, 100),
        generatedAt: buildGeneratedAt(),
        tags: buildFavoriteTags(),
      });
      setFavoriteMessage(response.message || (response.favorited ? "已收藏到个人话术库" : "已取消收藏"));
    } catch (error) {
      setFavoriteError(error instanceof Error ? error.message : "收藏失败，请稍后重试");
    } finally {
      setFavoriting(false);
    }
  };

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;
    if (val.length <= MAX_CONTENT_LENGTH) {
      setEditContent(val);
      onContentChange?.(val);
    }
  };

  return (
    <section className="flex min-h-0 flex-col gap-4">
      {/* Token消耗卡片，移除查看明细 */}
      <div className="flex items-center gap-2.5 rounded-2xl border border-slate-200/80 bg-white px-4 py-3.5 shadow-sm">
        <span className="grid h-7 w-7 place-items-center rounded-full bg-blue-50 text-blue-600 shrink-0">
          <Spark className="h-4 w-4" />
        </span>
        <span className="whitespace-nowrap text-[13px] font-medium text-slate-600 shrink-0">今日 Token 消耗</span>
        <div className="flex items-center gap-1 min-w-0 flex-1">
          <span className="text-[15px] font-bold text-slate-900 whitespace-nowrap">{usedToday.toLocaleString()}</span>
          <span className="text-[14px] text-slate-400">/</span>
          <span className="text-[14px] text-slate-500 whitespace-nowrap">{totalLimit.toLocaleString()}</span>
        </div>
        <span className="min-w-[42px] text-[12.5px] font-semibold text-blue-600 text-right shrink-0">
          {usagePercent.toFixed(0)}%
        </span>
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
                      onClick={() => onJumpStep(number)}
                      aria-current={current && latestMessage ? "step" : undefined}
                      className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-[12px] font-bold transition-all duration-300 ${
                        completed
                          ? "bg-blue-600 text-white"
                          : current && latestMessage
                            ? "bg-blue-600 text-white ring-4 ring-blue-100"
                            : "bg-white text-slate-400 ring-1 ring-slate-200 hover:border-blue-300 hover:ring-blue-200"
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
              <div className="mt-2 flex gap-2 flex-wrap">
                <span className="rounded-lg bg-blue-100 px-3 py-1.5 text-[12.5px] font-medium text-blue-700">{platformName}</span>
                <span className="rounded-lg bg-emerald-100 px-3 py-1.5 text-[12.5px] font-medium text-emerald-700">
                  {latestMessage.customerType || "专业友好"}
                </span>
              </div>

              <div className="mt-4 text-[12.5px] text-slate-400">{STEPS[step - 1]}</div>
              <div className="mt-2 relative">
                <textarea
                  value={editContent}
                  onChange={handleTextChange}
                  placeholder="该步骤暂无生成内容"
                  className="min-h-24 w-full rounded-xl border border-slate-200 bg-white p-3.5 pr-20 text-[13px] leading-relaxed text-slate-700 whitespace-pre-wrap outline-none focus:border-blue-400 resize-y"
                />
                <span className="absolute bottom-3.5 right-3.5 text-[11.5px] text-slate-400">
                  {editContent.length}/{MAX_CONTENT_LENGTH}
                </span>
              </div>

              <div className="mt-3 flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-[12.5px] text-emerald-700">
                <Check className="mt-0.5 h-4 w-4 shrink-0" />
                {latestMessage.riskWarning || "未发现高风险表达"}
                {latestMessage.riskSuggestion ? `；${latestMessage.riskSuggestion}` : ""}
              </div>

              <div className="mt-3 flex items-center justify-between">
                <div className="flex flex-col gap-1">
                  <span className="text-[12.5px] text-slate-400">
                    本轮消耗 {latestMessage.totalTokens?.toLocaleString() ?? 0} Token
                  </span>
                  {favoriteMessage && <span className="text-[12px] text-emerald-600">{favoriteMessage}</span>}
                  {favoriteError && <span className="text-[12px] text-red-600">{favoriteError}</span>}
                </div>
                <button
                  type="button"
                  onClick={() => void handleFavorite()}
                  disabled={favoriting || !editContent || !String(editContent).trim()}
                  className="flex items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-1.5 text-[12.5px] font-medium text-blue-600 transition-colors hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Flag className="h-4 w-4" />
                  {favoriting ? "收藏中..." : "收藏到话术库"}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* 底部单按钮，和截图样式统一 */}
        <div className="border-t border-slate-100 p-4">
  <button
    type="button"
    onClick={advance}
    disabled={!latestMessage || generating}
    className={`w-full rounded-xl py-3 text-[14px] font-semibold text-white transition-all active:scale-[.99] ${
      step >= 5
        ? "bg-emerald-500 hover:bg-emerald-600 disabled:bg-slate-300"
        : "bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300"
    }`}
  >
    {step >= 5 ? "已完成 ✓" : "确认，生成下一步"}
  </button>
</div>
      </div>
    </section>
  );
}
