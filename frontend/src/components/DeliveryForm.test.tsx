import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "../api";
import { DeliveryForm } from "./DeliveryForm";

afterEach(cleanup);

describe("delivery form submission", () => {
  it("submits trimmed values and clears the form after a successful save", async () => {
    const saveDelivery = vi.fn().mockResolvedValue(undefined);
    render(
      <DeliveryForm
        editingDelivery={null}
        isBusy={false}
        isSaving={false}
        onSave={saveDelivery}
        onCancelEdit={vi.fn()}
      />,
    );

    const orderReferenceInput = screen.getByLabelText(
      "Referensi pesanan",
    ) as HTMLInputElement;
    const destinationInput = screen.getByLabelText(
      "Alamat tujuan",
    ) as HTMLTextAreaElement;
    fireEvent.change(orderReferenceInput, { target: { value: " DO-001 " } });
    fireEvent.change(destinationInput, {
      target: { value: " Monas Jakarta " },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "+ Tambah pengiriman" }),
    );

    await waitFor(() =>
      expect(saveDelivery).toHaveBeenCalledWith({
        orderRef: "DO-001",
        destAddress: "Monas Jakarta",
        status: "PLANNED",
      }),
    );
    await waitFor(() => expect(orderReferenceInput.value).toBe(""));
    expect(destinationInput.value).toBe("");
  });

  it("preserves input and displays field errors when the server rejects a save", async () => {
    const saveDelivery = vi.fn().mockRejectedValue(
      new ApiError("Periksa isian formulir", {
        orderRef: "Referensi pesanan tidak valid",
      }),
    );
    render(
      <DeliveryForm
        editingDelivery={null}
        isBusy={false}
        isSaving={false}
        onSave={saveDelivery}
        onCancelEdit={vi.fn()}
      />,
    );

    const orderReferenceInput = screen.getByLabelText(
      "Referensi pesanan",
    ) as HTMLInputElement;
    const destinationInput = screen.getByLabelText(
      "Alamat tujuan",
    ) as HTMLTextAreaElement;
    fireEvent.change(orderReferenceInput, { target: { value: "DO-001" } });
    fireEvent.change(destinationInput, { target: { value: "Monas Jakarta" } });
    fireEvent.click(
      screen.getByRole("button", { name: "+ Tambah pengiriman" }),
    );

    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      "Periksa isian formulir",
    );
    expect(screen.getByText("Referensi pesanan tidak valid")).toBeTruthy();
    expect(orderReferenceInput.value).toBe("DO-001");
    expect(destinationInput.value).toBe("Monas Jakarta");
  });
});
