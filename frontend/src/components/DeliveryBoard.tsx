import {
  DELIVERY_STATUS_LABELS,
  DELIVERY_ZONES,
  type Delivery,
  type DeliveryFilters,
  type DeliveryStatus,
  type Zone,
} from "../types";
import { DeliveryCard } from "./DeliveryCard";

interface DeliveryBoardProps {
  deliveries: Delivery[];
  filters: DeliveryFilters;
  isLoading: boolean;
  errorMessage: string;
  isBusy: boolean;
  activeDeliveryId: number | null;
  onFiltersChange: (filters: DeliveryFilters) => void;
  onReload: () => void;
  onEdit: (delivery: Delivery) => void;
  onRequestDelete: (delivery: Delivery) => void;
  onRetryGeocoding: (delivery: Delivery) => void;
}

export function DeliveryBoard({
  deliveries,
  filters,
  isLoading,
  errorMessage,
  isBusy,
  activeDeliveryId,
  onFiltersChange,
  onReload,
  onEdit,
  onRequestDelete,
  onRetryGeocoding,
}: DeliveryBoardProps) {
  const hasActiveFilters = Boolean(filters.zone || filters.status);

  function renderDeliveryList() {
    if (isLoading) {
      return (
        <div className="empty-state" role="status">
          <span className="spinner" />
          Memuat pengiriman…
        </div>
      );
    }
    if (errorMessage) {
      return (
        <div className="empty-state">
          <strong>Daftar belum dapat dimuat</strong>
          <p role="alert">{errorMessage}</p>
          <button className="button secondary" onClick={onReload}>
            Coba lagi
          </button>
        </div>
      );
    }
    if (deliveries.length === 0) {
      return (
        <div className="empty-state">
          <span className="empty-symbol" aria-hidden="true">
            ＋
          </span>
          <h3>
            {hasActiveFilters
              ? "Tidak ada hasil untuk filter ini"
              : "Pengiriman pertama dimulai di sini"}
          </h3>
          <p>
            {hasActiveFilters
              ? "Ubah filter untuk melihat pengiriman lainnya."
              : "Isi referensi pesanan dan alamat tujuan untuk menghitung zona pengiriman."}
          </p>
          {hasActiveFilters && (
            <button
              className="button secondary"
              onClick={() => onFiltersChange({ zone: "", status: "" })}
            >
              Hapus filter
            </button>
          )}
        </div>
      );
    }
    return (
      <div className="delivery-list">
        {deliveries.map((delivery) => (
          <DeliveryCard
            key={delivery.id}
            delivery={delivery}
            isBusy={isBusy}
            isRetrying={activeDeliveryId === delivery.id}
            onEdit={onEdit}
            onRequestDelete={onRequestDelete}
            onRetryGeocoding={onRetryGeocoding}
          />
        ))}
      </div>
    );
  }

  return (
    <section
      className="board-panel"
      aria-labelledby="board-title"
      aria-busy={isLoading}
    >
      <div className="board-heading">
        <div className="panel-heading">
          <span className="panel-number">02</span>
          <h2 id="board-title">Daftar pengiriman</h2>
        </div>
        <button
          className="button secondary compact"
          disabled={isLoading}
          onClick={onReload}
        >
          Muat ulang
        </button>
      </div>
      <div className="filters">
        <label>
          Zona
          <select
            value={filters.zone}
            onChange={(event) =>
              onFiltersChange({
                ...filters,
                zone: event.target.value as Zone | "",
              })
            }
          >
            <option value="">Semua zona</option>
            {DELIVERY_ZONES.map((zone) => (
              <option key={zone}>{zone}</option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            value={filters.status}
            onChange={(event) =>
              onFiltersChange({
                ...filters,
                status: event.target.value as DeliveryStatus | "",
              })
            }
          >
            <option value="">Semua status</option>
            {Object.entries(DELIVERY_STATUS_LABELS).map(
              ([statusValue, statusLabel]) => (
                <option key={statusValue} value={statusValue}>
                  {statusLabel}
                </option>
              ),
            )}
          </select>
        </label>
        {!isLoading && !errorMessage && (
          <span className="result-count">{deliveries.length} pengiriman</span>
        )}
      </div>
      {renderDeliveryList()}
      <p className="board-footnote">
        Biaya dan waktu tiba adalah simulasi per zona. Jarak bukan jarak rute
        jalan; zona ditentukan sebelum pembulatan jarak.
      </p>
    </section>
  );
}
