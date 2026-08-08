import { useCallback, useEffect, useRef, useState } from "react";
import { PLATFORMS, platformFor } from "./config/workbench";
import { workbenchApi } from "./services/workbenchApi";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import ConversationList from "./components/ConversationList";
import ChatPanel from "./components/ChatPanel";
import AssistantPanel from "./components/AssistantPanel";
import ConversationHistoryPage from "./components/ConversationHistoryPage";
import MyEvaluationPage from "./components/MyEvaluationPage";
import type { SidebarItemId } from "./components/Sidebar";
import type { ChatMessage, Conversation, TokenInfo } from "./types";
import { getLoginUrl } from "./utils/auth";
import { isHistoryConversation } from "./utils/conversationStatus";

type WorkbenchView = "conversation" | "history" | "evaluation";

const STEP_FIELDS: Array<keyof ChatMessage> = [
  "intentRecognition",
  "replyStrategy",
  "recommendedScript",
  "hookGuidance",
  "successClose",
];

const CONFIRMED_EDITS_STORAGE_KEY = "assistant-panel-confirmed-edits";
const CONVERSATION_STEPS_STORAGE_KEY = "assistant-panel-conversation-steps";
const ACTIVE_CONVERSATION_STORAGE_KEY = "assistant-panel-active-conversation";
const STRATEGY_GENERATION_TIMEOUT_MS = 15_000;

type ConfirmedEdits = Record<string, string>;

function confirmedEditKey(conversationId: string, messageId: number, field: keyof ChatMessage) {
  return `${conversationId}:${messageId}:${String(field)}`;
}

function loadConfirmedEdits(): ConfirmedEdits {
  try {
    const saved = sessionStorage.getItem(CONFIRMED_EDITS_STORAGE_KEY);
    return saved ? JSON.parse(saved) as ConfirmedEdits : {};
  } catch {
    return {};
  }
}

function loadConversationSteps(): Record<string, number> {
  try {
    const saved = sessionStorage.getItem(CONVERSATION_STEPS_STORAGE_KEY);
    return saved ? JSON.parse(saved) as Record<string, number> : {};
  } catch {
    return {};
  }
}

function getWorkbenchView(): WorkbenchView {
  const requestedView = new URLSearchParams(window.location.search).get("view");
  return requestedView === "history" || requestedView === "evaluation" ? requestedView : "conversation";
}

function openConversationHistory(conversationId: string) {
  const target = new URL(window.location.href);
  target.searchParams.set("view", "history");
  target.searchParams.set("conversationId", conversationId);
  window.location.assign(target.toString());
}

function requiresCarrierName(message?: ChatMessage) {
  if (!message) return false;
  // 仅以客户原话判断物流顾虑，避免意图识别文本中“发货/物流”等泛化措辞误触发快递填写框
  return /快递|物流|配送|发货|派送|送货|运送/.test(message.question || "");
}

