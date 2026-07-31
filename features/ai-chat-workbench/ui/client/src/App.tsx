import { useEffect, useState } from "react";
import { PLATFORMS } from "./mock/data";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import ConversationList from "./components/ConversationList";
import ChatPanel from "./components/ChatPanel";
import AssistantPanel from "./components/AssistantPanel";
import ConversationHistoryPage from "./components/ConversationHistoryPage";
import MyEvaluationPage from "./components/MyEvaluationPage";
import type { SidebarItemId } from "./components/Sidebar";

type WorkbenchView = "conversation" | "history" | "evaluation";

function getWorkbenchView(): WorkbenchView {
  const requestedView = new URLSearchParams(window.location.search).get("view");
  return requestedView === "history" || requestedView === "evaluation" ? requestedView : "conversation";
}

function getLoginUrl() {
  const configuredUrl = import.meta.env.VITE_LOGIN_URL;
  if (configuredUrl) {
    return configuredUrl;
  }

  const isLocal = window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost";
  return isLocal
    ? `${window.location.protocol}//${window.location.hostname}:15173/`
    : `${window.location.origin}/`;
}

export default function App() {
  const [platform, setPlatform] = useState("tm");
  const [regen, setRegen] = useState(false);
  const [isAuthenticatedCustomerService, setIsAuthenticatedCustomerService] = useState(false);
  const [displayName, setDisplayName] = useState("客服");
  const [viewport, setViewport] = useState(() => ({
    width: window.innerWidth,
    height: window.innerHeight,
  }));
  const cur = PLATFORMS.find((p) => p.id === platform)!;
  const view = getWorkbenchView();
  const activeItem: SidebarItemId = view;
  const desktopWidth = 1440;
  const layoutScale = Math.min(1, viewport.width / desktopWidth);

  useEffect(() => {
    const controller = new AbortController();

    const verifyLogin = async () => {
      try {
        const response = await fetch("/api/v1/auth/me", {
          credentials: "include",
          signal: controller.signal,
        });
        const payload = response.ok ? await response.json() : null;
        const roleType = payload?.data?.roleType;

        if (response.ok && payload?.code === "OK" && roleType === "CUSTOMER_SERVICE") {
          setDisplayName(payload.data.username || payload.data.account || "客服");
          setIsAuthenticatedCustomerService(true);
          return;
        }
      } catch (error) {
        if (controller.signal.aborted) {
          return;
        }
      }

      window.location.replace(getLoginUrl());
    };

    void verifyLogin();
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const syncViewport = () => {
      setViewport({ width: window.innerWidth, height: window.innerHeight });
    };

    window.addEventListener("resize", syncViewport);
    return () => window.removeEventListener("resize", syncViewport);
  }, []);

  const doRegen = () => {
    setRegen(true);
    setTimeout(() => setRegen(false), 1200);
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
        @keyframes floaty{0%,100%{transform:translateY(0)}50%{transform:translateY(-3px)}}
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
            <ConversationList platformName={cur.name} />
            <ChatPanel platformId={platform} onPlatformChange={setPlatform} regen={regen} onRegen={doRegen} />
            <AssistantPanel platformName={cur.name} regen={regen} />
          </main>
        )}
      </div>
    </div>
    </div>
  );
}
