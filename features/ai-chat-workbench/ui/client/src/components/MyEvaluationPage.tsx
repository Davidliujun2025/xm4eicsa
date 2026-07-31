import { useEffect, useMemo, useState } from "react";

type EvaluationRecord = {
  id: number;
  platform: string;
  sourceType: string;
  content: string;
  hitWord: string;
  action: string;
  actionTime: string;
};

type EvaluationPageResult = {
  items: EvaluationRecord[];
  total: number;
  page: number;
  size: number;
};

const actionLabels: Record<string, string> = {
  HIT: "已命中",
  BLOCK_REPLY: "已阻止发送",
  WARN_AGENT: "已提醒客服",
};

export default function MyEvaluationPage() {
  const [result, setResult] = useState<EvaluationPageResult>({ items: [], total: 0, page: 1, size: 100 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    const loadEvaluations = async () => {
      try {
        const response = await fetch("/api/v1/evaluations/me?page=1&size=100", {
          credentials: "include",
          signal: controller.signal,
        });
        const payload = await response.json();
        if (!response.ok) {
          throw new Error(payload?.message || "评估记录加载失败");
        }
        setResult(payload);
      } catch (loadError) {
        if (!controller.signal.aborted) {
          setError(loadError instanceof Error ? loadError.message : "评估记录加载失败");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    void loadEvaluations();
    return () => controller.abort();
  }, []);

  const blockedCount = useMemo(() => result.items.filter((item) => item.action === "BLOCK_REPLY").length, [result.items]);
  const warnedCount = useMemo(() => result.items.filter((item) => item.action === "WARN_AGENT").length, [result.items]);

  return (
    <main className="min-h-0 flex-1 overflow-y-auto p-5">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <section className="grid grid-cols-3 gap-4">
        <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-400">累计风险记录</p>
          <p className="mt-2 text-3xl font-bold text-slate-900">{result.total}</p>
        </div>
        <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-400">AI 回复拦截</p>
          <p className="mt-2 text-3xl font-bold text-red-500">{blockedCount}</p>
        </div>
        <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm">
          <p className="text-sm text-slate-400">客服提醒</p>
          <p className="mt-2 text-3xl font-bold text-amber-500">{warnedCount}</p>
        </div>
      </section>

      <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200/80 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-4">
          <h2 className="font-bold text-slate-800">风险评估明细</h2>
          <p className="mt-1 text-xs text-slate-400">仅显示当前登录客服的违禁词命中与处置记录</p>
        </div>
        {loading && <p className="p-10 text-center text-sm text-slate-400">正在加载评估记录…</p>}
        {!loading && result.items.length === 0 && !error && (
          <p className="p-10 text-center text-sm text-slate-400">暂无风险评估记录</p>
        )}
        {!loading && result.items.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs text-slate-500">
                <tr>
                  <th className="px-6 py-3 font-medium">评估时间</th>
                  <th className="px-6 py-3 font-medium">平台</th>
                  <th className="px-6 py-3 font-medium">内容来源</th>
                  <th className="px-6 py-3 font-medium">命中词</th>
                  <th className="px-6 py-3 font-medium">处置结果</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {result.items.map((record) => (
                  <tr key={record.id} className="hover:bg-slate-50/70">
                    <td className="whitespace-nowrap px-6 py-4 text-slate-500">{new Date(record.actionTime).toLocaleString("zh-CN", { hour12: false })}</td>
                    <td className="px-6 py-4 text-slate-600">{record.platform}</td>
                    <td className="px-6 py-4 text-slate-600">{record.sourceType === "AI_ANSWER" ? "AI 回复" : "客户问题"}</td>
                    <td className="px-6 py-4 font-medium text-red-500">{record.hitWord}</td>
                    <td className="px-6 py-4 text-slate-600">{actionLabels[record.action] || record.action}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}
