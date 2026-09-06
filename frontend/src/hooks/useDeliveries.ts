import { useCallback, useEffect, useState } from "react";
import { deliveryApi, getRequestErrorMessage } from "../api";
import type { Delivery, DeliveryFilters, FactoryCoordinates } from "../types";

export function useDeliveries(filters: DeliveryFilters) {
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [factoryCoordinates, setFactoryCoordinates] =
    useState<FactoryCoordinates | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadErrorMessage, setLoadErrorMessage] = useState("");
  const [reloadVersion, setReloadVersion] = useState(0);
  const { zone, status } = filters;

  const reloadDeliveries = useCallback(() => {
    setReloadVersion((currentVersion) => currentVersion + 1);
  }, []);

  useEffect(() => {
    const abortController = new AbortController();
    setIsLoading(true);
    setLoadErrorMessage("");

    async function loadDeliveries() {
      try {
        const matchingDeliveries = await deliveryApi.listDeliveries(
          { zone, status },
          abortController.signal,
        );
        if (!abortController.signal.aborted) {
          setDeliveries(matchingDeliveries);
        }
      } catch (error) {
        if (!abortController.signal.aborted) {
          setLoadErrorMessage(getRequestErrorMessage(error));
        }
      } finally {
        if (!abortController.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    void loadDeliveries();
    return () => abortController.abort();
  }, [zone, status, reloadVersion]);

  useEffect(() => {
    const abortController = new AbortController();

    async function loadFactoryCoordinates() {
      try {
        const configuration = await deliveryApi.getConfiguration(
          abortController.signal,
        );
        if (!abortController.signal.aborted) {
          setFactoryCoordinates(configuration.factory);
        }
      } catch {
        // Missing configuration only affects the origin label, not the delivery list.
        if (!abortController.signal.aborted) {
          setFactoryCoordinates(null);
        }
      }
    }

    void loadFactoryCoordinates();
    return () => abortController.abort();
  }, [reloadVersion]);

  return {
    deliveries,
    factoryCoordinates,
    isLoading,
    loadErrorMessage,
    reloadDeliveries,
  };
}
