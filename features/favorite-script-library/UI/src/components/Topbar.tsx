import { Help, Bell, Chev } from "./icons";
import { useState, useEffect, useRef } from "react";
import { logoutAndRedirect } from "../utils/auth";

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
  const [dropdownOpen, setDropdownOpen] = useState(false);
  // 弹窗类型：''无弹窗 | 'logout'退出登录 | 'switch'切换账号
  const [modalType, setModalType] = useState<"" | "logout" | "switch">("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, []);

  const confirmAccountAction = async () => {
    setIsSubmitting(true);
    try {
      await logoutAndRedirect(window.location.href);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 打开退出确认弹窗
  const openLogoutModal = () => {
    setDropdownOpen(false);
    setModalType("logout");
  };

  // 打开切换账号弹窗
  const openSwitchModal = () => {
    setDropdownOpen(false);
    setModalType("switch");
  };

  // 关闭弹窗
  const closeModal = () => {
    setModalType("");
  };

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

        {/* 用户下拉容器 */}
        <div className="relative" ref={dropdownRef}>
          <button
            className="flex items-center gap-2 text-slate-700 transition-colors hover:text-blue-600"
            onClick={() => setDropdownOpen(!dropdownOpen)}
          >
            <span className="grid h-9 w-9 place-items-center rounded-full bg-gradient-to-br from-fuchsia-400 to-purple-500 text-[13px] font-bold text-white">
              {avatarText}
            </span>
            <span className="text-[14px] font-medium">{displayName}</span>
            <Chev
              className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${
                dropdownOpen ? "rotate-180" : ""
              }`}
            />
          </button>

          {/* 下拉菜单：切换账号、退出登录 */}
          {dropdownOpen && (
            <div className="absolute right-0 top-[calc(100%+8px)] z-50 w-[120px] rounded-lg border border-slate-200 bg-white py-2 shadow-lg">
              <button
                onClick={openSwitchModal}
                className="w-full px-4 py-2 text-left text-[14px] text-slate-600 transition-colors hover:bg-slate-100"
              >
                切换账号
              </button>
              <button
                onClick={openLogoutModal}
                className="w-full px-4 py-2 text-left text-[14px] text-slate-600 transition-colors hover:bg-slate-100 hover:text-red-500"
              >
                退出登录
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 全局确认弹窗 */}
      {modalType && (
        <>
          <div className="fixed inset-0 z-[100] bg-black/40 flex items-center justify-center">
            <div className="w-[360px] rounded-xl bg-white p-6 shadow-xl">
              <h3 className="text-[16px] font-medium text-slate-800 mb-6">
                {modalType === "logout" ? "确认退出登录" : "确认切换账号"}
              </h3>
              <div className="flex justify-end gap-3">
                <button
                  onClick={closeModal}
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 transition-colors"
                >
                  否
                </button>
                <button
                  onClick={() => void confirmAccountAction()}
                  disabled={isSubmitting}
                  className="px-5 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition-colors"
                >
                  {isSubmitting ? "处理中..." : "是"}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </header>
  );
}
