import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { deliveryApi } from "../api";
import type { Delivery, DeliveryFilters } from "../types";
import { useDeliveries } from "./useDeliveries";

afterEach(cleanup);

it("keeps the current filter results when an older request resolves late", async () => {
  const localDelivery: Delivery = {
    id: 1,
    orderRef: "DO-LOCAL",
    destAddress: "Monas Jakarta",
    destLat: -6.1751,
    destLng: 106.865,
    distanceKm: 0,
    zone: "LOCAL",
    status: "PLANNED",
    createdAt: "2026-09-06T00:00:00Z",
    geocodedAt: "2026-09-06T00:00:00Z",
    resolvedAddress: "Monas Jakarta",
    geocodeSource: "CACHE",
    estimate: { costIdr: 25000, minDays: 1, maxDays: 1 },
  };
  const obsoleteDelivery: Delivery = {
    ...localDelivery,
    id: 2,
    orderRef: "DO-OLD",
  };
  let resolveOlderRequest: (deliveries: Delivery[]) => void = () => {};
  const olderRequest = new Promise<Delivery[]>((resolve) => {
    resolveOlderRequest = resolve;
  });
  const listDeliveries = vi
    .spyOn(deliveryApi, "listDeliveries")
    .mockReturnValueOnce(olderRequest)
    .mockResolvedValueOnce([localDelivery]);
  vi.spyOn(deliveryApi, "getConfiguration").mockResolvedValue({
    factory: { latitude: -6.1751, longitude: 106.865 },
  });

  const initialFilters: DeliveryFilters = { zone: "", status: "" };
  const { result, rerender } = renderHook(
    (filters: DeliveryFilters) => useDeliveries(filters),
    {
      initialProps: initialFilters,
    },
  );
  await waitFor(() => expect(listDeliveries).toHaveBeenCalledTimes(1));
  rerender({ zone: "LOCAL", status: "" });
  await waitFor(() =>
    expect(result.current.deliveries).toEqual([localDelivery]),
  );

  await act(async () => {
    resolveOlderRequest([obsoleteDelivery]);
  });
  expect(result.current.deliveries).toEqual([localDelivery]);
  expect(result.current.loadErrorMessage).toBe("");
  expect(result.current.isLoading).toBe(false);
});