export default function App() {
  const [platformId, setPlatformId] = useState("");
  const [isAuthenticatedCustomerService, setIsAuthenticatedCustomerService] = useState(false);
  const [authError, setAuthError] = useState("");
  const [authRetryKey, setAuthRetryKey] = useState(0);
  const [displayName, setDisplayName] = useState("客服");
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [tokenInfo, setTokenInfo] = useState<TokenInfo>();
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState("");
  const [viewport, setViewport] = useState(() => ({ width: window.innerWidth, height: window.innerHeight }));
  const [isUserManualSelect, setIsUserManualSelect] = useState(false);
  const confirmedEditsRef = useRef<ConfirmedEdits>(loadConfirmedEdits());
  const conversationStepsRef = useRef<Record<string, number>>(loadConversationSteps());
  const selectedConversationIdRef = useRef<string | null>(null);
  const [strategyGenerating, setStrategyGenerating] = useState(false);
  const [strategyGenerationError, setStrategyGenerationError] = useState("");

  const [currentStep, setCurrentStep] = useState(1);
  const latestMessage = selectedConversation?.messages?.at(-1);
  const currentStepContent = latestMessage?.[STEP_FIELDS[currentStep - 1]];
  const isCurStepGenerated = typeof currentStepContent === "string" && currentStepContent.trim().length > 0;
  const generationLocked = Boolean(
    currentStep === 5
    && isCurStepGenerated
    && latestMessage
    && draft.trim() === latestMessage.question.trim(),
  );

  const runStrategyGeneration = async (request: (signal: AbortSignal) => Promise<Conversation>) => {
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), STRATEGY_GENERATION_TIMEOUT_MS);
    setStrategyGenerating(true);
    setStrategyGenerationError("");
    try {
      return await request(controller.signal);
    } catch (requestError) {
      if (requestError instanceof Error && requestError.name === "AbortError") {
        throw new Error("生成失败，请稍后重试");
      }
      throw requestError;
    } finally {
      window.clearTimeout(timeout);
      setStrategyGenerating(false);
    }
  };

  const applyConfirmedEdits = (conversation: Conversation): Conversation => ({
    ...conversation,
    messages: conversation.messages.map((message) => {
      const editedFields = STEP_FIELDS.reduce<Partial<ChatMessage>>((result, field) => {
        const key = confirmedEditKey(conversation.conversationId, message.id, field);
        if (Object.prototype.hasOwnProperty.call(confirmedEditsRef.current, key)) {
          result[field] = confirmedEditsRef.current[key] as never;
        }
        return result;
      }, {});
      return { ...message, ...editedFields };
    }),
  });

  // 确认当前步骤后，调用 DeepSeek 生成下一步并立即写入数据库。
  const handleNextStep = async (selectedCarrierName?: string) => {
    const next = Math.min(5, currentStep + 1);
    if (next === currentStep || generating || !selectedConversation || !latestMessage) return;

    const existingContent = latestMessage[STEP_FIELDS[next - 1]];
    if (typeof existingContent === "string" && existingContent.trim().length > 0) {
      setCurrentStep(next);
      return;
    }

    const confirmedIntent = typeof latestMessage.intentRecognition === "string"
      ? latestMessage.intentRecognition.trim()
      : "";
    if (next === 2 && !confirmedIntent) {
      setStrategyGenerationError("请先完成意图识别");
      return;
    }

    const resolvedCarrierName = selectedCarrierName?.trim()
      || (next === 2 && requiresCarrierName(latestMessage) ? "快递" : undefined);

    setGenerating(true);
    setError("");
    try {
      const request = (signal?: AbortSignal) => workbenchApi.generateStep(
          selectedConversation.conversationId,
          latestMessage.dialogRound,
          next,
          next === 2 ? {
            carrierName: resolvedCarrierName,
            intentRecognition: confirmedIntent,
            customerQuestion: latestMessage.question,
            platform: currentPlatform?.name,
          } : undefined,
          signal,
        );
      const response = next === 2
        ? await runStrategyGeneration((signal) => request(signal))
        : await request();
      const updated = applyConfirmedEdits(response);
      setSelectedConversation(updated);
      setConversations((current) => [
        updated,
        ...current.filter((item) => item.conversationId !== updated.conversationId),
      ]);
      setTokenInfo(await workbenchApi.getTokenUsage());
      setCurrentStep(next);
    } catch (generationError) {
      const message = generationError instanceof Error ? generationError.message : "生成失败，请稍后重试";
      if (next === 2) {
        if (message.includes("当前客户信息不足") || message.includes("请先完成意图识别")) {
          setStrategyGenerationError(message);
        } else if (message.includes("合作快递") || message.includes("物流顾虑")) {
          setStrategyGenerationError(message);
        } else {
          setStrategyGenerationError("生成失败，请稍后重试");
        }
      }
      setError(message);
    } finally {
      setGenerating(false);
    }
  };

  // 跳转到指定步骤
  const handleJumpStep = (targetStep: number) => {
    const safeStep = Math.max(1, Math.min(5, targetStep));
    if (selectedConversation) {
      conversationStepsRef.current = {
        ...conversationStepsRef.current,
        [selectedConversation.conversationId]: safeStep,
      };
      sessionStorage.setItem(CONVERSATION_STEPS_STORAGE_KEY, JSON.stringify(conversationStepsRef.current));
    }
    setCurrentStep(safeStep);
  };

  const handleAssistantContentChange = async (content: string) => {
    if (!selectedConversation || !latestMessage) return;
    const conversationId = selectedConversation.conversationId;
    const messageId = latestMessage.id;
    const field = STEP_FIELDS[currentStep - 1];
    const persisted = applyConfirmedEdits(await workbenchApi.updateStepContent(
      conversationId,
      latestMessage.dialogRound,
      currentStep,
      { content },
    ));
    const editKey = confirmedEditKey(conversationId, messageId, field);
    confirmedEditsRef.current = { ...confirmedEditsRef.current, [editKey]: content };
    sessionStorage.setItem(CONFIRMED_EDITS_STORAGE_KEY, JSON.stringify(confirmedEditsRef.current));
    const updateConversation = (conversation: Conversation): Conversation => ({
      ...conversation,
      messages: conversation.messages.map((message) =>
        message.id === messageId ? { ...message, [field]: content } : message,
      ),
    });

    setSelectedConversation((current) => current?.conversationId === conversationId
      ? updateConversation(persisted)
      : current);
    setConversations((current) => current.map((conversation) =>
      conversation.conversationId === conversationId ? updateConversation(conversation) : conversation,
    ));
  };

  const archiveSelectedConversation = async () => {
    if (!selectedConversation) return;
    const conversationId = selectedConversation.conversationId;
    if (selectedConversation.status === undefined || isHistoryConversation(selectedConversation)) {
      sessionStorage.removeItem(ACTIVE_CONVERSATION_STORAGE_KEY);
      openConversationHistory(conversationId);
      return;
    }
    setError("");
    try {
      const archived = applyConfirmedEdits(await workbenchApi.archiveConversation(conversationId));
      sessionStorage.removeItem(ACTIVE_CONVERSATION_STORAGE_KEY);
      setSelectedConversation(archived);
      setConversations((current) => current.map((conversation) =>
        conversation.conversationId === conversationId ? archived : conversation,
      ));
      openConversationHistory(conversationId);
    } catch (archiveError) {
      setError(archiveError instanceof Error ? archiveError.message : "对话归档失败，请重试");
    }
  };

  const currentPlatform = PLATFORMS.find((platform) => platform.id === platformId);
  const view = getWorkbenchView();
  const activeItem: SidebarItemId = view;
  const desktopWidth = 1440;
  const layoutScale = Math.min(1, viewport.width / desktopWidth);

  useEffect(() => {
    const controller = new AbortController();
    const verifyLogin = async () => {
      setAuthError("");
      try {
        const response = await fetch("/api/v1/auth/me", { credentials: "include", signal: controller.signal });
        if (response.status === 401 || response.status === 403) {
          window.location.replace(getLoginUrl(window.location.href));
          return;
        }
        if (!response.ok) {
          throw new Error(`登录状态验证失败（HTTP ${response.status}）`);
        }
        const payload = response.ok ? await response.json() : null;
        if (response.ok && payload?.code === "OK" && payload?.data?.roleType === "CUSTOMER_SERVICE") {
          setDisplayName(payload.data.username || payload.data.account || "客服");
          setIsAuthenticatedCustomerService(true);
          return;
        }
        if (payload?.code === "OK" && payload?.data?.roleType) {
          window.location.replace(getLoginUrl(window.location.href));
          return;
        }
        throw new Error("登录状态接口返回的数据格式不正确");
      } catch (verifyError) {
        if (controller.signal.aborted) return;
        setAuthError(verifyError instanceof Error ? verifyError.message : "登录状态验证失败");
      }
    };
    void verifyLogin();
    return () => controller.abort();
  }, [authRetryKey]);

  const loadWorkbench = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedConversations, loadedTokenInfo] = await Promise.all([
        workbenchApi.listConversations(),
        workbenchApi.getTokenUsage(),
      ]);
      const restoredConversations = loadedConversations.map(applyConfirmedEdits);
      setConversations(restoredConversations);
      setTokenInfo(loadedTokenInfo);
      const activeConversationId = sessionStorage.getItem(ACTIVE_CONVERSATION_STORAGE_KEY);
      const activeConversation = restoredConversations.find(
        (conversation) => conversation.conversationId === activeConversationId
          && !isHistoryConversation(conversation),
      );
      if (activeConversation) {
        setIsUserManualSelect(true);
        setSelectedConversation(activeConversation);
        setPlatformId(platformFor(activeConversation.platform).id);
        setDraft(activeConversation.messages?.at(-1)?.question ?? "");
        setCurrentStep(conversationStepsRef.current[activeConversation.conversationId] ?? 1);
      } else if (activeConversationId) {
        sessionStorage.removeItem(ACTIVE_CONVERSATION_STORAGE_KEY);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "工作台数据加载失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticatedCustomerService && view === "conversation") {
      void loadWorkbench();
    }
  }, [isAuthenticatedCustomerService, loadWorkbench, view]);

  const handleChangePlatform = (newPlatformId: string) => {
    // Selecting a platform while composing a new conversation must not auto-open
    // the latest historical conversation for that platform.
    setIsUserManualSelect(true);
    setPlatformId(newPlatformId);
  };

  useEffect(() => {
    selectedConversationIdRef.current = selectedConversation?.conversationId ?? null;
  }, [selectedConversation?.conversationId]);

  useEffect(() => {
    if (!selectedConversation || isHistoryConversation(selectedConversation)) return;
    const conversationId = selectedConversation.conversationId;
    conversationStepsRef.current = {
      ...conversationStepsRef.current,
      [conversationId]: currentStep,
    };
    sessionStorage.setItem(CONVERSATION_STEPS_STORAGE_KEY, JSON.stringify(conversationStepsRef.current));
    sessionStorage.setItem(ACTIVE_CONVERSATION_STORAGE_KEY, conversationId);
  }, [currentStep, selectedConversation]);

  useEffect(() => {
    if (!platformId) {
      setSelectedConversation(null);
      setDraft("");
      return;
    }
    if (!conversations.length) return;
    if (isUserManualSelect) return;

    const targetPlatform = PLATFORMS.find(p => p.id === platformId);
    if (!targetPlatform) return;

    const samePlatformConvs = conversations.filter(conv => {
      const convPlatform = platformFor(conv.platform);
      return convPlatform.id === platformId && !isHistoryConversation(conv);
    });

    if (samePlatformConvs.length > 0) {
      const latestConv = samePlatformConvs[0];
      setSelectedConversation(latestConv);
      setCurrentStep(conversationStepsRef.current[latestConv.conversationId] ?? 1);
      setDraft(latestConv.messages?.at(-1)?.question ?? "");
    } else {
      setSelectedConversation(null);
      setDraft("");
    }
  }, [platformId, conversations, isUserManualSelect]);

  useEffect(() => {
    const syncViewport = () => setViewport({ width: window.innerWidth, height: window.innerHeight });
    window.addEventListener("resize", syncViewport);
    return () => window.removeEventListener("resize", syncViewport);
  }, []);

  const selectConversation = async (conversation: Conversation) => {
    if (isHistoryConversation(conversation)) {
      openConversationHistory(conversation.conversationId);
      return;
    }
    if (selectedConversation) {
      conversationStepsRef.current = {
        ...conversationStepsRef.current,
        [selectedConversation.conversationId]: currentStep,
      };
      sessionStorage.setItem(CONVERSATION_STEPS_STORAGE_KEY, JSON.stringify(conversationStepsRef.current));
    }
    const targetConversationId = conversation.conversationId;
    selectedConversationIdRef.current = targetConversationId;
    setIsUserManualSelect(true);
    setSelectedConversation(conversation);
    setPlatformId(platformFor(conversation.platform).id);
    setDraft(conversation.messages?.at(-1)?.question ?? "");
    setError("");
    setCurrentStep(conversationStepsRef.current[targetConversationId] ?? 1);
    try {
      const detail = applyConfirmedEdits(await workbenchApi.getConversation(targetConversationId));
      if (selectedConversationIdRef.current !== targetConversationId) return;
      if (isHistoryConversation(detail)) {
        openConversationHistory(detail.conversationId);
        return;
      }
      setSelectedConversation(detail);
      setDraft(detail.messages?.at(-1)?.question ?? "");
      setConversations((current) => current.map((item) =>
        item.conversationId === detail.conversationId ? detail : item,
      ));
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : "会话详情加载失败");
    }
  };

  const newConversation = () => {
    selectedConversationIdRef.current = null;
    sessionStorage.removeItem(ACTIVE_CONVERSATION_STORAGE_KEY);
    setSelectedConversation(null);
    setDraft("");
    setError("");
    setPlatformId("");
    setIsUserManualSelect(true);
    setCurrentStep(1);
  };

  const generateReply = async (selectedCarrierName?: string): Promise<boolean> => {
    const question = draft.trim();
    if (!question || generating || !currentPlatform) return false;
    const sameQuestion = Boolean(
      selectedConversation
      && latestMessage
      && latestMessage.question.trim() === question,
    );
    const addingQuestionAtSuccessClose = Boolean(
      selectedConversation
      && latestMessage
      && !sameQuestion
      && currentStep === 5
      && isCurStepGenerated,
    );
    const confirmedIntent = typeof latestMessage?.intentRecognition === "string"
      ? latestMessage.intentRecognition.trim()
      : "";
    if (sameQuestion && currentStep === 2 && !confirmedIntent) {
      setStrategyGenerationError("请先完成意图识别");
      return false;
    }
    const resolvedCarrierName = selectedCarrierName?.trim()
      || (currentStep === 2 && requiresCarrierName(latestMessage) ? "快递" : undefined);

    setGenerating(true);
    setError("");
    const input = {
      question,
      platform: currentPlatform.name,
      customerType: selectedConversation?.messages?.at(-1)?.customerType || "直接咨询",
    };
    try {
      let updated: Conversation;
      if (selectedConversation && latestMessage && sameQuestion) {
        const strategyInput = currentStep === 2 ? {
          carrierName: resolvedCarrierName,
          intentRecognition: confirmedIntent,
          customerQuestion: latestMessage.question,
          platform: currentPlatform.name,
        } : undefined;
        const request = (signal?: AbortSignal) => isCurStepGenerated
          ? workbenchApi.regenerateStep(
              selectedConversation.conversationId,
              latestMessage.dialogRound,
              currentStep,
              strategyInput,
              signal,
            )
          : workbenchApi.generateStep(
              selectedConversation.conversationId,
              latestMessage.dialogRound,
              currentStep,
              strategyInput,
              signal,
            );
        updated = currentStep === 2
          ? await runStrategyGeneration((signal) => request(signal))
          : await request();
      } else if (addingQuestionAtSuccessClose && selectedConversation) {
        updated = await workbenchApi.addMessage(selectedConversation.conversationId, input);
        setCurrentStep(1);
      } else if (currentStep === 1) {
        updated = selectedConversation
          ? await workbenchApi.addMessage(selectedConversation.conversationId, input)
          : await workbenchApi.createConversation(input);
        setCurrentStep(1);
      } else {
        throw new Error("输入新的客户问题前，请先切换回第1步");
      }
      if (selectedConversation && latestMessage && sameQuestion
          && currentStep === 2 && isCurStepGenerated) {
        // AC11：重新生成成功后，旧的编辑确认不再生效，避免被重新套用
        clearConfirmedEdit(selectedConversation.conversationId,
          latestMessage.id, STEP_FIELDS[currentStep - 1]);
      }
      updated = applyConfirmedEdits(updated);
      setSelectedConversation(updated);
      setPlatformId(platformFor(updated.platform).id);
      setDraft(question);
      setConversations((current) => [
        updated,
        ...current.filter((item) => item.conversationId !== updated.conversationId),
      ]);
      setTokenInfo(await workbenchApi.getTokenUsage());
      return true;
    } catch (generationError) {
      const message = generationError instanceof Error ? generationError.message : "生成失败，请稍后重试";
      if (currentStep === 2) {
        if (message.includes("当前客户信息不足") || message.includes("请先完成意图识别")) {
          setStrategyGenerationError(message);
        } else if (message.includes("合作快递") || message.includes("物流顾虑")) {
          setStrategyGenerationError(message);
        } else {
          setStrategyGenerationError("生成失败，请稍后重试");
        }
      }
      setError(message);
      return false;
    } finally {
      setGenerating(false);
    }
  };

  const clearConfirmedEdit = (conversationId: string, messageId: number, field: keyof ChatMessage) => {
    const editKey = confirmedEditKey(conversationId, messageId, field);
    if (!Object.prototype.hasOwnProperty.call(confirmedEditsRef.current, editKey)) return;
    confirmedEditsRef.current = { ...confirmedEditsRef.current };
    delete confirmedEditsRef.current[editKey];
    sessionStorage.setItem(CONFIRMED_EDITS_STORAGE_KEY, JSON.stringify(confirmedEditsRef.current));
  };

  if (!isAuthenticatedCustomerService) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-[#f4f6fb]" aria-label="正在验证登录状态">
        {authError ? (
          <div className="rounded-2xl bg-white px-10 py-8 text-center shadow-sm">
            <p className="text-lg font-semibold text-slate-800">暂时无法验证登录状态</p>
            <p className="mt-2 text-sm text-slate-500">{authError}</p>
            <button
              type="button"
              className="mt-5 rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700"
              onClick={() => setAuthRetryKey((key) => key + 1)}
            >
              重新验证
            </button>
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div className="h-screen w-full overflow-hidden bg-[#f4f6fb]">
      <div
        className="flex overflow-hidden bg-[#f4f6fb] text-slate-800 antialiased"
        style={{
          width: layoutScale < 1 ? desktopWidth : viewport.width,
          height: viewport.height / layoutScale,
          transform: `scale(${layoutScale})`,
          transformOrigin: "top left",
          fontFamily: '"PingFang SC","Microsoft YaHei",system-ui,sans-serif',
        }}
      >
        <style>{`
          @keyframes shimmer{0%{background-position:-200% 0}100%{background-position:200% 0}}
          @keyframes spin{to{transform:rotate(360deg)}}
          @keyframes pop{0%{transform:scale(.6);opacity:0}60%{transform:scale(1.12)}100%{transform:scale(1);opacity:1}}
          .shimmer{background:linear-gradient(100deg,transparent 30%,rgba(37,99,235,.10) 50%,transparent 70%);background-size:200% 100%;animation:shimmer 1.2s linear infinite}
          .spin{animation:spin .9s linear infinite}
          .pop{animation:pop .35s cubic-bezier(.2,.9,.3,1.3)}
          ::-webkit-scrollbar{width:8px;height:8px}::-webkit-scrollbar-thumb{background:#d6dcea;border-radius:8px}::-webkit-scrollbar-thumb:hover{background:#c2cad8}
        `}</style>

        <Sidebar activeItem={activeItem} />

        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar
            displayName={displayName}
            title={view === "history" ? "对话记录" : view === "evaluation" ? "我的评估" : "智能对话工作台"}
            subtitle={
              view === "history"
                ? "查看当前账号的历史会话与完整沟通记录"
                : view === "evaluation"
                  ? "查看当前账号的风险命中与服务评估记录"
                  : "AI 生成高质量回复，助力客服高效服务"
            }
          />

          {view === "history" ? (
            <ConversationHistoryPage />
          ) : view === "evaluation" ? (
            <MyEvaluationPage />
          ) : (
          
          <main
            className="min-h-0 flex-1 overflow-hidden p-5"
            style={{
                display: "grid",
                gridTemplateColumns: "280px 1fr 1.2fr",
                gap: "1.25rem",
           }}
          >
              <ConversationList
                conversations={conversations}
                activeConversationId={selectedConversation?.conversationId}
                loading={loading}
                onSelect={(conversation) => void selectConversation(conversation)}
                onNewConversation={newConversation}
              />
              <ChatPanel
                platformId={platformId}
                onPlatformChange={handleChangePlatform}
                messages={selectedConversation?.messages ?? []}
                text={draft}
                onTextChange={(value) => {
                  setDraft(value);
                  if (currentStep !== 5 && latestMessage && value.trim() !== latestMessage.question.trim()) {
                    setCurrentStep(1);
                  }
                }}
                generating={generating}
                error={error}
                onGenerate={() => void generateReply()}
                isCurStepGenerated={isCurStepGenerated}
                generationDisabledReason={currentStep === 2 && !latestMessage?.intentRecognition?.trim()
                  ? "请先完成意图识别"
                  : undefined}
                generationLocked={generationLocked}
              />
              <AssistantPanel
                conversationId={selectedConversation?.conversationId}
                conversationStatus={selectedConversation?.status}
                lastActivityAt={selectedConversation?.updatedAt || selectedConversation?.createdAt}
                platformName={currentPlatform?.name ?? ""}
                messages={selectedConversation?.messages ?? []}
                tokenInfo={tokenInfo}
                generating={generating}
                strategyGenerating={strategyGenerating}
                strategyGenerationError={strategyGenerationError}
                step={currentStep}
                onNextStep={() => void handleNextStep()}
                onRetryStrategy={() => {
                  if (currentStep === 1) void handleNextStep();
                  else void generateReply();
                }}
                onJumpStep={handleJumpStep}
                onContentChange={handleAssistantContentChange}
                onConversationArchived={() => void archiveSelectedConversation()}
              />
            </main>
          )}
        </div>
      </div>
    </div>
  );
}
