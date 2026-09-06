import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError, getRequestErrorMessage } from "../api";
import {
  DELIVERY_STATUS_LABELS,
  type Delivery,
  type DeliveryInput,
  type DeliveryStatus,
} from "../types";

interface DeliveryFormProps {
  editingDelivery: Delivery | null;
  isBusy: boolean;
  isSaving: boolean;
  onSave: (deliveryInput: DeliveryInput) => Promise<void>;
  onCancelEdit: () => void;
}

const EMPTY_DELIVERY_INPUT: DeliveryInput = {
  orderRef: "",
  destAddress: "",
  status: "PLANNED",
};

export function DeliveryForm({
  editingDelivery,
  isBusy,
  isSaving,
  onSave,
  onCancelEdit,
}: DeliveryFormProps) {
  const [formValues, setFormValues] = useState<DeliveryInput>({
    ...EMPTY_DELIVERY_INPUT,
  });
  const [formErrorMessage, setFormErrorMessage] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const orderReferenceInputRef = useRef<HTMLInputElement>(null);
  const isEditing = editingDelivery !== null;
  let submitButtonLabel = isEditing
    ? "Simpan perubahan"
    : "+ Tambah pengiriman";
  if (isSaving) {
    submitButtonLabel = "Memproses alamat…";
  }

  useEffect(() => {
    setFormErrorMessage("");
    setFieldErrors({});
    if (!editingDelivery) {
      setFormValues({ ...EMPTY_DELIVERY_INPUT });
      return;
    }
    setFormValues({
      orderRef: editingDelivery.orderRef,
      destAddress: editingDelivery.destAddress,
      status: editingDelivery.status,
    });
    orderReferenceInputRef.current?.focus();
    orderReferenceInputRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "center",
    });
  }, [editingDelivery]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isBusy) {
      return;
    }
    setFormErrorMessage("");
    setFieldErrors({});
    const deliveryInput = {
      ...formValues,
      orderRef: formValues.orderRef.trim(),
      destAddress: formValues.destAddress.trim(),
    };
    try {
      await onSave(deliveryInput);
      setFormValues({ ...EMPTY_DELIVERY_INPUT });
    } catch (error) {
      setFormErrorMessage(getRequestErrorMessage(error));
      if (error instanceof ApiError) {
        setFieldErrors(error.fieldErrors);
      }
    }
  }

  return (
    <aside className="form-panel" aria-labelledby="form-title">
      <div className="panel-heading">
        <span className="panel-number">01</span>
        <h2 id="form-title">
          {isEditing ? "Edit pengiriman" : "Pengiriman baru"}
        </h2>
      </div>
      <form onSubmit={handleSubmit}>
        <fieldset disabled={isBusy}>
          <label htmlFor="order-ref">Referensi pesanan</label>
          <input
            ref={orderReferenceInputRef}
            id="order-ref"
            name="orderRef"
            required
            maxLength={50}
            placeholder="Contoh: DO-2026-001"
            value={formValues.orderRef}
            onChange={(event) =>
              setFormValues({ ...formValues, orderRef: event.target.value })
            }
            aria-invalid={Boolean(fieldErrors.orderRef)}
            aria-describedby={fieldErrors.orderRef ? "order-error" : undefined}
          />
          {fieldErrors.orderRef && (
            <span id="order-error" className="field-error">
              {fieldErrors.orderRef}
            </span>
          )}
          <label htmlFor="address">Alamat tujuan</label>
          <textarea
            id="address"
            name="destAddress"
            required
            maxLength={300}
            rows={4}
            placeholder="Nama jalan, kota, kode pos, Indonesia"
            value={formValues.destAddress}
            onChange={(event) =>
              setFormValues({ ...formValues, destAddress: event.target.value })
            }
            aria-invalid={Boolean(fieldErrors.destAddress)}
            aria-describedby="address-help address-error"
          />
          <span id="address-help" className="field-help">
            Gunakan alamat lokasi publik untuk demonstrasi.
          </span>
          <span id="address-error" className="field-error">
            {fieldErrors.destAddress}
          </span>
          <label htmlFor="delivery-status">Status pengiriman</label>
          <select
            id="delivery-status"
            value={formValues.status}
            onChange={(event) =>
              setFormValues({
                ...formValues,
                status: event.target.value as DeliveryStatus,
              })
            }
          >
            {Object.entries(DELIVERY_STATUS_LABELS).map(
              ([statusValue, statusLabel]) => (
                <option key={statusValue} value={statusValue}>
                  {statusLabel}
                </option>
              ),
            )}
          </select>
          <div className="form-actions">
            <button className="button primary" type="submit">
              {submitButtonLabel}
            </button>
            {isEditing && (
              <button
                className="button secondary"
                type="button"
                onClick={onCancelEdit}
              >
                Batal edit
              </button>
            )}
          </div>
        </fieldset>
        {isSaving && (
          <p className="field-help" role="status">
            Alamat baru dapat memerlukan beberapa detik.
          </p>
        )}
        {formErrorMessage && (
          <p className="error-box" role="alert">
            {formErrorMessage}
          </p>
        )}
      </form>
      <div className="form-note">
        <strong>Zona dihitung otomatis</strong>
        <p>
          Alamat yang sudah dikenali digunakan kembali. Jika koordinat belum
          tersedia, pengiriman tetap tersimpan dengan zona UNKNOWN.
        </p>
      </div>
    </aside>
  );
}
