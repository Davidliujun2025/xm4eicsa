import { useState, useRef, useEffect } from "react";
import { STEPS } from "../mock/data";
import { useCountUp } from "../hooks/useCountUp";
import { Spark, Check, Flag, Arrow } from "./icons";

// 收藏项类型
type FavoriteItem = {
  id: string;
  step: string;
  content: string;
  platform: string;
  questionType: string;
  time: string;
};

export default function AssistantPanel({ platformName, regen }: { platformName: string; regen: boolean }) {
  const [step, setStep] = useState(3);
  const [done, setDone] = useState(false);
  const tok = useCountUp(3600);
  const contentRef = useRef<HTMLDivElement>(null);

  // 收藏列表状态（从 localStorage 初始化）
  const [favorites, setFavorites] = useState<FavoriteItem[]>(() => {
    const stored = localStorage.getItem("favorites");
    return stored ? JSON.parse(stored) : [];
  });

  // 当收藏变化时保存到 localStorage
  useEffect(() => {
    localStorage.setItem("favorites", JSON.stringify(favorites));
  }, [favorites]);

  // 判断当前步骤是否已收藏
  const isFavorite = () => {
    const currentStepName = STEPS[step - 1];
    return favorites.some((f) => f.step === currentStepName && f.platform === platformName);
  };

  // 获取当前话术内容（编辑后的内容）
  const getCurrentContent = () => {
    return contentRef.current?.innerText || "";
  };

  // 切换收藏 / 取消收藏
  const toggleFavorite = () => {
    const currentStepName = STEPS[step - 1];
    const content = getCurrentContent();
    if (!content.trim()) return;

    if (isFavorite()) {
      // 取消收藏：移除匹配项（按步骤+平台）
      setFavorites((prev) =>
        prev.filter((f) => !(f.step === currentStepName && f.platform === platformName))
      );
    } else {
      // 添加收藏
      const newItem: FavoriteItem = {
        id: `${currentStepName}-${platformName}-${Date.now()}`,
        step: currentStepName,
        content,
        platform: platformName,
        questionType: `${platformName}咨询`, // 可根据实际情况从上下文传入 tag
        time: new Date().toISOString(),
      };
      setFavorites((prev) => [...prev, newItem]);
    }
  };

  // 跳转步骤
  const goToStep = (target: number) => {
    if (target >= 1 && target <= 5) {
      setStep(target);
      if (target >= 5) setDone(true);
      else setDone(false);
    }
  };

  const advance = () => {
    if (step < 5) {
      const next = step + 1;
      setStep(next);
      if (next >= 5) setDone(true);
    }
  };

  const handleViewDetail = () => {
    alert("查看明细功能开发中");
  };

  return (
    <section className="flex min-h-0 flex-col gap-4">
      {/* Token 卡 */}
      <div className="flex items-center gap-1.5 rounded-2xl border border-slate-200/80 bg-white px-3 py-2 shadow-sm">
        <span className="grid h-5 w-5 shrink-0 place-items-center rounded-full bg-blue-50 text-blue-600">
          <Spark className="h-3.5 w-3.5" />
        </span>
        <span className="whitespace-nowrap text-[11px] font-medium text-slate-600">今日Token消耗</span>
        <span className="text-[13px] font-bold text-slate-900">{tok.toLocaleString()}</span>
        <span className="text-[11px] text-slate-400">/ 10,000</span>
        <div className="h-1 w-12 overflow-hidden rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-blue-500 transition-all duration-1000"
            style={{ width: `${(tok / 10000) * 100}%` }}
          />
        </div>
        <span className="text-[11px] font-semibold text-blue-600">36%</span>
        <button
          onClick={handleViewDetail}
          className="ml-auto flex items-center gap-0.5 whitespace-nowrap text-[11px] font-medium text-blue-600 hover:opacity-70"
        >
          查看明细
          <Arrow className="h-3 w-3" />
        </button>
      </div>

      {/* AI 助手主体 */}
      <div className="flex min-h-0 flex-1 flex-col rounded-2xl border border-slate-200/80 bg-white shadow-sm">
        <div className="flex items-start justify-between border-b border-slate-100 px-5 py-4">
          <div>
            <div className="text-[16px] font-bold text-slate-800">AI智能助手</div>
            <div className="mt-0.5 text-[12.5px] text-slate-400">五步生成法</div>
          </div>
          <Spark className="h-6 w-6 text-blue-500" />
        </div>

        {/* 进度条 */}
        <div className="px-5 py-5">
          <div className="flex items-center">
            {STEPS.map((s, i) => {
              const n = i + 1;
              const isDone = n < step;
              const isCur = n === step;
              return (
                <div key={s} className="flex flex-1 flex-col items-center last:flex-none">
                  <div className="flex w-full items-center">
                    <button
                      onClick={() => goToStep(n)}
                      className={`grid h-7 w-7 shrink-0 place-items-center rounded-full text-[12px] font-bold transition-all duration-300 focus:outline-none focus:ring-2 focus:ring-blue-300 ${
                        isDone
                          ? "bg-blue-600 text-white"
                          : isCur
                          ? "bg-blue-600 text-white ring-4 ring-blue-100"
                          : "bg-white text-slate-400 ring-1 ring-slate-200 hover:ring-blue-400"
                      }`}
                    >
                      {isDone ? <Check className="h-4 w-4" /> : n}
                    </button>
                    {i < STEPS.length - 1 && (
                      <div
                        className={`mx-1 h-[3px] flex-1 rounded-full transition-colors duration-300 ${
                          n < step ? "bg-blue-600" : "bg-slate-200"
                        }`}
                      />
                    )}
                  </div>
                  <span
                    className={`mt-2 whitespace-nowrap text-[11.5px] ${
                      isCur ? "font-semibold text-blue-600" : isDone ? "text-blue-600" : "text-slate-400"
                    }`}
                  >
                    {s}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* 话术卡 */}
        <div className="min-h-0 flex-1 overflow-y-auto px-5 pb-4">
          <div className={`rounded-2xl border border-slate-100 bg-slate-50/50 p-4 ${regen ? "shimmer" : ""}`}>
            <div className="flex items-center justify-between">
              <span className="text-[16px] font-bold text-slate-800">
                第{step}步 – {STEPS[step - 1]}
              </span>
              <span className="rounded-md bg-blue-50 px-2.5 py-1 text-[11.5px] font-medium text-blue-600">
                适用平台：{platformName}
              </span>
            </div>

            <div className="mt-3 text-[12.5px] text-slate-400">适用场景与语气</div>
            <div className="mt-2 flex gap-2">
              <span className="rounded-lg bg-blue-100 px-3 py-1.5 text-[12.5px] font-medium text-blue-700">
                {platformName}售后
              </span>
              <span className="rounded-lg bg-emerald-100 px-3 py-1.5 text-[12.5px] font-medium text-emerald-700">
                专业友好
              </span>
            </div>

            <div className="mt-4 text-[12.5px] text-slate-400">推荐话术</div>
            <div
              ref={contentRef}
              contentEditable
              suppressContentEditableWarning
              className="mt-2 rounded-xl border border-slate-200 bg-white p-3.5 text-[13px] leading-relaxed text-slate-700 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
              您好，感谢您关注我们的商品！若订单页面标注支持七天无理由退货，且商品未拆封、包装及配件完整，可在签收后 7 天内通过“我的订单—申请售后”提交申请。具体条件和非质量问题的运费承担方式，请以{platformName}最新规则及订单页面提示为准。
            </div>

            <div className="mt-3 flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-[12.5px] text-emerald-700">
              <Check className="mt-0.5 h-4 w-4 shrink-0" />
              风险检测：未发现高风险表达；回复前请核对{platformName}最新规则。
            </div>

            <div className="mt-3 flex items-center justify-between">
              <span className="text-[12.5px] text-slate-400">话术已生成，可直接用于客服回复</span>
              {/* 收藏按钮 */}
              <button
                onClick={toggleFavorite}
                className={`flex items-center gap-1.5 whitespace-nowrap rounded-lg border px-4 py-2 text-[12.5px] font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-300 ${
                  isFavorite()
                    ? "bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100"
                    : "border-blue-200 text-blue-600 hover:bg-blue-50"
                }`}
              >
                <Flag className="h-4 w-4" />
                {isFavorite() ? "已收藏" : "收藏到话术库"}
              </button>
            </div>
          </div>
        </div>

        {/* 底部按钮 */}
        <div className="border-t border-slate-100 p-4">
          <button
            onClick={advance}
            disabled={done}
            className={`w-full rounded-xl py-3.5 text-[15px] font-semibold text-white shadow-lg transition-all active:scale-[.99] ${
              done ? "bg-emerald-500 shadow-emerald-500/25" : "bg-blue-600 shadow-blue-600/25 hover:bg-blue-700"
            }`}
          >
            {done ? "已完成全部步骤 ✓" : "确认，生成钩子引导"}
          </button>
        </div>
      </div>
    </section>
  );
}