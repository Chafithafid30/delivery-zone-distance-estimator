import {
  formatCurrency,
  formatDateTime,
  formatDistance,
  formatEstimatedArrival,
} from "../formatters";
import { DELIVERY_STATUS_LABELS, type Delivery } from "../types";
import { ZoneDetails } from "./ZoneDetails";

interface DeliveryCardProps {
  delivery: Delivery;
  isBusy: boolean;
  isRetrying: boolean;
  onEdit: (delivery: Delivery) => void;
  onRequestDelete: (delivery: Delivery) => void;
  onRetryGeocoding: (delivery: Delivery) => void;
}

export function DeliveryCard({
  delivery,
  isBusy,
  isRetrying,
  onEdit,
  onRequestDelete,
  onRetryGeocoding,
}: DeliveryCardProps) {
  return (
    <article className="delivery-card">
      <div className="delivery-top">
        <div>
          <h3>{delivery.orderRef}</h3>
          <span className="created">{formatDateTime(delivery.createdAt)}</span>
        </div>
        <span className={`status status-${delivery.status.toLowerCase()}`}>
          {DELIVERY_STATUS_LABELS[delivery.status]}
        </span>
      </div>
      <p className="destination">{delivery.destAddress}</p>
      <div className="delivery-metrics">
        <div>
          <span className="metric-label">Zona tujuan</span>
          <ZoneDetails delivery={delivery} />
        </div>
        <div>
          <span className="metric-label">Jarak</span>
          <strong>{formatDistance(delivery.distanceKm)}</strong>
        </div>
        <div>
          <span className="metric-label">Estimasi biaya</span>
          <strong>
            {delivery.estimate
              ? formatCurrency(delivery.estimate.costIdr)
              : "—"}
          </strong>
        </div>
        <div>
          <span className="metric-label">Estimasi tiba</span>
          <strong>{formatEstimatedArrival(delivery.estimate)}</strong>
        </div>
      </div>
      <div className="delivery-actions">
        {delivery.zone === "UNKNOWN" && (
          <button
            className="text-button retry"
            disabled={isBusy}
            onClick={() => onRetryGeocoding(delivery)}
          >
            {isRetrying ? "Memproses…" : "Coba hitung zona"}
          </button>
        )}
        <div className="action-spacer" />
        <button
          className="text-button"
          disabled={isBusy}
          onClick={() => onEdit(delivery)}
          aria-label={`Edit ${delivery.orderRef}`}
        >
          Edit
        </button>
        <button
          className="text-button danger"
          disabled={isBusy}
          onClick={() => onRequestDelete(delivery)}
          aria-label={`Hapus ${delivery.orderRef}`}
        >
          Hapus
        </button>
      </div>
    </article>
  );
}
