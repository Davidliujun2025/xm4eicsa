// src/components/PersonalLibrary.tsx
import { useState, useEffect } from "react";
import { BookOpen, X, Sparkles } from "lucide-react";

type FavoriteItem = {
  id: string;
  step: string;
  content: string;
  platform: string;
  questionType: string;
  time: string;
};

export default function PersonalLibrary() {
  const [favorites, setFavorites] = useState<FavoriteItem[]>([]);

  // 加载收藏数据
  const loadFavorites = () => {
    const stored = localStorage.getItem("favorites");
    if (stored) {
      try {
        setFavorites(JSON.parse(stored));
      } catch {
        setFavorites([]);
      }
    } else {
      setFavorites([]);
    }
  };

  useEffect(() => {
    loadFavorites();
    // 监听 storage 变化（其他标签页修改时同步）
    const handleStorageChange = () => loadFavorites();
    window.addEventListener("storage", handleStorageChange);
    return () => window.removeEventListener("storage", handleStorageChange);
  }, []);

  // 取消收藏
  const removeFavorite = (id: string) => {
    const updated = favorites.filter((item) => item.id !== id);
    setFavorites(updated);
    localStorage.setItem("favorites", JSON.stringify(updated));
    // 触发同一页面内其他组件的更新（如果存在）
    window.dispatchEvent(new Event("storage"));
  };

  // 格式化时间
  const formatTime = (isoString: string) => {
    const date = new Date(isoString);
    return date.toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="h-full overflow-y-auto p-6 bg-gray-50">
      <div className="flex items-center gap-3 mb-6">
        <BookOpen className="h-6 w-6 text-blue-600" />
        <h2 className="text-2xl font-bold text-slate-800">个人话术库</h2>
        <span className="text-sm text-slate-400 bg-white px-3 py-1 rounded-full shadow-sm">
          共 {favorites.length} 条收藏
        </span>
      </div>

      {favorites.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-64 text-slate-400">
          <Sparkles className="h-12 w-12 mb-4 opacity-20" />
          <p className="text-lg font-medium">还没有收藏的话术</p>
          <p className="text-sm">在智能对话工作台生成话术后，点击“收藏到话术库”即可保存</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {favorites.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5 hover:shadow-md transition-shadow relative"
            >
              <button
                onClick={() => removeFavorite(item.id)}
                className="absolute top-3 right-3 p-1 rounded-full hover:bg-slate-100 text-slate-400 hover:text-red-500 transition-colors"
                aria-label="取消收藏"
              >
                <X className="h-5 w-5" />
              </button>

              <div className="flex items-center gap-2 mb-3">
                <span className="bg-blue-50 text-blue-700 text-xs font-medium px-2.5 py-0.5 rounded-full">
                  {item.step}
                </span>
                <span className="bg-purple-50 text-purple-700 text-xs font-medium px-2.5 py-0.5 rounded-full">
                  {item.platform}
                </span>
                <span className="bg-gray-100 text-gray-600 text-xs font-medium px-2.5 py-0.5 rounded-full">
                  {item.questionType}
                </span>
              </div>

              <p className="text-sm text-slate-700 leading-relaxed mb-3 line-clamp-4">
                {item.content}
              </p>

              <div className="text-xs text-slate-400 border-t border-slate-100 pt-2 flex justify-between">
                <span>收藏时间：{formatTime(item.time)}</span>
                <span className="text-blue-500 hover:text-blue-700 cursor-pointer" 
                      onClick={() => navigator.clipboard.writeText(item.content)}>
                  复制话术
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}