import {
  BarChart3,
  Bot,
  ChevronDown,
  ClipboardCheck,
  FileWarning,
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

import carePilotLogo from "../assets/carepilot-logo.svg";
import { useMemo, useState } from "react";
import CreateAgentDrawer from "../components/user/CreateAgentDrawer";
import type { User } from "../types/user";
import "../styles/user-management.css";

type RoleFilter =
  | "ALL"
  | "CUSTOMER_SERVICE"
  | "SYSTEM_ADMIN";

type StatusFilter =
  | "ALL"
  | "ENABLED"
  | "DISABLED";

const INITIAL_USERS: User[] = [
  {
    userId: 1,
    name: "张小雨",
    phone: "138****5678",
    email: "kefu01@example.com",
    role: "CUSTOMER_SERVICE",
    roleName: "客服人员",
    status: "ENABLED",
    statusName: "启用",
    createdAt: "2026-07-18 14:30:00",
  },
  {
    userId: 2,
    name: "李思雨",
    phone: "136****2058",
    email: "service02@example.com",
    role: "CUSTOMER_SERVICE",
    roleName: "客服人员",
    status: "ENABLED",
    statusName: "启用",
    createdAt: "2026-07-17 10:20:00",
  },
  {
    userId: 3,
    name: "陈一冉",
    phone: "139****8877",
    email: "admin@example.com",
    role: "SYSTEM_ADMIN",
    roleName: "系统管理员",
    status: "ENABLED",
    statusName: "启用",
    createdAt: "2026-07-16 09:15:00",
  },
  {
    userId: 4,
    name: "刘佳琪",
    phone: "135****4012",
    email: null,
    role: "CUSTOMER_SERVICE",
    roleName: "客服人员",
    status: "DISABLED",
    statusName: "禁用",
    createdAt: "2026-07-15 16:40:00",
  },
  {
    userId: 5,
    name: "赵天宇",
    phone: "188****2218",
    email: "zhaotianyu@example.com",
    role: "CUSTOMER_SERVICE",
    roleName: "客服人员",
    status: "ENABLED",
    statusName: "启用",
    createdAt: "2026-07-14 09:22:18",
  },
  {
    userId: 6,
    name: "周子轩",
    phone: "187****3321",
    email: "zhouzixuan@example.com",
    role: "CUSTOMER_SERVICE",
    roleName: "客服人员",
    status: "ENABLED",
    statusName: "启用",
    createdAt: "2026-07-13 14:33:08",
  },
];

const NAVIGATION_ITEMS = [
  {
    label: "管理看板",
    icon: Gauge,
  },
  {
    label: "用户管理",
    icon: Users,
  },
  {
    label: "Token 管理",
    icon: KeyRound,
  },
  {
    label: "违禁词管理",
    icon: ShieldAlert,
  },
  {
    label: "评估报告",
    icon: ClipboardCheck,
  },
  {
    label: "操作日志",
    icon: History,
  },
  {
    label: "系统设置",
    icon: Settings,
  },
];

export default function UserManagementPage() {
  const [drawerOpen, setDrawerOpen] =
    useState(false);

  const [users, setUsers] =
    useState<User[]>(INITIAL_USERS);

  const [successMessage, setSuccessMessage] =
    useState("");

  const [keyword, setKeyword] =
    useState("");

  const [roleFilter, setRoleFilter] =
    useState<RoleFilter>("ALL");

  const [statusFilter, setStatusFilter] =
    useState<StatusFilter>("ALL");

  const [editingUser, setEditingUser] =
    useState<User | null>(null);

  const [editName, setEditName] =
    useState("");

  const [editNameError, setEditNameError] =
    useState("");

  const [moreUser, setMoreUser] =
    useState<User | null>(null);
  


  const filteredUsers = useMemo(() => {
    const normalizedKeyword = keyword
      .trim()
      .toLowerCase();

    return users.filter((user) => {
      const matchesKeyword =
        !normalizedKeyword ||
        user.name
          .toLowerCase()
          .includes(normalizedKeyword) ||
        user.phone
          .toLowerCase()
          .includes(normalizedKeyword) ||
        (user.email ?? "")
          .toLowerCase()
          .includes(normalizedKeyword);

      const matchesRole =
        roleFilter === "ALL" ||
        user.role === roleFilter;

      const matchesStatus =
        statusFilter === "ALL" ||
        user.status === statusFilter;

      return (
        matchesKeyword &&
        matchesRole &&
        matchesStatus
      );
    });
  }, [
    users,
    keyword,
    roleFilter,
    statusFilter,
  ]);

  const customerServiceCount =
    users.filter(
      (user) =>
        user.role === "CUSTOMER_SERVICE",
    ).length;

  const administratorCount =
    users.filter(
      (user) =>
        user.role === "SYSTEM_ADMIN",
    ).length;

  const disabledCount =
    users.filter(
      (user) =>
        user.status === "DISABLED",
    ).length;

  function showSuccess(message: string) {
    setSuccessMessage(message);

    window.setTimeout(() => {
      setSuccessMessage("");
    }, 2000);
  }

  function handleCreated(user: User) {
    setUsers((previous) => [
      {
        ...user,
        phone: maskPhone(user.phone),
      },
      ...previous,
    ]);

    showSuccess(
      "客服人员账号创建成功",
    );
  }

  function handleRefresh() {
    setKeyword("");
    setRoleFilter("ALL");
    setStatusFilter("ALL");

    showSuccess("用户列表已刷新");
  }

  function openEditModal(user: User) {
    setEditingUser(user);
    setEditName(user.name);
    setEditNameError("");
  }

  function closeEditModal() {
    setEditingUser(null);
    setEditName("");
    setEditNameError("");
  }

  function validateEditName(
    value: string,
  ): string {
    const normalizedName =
      value.trim();

    if (!normalizedName) {
      return "请输入用户姓名";
    }

    if (
      normalizedName.length < 2 ||
      normalizedName.length > 20
    ) {
      return "用户姓名长度应为2–20个字符";
    }

    if (
      !/^[\u4e00-\u9fffA-Za-z]+(?:\s[\u4e00-\u9fffA-Za-z]+)*$/.test(
        normalizedName,
      )
    ) {
      return "用户姓名应支持中文、英文或中英文组合";
    }

    return "";
  }

  function handleSaveEdit() {
    if (!editingUser) {
      return;
    }

    const error =
      validateEditName(editName);

    if (error) {
      setEditNameError(error);
      return;
    }

    const normalizedName =
      editName.trim();

    setUsers((previous) =>
      previous.map((user) =>
        user.userId ===
        editingUser.userId
          ? {
              ...user,
              name: normalizedName,
            }
          : user,
      ),
    );

    closeEditModal();
    showSuccess(
      "用户信息修改成功",
    );
  }

  function handleToggleStatus(
    user: User,
  ) {
    const nextStatus =
      user.status === "ENABLED"
        ? "DISABLED"
        : "ENABLED";

    const nextStatusName =
      nextStatus === "ENABLED"
        ? "启用"
        : "禁用";

    const confirmed =
      window.confirm(
        user.status === "ENABLED"
          ? `确认禁用客服人员“${user.name}”吗？`
          : `确认启用客服人员“${user.name}”吗？`,
      );

    if (!confirmed) {
      return;
    }

    setUsers((previous) =>
      previous.map((item) =>
        item.userId === user.userId
          ? {
              ...item,
              status: nextStatus,
              statusName:
                nextStatusName,
            }
          : item,
      ),
    );

    showSuccess(
      nextStatus === "ENABLED"
        ? "用户账号已启用"
        : "用户账号已禁用",
    );
  }

  function handleMoreAction(
    action:
      | "RESET_PASSWORD"
      | "VIEW_LOG",
  ) {
    if (!moreUser) {
      return;
    }

    if (
      action === "RESET_PASSWORD"
    ) {
      window.alert(
        `“${moreUser.name}”的重置初始密码功能将在后端接口完成后接入。`,
      );
    }

    if (action === "VIEW_LOG") {
      window.alert(
        `“${moreUser.name}”的操作日志功能由对应模块后续接入。`,
      );
    }

    setMoreUser(null);
  }

  return (
    <div className="app-layout">
      {successMessage && (
        <div className="success-toast">
          {successMessage}
        </div>
      )}

      <aside className="sidebar">
        <div className="brand-area">
          <img
            src={carePilotLogo}
            alt="CarePilot AI 智能客服助手"
            className="brand-logo-image"
          />
        </div>

        <nav className="sidebar-nav">
          {NAVIGATION_ITEMS.map(
            (item) => {
              const Icon =
                item.icon;

              const isActive =
                item.label ===
                "用户管理";

              return (
                <button
                  key={item.label}
                  type="button"
                  className={`sidebar-nav__item ${
                    isActive
                      ? "is-active"
                      : ""
                  }`}
                  onClick={() => {
                    if (!isActive) {
                      window.alert(
                        `${item.label}由其他前端模块负责，当前页面暂未接入。`,
                      );
                    }
                  }}
                >
                  <Icon size={19} />
                  <span>
                    {item.label}
                  </span>
                </button>
              );
            },
          )}
        </nav>
      </aside>

      <div
  className={`management-page ${
    drawerOpen ? "has-drawer" : ""
  }`}
>
        <main className="management-content">
          <header className="page-header">
            <div>
              <h1>用户管理</h1>
              <p>
                管理系统用户账号与使用状态
              </p>
            </div>

            <div className="page-header__right">
              <div className="service-status">
                <span />
                AI服务正常
              </div>

              <div className="current-user">
                <div className="current-user__avatar">
                  陈
                </div>

                <span>陈一冉</span>
                <ChevronDown
                  size={16}
                />
              </div>
            </div>
          </header>

          <section className="statistic-grid">
            <StatisticCard
              title="总用户数"
              value={users.length}
              trend={12}
            />

            <StatisticCard
              title="客服人员数"
              value={
                customerServiceCount
              }
              trend={10}
            />

            <StatisticCard
              title="系统管理员数"
              value={
                administratorCount
              }
              trend={2}
              trendType="warning"
            />

            <StatisticCard
              title="禁用账号数"
              value={disabledCount}
              trend={1}
              trendType="warning"
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
                    onChange={(
                      event,
                    ) =>
                      setKeyword(
                        event.target
                          .value,
                      )
                    }
                  />
                </label>

                <label className="filter-group">
                  <span>角色：</span>

                  <select
                    value={
                      roleFilter
                    }
                    onChange={(
                      event,
                    ) =>
                      setRoleFilter(
                        event.target
                          .value as RoleFilter,
                      )
                    }
                  >
                    <option value="ALL">
                      全部角色
                    </option>

                    <option value="CUSTOMER_SERVICE">
                      客服人员
                    </option>

                    <option value="SYSTEM_ADMIN">
                      系统管理员
                    </option>
                  </select>
                </label>

                <label className="filter-group">
                  <span>状态：</span>

                  <select
                    value={
                      statusFilter
                    }
                    onChange={(
                      event,
                    ) =>
                      setStatusFilter(
                        event.target
                          .value as StatusFilter,
                      )
                    }
                  >
                    <option value="ALL">
                      全部状态
                    </option>

                    <option value="ENABLED">
                      启用
                    </option>

                    <option value="DISABLED">
                      禁用
                    </option>
                  </select>
                </label>
              </div>

              <div className="toolbar-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={
                    handleRefresh
                  }
                >
                  <RefreshCw
                    size={17}
                  />
                  刷新
                </button>

                <button
                  type="button"
                  className="primary-button"
                  onClick={() =>
                    setDrawerOpen(
                      true,
                    )
                  }
                >
                  <Plus size={17} />
                  创建客服人员账号
                </button>
              </div>
            </div>

            <div className="user-table">
              <div className="user-table__row user-table__head">
                <span>
                  用户姓名
                </span>
                <span>手机号</span>
                <span>邮箱</span>
                <span>角色</span>
                <span>状态</span>
                <span>
                  创建时间
                </span>
                <span>操作</span>
              </div>

              {filteredUsers.length ===
              0 ? (
                <div className="empty-state">
                  <Users
                    size={34}
                  />
                  <p>
                    暂无相关数据
                  </p>
                </div>
              ) : (
                filteredUsers.map(
                  (user) => (
                    <div
                      className="user-table__row"
                      key={
                        user.userId
                      }
                    >
                      <strong>
                        {user.name}
                      </strong>

                      <span>
                        {user.phone}
                      </span>

                      <span>
                        {user.email ||
                          "—"}
                      </span>

                      <span
                        className={
                          user.role ===
                          "SYSTEM_ADMIN"
                            ? "role-tag role-tag--admin"
                            : "role-tag"
                        }
                      >
                        {
                          user.roleName
                        }
                      </span>

                      <span
                        className={
                          user.status ===
                          "DISABLED"
                            ? "status-tag status-tag--disabled"
                            : "status-tag"
                        }
                      >
                        {
                          user.statusName
                        }
                      </span>

                      <time>
                        {
                          user.createdAt
                        }
                      </time>

                      <div className="user-actions">
                        <button
                          type="button"
                          className="text-action"
                          onClick={() =>
                            openEditModal(
                              user,
                            )
                          }
                        >
                          编辑
                        </button>

                        <button
                          type="button"
                          className={
                            user.status ===
                            "ENABLED"
                              ? "text-action text-action--danger"
                              : "text-action"
                          }
                          onClick={() =>
                            handleToggleStatus(
                              user,
                            )
                          }
                        >
                          {user.status ===
                          "ENABLED"
                            ? "禁用"
                            : "启用"}
                        </button>

                        <button
                          type="button"
                          className="text-action text-action--more"
                          onClick={() =>
                            setMoreUser(
                              user,
                            )
                          }
                        >
                          更多
                          <ChevronDown
                            size={
                              14
                            }
                          />
                        </button>
                      </div>
                    </div>
                  ),
                )
              )}
            </div>

            <div className="table-footer">
              <span>
                共{" "}
                {
                  filteredUsers.length
                }{" "}
                条
              </span>

              <div className="pagination">
                <button
                  type="button"
                  className="pagination__item is-active"
                >
                  1
                </button>
              </div>
            </div>
          </section>
        </main>

        <CreateAgentDrawer
          open={drawerOpen}
          onClose={() =>
            setDrawerOpen(false)
          }
          onCreated={handleCreated}
        />

        {editingUser && (
          <div
            className="modal-overlay"
            onMouseDown={
              closeEditModal
            }
          >
            <section
              className="account-modal edit-user-modal"
              onMouseDown={(
                event,
              ) =>
                event.stopPropagation()
              }
            >
              <header className="account-modal__header">
                <div>
                  <h2>
                    编辑用户信息
                  </h2>

                  <p>
                    {
                      editingUser.name
                    }
                    <span>·</span>
                    {
                      editingUser.phone
                    }
                  </p>
                </div>

                <button
                  type="button"
                  className="modal-close-button"
                  onClick={
                    closeEditModal
                  }
                  aria-label="关闭编辑弹窗"
                >
                  <X size={21} />
                </button>
              </header>

              <div className="account-modal__body">
                <label className="modal-form-field">
                  <span>
                    用户姓名
                    <b>*</b>
                  </span>

                  <input
                    value={editName}
                    maxLength={20}
                    onChange={(
                      event,
                    ) => {
                      setEditName(
                        event.target
                          .value,
                      );

                      if (
                        editNameError
                      ) {
                        setEditNameError(
                          "",
                        );
                      }
                    }}
                    onBlur={() =>
                      setEditNameError(
                        validateEditName(
                          editName,
                        ),
                      )
                    }
                  />

                  {editNameError ? (
                    <small className="modal-field-error">
                      {
                        editNameError
                      }
                    </small>
                  ) : (
                    <small>
                      手机号作为登录账号，不可在此演示中修改。
                    </small>
                  )}
                </label>
              </div>

              <footer className="account-modal__footer">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={
                    closeEditModal
                  }
                >
                  取消
                </button>

                <button
                  type="button"
                  className="primary-button"
                  onClick={
                    handleSaveEdit
                  }
                  disabled={
                    editName
                      .trim()
                      .length < 2
                  }
                >
                  保存修改
                </button>
              </footer>
            </section>
          </div>
        )}

        {moreUser && (
          <div
            className="modal-overlay"
            onMouseDown={() =>
              setMoreUser(null)
            }
          >
            <section
              className="account-modal more-action-modal"
              onMouseDown={(
                event,
              ) =>
                event.stopPropagation()
              }
            >
              <header className="account-modal__header">
                <div>
                  <h2>
                    更多操作
                  </h2>

                  <p>
                    {moreUser.name}
                    <span>·</span>
                    {moreUser.phone}
                  </p>
                </div>

                <button
                  type="button"
                  className="modal-close-button"
                  onClick={() =>
                    setMoreUser(
                      null,
                    )
                  }
                  aria-label="关闭更多操作弹窗"
                >
                  <X size={21} />
                </button>
              </header>

              <div className="account-modal__body">
                <p className="more-action-description">
                  选择需要对该账号执行的操作：
                </p>

                <button
                  type="button"
                  className="more-action-button"
                  onClick={() =>
                    handleMoreAction(
                      "RESET_PASSWORD",
                    )
                  }
                >
                  <KeyRound
                    size={19}
                  />
                  <span>
                    重置初始密码
                  </span>
                </button>

                <button
                  type="button"
                  className="more-action-button"
                  onClick={() =>
                    handleMoreAction(
                      "VIEW_LOG",
                    )
                  }
                >
                  <History
                    size={19}
                  />
                  <span>
                    查看操作日志
                  </span>
                </button>

                <button
                  type="button"
                  className="more-action-button"
                  onClick={() =>
                    setMoreUser(
                      null,
                    )
                  }
                >
                  <Users
                    size={19}
                  />
                  <span>
                    返回用户列表
                  </span>
                </button>
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
  trendType?:
    | "normal"
    | "warning";
}

function StatisticCard({
  title,
  value,
  trend,
  trendType = "normal",
}: StatisticCardProps) {
  return (
    <article className="statistic-card">
      <div className="statistic-card__content">
        <span className="statistic-card__title">
          {title}
        </span>

        <strong>{value}</strong>

        <small
          className={
            trendType ===
            "warning"
              ? "trend-text trend-text--warning"
              : "trend-text"
          }
        >
          较上月 +{trend}
        </small>
      </div>

      <div className="statistic-icon">
        <Users size={23} />
      </div>
    </article>
  );
}

function maskPhone(
  phone: string,
) {
  if (phone.includes("*")) {
    return phone;
  }

  return `${phone.slice(
    0,
    3,
  )}****${phone.slice(-4)}`;
}