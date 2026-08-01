import {
  ChevronDown,
  ClipboardCheck,
  Gauge,
  History,
  KeyRound,
  Plus,
  RefreshCw,
  Search,
  Settings,
  ShieldAlert,
  Users,
  X,
} from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import {
  ApiError,
  getCurrentUser,
  getCustomerServiceUserOperationLogs,
  getCustomerServiceUsers,
  resetCustomerServiceUserPassword,
  updateCustomerServiceUser,
  updateCustomerServiceUserStatus,
} from "../api/userApi";
import carePilotLogo from "../assets/carepilot-logo.png";
import CreateAgentDrawer from "../components/user/CreateAgentDrawer";
import "../styles/user-management.css";
import type {
  OperationLog,
  User,
  UserRole,
  UserStatus,
} from "../types/user";
import { validatePassword } from "../utils/validators";
import { getUserStatistics } from "../api/userApi";

type RoleFilter = "ALL" | UserRole;
type StatusFilter = "ALL" | UserStatus;

const PAGE_SIZE = 10;

const NAVIGATION_ITEMS = [
  { label: "管理看板", icon: Gauge },
  { label: "用户管理", icon: Users },
  { label: "Token 管理", icon: KeyRound },
  { label: "违禁词管理", icon: ShieldAlert },
  { label: "评估报告", icon: ClipboardCheck },
  { label: "操作日志", icon: History },
  { label: "系统设置", icon: Settings },
];

const NAV_LINKS: Record<string, string> = {
  "用户管理": import.meta.env.VITE_ADMIN_USERS_URL || `${window.location.origin}/admin/users/`,
  "Token 管理": import.meta.env.VITE_ADMIN_TOKENS_URL || `${window.location.origin}/admin/tokens/`,
  "违禁词管理": import.meta.env.VITE_ADMIN_FORBIDDEN_WORDS_URL || `${window.location.origin}/admin/forbidden-words/`,
};

