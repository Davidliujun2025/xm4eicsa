import logo from "../assets/logo.png";
import { Chat, Clock, Book, Clip, Chart, Chev } from "./icons";
import { Settings } from "lucide-react";
import { useState, useEffect } from "react";

export type SidebarItemId = "conversation" | "history" | "scripts" | "evaluation" | "dashboard" | "tokenUsage" | "setting";

type SidebarItem = {
  id: SidebarItemId;
  Icon: typeof Chat;
  label: string;
  isParent?: boolean;
  parentId?: SidebarItemId;
};

const NAV: SidebarItem[] = [
  { id: "conversation", Icon: Chat, label: "智能对话" },
  { id: "history", Icon: Clock, label: "对话记录" },
  { id: "scripts", Icon: Book, label: "个人话术库" },
  { id: "evaluation", Icon: Clip, label: "我的评估" },
  { id: "dashboard", Icon: Chart, label: "数据看板", isParent: true },
  { id: "tokenUsage", Icon: Chart, label: "Token统计", parentId: "dashboard" },
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
      return "";
    case "tokenUsage":
      return import.meta.env.VITE_TOKEN_USAGE_URL
        || (local ? localAppUrl(5180) : `${window.location.origin}/token-usage/`);
    case "setting":
      return local ? `${window.location.origin}/?view=setting` : `${window.location.origin}/workbench/?view=setting`;
    default:
      return window.location.origin;
  }
}

export default function Sidebar({ activeItem }: { activeItem: SidebarItemId }) {
  const [expandState, setExpandState] = useState<Record<string, boolean>>(() => {
    const saved = localStorage.getItem("sidebar_expand");
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return {};
      }
    }
    return { dashboard: true };
  });

  useEffect(() => {
    localStorage.setItem("sidebar_expand", JSON.stringify(expandState));
  }, [expandState]);

  const toggleExpand = (parentId: SidebarItemId) => {
    setExpandState(prev => ({
      ...prev,
      [parentId]: !prev[parentId]
    }));
  };

  const dashboardOpen = expandState.dashboard ?? true;
  const rootItems = NAV.filter(item => !item.parentId);

  return (
    <aside className="flex w-[232px] shrink-0 flex-col border-r border-slate-200/80 bg-white h-screen">
      {/* Logo仅图片，清除所有文字 */}
      <div className="px-5 py-5">
        <img
          className="h-16 object-contain"
          src={logo}
          alt="CarePilot AI"
        />
      </div>

      <nav className="flex flex-col gap-1 px-3 flex-1" aria-label="客服工作台主导航">
        {rootItems.map(rootItem => {
          const { id, Icon, label, isParent } = rootItem;
          const active = activeItem === id;

          if (isParent) {
            const isOpen = expandState[id] ?? true;
            return (
              <button
                key={id}
                type="button"
                onClick={() => toggleExpand(id)}
                className={`group flex items-center justify-between rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200 ${
                  active ? "bg-blue-50 text-blue-600" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className={`h-[22px] w-[22px] transition-transform duration-200 ${active ? "" : "group-hover:scale-110"}`} />
                  {label}
                </div>
                <Chev
                  className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${isOpen ? "rotate-180" : ""}`}
                />
              </button>
            );
          }

          // 普通一级菜单
          return (
            <button
              key={id}
              type="button"
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

        {/* ========= 重点改动：对齐App.tsx写法，移除通用子菜单遍历，硬编码Token统计 ========= */}
        {dashboardOpen && (
          <button
            type="button"
            onClick={() => window.location.assign(getSidebarTarget("tokenUsage"))}
            className={`rounded-xl pl-8 pr-4 py-3 text-[15px] font-medium transition-all duration-200 text-left ${
              activeItem === "tokenUsage" ? "bg-blue-50 text-blue-600" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
            }`}
          >
            Token统计
          </button>
        )}

        {/* 底部设置 */}
        <button
          type="button"
          onClick={() => window.location.assign(getSidebarTarget("setting"))}
          className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200 mt-3 ${
            activeItem === "setting" ? "bg-blue-50 text-blue-600" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
          }`}
        >
          <Settings className={`h-[22px] w-[22px] transition-transform duration-200 group-hover:scale-110`} />
          设置
        </button>
      </nav>
    </aside>
  );
}