import { useState } from "react";
import { Plus, AlertCircle } from "lucide-react";
import { STEPS } from "../mock/data";
import type { GenerationStatus } from "../types";

import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import ConversationList from "./ConversationList";
import ChatPanel from "./ChatPanel";
import PersonalLibrary from "./PersonalLibrary";

console.log("🔥🔥🔥 IntelligentConversationWorkbench 文件被加载了！");

type Conv = {
  t: string;
  time: string;
  tag: string;
  platform?: string;
  messages?: { role: "user" | "assistant"; content: string; time?: string }[];
};

export default function IntelligentConversationWorkbench() {
  const [activeMenu, setActiveMenu] = useState(0);
  const [selectedConversation, setSelectedConversation] = useState<Conv | null>(null);
  const [generationStatus, setGenerationStatus] = useState<GenerationStatus>("idle");
  // currentStep 暂时未使用，但可以保留以备将来
  // const [currentStep, setCurrentStep] = useState(0); // 注释掉以避免警告

  const handleMenuSelect = (index: number) => {
    console.log("菜单切换至:", index);
    setActiveMenu(index);
    if (index !== 0) {
      setSelectedConversation(null);
    }
  };

  const handleSelectConversation = (conv: Conv) => {
    console.log("📌 选中对话:", conv);
    setSelectedConversation(conv);
  };

  const handleNewConversation = () => {
    setSelectedConversation(null);
    setGenerationStatus("idle");
    // setCurrentStep(0);
  };

  const handleGenerate = () => {
    setGenerationStatus("generating");
    // setCurrentStep(0);
    let step = 0;
    const interval = setInterval(() => {
      step++;
      // setCurrentStep(step);
      if (step >= STEPS.length) {
        clearInterval(interval);
        setGenerationStatus("success");
      }
    }, 800);
  };

  const renderMainContent = () => {
    switch (activeMenu) {
      case 0:
        return (
          <>
            <div className="w-1/2 border-r border-gray-200 p-6 flex flex-col overflow-y-auto">
              <ChatPanel
                selectedConversation={selectedConversation}
                onNewConversation={handleNewConversation}
                platformId={selectedConversation?.platform || "tm"}
                onPlatformChange={(id) => console.log("Platform changed:", id)}
                regen={generationStatus === "generating"}
                onRegen={handleGenerate}
              />
            </div>
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
                        亲爱的顾客，您好！非常理解您的心情。我们支持七天无理由退货的，只要商品未拆封且不影响二次销售，您可以在订单详情页直接点击"申请售后"-"退货退款"进行操作哦。
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
          </>
        );
      case 2:
        return <PersonalLibrary />;
      default:
        return (
          <div className="flex-1 flex items-center justify-center text-slate-400">
            <div className="text-center">
              <div className="text-6xl mb-4">🚧</div>
              <p className="text-lg font-medium">功能开发中</p>
              <p className="text-sm">敬请期待</p>
            </div>
          </div>
        );
    }
  };

  console.log("🔍 传递的 onSelectConversation:", handleSelectConversation);

  return (
    <div className="flex h-screen w-full bg-gray-50 text-gray-900 font-sans overflow-hidden">
      <Sidebar activeIndex={activeMenu} onSelect={handleMenuSelect} />

      {activeMenu === 0 && (
        <div className="w-80 bg-white border-r border-gray-200 flex flex-col shrink-0">
          <div className="p-4 border-b border-gray-100">
            <button
              onClick={handleNewConversation}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg flex items-center justify-center gap-2 font-medium transition-colors"
            >
              <Plus size={18} />
              新建对话
            </button>
          </div>
          <div className="flex-1 overflow-y-auto">
            <ConversationList
              platformName="天猫"
              onSelectConversation={handleSelectConversation}
            />
          </div>
        </div>
      )}

      <main className="flex-1 flex flex-col min-w-0">
        <Topbar />
        <div className="flex-1 flex overflow-hidden">
          {renderMainContent()}
        </div>
      </main>
    </div>
  );
}