export default function UserManagementPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [keyword, setKeyword] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("ALL");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalUsers, setTotalUsers] = useState(0);
  const [customerServiceCount, setCustomerServiceCount] = useState(0);
  const [administratorCount, setAdministratorCount] = useState(0);
  const [disabledCount, setDisabledCount] = useState(0);
  const [currentUserName, setCurrentUserName] = useState("");

  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editNameError, setEditNameError] = useState("");
  const [editEmailError, setEditEmailError] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [moreUser, setMoreUser] = useState<User | null>(null);

  const [resetPasswordUser, setResetPasswordUser] = useState<User | null>(null);
  const [resetPassword, setResetPassword] = useState("");
  const [resetPasswordError, setResetPasswordError] = useState("");
  const [resetSubmitting, setResetSubmitting] = useState(false);

  const [logUser, setLogUser] = useState<User | null>(null);
  const [operationLogs, setOperationLogs] = useState<OperationLog[]>([]);
  const [logsLoading, setLogsLoading] = useState(false);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getCustomerServiceUsers({
        page: currentPage,
        pageSize: PAGE_SIZE,
        keyword,
        role: roleFilter,
        status: statusFilter,
      });
      setUsers(response.data.list);
      
      setUsers(response.data.list);
    } catch (error: unknown) {
      handleError(error, "用户列表加载失败");
    } finally {
      setLoading(false);
    }
  }, [currentPage, keyword, roleFilter, statusFilter]);

  const loadStatistics = useCallback(async()=>{

    try{
  
      const response =
        await getUserStatistics();
  
  
      const data =
        response.data;
  
  
      setTotalUsers(
        data.totalUsers
      );
  
  
      setCustomerServiceCount(
        data.customerServiceCount
      );
  
  
      setAdministratorCount(
        data.adminCount
      );
  
  
      setDisabledCount(
        data.disabledCount
      );
  
  
    }catch(error){
  
      console.error(
        "统计加载失败",
        error
      );
  
    }
  
  
  },[]);
    
  useEffect(() => {

    const timer = window.setTimeout(() => {
   
      void loadUsers();
   
      void loadStatistics();
   
    },300);
   
   
    return () =>
      window.clearTimeout(timer);
   
   
   },[
    loadUsers,
    loadStatistics
   ]);

  useEffect(() => {
    getCurrentUser()
      .then((response) => {
        setCurrentUserName(response.data.username);
      })
      .catch(() => {
        window.location.href = `/?returnUrl=${encodeURIComponent(window.location.pathname)}`;
      });
  }, []);

  const totalPages = Math.max(1, Math.ceil(totalUsers / PAGE_SIZE));


  function showSuccess(message: string) {
    setSuccessMessage(message);
    window.setTimeout(() => setSuccessMessage(""), 2000);
  }

  function handleError(error: unknown, fallback: string) {
    console.error(fallback, error);
    if (error instanceof ApiError) {
      window.alert(error.response.message || fallback);
      return;
    }
    window.alert(error instanceof Error ? error.message : fallback);
  }

  function handleCreated() {
    showSuccess("客服人员账号创建成功");
    setCurrentPage(1);
    void loadUsers();
    void loadStatistics();
  }

  function handleRefresh() {
    setKeyword("");
    setRoleFilter("ALL");
    setStatusFilter("ALL");
    setCurrentPage(1);
    void loadUsers();
    showSuccess("用户列表已刷新");
  }

  function validateEditName(value: string): string {
    const normalized = value.trim();
    if (!normalized) return "请输入用户姓名";
    if (normalized.length < 2 || normalized.length > 20) {
      return "用户姓名长度应为2–20个字符";
    }
    if (!/^[\u4e00-\u9fffA-Za-z]+(?:\s[\u4e00-\u9fffA-Za-z]+)*$/.test(normalized)) {
      return "姓名仅支持中文、英文或中英文组合";
    }
    return "";
  }

  function validateEditEmail(value: string): string {
    const normalized = value.trim();
    if (!normalized) return "";
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized)) {
      return "请输入正确的邮箱地址";
    }
    return "";
  }

  function openEditModal(user: User) {
    setEditingUser(user);
    setEditName(user.name);
    setEditEmail(user.email ?? "");
    setEditNameError("");
    setEditEmailError("");
  }

  function closeEditModal() {
    if (editSubmitting) return;
    setEditingUser(null);
    setEditName("");
    setEditEmail("");
    setEditNameError("");
    setEditEmailError("");
  }

  async function handleSaveEdit() {
    if (!editingUser) return;

    const nameError = validateEditName(editName);
    const emailError = validateEditEmail(editEmail);
    setEditNameError(nameError);
    setEditEmailError(emailError);
    if (nameError || emailError) return;

    try {
      setEditSubmitting(true);
      const response = await updateCustomerServiceUser(editingUser.userId, {
        name: editName,
        email: editEmail.trim() || null,
      });
      setUsers((previous) =>
        previous.map((user) =>
          user.userId === response.data.userId ? response.data : user,
        ),
      );
      setEditingUser(null);
      setEditName("");
      setEditEmail("");
      showSuccess("用户信息修改成功");
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        const fieldErrors = error.response.data?.fieldErrors;
        if (fieldErrors?.name) setEditNameError(fieldErrors.name);
        if (fieldErrors?.email) setEditEmailError(fieldErrors.email);
        if (fieldErrors?.name || fieldErrors?.email) return;
        if (error.response.code === "EMAIL_ALREADY_EXISTS") {
          setEditEmailError(error.response.message);
          return;
        }
      }
      handleError(error, "用户信息修改失败");
    } finally {
      setEditSubmitting(false);
    }
  }

  async function handleToggleStatus(user: User) {
    const nextStatus: UserStatus =
      user.status === "ENABLED" ? "DISABLED" : "ENABLED";
    if (user.role === "SYSTEM_ADMIN" && nextStatus === "DISABLED") {
      return;
    }
    const confirmed = window.confirm(
      nextStatus === "DISABLED"
        ? `确认禁用客服人员“${user.name}”吗？`
        : `确认启用客服人员“${user.name}”吗？`,
    );
    if (!confirmed) return;

    try {
      const response = await updateCustomerServiceUserStatus(user.userId, {
        status: nextStatus,
      });
      setUsers((previous) =>
        previous.map((item) =>
          item.userId === response.data.userId ? response.data : item,
        ),
      );
      showSuccess(nextStatus === "ENABLED" ? "用户账号已启用" : "用户账号已禁用");
    } catch (error: unknown) {
      handleError(error, "账号状态修改失败");
    }
  }

  function openResetPassword(user: User) {
    setMoreUser(null);
    setResetPasswordUser(user);
    setResetPassword("");
    setResetPasswordError("");
  }

  async function handleResetPassword() {
    if (!resetPasswordUser) return;
    const error = validatePassword(resetPassword, resetPasswordUser.phone);
    if (error) {
      setResetPasswordError(error);
      return;
    }

    try {
      setResetSubmitting(true);
      await resetCustomerServiceUserPassword(resetPasswordUser.userId, {
        initialPassword: resetPassword,
      });
      setResetPasswordUser(null);
      setResetPassword("");
      showSuccess("密码重置成功");
    } catch (requestError: unknown) {
      if (requestError instanceof ApiError) {
        setResetPasswordError(requestError.response.message);
        return;
      }
      handleError(requestError, "密码重置失败");
    } finally {
      setResetSubmitting(false);
    }
  }

  async function openOperationLogs(user: User) {
    setMoreUser(null);
    setLogUser(user);
    setOperationLogs([]);
    try {
      setLogsLoading(true);
      const response = await getCustomerServiceUserOperationLogs(user.userId);
      setOperationLogs(response.data.list);
    } catch (error: unknown) {
      handleError(error, "操作日志加载失败");
    } finally {
      setLogsLoading(false);
    }
  }

  return (
    <div className="app-layout">
      {successMessage && <div className="success-toast">{successMessage}</div>}

      <aside className="sidebar">
        <div className="brand-area">
          <img
            src={carePilotLogo}
            alt="CarePilot AI 智能客服助手"
            className="brand-logo-image"
          />
        </div>

        <nav className="sidebar-nav">
          {NAVIGATION_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = item.label === "用户管理";
            return (
              <button
                key={item.label}
                type="button"
                className={`sidebar-nav__item ${isActive ? "is-active" : ""}`}
                onClick={() => {
                  const target = NAV_LINKS[item.label];
                  if (target) {
                    window.location.href = target;
                  }
                }}
              >
                <Icon size={19} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>
      </aside>

      <div className={`management-page ${drawerOpen ? "has-drawer" : ""}`}>
        <main className="management-content">
          <header className="page-header">
            <div>
              <h1>用户管理</h1>
              <p>管理系统用户账号与使用状态</p>
            </div>
            <div className="page-header__right">
              <div className="service-status"><span />AI服务正常</div>
              <div className="current-user">
                <div className="current-user__avatar">{currentUserName.slice(0, 1) || "?"}</div>
                <span>{currentUserName || "正在加载"}</span>
                <ChevronDown size={16} />
              </div>
            </div>
          </header>


          <section className="statistic-grid">

            <StatisticCard
              title="总用户数"
              value={totalUsers}
              trend={totalUsers}
            />

            <StatisticCard
              title="本页客服人员"
              value={customerServiceCount}
              trend={customerServiceCount}
            />

            <StatisticCard
              title="本页系统管理员"
              value={administratorCount}
              trend={administratorCount}
            />

            <StatisticCard
              title="本页禁用账号"
              value={disabledCount}
              trend={disabledCount}
            />

          </section>


          <section className="user-card">
            <div className="user-toolbar">
              <div className="toolbar-filters">
                <label className="search-box">
                  <Search size={17} />
                  <input
                    value={keyword}
                    placeholder="搜索姓名 / 手机号 / 邮箱"
                    onChange={(event) => {
                      setKeyword(event.target.value);
                      setCurrentPage(1);
                    }}
                  />
                </label>

                <label className="filter-group">
                  <span>角色：</span>
                  <select
                    value={roleFilter}
                    onChange={(event) => {
                      setRoleFilter(event.target.value as RoleFilter);
                      setCurrentPage(1);
                    }}
                  >
                    <option value="ALL">全部角色</option>
                    <option value="CUSTOMER_SERVICE">客服人员</option>
                    <option value="SYSTEM_ADMIN">系统管理员</option>
                  </select>
                </label>

                <label className="filter-group">
                  <span>状态：</span>
                  <select
                    value={statusFilter}
                    onChange={(event) => {
                      setStatusFilter(event.target.value as StatusFilter);
                      setCurrentPage(1);
                    }}
                  >
                    <option value="ALL">全部状态</option>
                    <option value="ENABLED">启用</option>
                    <option value="DISABLED">禁用</option>
                  </select>
                </label>
              </div>

              <div className="toolbar-actions">
                <button type="button" className="secondary-button" onClick={handleRefresh}>
                  <RefreshCw size={17} />刷新
                </button>
                <button type="button" className="primary-button" onClick={() => setDrawerOpen(true)}>
                  <Plus size={17} />创建客服人员账号
                </button>
              </div>
            </div>

            <div className="user-table">
              <div className="user-table__row user-table__head">
                <span>用户姓名</span><span>手机号</span><span>邮箱</span>
                <span>角色</span><span>状态</span><span>创建时间</span><span>操作</span>
              </div>

              {loading ? (
                <div className="empty-state"><p>正在加载用户列表...</p></div>
              ) : users.length === 0 ? (
                <div className="empty-state"><Users size={34} /><p>暂无相关数据</p></div>
              ) : (
                users.map((user) => (
                  <div className="user-table__row" key={`${user.userId}-${user.phone}`}>
                    <strong>{user.name}</strong>
                    <span>{maskPhone(user.phone)}</span>
                    <span>{user.email || "—"}</span>
                    <span className={user.role === "SYSTEM_ADMIN" ? "role-tag role-tag--admin" : "role-tag"}>
                      {user.roleName}
                    </span>
                    <span className={user.status === "DISABLED" ? "status-tag status-tag--disabled" : "status-tag"}>
                      {user.statusName}
                    </span>
                    <time>{formatDateTime(user.createdAt)}</time>
                    <div className="user-actions">
                      <button type="button" className="text-action" onClick={() => openEditModal(user)}>编辑</button>
                      <button
                        type="button"
                        className={user.status === "ENABLED" && user.role !== "SYSTEM_ADMIN" ? "text-action text-action--danger" : "text-action"}
                        disabled={user.role === "SYSTEM_ADMIN" && user.status === "ENABLED"}
                        title={user.role === "SYSTEM_ADMIN" && user.status === "ENABLED" ? "管理员账号不允许禁用" : undefined}
                        onClick={() => void handleToggleStatus(user)}
                      >
                        {user.role === "SYSTEM_ADMIN" && user.status === "ENABLED"
                          ? "禁止禁用"
                          : user.status === "ENABLED" ? "禁用" : "启用"}
                      </button>
                      <button type="button" className="text-action text-action--more" onClick={() => setMoreUser(user)}>
                        更多<ChevronDown size={14} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="table-footer">
              <span>共 {totalUsers} 条</span>
              <div className="pagination">
                <button
                  type="button"
                  className="pagination__item"
                  disabled={currentPage <= 1}
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                >
                  上一页
                </button>
                <button type="button" className="pagination__item is-active">{currentPage}</button>
                <span>/ {totalPages}</span>
                <button
                  type="button"
                  className="pagination__item"
                  disabled={currentPage >= totalPages}
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                >
                  下一页
                </button>
              </div>
            </div>
          </section>
        </main>

        <CreateAgentDrawer
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          onCreated={() => handleCreated()}
        />

        {editingUser && (
          <div className="modal-overlay" onMouseDown={closeEditModal}>
            <section className="account-modal edit-user-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
              <header className="account-modal__header">
                <div><h2>编辑用户信息</h2><p>{editingUser.name}<span>·</span>{maskPhone(editingUser.phone)}</p></div>
                <button type="button" className="modal-close-button" onClick={closeEditModal} disabled={editSubmitting} aria-label="关闭编辑弹窗"><X size={21} /></button>
              </header>

              <div className="account-modal__body">
                <label className="modal-form-field">
                  <span>用户姓名<b>*</b></span>
                  <input
                    value={editName}
                    maxLength={20}
                    disabled={editSubmitting}
                    onChange={(event) => { setEditName(event.target.value); setEditNameError(""); }}
                    onBlur={() => setEditNameError(validateEditName(editName))}
                  />
                  {editNameError ? <small className="modal-field-error">{editNameError}</small> : <small>支持中文、英文或中英文组合，长度2–20个字符。</small>}
                </label>

                <label className="modal-form-field">
                  <span>手机号</span>
                  <input value={maskPhone(editingUser.phone)} disabled readOnly />
                  <small>手机号是登录账号，当前不可修改。</small>
                </label>

                <label className="modal-form-field">
                  <span>邮箱</span>
                  <input
                    type="email"
                    value={editEmail}
                    placeholder="请输入邮箱地址"
                    disabled={editSubmitting}
                    onChange={(event) => { setEditEmail(event.target.value); setEditEmailError(""); }}
                    onBlur={() => setEditEmailError(validateEditEmail(editEmail))}
                  />
                  {editEmailError ? <small className="modal-field-error">{editEmailError}</small> : <small>邮箱可不填写，仅用于密码重置。</small>}
                </label>

                <label className="modal-form-field"><span>角色</span><input value={editingUser.roleName} disabled readOnly /></label>
                <label className="modal-form-field"><span>当前状态</span><input value={editingUser.statusName} disabled readOnly /></label>
              </div>

              <footer className="account-modal__footer">
                <button type="button" className="secondary-button" onClick={closeEditModal} disabled={editSubmitting}>取消</button>
                <button type="button" className="primary-button" onClick={() => void handleSaveEdit()} disabled={editSubmitting}>
                  {editSubmitting ? "正在保存" : "确认保存"}
                </button>
              </footer>
            </section>
          </div>
        )}

        {moreUser && (
          <div className="modal-overlay" onMouseDown={() => setMoreUser(null)}>
            <section className="account-modal more-action-modal" onMouseDown={(event) => event.stopPropagation()}>
              <header className="account-modal__header">
                <div><h2>更多操作</h2><p>{moreUser.name}<span>·</span>{maskPhone(moreUser.phone)}</p></div>
                <button type="button" className="modal-close-button" onClick={() => setMoreUser(null)} aria-label="关闭更多操作弹窗"><X size={21} /></button>
              </header>
              <div className="account-modal__body">
                <p className="more-action-description">选择需要对该账号执行的操作：</p>
                <button type="button" className="more-action-button" onClick={() => openResetPassword(moreUser)}><KeyRound size={19} /><span>重置初始密码</span></button>
                <button type="button" className="more-action-button" onClick={() => void openOperationLogs(moreUser)}><History size={19} /><span>查看操作日志</span></button>
                <button type="button" className="more-action-button" onClick={() => setMoreUser(null)}><Users size={19} /><span>返回用户列表</span></button>
              </div>
            </section>
          </div>
        )}

        {resetPasswordUser && (
          <div className="modal-overlay" onMouseDown={() => !resetSubmitting && setResetPasswordUser(null)}>
            <section className="account-modal" onMouseDown={(event) => event.stopPropagation()}>
              <header className="account-modal__header">
                <div><h2>重置初始密码</h2><p>{resetPasswordUser.name}</p></div>
                <button type="button" className="modal-close-button" onClick={() => setResetPasswordUser(null)} disabled={resetSubmitting}><X size={21} /></button>
              </header>
              <div className="account-modal__body">
                <label className="modal-form-field">
                  <span>新密码<b>*</b></span>
                  <input
                    type="password"
                    value={resetPassword}
                    placeholder="请输入12–20位新密码"
                    disabled={resetSubmitting}
                    onChange={(event) => { setResetPassword(event.target.value); setResetPasswordError(""); }}
                  />
                  {resetPasswordError ? <small className="modal-field-error">{resetPasswordError}</small> : <small>必须包含字母和数字，可使用 @ # $ % _ !</small>}
                </label>
              </div>
              <footer className="account-modal__footer">
                <button type="button" className="secondary-button" onClick={() => setResetPasswordUser(null)} disabled={resetSubmitting}>取消</button>
                <button type="button" className="primary-button" onClick={() => void handleResetPassword()} disabled={resetSubmitting}>
                  {resetSubmitting ? "正在重置" : "确认重置"}
                </button>
              </footer>
            </section>
          </div>
        )}

        {logUser && (
          <div className="modal-overlay" onMouseDown={() => setLogUser(null)}>
            <section className="account-modal" onMouseDown={(event) => event.stopPropagation()}>
              <header className="account-modal__header">
                <div><h2>账号操作日志</h2><p>{logUser.name}</p></div>
                <button type="button" className="modal-close-button" onClick={() => setLogUser(null)}><X size={21} /></button>
              </header>
              <div className="account-modal__body">
                {logsLoading ? <p>正在加载日志...</p> : operationLogs.length === 0 ? <p>暂无操作日志</p> : (
                  <div className="operation-log-list">
                    {operationLogs.map((log) => (
                      <div key={log.logId} className="operation-log-item">
                        <strong>{translateAction(log.action)}</strong>
                        <p>{log.detail}</p>
                        <time>{formatDateTime(log.createdAt)}</time>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}

interface StatisticCardProps {
  title: string;
  value: number;
  trend: number;
  trendType?: "normal" | "warning";
}

function StatisticCard({ title, value, trend, trendType = "normal" }: StatisticCardProps) {
  return (
    <article className="statistic-card">
      <div className="statistic-card__content">
        <span className="statistic-card__title">{title}</span>
        <strong>{value}</strong>
        <small className={trendType === "warning" ? "trend-text trend-text--warning" : "trend-text"}>较上月 +{trend}</small>
      </div>
      <div className="statistic-icon"><Users size={23} /></div>
    </article>
  );
}

function maskPhone(phone: string): string {
  if (phone.includes("*")) return phone;
  if (!/^1[3-9]\d{9}$/.test(phone)) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}

function formatDateTime(value: string): string {
  if (!value) return "—";
  return value.replace("T", " ").slice(0, 19);
}

function translateAction(action: string): string {
  const labels: Record<string, string> = {
    CREATE: "创建账号",
    UPDATE_PROFILE: "修改资料",
    UPDATE_STATUS: "修改状态",
    RESET_PASSWORD: "重置密码",
  };
  return labels[action] ?? action;
}
