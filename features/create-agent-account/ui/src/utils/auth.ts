function isLocalDevelopment() {
  return import.meta.env.DEV &&
    (window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost");
}

function buildBaseLoginUrl() {
  if (isLocalDevelopment()) {
    return import.meta.env.VITE_LOGIN_URL ||
      `${window.location.protocol}//${window.location.hostname}:15173/`;
  }

  return `${window.location.origin}/`;
}

function appendReturnUrl(loginUrl: string, returnUrl?: string) {
  if (!returnUrl) {
    return loginUrl;
  }

  const targetUrl = new URL(loginUrl, window.location.origin);
  targetUrl.searchParams.set("returnUrl", returnUrl);
  return targetUrl.toString();
}

function readCookie(name: string) {
  const cookie = document.cookie
    .split("; ")
    .find((item) => item.startsWith(`${name}=`));
  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : "";
}

async function ensureCsrfToken() {
  if (readCookie("XSRF-TOKEN")) {
    return readCookie("XSRF-TOKEN");
  }

  await fetch("/api/v1/public/login-config", {
    credentials: "include",
  });

  return readCookie("XSRF-TOKEN");
}

export function getLoginUrl(returnUrl?: string) {
  return appendReturnUrl(buildBaseLoginUrl(), returnUrl);
}

export async function logoutAndRedirect(returnUrl?: string) {
  try {
    const csrfToken = await ensureCsrfToken();
    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), 4000);
    try {
      await fetch("/api/v1/auth/logout", {
        method: "POST",
        credentials: "include",
        signal: controller.signal,
        headers: csrfToken
          ? {
              "X-XSRF-TOKEN": csrfToken,
            }
          : undefined,
      });
    } finally {
      window.clearTimeout(timeoutId);
    }
  } finally {
    window.location.replace(getLoginUrl(returnUrl));
  }
}
