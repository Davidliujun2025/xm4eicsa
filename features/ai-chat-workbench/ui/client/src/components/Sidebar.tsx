import { useState, useEffect } from "react";
import { Chat, Clock, Book, Clip, Chart } from "./icons";
import { RiSettings3Line as GearIcon } from "react-icons/ri";

const NAV = [
  { Icon: Chat, t: "智能对话" },
  { Icon: Clock, t: "对话记录" },
  { Icon: Book, t: "个人话术库" },
  { Icon: Clip, t: "我的评估" },
];

type Props = {
  activeIndex?: number;
  onSelect?: (index: number) => void;
};

export default function Sidebar({ activeIndex = 0, onSelect }: Props) {
  const [activeIdx, setActiveIdx] = useState(activeIndex);
  const [dashboardOpen, setDashboardOpen] = useState(true);

  const appLinks: Record<number, string> = {
    0: `${window.location.origin}/workbench/`,
    2: `${window.location.origin}/favorite-script-library/`,
    4: `${window.location.origin}/token-usage/`,
  };

  useEffect(() => {
    setActiveIdx(activeIndex);
  }, [activeIndex]);

  const handleSelect = (idx: number) => {
    if (!onSelect && appLinks[idx]) {
      window.location.href = appLinks[idx];
      return;
    }
    setActiveIdx(idx);
    if (onSelect) onSelect(idx);
  };

  return (
    <aside className="flex w-[232px] shrink-0 flex-col border-r border-slate-200/80 bg-white">
      <div className="px-5 pt-6 pb-6">
        <img src="/logo.png" alt="CarePilot AI" className="w-full h-auto object-contain" />
      </div>
      <nav className="flex flex-col gap-1 px-3">
        {NAV.map((n, idx) => {
          const isActive = activeIdx === idx;
          return (
            <button
              key={n.t}
              onClick={() => handleSelect(idx)}
              className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200 ${
                isActive
                  ? "bg-blue-50 text-blue-600"
                  : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
              }`}
            >
              <n.Icon className={`h-[22px] w-[22px] ${isActive ? "" : "group-hover:scale-110"}`} />
              {n.t}
            </button>
          );
        })}

        <button
          type="button"
          onClick={() => setDashboardOpen((v) => !v)}
          className="group flex items-center justify-between rounded-xl px-4 py-3 text-[15px] font-medium text-slate-500 transition-all duration-200 hover:bg-slate-50 hover:text-slate-800"
        >
          <span className="flex items-center gap-3">
            <Chart className="h-[22px] w-[22px] group-hover:scale-110" />
            数据看板
          </span>
          <span
            className={`text-[12px] leading-none transition-transform duration-200 ${
              dashboardOpen ? "rotate-0" : "-rotate-90"
            }`}
          >
            ▼
          </span>
        </button>

        {dashboardOpen && (
          <button
            type="button"
            onClick={() => handleSelect(4)}
            className={`rounded-xl px-4 py-2.5 pl-[62px] text-left text-[13px] font-medium transition-all duration-200 ${
              activeIdx === 4
                ? "bg-blue-50 text-blue-600"
                : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
            }`}
          >
            Token统计
          </button>
        )}

        <button
          type="button"
          onClick={() => handleSelect(5)}
          className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200 ${
            activeIdx === 5
              ? "bg-blue-50 text-blue-600"
              : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
          }`}
        >
          <GearIcon className={`h-[22px] w-[22px] ${activeIdx === 5 ? "" : "group-hover:scale-110"}`} />
          设置
        </button>
      </nav>
    </aside>
  );
}