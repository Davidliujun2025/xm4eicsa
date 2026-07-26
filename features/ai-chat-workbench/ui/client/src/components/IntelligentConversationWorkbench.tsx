import { useState } from "react";
import { 
  MessageSquare, Plus, Search, Bell, Settings, Send, 
  Sparkles, Clock, Zap, CheckCircle2, AlertCircle 
} from "lucide-react";
import { conversations, navItems, sampleQuestion, steps } from "../mock/data";
import type { GenerationStatus, StepCard } from "../types";

export default function IntelligentConversationWorkbench() {
  const [activeNav, setActiveNav] = useState(0);
  const [activeConversationId, setActiveConversationId] = useState(1);
  const [inputValue, setInputValue] = useState(sampleQuestion);
  const [generationStatus, setGenerationStatus] = useState<GenerationStatus>("idle");
  const [currentStep, setCurrentStep] = useState(0);

  // 模拟 AI 生成过程
  const handleGenerate = () => {
    if (!inputValue.trim()) return;
    setGenerationStatus("generating");
    setCurrentStep(0);
    
    let step = 0;
    const interval = setInterval(() => {
      step++;
      setCurrentStep(step);
      if (step >= steps.length) {
        clearInterval(interval);
        setGenerationStatus("success");
      }
    }, 800);
  };

  // 获取当前步骤的 UI 状态
  const getStepStatus = (index: number): "done" | "active" | "pending" => {
    if (generationStatus === "idle") return "pending";
    if (index < currentStep) return "done";
    if (index === currentStep && generationStatus === "generating") return "active";
    if (generationStatus === "success") return "done";
    return "pending";
  };

  return (
    <div className="flex h-screen w-full bg-gray-50 text-gray-900 font-sans overflow-hidden">
      {/* 1. 左侧导航栏 */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col shrink-0">
        <div className="p-6 flex items-center gap-3">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white">
            <MessageSquare size={20} />
          </div>
          <span className="text-xl font-bold tracking-tight">CarePilot</span>
        </div>
        <nav className="flex-1 px-3 space-y-1">
          {navItems.map((item, idx) => (
            <button
              key={idx}
              onClick={() => setActiveNav(idx)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                activeNav === idx 
                  ? "bg-blue-50 text-blue-700 font-medium" 
                  : "text-gray-600 hover:bg-gray-50"
              }`}
            >
              <item.icon size={20} />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
        <div className="p-4 border-t border-gray-100">
          <button className="w-full flex items-center gap-3 px-4 py-3 text-gray-600 hover:bg-gray-50 rounded-lg">
            <Settings size={20} />
            <span>系统设置</span>
          </button>
        </div>
      </aside>

      {/* 2. 中间会话列表 */}
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col shrink-0">
        <div className="p-4 border-b border-gray-100">
          <button className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg flex items-center justify-center gap-2 font-medium transition-colors">
            <Plus size={18} />
            新建对话
          </button>
        </div>
        <div className="p-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
            <input 
              type="text" 
              placeholder="搜索历史对话..." 
              className="w-full pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        </div>
        <div className="flex-1 overflow-y-auto">
          {conversations.map((conv) => (
            <div
              key={conv.id}
              onClick={() => setActiveConversationId(conv.id)}
              className={`p-4 border-b border-gray-50 cursor-pointer transition-colors ${
                activeConversationId === conv.id ? "bg-blue-50 border-l-4 border-l-blue-600" : "hover:bg-gray-50"
              }`}
            >
              <div className="flex justify-between items-start mb-1">
                <span className="text-xs font-medium text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">{conv.platform}</span>
                <span className="text-xs text-gray-400 flex items-center gap-1"><Clock size={12} />{conv.time}</span>
              </div>
              <p className="text-sm font-medium text-gray-800 truncate">{conv.question}</p>
              <p className="text-xs text-gray-500 mt-1">{conv.category}</p>
            </div>
          ))}
        </div>
      </div>

      {/* 3. 右侧核心工作台 */}
      <main className="flex-1 flex flex-col min-w-0">
        {/* 顶部 Header */}
        <header className="h-16 border-b border-gray-200 bg-white flex items-center justify-between px-6 shrink-0">
          <div className="flex items-center gap-3">
            <h2 className="text-lg font-semibold">智能对话工作台</h2>
            <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">AI 引擎已就绪</span>
          </div>
          <div className="flex items-center gap-4">
            <button className="p-2 hover:bg-gray-100 rounded-full"><Bell size={20} className="text-gray-500" /></button>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-xs font-bold text-gray-600">OP</div>
              <span className="text-sm font-medium hidden md:block">客服专员</span>
            </div>
          </div>
        </header>

        {/* 核心内容区 */}
        <div className="flex-1 flex overflow-hidden">
          {/* 左侧：输入与生成控制 */}
          <div className="w-1/2 border-r border-gray-200 p-6 flex flex-col">
            <div className="mb-4 flex items-center justify-between">
              <h3 className="font-semibold text-gray-800">客户提问</h3>
              <button 
                onClick={handleGenerate}
                disabled={generationStatus === "generating" || !inputValue.trim()}
                className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white px-4 py-2 rounded-lg text-sm font-medium transition-all shadow-sm"
              >
                <Sparkles size={16} />
                {generationStatus === "generating" ? "生成中..." : "AI 辅助生成"}
              </button>
            </div>
            <textarea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              className="flex-1 w-full p-4 border border-gray-200 rounded-xl resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm leading-relaxed"
              placeholder="请输入客户的问题..."
            />
            
            {/* 生成状态指示器 */}
            {generationStatus !== "idle" && (
              <div className="mt-4 p-4 bg-white border border-gray-200 rounded-xl shadow-sm">
                <div className="flex items-center gap-3 mb-3">
                  {generationStatus === "generating" && <Zap className="text-yellow-500 animate-pulse" size={18} />}
                  {generationStatus === "success" && <CheckCircle2 className="text-green-600" size={18} />}
                  <span className="text-sm font-medium text-gray-700">
                    {generationStatus === "generating" ? "正在分析意图并生成话术..." : "生成完毕！请检查右侧建议"}
                  </span>
                </div>
                <div className="flex gap-2">
                  {steps.map((step, idx) => (
                    <div key={idx} className="flex-1 flex flex-col items-center gap-1">
                      <div className={`w-full h-1.5 rounded-full transition-all ${
                        getStepStatus(idx) === "done" ? "bg-green-500" : 
                        getStepStatus(idx) === "active" ? "bg-blue-500 animate-pulse" : "bg-gray-200"
                      }`} />
                      <span className="text-[10px] text-gray-500">{step}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* 右侧：AI 建议面板 */}
          <div className="w-1/2 p-6 bg-gray-50 overflow-y-auto">
            <h3 className="font-semibold text-gray-800 mb-4">AI 推荐话术</h3>
            {generationStatus === "success" ? (
              <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm hover:shadow-md transition-shadow cursor-pointer group">
                    <div className="flex justify-between items-start mb-2">
                      <span className="text-xs font-bold text-blue-600 uppercase tracking-wide">推荐话术 {i}</span>
                      <span className="text-[10px] bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">高转化</span>
                    </div>
                    <p className="text-sm text-gray-700 leading-relaxed mb-4">
                      亲爱的顾客，您好！非常理解您的心情。我们支持七天无理由退货的，只要商品未拆封且不影响二次销售，您可以在订单详情页直接点击“申请售后”-“退货退款”进行操作哦。
                    </p>
                    <div className="flex items-center justify-between pt-3 border-t border-gray-100">
                      <div className="flex gap-2">
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded">温和语气</span>
                        <span className="text-[10px] bg-gray-100 text-gray-600 px-2 py-0.5 rounded">明确指引</span>
                      </div>
                      <button className="text-xs text-blue-600 font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                        一键复制
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-gray-400">
                <AlertCircle size={48} className="mb-4 opacity-20" />
                <p className="text-sm">在左侧输入客户问题并点击生成</p>
                <p className="text-xs mt-1">AI 将为您提供最佳回复建议</p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}