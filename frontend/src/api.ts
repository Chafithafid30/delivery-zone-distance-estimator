import type { Delivery, DeliveryInput, DeliveryStatus, Zone } from './types';

export class ApiError extends Error {
  constructor(message: string, public fields: Record<string, string> = {}) { super(message); }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch('/api' + path, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(error.message || `Permintaan gagal (${response.status}). Coba kembali.`, error.errors);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export const api = {
  list: (zone: Zone | '', status: DeliveryStatus | '', signal: AbortSignal) => {
    const query = new URLSearchParams();
    if (zone) query.set('zone', zone);
    if (status) query.set('status', status);
    return request<Delivery[]>('/deliveries?' + query, { signal });
  },
  config: (signal: AbortSignal) => request<{ factory: { latitude: number; longitude: number } }>('/config', { signal }),
  create: (input: DeliveryInput) => request<Delivery>('/deliveries', { method: 'POST', body: JSON.stringify(input) }),
  update: (id: number, input: DeliveryInput) => request<Delivery>(`/deliveries/${id}`, { method: 'PUT', body: JSON.stringify(input) }),
  remove: (id: number) => request<void>(`/deliveries/${id}`, { method: 'DELETE' }),
  retry: (id: number) => request<Delivery>(`/deliveries/${id}/geocode`, { method: 'POST' }),
};
