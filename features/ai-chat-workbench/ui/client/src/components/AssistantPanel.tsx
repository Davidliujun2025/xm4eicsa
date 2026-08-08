import { useEffect, useRef, useState } from "react";
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
  conversationId?: string;
  conversationStatus?: "ACTIVE" | "HISTORY";
  lastActivityAt?: string;
  platformName: string;
  messages: ChatMessage[];
  tokenInfo?: TokenInfo;
  generating: boolean;
  strategyGenerating?: boolean;
  strategyGenerationError?: string;
  archiving?: boolean;
  step: number;
  onNextStep: () => void;
  onRetryStrategy?: () => void;
  onJumpStep: (targetStep: number) => void;
  onContentChange?: (text: string) => void | Promise<void>;
  onConversationArchived?: (reason: "completed" | "inactive") => void;
};

const INACTIVITY_TIMEOUT_MS = 25 * 60 * 1000;

export default function AssistantPanel({
  conversationId,
  conversationStatus,
  lastActivityAt,
  platformName,
  messages,
  tokenInfo,
  generating,
  strategyGenerating = false,
  strategyGenerationError = "",
  archiving = false,
  step,
  onNextStep,
  onRetryStrategy,
  onJumpStep,
  onContentChange,
  onConversationArchived,
}: Props) {
  const latestMessage = messages.at(-1);
  const usedToday = useCountUp(tokenInfo?.usedToday ?? 0);
  const totalLimit = tokenInfo?.totalLimit ?? 0;
  const usagePercent = Math.min(100, Math.max(0, tokenInfo?.usagePercent ?? 0));
  const originContent = latestMessage?.[STEP_FIELDS[step - 1]];
  const successCloseReadOnly = step === 5;
  const [editContent, setEditContent] = useState("");
  const [editing, setEditing] = useState(false);
  const [dirty, setDirty] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const panelRef = useRef<HTMLElement>(null);
  const editContentRef = useRef("");
  const dirtyRef = useRef(false);
  const draftKeyRef = useRef("");
  const onContentChangeRef = useRef(onContentChange);
  const onConversationArchivedRef = useRef(onConversationArchived);
  const MAX_CONTENT_LENGTH = 500;

  const [favoriteMessage, setFavoriteMessage] = useState("");
  const [favoriteError, setFavoriteError] = useState("");
  const [favoriting, setFavoriting] = useState(false);
  const [savingEdit, setSavingEdit] = useState(false);
  const [editSaveError, setEditSaveError] = useState("");
  const [strategyWaitingLong, setStrategyWaitingLong] = useState(false);

  const advance = () => onNextStep();

  const commitCurrentEdit = async (): Promise<boolean> => {
    if (!dirtyRef.current) return false;
    setSavingEdit(true);
    setEditSaveError("");
    try {
      await onContentChangeRef.current?.(editContentRef.current);
      if (draftKeyRef.current) sessionStorage.removeItem(draftKeyRef.current);
      dirtyRef.current = false;
      setDirty(false);
      setEditing(false);
      return true;
    } catch (saveError) {
      setEditSaveError(saveError instanceof Error ? saveError.message : "编辑内容保存失败，请重试");
    } finally {
      setSavingEdit(false);
    }
    return false;
  };

  const stashCurrentEdit = () => {
    if (!dirtyRef.current || !draftKeyRef.current) return;
    sessionStorage.setItem(draftKeyRef.current, editContentRef.current);
  };

  useEffect(() => {
    const text = typeof originContent === "string" ? originContent : "";
    const draftKey = `assistant-panel-draft:${conversationId ?? "none"}:${latestMessage?.id ?? "none"}:${latestMessage?.dialogRound ?? 0}:${step}`;
    const stashedContent = successCloseReadOnly ? null : sessionStorage.getItem(draftKey);
    const restoredContent = stashedContent ?? text;
    const restoredDirty = !successCloseReadOnly && stashedContent !== null && stashedContent !== text;
    draftKeyRef.current = draftKey;
    if (successCloseReadOnly) sessionStorage.removeItem(draftKey);
    setEditContent(restoredContent);
    editContentRef.current = restoredContent;
    setEditing(restoredDirty);
    setDirty(restoredDirty);
    dirtyRef.current = restoredDirty;
  }, [conversationId, latestMessage?.dialogRound, latestMessage?.id, originContent, step, successCloseReadOnly]);

  useEffect(() => {
    onContentChangeRef.current = onContentChange;
  }, [onContentChange]);

  useEffect(() => {
    onConversationArchivedRef.current = onConversationArchived;
  }, [onConversationArchived]);

  useEffect(() => {
    // status 缺失代表前后端版本尚未同步，此时不发起归档请求，避免旧后端返回“系统繁忙”。
    if (!conversationId || !latestMessage?.id || conversationStatus !== "ACTIVE") return;
    const lastActivityTime = lastActivityAt ? new Date(lastActivityAt).getTime() : Date.now();
    const elapsed = Number.isNaN(lastActivityTime) ? 0 : Math.max(0, Date.now() - lastActivityTime);
    const remaining = Math.max(0, INACTIVITY_TIMEOUT_MS - elapsed);
    const timeout = window.setTimeout(() => {
      stashCurrentEdit();
      onConversationArchivedRef.current?.("inactive");
    }, remaining);
    return () => window.clearTimeout(timeout);
  }, [conversationId, conversationStatus, lastActivityAt, latestMessage?.id]);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = "auto";
    textarea.style.height = `${textarea.scrollHeight}px`;
  }, [editContent]);

  useEffect(() => {
    setStrategyWaitingLong(false);
    if (!strategyGenerating) return;
    const timer = window.setTimeout(() => setStrategyWaitingLong(true), 5_000);
    return () => window.clearTimeout(timer);
  }, [strategyGenerating]);

  useEffect(() => {
    const saveBeforeExternalNavigation = (event: PointerEvent) => {
      if (!dirtyRef.current) return;
      const target = event.target instanceof Element ? event.target : null;
      const navigationControl = target?.closest("a[href], button");
      if (!navigationControl || panelRef.current?.contains(navigationControl)) return;
      stashCurrentEdit();
    };
    const saveBeforePageLeave = () => stashCurrentEdit();

    document.addEventListener("pointerdown", saveBeforeExternalNavigation, true);
    window.addEventListener("pagehide", saveBeforePageLeave);
    return () => {
      document.removeEventListener("pointerdown", saveBeforeExternalNavigation, true);
      window.removeEventListener("pagehide", saveBeforePageLeave);
      stashCurrentEdit();
    };
  }, []);

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
    const isWithinLimit = val.length <= MAX_CONTENT_LENGTH;
    const isReducingOrReplacingExistingContent = val.length <= editContentRef.current.length;
    if (isWithinLimit || isReducingOrReplacingExistingContent) {
      setEditContent(val);
      editContentRef.current = val;
      const changed = val !== (typeof originContent === "string" ? originContent : "");
      dirtyRef.current = changed;
      setDirty(changed);
      if (!changed && draftKeyRef.current) sessionStorage.removeItem(draftKeyRef.current);
    }
  };

  const saveEditedContent = async () => {
    await commitCurrentEdit();
  };

  const jumpStep = (targetStep: number) => {
    stashCurrentEdit();
    onJumpStep(targetStep);
  };

  const intentRecognitionComplete = typeof latestMessage?.intentRecognition === "string"
    && latestMessage.intentRecognition.trim().length > 0;
  const intentRequired = step === 1 && !dirty && !intentRecognitionComplete;
  const contentGenerated = typeof originContent === "string" && originContent.trim().length > 0;

  return (
    <section ref={panelRef} className="flex min-h-0 flex-col gap-4">
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
                <div key={label} className="relative flex flex-1 flex-col items-center">
                  {index < STEPS.length - 1 && (
                    <div className={`absolute left-1/2 top-3.5 h-[3px] w-full -translate-y-1/2 ${completed ? "bg-blue-600" : "bg-slate-200"}`} />
                  )}
                  <div className="relative z-10 flex items-center justify-center">
                    <button
                      type="button"
                      onClick={() => jumpStep(number)}
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
                  </div>
                  <span className={`mt-2 w-full whitespace-nowrap text-center text-[11.5px] ${
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

              <div className="mt-4 flex items-center justify-between gap-3">
                <span className="text-[12.5px] text-slate-400">{STEPS[step - 1]}</span>
                {editing ? (
                  <span className="rounded-md bg-amber-50 px-2.5 py-1 text-[11.5px] font-medium text-amber-600">
                    编辑中
                  </span>
                ) : null}
              </div>
              <div className="mt-2 relative">
                <textarea
                  ref={textareaRef}
                  value={editContent}
                  maxLength={MAX_CONTENT_LENGTH}
                  onChange={handleTextChange}
                  onFocus={() => {
                    if (!successCloseReadOnly) setEditing(true);
                  }}
                  readOnly={successCloseReadOnly}
                  aria-label={`${STEPS[step - 1]}生成结果`}
                  placeholder="该步骤暂无生成内容"
                  className={`min-h-40 w-full overflow-hidden rounded-xl border bg-white p-3.5 pb-8 text-[13px] leading-relaxed text-slate-700 whitespace-pre-wrap outline-none resize-none ${
                    successCloseReadOnly
                      ? "cursor-default border-slate-200 bg-slate-50"
                      : editing
                        ? "border-blue-300 ring-2 ring-blue-50 focus:border-blue-400"
                        : "cursor-text border-slate-200"
                  }`}
                />
                <span className={`absolute bottom-3.5 right-3.5 text-[11.5px] ${
                  editContent.length > MAX_CONTENT_LENGTH ? "text-amber-500" : "text-slate-400"
                }`}>
                  {editContent.length}/{MAX_CONTENT_LENGTH}
                </span>
              </div>

              <div className="mt-3 flex items-center justify-between">
                <div className="flex flex-col gap-1">
                  <span className="text-[12.5px] text-slate-400">
                    本轮消耗 {latestMessage.totalTokens?.toLocaleString() ?? 0} Token
                  </span>
                  {favoriteMessage && <span className="text-[12px] text-emerald-600">{favoriteMessage}</span>}
                  {favoriteError && <span className="text-[12px] text-red-600">{favoriteError}</span>}
                </div>
                <div className="flex items-center gap-2">
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
              {editSaveError && (
                <div className="mt-3 rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-[12.5px] text-red-600">
                  {editSaveError}
                </div>
              )}
              {strategyGenerating && step <= 2 && (
                <div className="mt-3 rounded-xl border border-blue-200 bg-blue-50 px-3 py-2.5 text-[12.5px] text-blue-700">
                  {strategyWaitingLong ? "AI正在努力生成策略，请稍候" : "生成中"}
                </div>
              )}
              {strategyGenerationError && step <= 2 && !strategyGenerating && (
                <div className="mt-3 flex items-center justify-between gap-3 rounded-xl border border-red-200 bg-red-50 px-3 py-2.5 text-[12.5px] text-red-600">
                  <span>{strategyGenerationError}</span>
                  <button type="button" onClick={onRetryStrategy} className="shrink-0 font-medium text-blue-600 hover:text-blue-700">
                    重试
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 底部单按钮，和截图样式统一 */}
        <div className="border-t border-slate-100 p-4">
  <div title={intentRequired ? "请先完成意图识别" : undefined}>
    <button
      type="button"
      onClick={dirty
        ? () => void saveEditedContent()
        : strategyGenerationError && step <= 2
          ? onRetryStrategy
          : step === 2 && !contentGenerated && !strategyGenerating
            ? onRetryStrategy
            : step >= 5
              ? () => onConversationArchivedRef.current?.("completed")
              : advance}
      disabled={!latestMessage || generating || savingEdit || archiving || conversationStatus === "HISTORY" || intentRequired}
      className={`w-full rounded-xl py-3 text-[14px] font-semibold text-white transition-all active:scale-[.99] ${
        dirty
          ? "bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300"
          : step >= 5
          ? "bg-emerald-500 hover:bg-emerald-600 disabled:bg-slate-300"
          : "bg-blue-600 hover:bg-blue-700 disabled:bg-slate-300"
      }`}
    >
      {archiving && step >= 5
        ? "正在生成评估报告"
        : savingEdit
        ? "保存中..."
        : strategyGenerating && step <= 2
          ? "生成中"
          : dirty
            ? "保存已编辑内容"
            : strategyGenerationError && step <= 2
              ? "重试生成回复策略"
              : step === 2 && !contentGenerated && !strategyGenerating
                ? "生成回复策略"
                : step >= 5
                ? "已完成，结束此次对话"
                : "确认，生成下一步"}
    </button>
  </div>
</div>
      </div>
    </section>
  );
}
