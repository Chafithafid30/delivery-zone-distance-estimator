export type Zone = "LOCAL" | "REGIONAL" | "LONG_HAUL" | "UNKNOWN";
export type DeliveryStatus =
  | "PLANNED"
  | "IN_TRANSIT"
  | "DELIVERED"
  | "CANCELLED";
export type GeocodeSource = "CACHE" | "NOMINATIM" | "UNAVAILABLE" | "NOT_FOUND";
export interface Delivery {
  id: number;
  orderRef: string;
  destAddress: string;
  destLat: number | null;
  destLng: number | null;
  distanceKm: number | null;
  zone: Zone;
  status: DeliveryStatus;
  createdAt: string;
  geocodedAt: string | null;
  resolvedAddress: string | null;
  geocodeSource: GeocodeSource;
  estimate: ShippingEstimate | null;
}
export interface DeliveryInput {
  orderRef: string;
  destAddress: string;
  status: DeliveryStatus;
}
export const DELIVERY_STATUS_LABELS: Record<DeliveryStatus, string> = {
  PLANNED: "Direncanakan",
  IN_TRANSIT: "Dalam perjalanan",
  DELIVERED: "Terkirim",
  CANCELLED: "Dibatalkan",
};
export const DELIVERY_ZONES: Zone[] = [
  "LOCAL",
  "REGIONAL",
  "LONG_HAUL",
  "UNKNOWN",
];

export interface ShippingEstimate {
  costIdr: number;
  minDays: number;
  maxDays: number;
}

export interface FactoryCoordinates {
  latitude: number;
  longitude: number;
}

export interface DeliveryFilters {
  zone: Zone | "";
  status: DeliveryStatus | "";
}
