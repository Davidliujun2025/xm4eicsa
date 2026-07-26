import { useState } from "react";
import { PLATFORMS } from "./mock/data";
import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";
import ConversationList from "./components/ConversationList";
import ChatPanel from "./components/ChatPanel";
import AssistantPanel from "./components/AssistantPanel";

export default function App() {
  const [platform, setPlatform] = useState("tm");
  const [regen, setRegen] = useState(false);
  const cur = PLATFORMS.find((p) => p.id === platform)!;

  const doRegen = () => {
    setRegen(true);
    setTimeout(() => setRegen(false), 1200);
  };

  return (
    <div
      className="flex h-screen w-full overflow-hidden bg-[#f4f6fb] text-slate-800 antialiased"
      style={{ fontFamily: '"PingFang SC","Microsoft YaHei",system-ui,sans-serif' }}
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

      <Sidebar />

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />

        <main className="grid min-h-0 flex-1 grid-cols-[300px_minmax(0,1fr)_372px] gap-5 overflow-hidden p-5">
          <ConversationList platformName={cur.name} />
          <ChatPanel platformId={platform} onPlatformChange={setPlatform} regen={regen} onRegen={doRegen} />
          <AssistantPanel platformName={cur.name} regen={regen} />
        </main>
      </div>
    </div>
  );
}