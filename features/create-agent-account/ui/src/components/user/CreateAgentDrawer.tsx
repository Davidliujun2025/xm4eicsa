import {
  CheckCircle2,
  Eye,
  EyeOff,
  Info,
  LoaderCircle,
  X,
} from "lucide-react";
import {
  useMemo,
  useState,
} from "react";

import type {
  ChangeEvent,
  FormEvent,
  ReactNode,
} from "react";

import {
  ApiError,
  createCustomerServiceUser,
} from "../../api/userApi";

import type {
  CreateAgentRequest,
  FieldErrors,
  User,
} from "../../types/user";

import {
  validateCreateAgentForm,
  validateEmail,
  validateName,
  validatePassword,
  validatePhone,
} from "../../utils/validators";

interface CreateAgentDrawerProps {
  open: boolean;
  onClose: () => void;
  onCreated: (user: User) => void;
}

const EMPTY_FORM: CreateAgentRequest = {
  name: "",
  phone: "",
  email: "",
  initialPassword: "",
};

export default function CreateAgentDrawer({
  open,
  onClose,
  onCreated,
}: CreateAgentDrawerProps) {
  const [form, setForm] =
    useState<CreateAgentRequest>(EMPTY_FORM);

  const [errors, setErrors] =
    useState<FieldErrors>({});

  const [showPassword, setShowPassword] =
    useState(false);

  const [submitting, setSubmitting] =
    useState(false);

  /**
   * 这里只判断三个必填项是否已填写。
   * 最终是否可以创建，仍由 handleSubmit 中的完整校验决定。
   */
  const isFormComplete = useMemo(() => {
    return Boolean(
      form.name.trim() &&
        form.phone.trim() &&
        form.initialPassword,
    );
  }, [form]);

  if (!open) {
    return null;
  }

  function updateField(
    event: ChangeEvent<HTMLInputElement>,
  ) {
    const field = event.target
      .name as keyof CreateAgentRequest;

    let value = event.target.value;

    /**
     * 手机号输入框只保留数字，
     * 并且最多允许输入 11 位。
     */
    if (field === "phone") {
      value = value
        .replace(/\D/g, "")
        .slice(0, 11);
    }

    setForm((previous) => ({
      ...previous,
      [field]: value,
    }));

    /**
     * 用户修改字段后，清除这个字段原有的错误提示。
     */
    setErrors((previous) => ({
      ...previous,
      [field]: undefined,
    }));
  }

  function validateSingleField(
    field: keyof CreateAgentRequest,
  ) {
    let error = "";

    if (field === "name") {
      error = validateName(form.name);
    }

    if (field === "phone") {
      error = validatePhone(form.phone);
    }

    if (field === "email") {
      error = validateEmail(
        form.email ?? "",
      );
    }

    if (field === "initialPassword") {
      error = validatePassword(
        form.initialPassword,
        form.phone,
      );
    }

    setErrors((previous) => ({
      ...previous,
      [field]: error || undefined,
    }));
  }

  function resetDrawer() {
    setForm(EMPTY_FORM);
    setErrors({});
    setShowPassword(false);
  }

  function handleClose() {
    if (submitting) {
      return;
    }

    resetDrawer();
    onClose();
  }

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const validationErrors =
      validateCreateAgentForm(form);

    setErrors(validationErrors);

    if (
      Object.keys(validationErrors).length >
      0
    ) {
      return;
    }

    try {
      setSubmitting(true);
    
      const response =
        await createCustomerServiceUser(form);
    
      if (!response.data) {
        throw new Error(
          "后端没有返回新用户数据",
        );
      }
    
      onCreated(response.data);
    
      resetDrawer();
      onClose();
    } catch (error: unknown) {
      if (error instanceof ApiError) {
        if (
          error.response.code ===
          "PHONE_ALREADY_EXISTS"
        ) {
          setErrors((previous) => ({
            ...previous,
            phone: error.response.message,
          }));
    
          return;
        }
    
        if (
          error.response.code ===
          "VALIDATION_ERROR"
        ) {
          const fieldErrors =
            error.response.data?.fieldErrors;
    
          if (fieldErrors) {
            setErrors(fieldErrors);
            return;
          }
    
          const message =
            error.response.message;
    
          if (message.includes("手机号")) {
            setErrors((previous) => ({
              ...previous,
              phone: message,
            }));
            return;
          }
    
          if (message.includes("邮箱")) {
            setErrors((previous) => ({
              ...previous,
              email: message,
            }));
            return;
          }
    
          if (message.includes("密码")) {
            setErrors((previous) => ({
              ...previous,
              initialPassword: message,
            }));
            return;
          }
        }
    
        window.alert(
          error.response.message ||
            "创建失败，请稍后重试",
        );
    
        return;
      }
    
      window.alert(
        "无法连接后端，请确认 Spring Boot 已启动",
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <aside
      className="create-drawer"
      aria-label="创建客服人员账号"
    >
      <header className="create-drawer__header">
        <div>
          <h2>创建客服人员账号</h2>

          <p>
            新客服人员创建后即可使用智能客服工具
          </p>
        </div>

        <button
          type="button"
          className="icon-button"
          onClick={handleClose}
          disabled={submitting}
          aria-label="关闭创建面板"
        >
          <X size={20} />
        </button>
      </header>

      <form
        className="create-form"
        onSubmit={handleSubmit}
        noValidate
      >
        <div className="create-form__body">
          <div className="info-card">
            <Info size={18} />

            <span>
              手机号将作为唯一登录账号；邮箱仅用于接收修改密码验证码，不作为登录账号。
            </span>
          </div>

          <FormField
            label="用户姓名"
            required
            error={errors.name}
            hint="支持中文、英文或中英文组合，长度2–20个字符"
          >
            <div
              className={`input-shell ${
                errors.name
                  ? "has-error"
                  : ""
              }`}
            >
              <input
                name="name"
                value={form.name}
                placeholder="请输入用户姓名"
                maxLength={20}
                autoComplete="off"
                onChange={updateField}
                onBlur={() =>
                  validateSingleField(
                    "name",
                  )
                }
              />

              <span className="character-count">
                {form.name.length}/20
              </span>
            </div>
          </FormField>

          <FormField
            label="手机号"
            required
            error={errors.phone}
            hint="中国大陆11位手机号，将作为唯一登录账号"
          >
            <div
              className={`input-shell ${
                errors.phone
                  ? "has-error"
                  : ""
              }`}
            >
              <input
                name="phone"
                value={form.phone}
                placeholder="请输入手机号"
                inputMode="numeric"
                maxLength={11}
                autoComplete="off"
                onChange={updateField}
                onBlur={() =>
                  validateSingleField(
                    "phone",
                  )
                }
              />

              <span className="character-count">
                {form.phone.length}/11
              </span>
            </div>
          </FormField>

          <FormField
            label="邮箱"
            optional
            error={errors.email}
            hint="仅用于接收修改密码验证码，不作为登录账号"
          >
            <div
              className={`input-shell ${
                errors.email
                  ? "has-error"
                  : ""
              }`}
            >
              <input
                name="email"
                type="email"
                value={form.email ?? ""}
                placeholder="请输入邮箱地址"
                autoComplete="off"
                onChange={updateField}
                onBlur={() =>
                  validateSingleField(
                    "email",
                  )
                }
              />
            </div>
          </FormField>

          <FormField
            label="初始密码"
            required
            error={
              errors.initialPassword
            }
            hint="12–20位，必须同时包含英文字母和数字"
          >
            <div
              className={`input-shell ${
                errors.initialPassword
                  ? "has-error"
                  : ""
              }`}
            >
              <input
                name="initialPassword"
                type={
                  showPassword
                    ? "text"
                    : "password"
                }
                value={
                  form.initialPassword
                }
                placeholder="请输入初始密码"
                minLength={12}
                maxLength={20}
                autoComplete="new-password"
                onChange={updateField}
                onBlur={() =>
                  validateSingleField(
                    "initialPassword",
                  )
                }
              />

              <span className="character-count">
                {
                  form.initialPassword
                    .length
                }
                /20
              </span>

              <button
                type="button"
                className="password-toggle"
                onClick={() =>
                  setShowPassword(
                    (previous) =>
                      !previous,
                  )
                }
                aria-label={
                  showPassword
                    ? "隐藏密码"
                    : "显示密码"
                }
              >
                {showPassword ? (
                  <EyeOff size={18} />
                ) : (
                  <Eye size={18} />
                )}
              </button>
            </div>
          </FormField>

          <FormField
            label="角色"
            hint="角色固定，不可切换为系统管理员"
          >
            <div className="input-shell is-disabled">
              <input
                value="客服人员"
                disabled
                readOnly
              />
            </div>
          </FormField>
        </div>

        <footer className="create-form__footer">
          <button
            type="button"
            className="secondary-button"
            onClick={handleClose}
            disabled={submitting}
          >
            取消
          </button>

          <button
            type="submit"
            className="primary-button"
            disabled={
              !isFormComplete ||
              submitting
            }
          >
            {submitting ? (
              <>
                <LoaderCircle
                  className="spinning"
                  size={17}
                />
                正在创建
              </>
            ) : (
              "确认创建"
            )}
          </button>
        </footer>
      </form>
    </aside>
  );
}

interface FormFieldProps {
  label: string;
  required?: boolean;
  optional?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
}

function FormField({
  label,
  required = false,
  optional = false,
  error,
  hint,
  children,
}: FormFieldProps) {
  return (
    <div className="form-field">
      <label>
        {label}

        {required && (
          <span className="required-mark">
            *
          </span>
        )}

        {optional && (
          <span className="optional-mark">
            （可选）
          </span>
        )}
      </label>

      {children}

      {error ? (
        <p className="field-message is-error">
          {error}
        </p>
      ) : (
        hint && (
          <p className="field-message is-success">
            <CheckCircle2 size={13} />
            {hint}
          </p>
        )
      )}
    </div>
  );
}
