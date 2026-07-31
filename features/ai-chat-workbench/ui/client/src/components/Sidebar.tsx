import logo from "../assets/logo.png";
import { Chat, Clock, Book, Clip, Chart } from "./icons";

const NAV = [
  { Icon: Chat, t: "智能对话", on: true },
  { Icon: Clock, t: "对话记录" },
  { Icon: Book, t: "个人话术库" },
  { Icon: Clip, t: "我的评估" },
  { Icon: Chart, t: "个人看板" },
];

export default function Sidebar() {
  return (
    <aside className="flex w-[232px] shrink-0 flex-col border-r border-slate-200/80 bg-white">
      <div className="flex items-center gap-3 px-5 py-5">
        <img className="h-16 w-16 shrink-0 object-contain" src={logo} alt="CarePilot AI" />
        <div className="leading-tight">
          <div className="text-[17px] font-extrabold tracking-tight text-blue-600">CarePilot AI</div>
          <div className="text-[12px] text-slate-400">智能客服助手</div>
        </div>
      </div>

      <nav className="flex flex-col gap-1 px-3">
        {NAV.map((n) => (
          <button
            key={n.t}
            className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-[15px] font-medium transition-all duration-200  $ {
              n.on ? "bg-blue-50 text-blue-600" : "text-slate-500 hover:bg-slate-50 hover:text-slate-800"
            }`}
          >
            <n.Icon className={`h-[22px] w-[22px] transition-transform duration-200  $ {n.on ? "" : "group-hover:scale-110"}`} />
            {n.t}
          </button>
        ))}
      </nav>
    </aside>
  );
}
