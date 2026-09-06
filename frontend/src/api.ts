import type {
  Delivery,
  DeliveryFilters,
  DeliveryInput,
  FactoryCoordinates,
} from "./types";

interface ApiErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function getRequestErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "Tidak dapat terhubung ke server. Periksa koneksi lalu coba kembali.";
}

async function sendRequest<ResponseBody>(
  path: string,
  options: RequestInit = {},
): Promise<ResponseBody> {
  const headers = new Headers(options.headers);
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`/api${path}`, { ...options, headers });
  if (!response.ok) {
    const errorResponse: ApiErrorResponse = await response
      .json()
      .catch(() => ({}));
    const message =
      errorResponse.message ||
      `Permintaan gagal (${response.status}). Coba kembali.`;
    throw new ApiError(message, errorResponse.errors);
  }
  if (response.status === 204) {
    return undefined as ResponseBody;
  }
  return response.json();
}

export const deliveryApi = {
  listDeliveries(
    filters: DeliveryFilters,
    signal: AbortSignal,
  ): Promise<Delivery[]> {
    const queryParameters = new URLSearchParams();
    if (filters.zone) {
      queryParameters.set("zone", filters.zone);
    }
    if (filters.status) {
      queryParameters.set("status", filters.status);
    }
    return sendRequest(`/deliveries?${queryParameters}`, { signal });
  },

  getConfiguration(
    signal: AbortSignal,
  ): Promise<{ factory: FactoryCoordinates }> {
    return sendRequest("/config", { signal });
  },

  createDelivery(deliveryInput: DeliveryInput): Promise<Delivery> {
    return sendRequest("/deliveries", {
      method: "POST",
      body: JSON.stringify(deliveryInput),
    });
  },

  updateDelivery(
    deliveryId: number,
    deliveryInput: DeliveryInput,
  ): Promise<Delivery> {
    return sendRequest(`/deliveries/${deliveryId}`, {
      method: "PUT",
      body: JSON.stringify(deliveryInput),
    });
  },

  deleteDelivery(deliveryId: number): Promise<void> {
    return sendRequest(`/deliveries/${deliveryId}`, { method: "DELETE" });
  },

  retryGeocoding(deliveryId: number): Promise<Delivery> {
    return sendRequest(`/deliveries/${deliveryId}/geocode`, { method: "POST" });
  },
};
