import { useCallback, useEffect, useMemo, useState } from "react";

type EvaluationRecord = {
  id: number;
  conversationId: string;
  platform: string;
  totalScore: number;
  serviceAttitudeScore: number;
  problemSolvingScore: number;
  empathyScore: number;
  complianceScore: number;
  conversionGuidanceScore: number;
  responseEfficiencyScore: number;
  strengths: string;
  problems: string;
  suggestions: string;
  forbiddenHitCount: number;
  forbiddenWordsJson: string;
  transcript: string;
  totalTokens: number;
  generatedAt: string;
};

type EvaluationPageResult = { items: EvaluationRecord[]; total: number; page: number; size: number; totalPages: number };
type EvaluationStats = {
  usedToday: number;
  remainingToday: number;
  dailyLimit: number;
  cumulativeReceptionCount: number;
  cumulativeAverageScore: number;
  cumulativeTokenUsage: number;
};

const platforms = ["", "淘宝", "天猫", "京东", "拼多多", "抖音", "小红书", "快手", "视频号", "微信小店", "其他平台"];
const dimensions: Array<[keyof EvaluationRecord, string]> = [
  ["serviceAttitudeScore", "服务态度"], ["problemSolvingScore", "问题解决"],
  ["empathyScore", "共情能力"], ["complianceScore", "合规性"],
  ["conversionGuidanceScore", "转化引导"], ["responseEfficiencyScore", "响应效率"],
];

