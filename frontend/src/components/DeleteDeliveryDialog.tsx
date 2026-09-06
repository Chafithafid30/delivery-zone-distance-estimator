import { useEffect, useRef } from "react";
import type { Delivery } from "../types";

interface DeleteDeliveryDialogProps {
  delivery: Delivery | null;
  isDeleting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

export function DeleteDeliveryDialog({
  delivery,
  isDeleting,
  onCancel,
  onConfirm,
}: DeleteDeliveryDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }
    if (delivery && !dialog.open) {
      dialog.showModal();
    } else if (!delivery) {
      dialog.close();
    }
  }, [delivery]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="delete-title"
      onCancel={(event) => {
        if (isDeleting) {
          event.preventDefault();
          return;
        }
        onCancel();
      }}
    >
      <h2 id="delete-title">Hapus pengiriman?</h2>
      <p>
        <strong>{delivery?.orderRef}</strong> akan dihapus dari daftar. Tindakan
        ini tidak dapat dibatalkan.
      </p>
      <div className="dialog-actions">
        <button
          className="button secondary"
          disabled={isDeleting}
          onClick={onCancel}
        >
          Batal
        </button>
        <button
          className="button destructive"
          disabled={isDeleting}
          onClick={onConfirm}
        >
          {isDeleting ? "Menghapus…" : "Hapus pengiriman"}
        </button>
      </div>
    </dialog>
  );
}
