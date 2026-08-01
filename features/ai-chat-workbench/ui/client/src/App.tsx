import { useCallback, useEffect, useState } from "react";
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
import type { Conversation, TokenInfo } from "./types";

type WorkbenchView = "conversation" | "history" | "evaluation";

function getWorkbenchView(): WorkbenchView {
  const requestedView = new URLSearchParams(window.location.search).get("view");
  return requestedView === "history" || requestedView === "evaluation" ? requestedView : "conversation";
}

function getLoginUrl() {
  const configuredUrl = import.meta.env.VITE_LOGIN_URL;
  if (configuredUrl) return configuredUrl;
  const isLocal = window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost";
  return isLocal
    ? `${window.location.protocol}//${window.location.hostname}:15173/`
    : `${window.location.origin}/`;
}

export default function App() {
  const [platformId, setPlatformId] = useState("tm");
  const [isAuthenticatedCustomerService, setIsAuthenticatedCustomerService] = useState(false);
  const [displayName, setDisplayName] = useState("客服");
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [tokenInfo, setTokenInfo] = useState<TokenInfo>();
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState("");
  const [viewport, setViewport] = useState(() => ({ width: window.innerWidth, height: window.innerHeight }));

  const currentPlatform = PLATFORMS.find((platform) => platform.id === platformId) ?? PLATFORMS[1];
  const view = getWorkbenchView();
  const activeItem: SidebarItemId = view;
  const desktopWidth = 1440;
  const layoutScale = Math.min(1, viewport.width / desktopWidth);

  useEffect(() => {
    const controller = new AbortController();
    const verifyLogin = async () => {
      try {
        const response = await fetch("/api/v1/auth/me", { credentials: "include", signal: controller.signal });
        const payload = response.ok ? await response.json() : null;
        if (response.ok && payload?.code === "OK" && payload?.data?.roleType === "CUSTOMER_SERVICE") {
          setDisplayName(payload.data.username || payload.data.account || "客服");
          setIsAuthenticatedCustomerService(true);
          return;
        }
      } catch {
        if (controller.signal.aborted) return;
      }
      window.location.replace(getLoginUrl());
    };
    void verifyLogin();
    return () => controller.abort();
  }, []);

  const loadWorkbench = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [loadedConversations, loadedTokenInfo] = await Promise.all([
        workbenchApi.listConversations(),
        workbenchApi.getTokenUsage(),
      ]);
      setConversations(loadedConversations);
      setTokenInfo(loadedTokenInfo);
      const first = loadedConversations[0] ?? null;
      setSelectedConversation(first);
      if (first) {
        setPlatformId(platformFor(first.platform).id);
        setDraft(first.messages?.at(-1)?.question ?? "");
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

  useEffect(() => {
    const syncViewport = () => setViewport({ width: window.innerWidth, height: window.innerHeight });
    window.addEventListener("resize", syncViewport);
    return () => window.removeEventListener("resize", syncViewport);
  }, []);

  const selectConversation = async (conversation: Conversation) => {
    setSelectedConversation(conversation);
    setPlatformId(platformFor(conversation.platform).id);
    setDraft(conversation.messages?.at(-1)?.question ?? "");
    setError("");
    try {
      const detail = await workbenchApi.getConversation(conversation.conversationId);
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
    setSelectedConversation(null);
    setDraft("");
    setError("");
  };

  const generateReply = async () => {
    const question = draft.trim();
    if (!question || generating) return;
    setGenerating(true);
    setError("");
    const input = {
      question,
      platform: currentPlatform.name,
      customerType: selectedConversation?.messages?.at(-1)?.customerType || "直接咨询",
    };
    try {
      const updated = selectedConversation
        ? await workbenchApi.addMessage(selectedConversation.conversationId, input)
        : await workbenchApi.createConversation(input);
      setSelectedConversation(updated);
      setPlatformId(platformFor(updated.platform).id);
      setDraft(question);
      setConversations((current) => [
        updated,
        ...current.filter((item) => item.conversationId !== updated.conversationId),
      ]);
      setTokenInfo(await workbenchApi.getTokenUsage());
    } catch (generationError) {
      setError(generationError instanceof Error ? generationError.message : "AI 回复生成失败");
    } finally {
      setGenerating(false);
    }
  };

  if (!isAuthenticatedCustomerService) {
    return <div className="h-screen w-full bg-[#f4f6fb]" aria-label="正在验证登录状态" />;
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
            <main className="workbench-grid grid min-h-0 flex-1 gap-5 overflow-hidden p-5">
              <ConversationList
                conversations={conversations}
                activeConversationId={selectedConversation?.conversationId}
                loading={loading}
                onSelect={(conversation) => void selectConversation(conversation)}
                onNewConversation={newConversation}
              />
              <ChatPanel
                platformId={platformId}
                onPlatformChange={setPlatformId}
                messages={selectedConversation?.messages ?? []}
                text={draft}
                onTextChange={setDraft}
                generating={generating}
                error={error}
                onGenerate={() => void generateReply()}
              />
              <AssistantPanel
                platformName={currentPlatform.name}
                messages={selectedConversation?.messages ?? []}
                tokenInfo={tokenInfo}
                generating={generating}
              />
            </main>
          )}
        </div>
      </div>
    </div>
  );
}
