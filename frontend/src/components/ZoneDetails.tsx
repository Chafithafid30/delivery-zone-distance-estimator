import { formatDateTime } from "../formatters";
import type { Delivery } from "../types";

interface ZoneDetailsProps {
  delivery: Delivery;
}

export function ZoneDetails({ delivery }: ZoneDetailsProps) {
  const unresolvedMessage =
    delivery.geocodeSource === "NOT_FOUND"
      ? "Alamat belum ditemukan. Tambahkan kota atau kode pos, lalu simpan kembali."
      : "Koordinat belum tersedia. Coba lagi saat layanan alamat kembali tersedia.";

  return (
    <details className="zone-details">
      <summary>
        <span className={`zone zone-${delivery.zone.toLowerCase()}`}>
          {delivery.zone}
        </span>
        <span className="detail-label">Detail</span>
      </summary>
      <div className="coordinates">
        {delivery.destLat !== null && delivery.destLng !== null ? (
          <>
            <strong>{delivery.resolvedAddress}</strong>
            <span>
              {delivery.destLat.toFixed(6)}, {delivery.destLng.toFixed(6)}
            </span>
            <span>
              Diambil:{" "}
              {delivery.geocodedAt ? formatDateTime(delivery.geocodedAt) : "—"}
            </span>
            <span>
              Sumber saat diproses:{" "}
              {delivery.geocodeSource === "CACHE"
                ? "Cache alamat"
                : "Nominatim"}
            </span>
          </>
        ) : (
          <span>{unresolvedMessage}</span>
        )}
      </div>
    </details>
  );
}
