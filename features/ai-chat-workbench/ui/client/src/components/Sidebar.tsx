import logo from "../assets/logo.png";
import { Chat, Clock, Book, Clip, Chart } from "./icons";

export type SidebarItemId = "conversation" | "history" | "scripts" | "evaluation" | "dashboard";

type SidebarItem = {
  id: SidebarItemId;
  Icon: typeof Chat;
  label: string;
};

const NAV: SidebarItem[] = [
  { id: "conversation", Icon: Chat, label: "智能对话" },
  { id: "history", Icon: Clock, label: "对话记录" },
  { id: "scripts", Icon: Book, label: "个人话术库" },
  { id: "evaluation", Icon: Clip, label: "我的评估" },
  { id: "dashboard", Icon: Chart, label: "个人看板" },
];

function isLocalDevelopment() {
  return window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost";
}

function localAppUrl(port: number, path = "/") {
  return `${window.location.protocol}//${window.location.hostname}:${port}${path}`;
}

export function getSidebarTarget(id: SidebarItemId) {
  const local = isLocalDevelopment();

  switch (id) {
    case "conversation":
      return local ? `${window.location.origin}/` : `${window.location.origin}/workbench/`;
    case "history":
      return local ? `${window.location.origin}/?view=history` : `${window.location.origin}/workbench/?view=history`;
    case "scripts":
      return import.meta.env.VITE_FAVORITE_SCRIPT_URL
        || (local ? localAppUrl(5178) : `${window.location.origin}/favorite-script-library/`);
    case "evaluation":
      return local ? `${window.location.origin}/?view=evaluation` : `${window.location.origin}/workbench/?view=evaluation`;
    case "dashboard":
      return import.meta.env.VITE_TOKEN_USAGE_URL
        || (local ? localAppUrl(5180) : `${window.location.origin}/token-usage/`);
  }
}

export default function Sidebar({ activeItem }: { activeItem: SidebarItemId }) {
  return (
    <aside className="flex w-[232px] shrink-0 flex-col border-r border-slate-200/80 bg-white">
      <div className="flex items-center gap-3 px-5 py-5">
        <img className="h-16 w-16 shrink-0 object-contain" src={logo} alt="CarePilot AI" />
        <div className="leading-tight">
          <div className="text-[17px] font-extrabold tracking-tight text-blue-600">CarePilot AI</div>
          <div className="text-[12px] text-slate-400">智能客服助手</div>
        </div>
      </div>

      <nav className="flex flex-col gap-1 px-3" aria-label="客服工作台主导航">
        {NAV.map(({ id, Icon, label }) => {
          const active = activeItem === id;
          return (
            <button
              key={id}
              type="button"
              aria-current={active ? "page" : undefined}
              onClick={() => window.location.assign(getSidebarTarget(id))}
              className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200 ${
                active ? "bg-blue-50 text-blue-600" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
              }`}
            >
              <Icon className={`h-[22px] w-[22px] transition-transform duration-200 ${active ? "" : "group-hover:scale-110"}`} />
              {label}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
