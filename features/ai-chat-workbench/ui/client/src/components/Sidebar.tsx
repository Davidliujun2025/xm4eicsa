import { useState, useEffect } from "react";
import { Chat, Clock, Book, Clip, Chart } from "./icons";

const NAV = [
  { Icon: Chat, t: "智能对话" },
  { Icon: Clock, t: "对话记录" },
  { Icon: Book, t: "个人话术库" },
  { Icon: Clip, t: "我的评估" },
  { Icon: Chart, t: "个人看板" },
];

type Props = {
  activeIndex?: number;
  onSelect?: (index: number) => void;
};

export default function Sidebar({ activeIndex = 0, onSelect }: Props) {
  const [activeIdx, setActiveIdx] = useState(activeIndex);

  useEffect(() => {
    setActiveIdx(activeIndex);
  }, [activeIndex]);

  const handleSelect = (idx: number) => {
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
      </nav>
    </aside>
  );
}