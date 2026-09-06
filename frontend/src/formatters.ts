import type { ShippingEstimate } from "./types";

const currencyFormatter = new Intl.NumberFormat("id-ID", {
  style: "currency",
  currency: "IDR",
  maximumFractionDigits: 0,
});
const distanceFormatter = new Intl.NumberFormat("id-ID", {
  maximumFractionDigits: 2,
});
const dateTimeFormatter = new Intl.DateTimeFormat("id-ID", {
  dateStyle: "medium",
  timeStyle: "short",
});

export function formatCurrency(amountIdr: number): string {
  return currencyFormatter.format(amountIdr);
}

export function formatDistance(distanceKm: number | null): string {
  if (distanceKm === null) {
    return "—";
  }
  return `${distanceFormatter.format(distanceKm)} km`;
}

export function formatDateTime(timestamp: string): string {
  return dateTimeFormatter.format(new Date(timestamp));
}

export function formatEstimatedArrival(
  estimate: ShippingEstimate | null,
): string {
  if (!estimate) {
    return "—";
  }
  if (estimate.minDays === estimate.maxDays) {
    return `${estimate.minDays} hari`;
  }
  return `${estimate.minDays}–${estimate.maxDays} hari`;
}