export default function MyEvaluationPage() {
  const [result, setResult] = useState<EvaluationPageResult>({ items: [], total: 0, page: 1, size: 20, totalPages: 0 });
  const [stats, setStats] = useState<EvaluationStats | null>(null);
  const [detail, setDetail] = useState<EvaluationRecord | null>(null);
  const [filters, setFilters] = useState({ from: "", to: "", minScore: "", maxScore: "", platform: "" });
  const [applied, setApplied] = useState(filters);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const params = useMemo(() => {
    const value = new URLSearchParams({ page: String(page), size: "20" });
    Object.entries(applied).forEach(([key, item]) => item && value.set(key, item));
    return value;
  }, [applied, page]);

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [reportsResponse, statsResponse] = await Promise.all([
        fetch(`/api/v1/evaluations/me?${params}`, { credentials: "include" }),
        fetch("/api/v1/evaluations/me/stats", { credentials: "include" }),
      ]);
      if (!reportsResponse.ok || !statsResponse.ok) throw new Error("评估报告加载失败");
      setResult(await reportsResponse.json());
      setStats(await statsResponse.json());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "评估报告加载失败");
    } finally { setLoading(false); }
  }, [params]);

  useEffect(() => { void load(); }, [load]);

  const showDetail = async (id: number) => {
    setError("");
    try {
      const response = await fetch(`/api/v1/evaluations/me/${id}`, { credentials: "include" });
      if (!response.ok) throw new Error("报告详情加载失败");
      setDetail(await response.json());
    } catch (detailError) { setError(detailError instanceof Error ? detailError.message : "报告详情加载失败"); }
  };

  const exportExcel = () => {
    const exportParams = new URLSearchParams();
    Object.entries(applied).forEach(([key, item]) => item && exportParams.set(key, item));
    window.location.href = `/api/v1/evaluations/me/export.xls?${exportParams}`;
  };

  return (
    <main className="min-h-0 flex-1 overflow-y-auto bg-slate-50 p-5 text-slate-800">
      {error && <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>}
      <section className="grid grid-cols-2 gap-4 xl:grid-cols-5">
        {[
          ["今日 Token", stats?.usedToday ?? 0], ["剩余额度", stats?.remainingToday ?? 0],
          ["累计接待", stats?.cumulativeReceptionCount ?? 0],
          ["累计平均分", stats?.cumulativeAverageScore ?? 0], ["累计 Token", stats?.cumulativeTokenUsage ?? 0],
        ].map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm text-slate-400">{label}</p>
            <p className="mt-2 text-2xl font-bold">{Number(value).toLocaleString()}</p>
          </div>
        ))}
      </section>

      <section className="mt-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-end gap-3">
          <label className="text-xs text-slate-500">开始日期<input type="date" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} className="mt-1 block rounded-lg border px-3 py-2 text-sm" /></label>
          <label className="text-xs text-slate-500">结束日期<input type="date" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} className="mt-1 block rounded-lg border px-3 py-2 text-sm" /></label>
          <label className="text-xs text-slate-500">最低分<input type="number" min="0" max="100" value={filters.minScore} onChange={(e) => setFilters({ ...filters, minScore: e.target.value })} className="mt-1 block w-24 rounded-lg border px-3 py-2 text-sm" /></label>
          <label className="text-xs text-slate-500">最高分<input type="number" min="0" max="100" value={filters.maxScore} onChange={(e) => setFilters({ ...filters, maxScore: e.target.value })} className="mt-1 block w-24 rounded-lg border px-3 py-2 text-sm" /></label>
          <label className="text-xs text-slate-500">接待平台<select value={filters.platform} onChange={(e) => setFilters({ ...filters, platform: e.target.value })} className="mt-1 block rounded-lg border px-3 py-2 text-sm">{platforms.map((item) => <option key={item} value={item}>{item || "全部平台"}</option>)}</select></label>
          <button onClick={() => { setPage(1); setApplied(filters); }} className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white">查询</button>
          <button onClick={exportExcel} className="rounded-lg border border-blue-200 px-5 py-2 text-sm font-medium text-blue-600">批量导出 Excel</button>
        </div>
      </section>

      <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b px-6 py-4"><h2 className="font-bold">接待评估报告</h2><p className="mt-1 text-xs text-slate-400">仅展示当前登录客服本人的报告，共 {result.total} 份</p></div>
        {loading ? <p className="p-10 text-center text-sm text-slate-400">正在加载…</p> : result.items.length === 0 ? <p className="p-10 text-center text-sm text-slate-400">暂无评估报告，完成一次五步对话后将自动生成</p> : (
          <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-6 py-3">时间</th><th className="px-6 py-3">平台</th><th className="px-6 py-3">总分</th><th className="px-6 py-3">违禁词</th><th className="px-6 py-3">Token</th><th className="px-6 py-3">操作</th></tr></thead>
          <tbody className="divide-y">{result.items.map((record) => <tr key={record.id} className="hover:bg-slate-50"><td className="px-6 py-4 text-slate-500">{new Date(record.generatedAt).toLocaleString("zh-CN", { hour12: false })}</td><td className="px-6 py-4">{record.platform}</td><td className="px-6 py-4 text-lg font-bold text-blue-600">{record.totalScore}</td><td className="px-6 py-4">{record.forbiddenHitCount}</td><td className="px-6 py-4">{record.totalTokens.toLocaleString()}</td><td className="px-6 py-4"><button onClick={() => void showDetail(record.id)} className="mr-4 text-blue-600">查看详情</button><button onClick={() => window.open(`/api/v1/evaluations/me/${record.id}/export.pdf`, "_blank")} className="text-blue-600">导出 PDF</button></td></tr>)}</tbody></table></div>
        )}
        <div className="flex items-center justify-end gap-3 border-t px-6 py-3 text-sm"><button disabled={page <= 1} onClick={() => setPage((v) => v - 1)} className="rounded border px-3 py-1.5 disabled:opacity-40">上一页</button><span>{page} / {Math.max(1, result.totalPages)}</span><button disabled={page >= result.totalPages} onClick={() => setPage((v) => v + 1)} className="rounded border px-3 py-1.5 disabled:opacity-40">下一页</button></div>
      </section>

      {detail && <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 p-6" onClick={() => setDetail(null)}><div className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-2xl bg-white p-6 shadow-xl" onClick={(e) => e.stopPropagation()}><div className="flex items-start justify-between"><div><h2 className="text-xl font-bold">接待评估报告 #{detail.id}</h2><p className="mt-1 text-sm text-slate-400">{detail.platform} · {new Date(detail.generatedAt).toLocaleString("zh-CN", { hour12: false })}</p></div><button onClick={() => setDetail(null)} className="text-2xl text-slate-400">×</button></div>
        <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-7"><div className="rounded-xl bg-blue-50 p-4 text-center"><div className="text-xs text-blue-500">总体评分</div><div className="text-3xl font-bold text-blue-600">{detail.totalScore}</div></div>{dimensions.map(([key, label]) => <div key={key} className="rounded-xl bg-slate-50 p-4 text-center"><div className="text-xs text-slate-400">{label}</div><div className="mt-1 text-xl font-bold">{String(detail[key])}</div></div>)}</div>
        <div className="mt-5 grid gap-4 md:grid-cols-3"><article className="rounded-xl border p-4"><h3 className="font-bold text-emerald-600">做得好的地方</h3><p className="mt-2 whitespace-pre-wrap text-sm">{detail.strengths}</p></article><article className="rounded-xl border p-4"><h3 className="font-bold text-amber-600">存在的问题</h3><p className="mt-2 whitespace-pre-wrap text-sm">{detail.problems}</p></article><article className="rounded-xl border p-4"><h3 className="font-bold text-blue-600">改进建议</h3><p className="mt-2 whitespace-pre-wrap text-sm">{detail.suggestions}</p></article></div>
        <div className="mt-4 rounded-xl border p-4"><h3 className="font-bold">违禁词命中统计：{detail.forbiddenHitCount} 次</h3><p className="mt-2 text-sm text-red-500">{detail.forbiddenWordsJson}</p></div><div className="mt-4 rounded-xl border p-4"><h3 className="font-bold">原始对话记录</h3><pre className="mt-2 whitespace-pre-wrap font-sans text-sm leading-6 text-slate-600">{detail.transcript}</pre></div></div></div>}
    </main>
  );
}
