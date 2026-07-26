import type {
  ApiErrorResponse,
  CreateAgentRequest,
  CreateAgentResponse,
} from "../types/user";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL;

export class ApiError extends Error {
  status: number;
  response: ApiErrorResponse;

  constructor(
    status: number,
    response: ApiErrorResponse,
  ) {
    super(response.message);

    this.name = "ApiError";
    this.status = status;
    this.response = response;
  }
}

export async function createCustomerServiceUser(
  request: CreateAgentRequest,
): Promise<CreateAgentResponse> {
  const token =
    localStorage.getItem(
      "accessToken",
    );

  const response = await fetch(
    `${API_BASE_URL}/api/admin/customer-service-users`,
    {
      method: "POST",

      headers: {
        "Content-Type":
          "application/json",

        ...(token
          ? {
              Authorization:
                `Bearer ${token}`,
            }
          : {}),
      },

      body: JSON.stringify({
        name: request.name.trim(),
        phone: request.phone.trim(),
        email:
          request.email?.trim() ||
          null,
        initialPassword:
          request.initialPassword,
      }),
    },
  );

  let result:
    | CreateAgentResponse
    | ApiErrorResponse;

  try {
    result =
      (await response.json()) as
        | CreateAgentResponse
        | ApiErrorResponse;
  } catch {
    throw new Error(
      "后端返回的数据不是有效的 JSON",
    );
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      result as ApiErrorResponse,
    );
  }

  return result as CreateAgentResponse;
}