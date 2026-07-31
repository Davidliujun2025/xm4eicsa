import { Help, Bell, Chev } from "./icons";

type TopbarProps = {
  title?: string;
  subtitle?: string;
  displayName?: string;
};

export default function Topbar({
  title = "智能对话工作台",
  subtitle = "AI 生成高质量回复，助力客服高效服务",
  displayName = "客服",
}: TopbarProps) {
  const avatarText = displayName.trim().slice(0, 1) || "客";

  return (
    <header className="flex items-center justify-between border-b border-slate-200/80 bg-white px-8 py-4">
      <div>
        <h1 className="text-[22px] font-bold tracking-tight text-slate-900">{title}</h1>
        <p className="mt-0.5 text-[13px] text-slate-400">{subtitle}</p>
      </div>

      <div className="flex items-center gap-6">
        <button className="flex items-center gap-1.5 text-[14px] text-slate-500 transition-colors hover:text-blue-600">
          <Help className="h-[18px] w-[18px]" />
          帮助中心
        </button>

        <button className="relative text-slate-500 transition-colors hover:text-blue-600">
          <Bell className="h-[22px] w-[22px]" />
          <span className="absolute -right-1 -top-1 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
            2
          </span>
        </button>

        <button className="flex items-center gap-2">
          <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-fuchsia-400 to-purple-500 text-[13px] font-bold text-white">
            {avatarText}
          </span>
          <span className="text-[14px] font-medium text-slate-700">{displayName}</span>
          <Chev className="h-4 w-4 text-slate-400" />
        </button>
      </div>
    </header>
  );
}
