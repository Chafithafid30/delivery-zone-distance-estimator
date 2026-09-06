import { useState } from "react";
import { deliveryApi, getRequestErrorMessage } from "./api";
import { DeliveryBoard } from "./components/DeliveryBoard";
import { DeliveryForm } from "./components/DeliveryForm";
import { DeleteDeliveryDialog } from "./components/DeleteDeliveryDialog";
import { useDeliveries } from "./hooks/useDeliveries";
import type { Delivery, DeliveryFilters, DeliveryInput } from "./types";

export default function App() {
  const [filters, setFilters] = useState<DeliveryFilters>({
    zone: "",
    status: "",
  });
  const [editingDelivery, setEditingDelivery] = useState<Delivery | null>(null);
  const [deliveryPendingDeletion, setDeliveryPendingDeletion] =
    useState<Delivery | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [activeDeliveryId, setActiveDeliveryId] = useState<number | null>(null);
  const [notificationMessage, setNotificationMessage] = useState("");
  const [actionErrorMessage, setActionErrorMessage] = useState("");
  const {
    deliveries,
    factoryCoordinates,
    isLoading,
    loadErrorMessage,
    reloadDeliveries,
  } = useDeliveries(filters);
  const isBusy = isSaving || activeDeliveryId !== null;

  function clearActionFeedback() {
    setNotificationMessage("");
    setActionErrorMessage("");
  }

  function beginEditingDelivery(delivery: Delivery) {
    setEditingDelivery({ ...delivery });
  }

  async function handleSaveDelivery(deliveryInput: DeliveryInput) {
    setIsSaving(true);
    clearActionFeedback();
    try {
      const savedDelivery = editingDelivery
        ? await deliveryApi.updateDelivery(editingDelivery.id, deliveryInput)
        : await deliveryApi.createDelivery(deliveryInput);
      let message = `${savedDelivery.orderRef} berhasil disimpan.`;
      if (savedDelivery.zone === "UNKNOWN") {
        message += " Zona belum diketahui; pengiriman tetap tersimpan.";
      }
      if (filters.zone || filters.status) {
        message += " Filter aktif dapat menyembunyikan pengiriman ini.";
      }
      setNotificationMessage(message);
      setEditingDelivery(null);
      reloadDeliveries();
    } finally {
      // The form displays validation errors and keeps the user's input on failure.
      setIsSaving(false);
    }
  }

  async function handleDeleteDelivery() {
    if (!deliveryPendingDeletion || isBusy) {
      return;
    }
    const deliveryToDelete = deliveryPendingDeletion;
    setActiveDeliveryId(deliveryToDelete.id);
    clearActionFeedback();
    try {
      await deliveryApi.deleteDelivery(deliveryToDelete.id);
      if (editingDelivery?.id === deliveryToDelete.id) {
        setEditingDelivery(null);
      }
      setNotificationMessage(`${deliveryToDelete.orderRef} berhasil dihapus.`);
      reloadDeliveries();
    } catch (error) {
      setActionErrorMessage(getRequestErrorMessage(error));
    } finally {
      setDeliveryPendingDeletion(null);
      setActiveDeliveryId(null);
    }
  }

  async function handleRetryGeocoding(delivery: Delivery) {
    if (isBusy) {
      return;
    }
    setActiveDeliveryId(delivery.id);
    clearActionFeedback();
    try {
      const updatedDelivery = await deliveryApi.retryGeocoding(delivery.id);
      const message =
        updatedDelivery.zone === "UNKNOWN"
          ? `${updatedDelivery.orderRef}: koordinat masih belum tersedia. Coba kembali nanti atau perjelas alamat.`
          : `${updatedDelivery.orderRef}: zona berhasil diperbarui menjadi ${updatedDelivery.zone}.`;
      setNotificationMessage(message);
      reloadDeliveries();
    } catch (error) {
      setActionErrorMessage(getRequestErrorMessage(error));
    } finally {
      setActiveDeliveryId(null);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main">
          <span className="brand-mark" aria-hidden="true">
            DZ
          </span>
          Delivery Zone
        </a>
        <span className="topbar-label">OPERASIONAL PENGIRIMAN</span>
      </header>
      <main id="main">
        <div className="page-heading">
          <div>
            <p className="eyebrow">DISPATCH / JAKARTA</p>
            <h1>Papan pengiriman</h1>
          </div>
          <div className="origin">
            <span>Titik keberangkatan</span>
            <strong>Pabrik Jakarta</strong>
            <span className="mono">
              {factoryCoordinates
                ? `${factoryCoordinates.latitude.toFixed(4)}, ${factoryCoordinates.longitude.toFixed(4)}`
                : "Koordinat belum tersedia"}
            </span>
          </div>
        </div>
        <div className="zone-legend" aria-label="Batas zona pengiriman">
          <div>
            <span className="legend-line local" />
            <strong>LOCAL</strong>
            <span>&lt; 50 km</span>
          </div>
          <div>
            <span className="legend-line regional" />
            <strong>REGIONAL</strong>
            <span>50–300 km</span>
          </div>
          <div>
            <span className="legend-line long-haul" />
            <strong>LONG_HAUL</strong>
            <span>&gt; 300 km</span>
          </div>
          <p>Jarak garis lurus dari pabrik</p>
        </div>
        {notificationMessage && (
          <div className="notice" role="status">
            {notificationMessage}
            <button
              className="dismiss"
              aria-label="Tutup pemberitahuan"
              onClick={() => setNotificationMessage("")}
            >
              ×
            </button>
          </div>
        )}
        {actionErrorMessage && (
          <div className="error-box" role="alert">
            {actionErrorMessage}
          </div>
        )}
        <div className="workspace">
          <DeliveryForm
            editingDelivery={editingDelivery}
            isBusy={isBusy}
            isSaving={isSaving}
            onSave={handleSaveDelivery}
            onCancelEdit={() => setEditingDelivery(null)}
          />
          <DeliveryBoard
            deliveries={deliveries}
            filters={filters}
            isLoading={isLoading}
            errorMessage={loadErrorMessage}
            isBusy={isBusy}
            activeDeliveryId={activeDeliveryId}
            onFiltersChange={setFilters}
            onReload={reloadDeliveries}
            onEdit={beginEditingDelivery}
            onRequestDelete={setDeliveryPendingDeletion}
            onRetryGeocoding={handleRetryGeocoding}
          />
        </div>
      </main>
      <footer>
        <span>Delivery Zone · Case B-002</span>
        <span>
          Data lokasi ©{" "}
          <a
            href="https://www.openstreetmap.org/copyright"
            target="_blank"
            rel="noreferrer"
          >
            OpenStreetMap contributors
          </a>{" "}
          ·{" "}
          <a
            href="https://operations.osmfoundation.org/policies/nominatim/"
            target="_blank"
            rel="noreferrer"
          >
            Kebijakan Nominatim
          </a>
        </span>
      </footer>
      <DeleteDeliveryDialog
        delivery={deliveryPendingDeletion}
        isDeleting={
          deliveryPendingDeletion !== null &&
          activeDeliveryId === deliveryPendingDeletion.id
        }
        onCancel={() => setDeliveryPendingDeletion(null)}
        onConfirm={handleDeleteDelivery}
      />
    </div>
  );
}
