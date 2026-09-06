import { useEffect, useRef, useState, type FormEvent } from 'react';
import { api, ApiError } from './api';
import { statusLabels, zones, type Delivery, type DeliveryInput, type DeliveryStatus, type Zone } from './types';

const emptyForm: DeliveryInput = { orderRef: '', destAddress: '', status: 'PLANNED' };
const money = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 });
const number = new Intl.NumberFormat('id-ID', { maximumFractionDigits: 2 });
const dateTime = (value: string) => new Date(value).toLocaleString('id-ID', { dateStyle: 'medium', timeStyle: 'short' });
const errorMessage = (error: unknown) => error instanceof ApiError ? error.message : 'Tidak dapat terhubung ke server. Periksa koneksi lalu coba kembali.';
const isAbort = (error: unknown) => error instanceof DOMException && error.name === 'AbortError';

function ZoneDetails({ delivery }: { delivery: Delivery }) {
  return <details className="zone-details">
    <summary><span className={`zone zone-${delivery.zone.toLowerCase()}`}>{delivery.zone}</span><span className="detail-label">Detail</span></summary>
    <div className="coordinates">
      {delivery.destLat !== null && delivery.destLng !== null ? <>
        <strong>{delivery.resolvedAddress}</strong>
        <span>{delivery.destLat.toFixed(6)}, {delivery.destLng.toFixed(6)}</span>
        <span>Diambil: {delivery.geocodedAt ? dateTime(delivery.geocodedAt) : '—'}</span>
        <span>Sumber saat diproses: {delivery.geocodeSource === 'CACHE' ? 'Cache alamat' : 'Nominatim'}</span>
      </> : <span>{delivery.geocodeSource === 'NOT_FOUND' ? 'Alamat belum ditemukan. Tambahkan kota atau kode pos, lalu simpan kembali.' : 'Koordinat belum tersedia. Coba lagi saat layanan alamat kembali tersedia.'}</span>}
    </div>
  </details>;
}

export default function App() {
  const [deliveries, setDeliveries] = useState<Delivery[]>([]);
  const [zone, setZone] = useState<Zone | ''>('');
  const [status, setStatus] = useState<DeliveryStatus | ''>('');
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [form, setForm] = useState<DeliveryInput>({ ...emptyForm });
  const [editing, setEditing] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState('');
  const [actionError, setActionError] = useState('');
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Delivery | null>(null);
  const [factory, setFactory] = useState<{ latitude: number; longitude: number } | null>(null);
  const orderRefInput = useRef<HTMLInputElement>(null);
  const deleteDialog = useRef<HTMLDialogElement>(null);
  const busy = saving || pendingId !== null;

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setListError('');
    api.list(zone, status, controller.signal).then(setDeliveries).catch(error => {
      if (!isAbort(error)) setListError(errorMessage(error));
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [zone, status, refresh]);

  useEffect(() => {
    const controller = new AbortController();
    api.config(controller.signal).then(result => setFactory(result.factory)).catch(() => {});
    return () => controller.abort();
  }, [refresh]);

  useEffect(() => {
    if (deleteTarget) deleteDialog.current?.showModal();
    else deleteDialog.current?.close();
  }, [deleteTarget]);

  function resetForm() {
    setEditing(null);
    setForm({ ...emptyForm });
    setFormError('');
    setFieldErrors({});
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setSaving(true); setFormError(''); setFieldErrors({}); setNotice(''); setActionError('');
    try {
      const input = { ...form, orderRef: form.orderRef.trim(), destAddress: form.destAddress.trim() };
      const result = editing === null ? await api.create(input) : await api.update(editing, input);
      setNotice(`${result.orderRef} berhasil disimpan.${result.zone === 'UNKNOWN' ? ' Zona belum diketahui; pengiriman tetap tersimpan.' : ''}${zone || status ? ' Filter aktif dapat menyembunyikan pengiriman ini.' : ''}`);
      resetForm();
      setRefresh(value => value + 1);
    } catch (error) {
      setFormError(errorMessage(error));
      if (error instanceof ApiError) setFieldErrors(error.fields);
    } finally { setSaving(false); }
  }

  function edit(delivery: Delivery) {
    setEditing(delivery.id);
    setForm({ orderRef: delivery.orderRef, destAddress: delivery.destAddress, status: delivery.status });
    setFormError(''); setFieldErrors({});
    orderRefInput.current?.focus();
    orderRefInput.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  async function remove() {
    if (!deleteTarget || busy) return;
    const target = deleteTarget;
    setPendingId(target.id); setActionError(''); setNotice('');
    try {
      await api.remove(target.id);
      if (editing === target.id) resetForm();
      setDeleteTarget(null);
      setNotice(`${target.orderRef} berhasil dihapus.`);
      setRefresh(value => value + 1);
    } catch (error) { setActionError(errorMessage(error)); setDeleteTarget(null); }
    finally { setPendingId(null); }
  }

  async function retry(delivery: Delivery) {
    if (busy) return;
    setPendingId(delivery.id); setActionError(''); setNotice('');
    try {
      const result = await api.retry(delivery.id);
      setNotice(result.zone === 'UNKNOWN' ? `${result.orderRef}: koordinat masih belum tersedia. Coba kembali nanti atau perjelas alamat.` : `${result.orderRef}: zona berhasil diperbarui menjadi ${result.zone}.`);
      setRefresh(value => value + 1);
    } catch (error) { setActionError(errorMessage(error)); }
    finally { setPendingId(null); }
  }

  return <div className="app-shell">
    <header className="topbar"><a className="brand" href="#main"><span className="brand-mark" aria-hidden="true">DZ</span>Delivery Zone</a><span className="topbar-label">OPERASIONAL PENGIRIMAN</span></header>
    <main id="main">
      <div className="page-heading"><div><p className="eyebrow">DISPATCH / JAKARTA</p><h1>Papan pengiriman</h1></div><div className="origin"><span>Titik keberangkatan</span><strong>Pabrik Jakarta</strong><span className="mono">{factory ? `${factory.latitude.toFixed(4)}, ${factory.longitude.toFixed(4)}` : 'Koordinat belum tersedia'}</span></div></div>
      <div className="zone-legend" aria-label="Batas zona pengiriman"><div><span className="legend-line local" /><strong>LOCAL</strong><span>&lt; 50 km</span></div><div><span className="legend-line regional" /><strong>REGIONAL</strong><span>50–300 km</span></div><div><span className="legend-line long-haul" /><strong>LONG_HAUL</strong><span>&gt; 300 km</span></div><p>Jarak garis lurus dari pabrik</p></div>
      {notice && <div className="notice" role="status">{notice}<button className="dismiss" aria-label="Tutup pemberitahuan" onClick={() => setNotice('')}>×</button></div>}
      {actionError && <div className="error-box" role="alert">{actionError}</div>}
      <div className="workspace">
        <aside className="form-panel" aria-labelledby="form-title">
          <div className="panel-heading"><span className="panel-number">01</span><h2 id="form-title">{editing === null ? 'Pengiriman baru' : 'Edit pengiriman'}</h2></div>
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              <label htmlFor="order-ref">Referensi pesanan</label>
              <input ref={orderRefInput} id="order-ref" name="orderRef" required maxLength={50} placeholder="Contoh: DO-2026-001" value={form.orderRef} onChange={e => setForm({ ...form, orderRef: e.target.value })} aria-invalid={!!fieldErrors.orderRef} aria-describedby={fieldErrors.orderRef ? 'order-error' : undefined} />
              {fieldErrors.orderRef && <span id="order-error" className="field-error">{fieldErrors.orderRef}</span>}
              <label htmlFor="address">Alamat tujuan</label>
              <textarea id="address" name="destAddress" required maxLength={300} rows={4} placeholder="Nama jalan, kota, kode pos, Indonesia" value={form.destAddress} onChange={e => setForm({ ...form, destAddress: e.target.value })} aria-invalid={!!fieldErrors.destAddress} aria-describedby="address-help address-error" />
              <span id="address-help" className="field-help">Gunakan alamat lokasi publik untuk demonstrasi.</span>
              <span id="address-error" className="field-error">{fieldErrors.destAddress}</span>
              <label htmlFor="delivery-status">Status pengiriman</label>
              <select id="delivery-status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value as DeliveryStatus })}>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
              <div className="form-actions"><button className="button primary" type="submit">{saving ? 'Memproses alamat…' : editing === null ? '+ Tambah pengiriman' : 'Simpan perubahan'}</button>{editing !== null && <button className="button secondary" type="button" onClick={resetForm}>Batal edit</button>}</div>
            </fieldset>
            {saving && <p className="field-help" role="status">Alamat baru dapat memerlukan beberapa detik.</p>}
            {formError && <p className="error-box" role="alert">{formError}</p>}
          </form>
          <div className="form-note"><strong>Zona dihitung otomatis</strong><p>Alamat yang sudah dikenali digunakan kembali. Jika koordinat belum tersedia, pengiriman tetap tersimpan dengan zona UNKNOWN.</p></div>
        </aside>
        <section className="board-panel" aria-labelledby="board-title" aria-busy={loading}>
          <div className="board-heading"><div className="panel-heading"><span className="panel-number">02</span><h2 id="board-title">Daftar pengiriman</h2></div><button className="button secondary compact" disabled={loading} onClick={() => setRefresh(value => value + 1)}>Muat ulang</button></div>
          <div className="filters"><label>Zona<select value={zone} onChange={e => setZone(e.target.value as Zone | '')}><option value="">Semua zona</option>{zones.map(value => <option key={value}>{value}</option>)}</select></label><label>Status<select value={status} onChange={e => setStatus(e.target.value as DeliveryStatus | '')}><option value="">Semua status</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>{!loading && !listError && <span className="result-count">{deliveries.length} pengiriman</span>}</div>
          {loading ? <div className="empty-state" role="status"><span className="spinner" />Memuat pengiriman…</div>
            : listError ? <div className="empty-state"><strong>Daftar belum dapat dimuat</strong><p role="alert">{listError}</p><button className="button secondary" onClick={() => setRefresh(value => value + 1)}>Coba lagi</button></div>
            : deliveries.length === 0 ? <div className="empty-state"><span className="empty-symbol" aria-hidden="true">＋</span><h3>{zone || status ? 'Tidak ada hasil untuk filter ini' : 'Pengiriman pertama dimulai di sini'}</h3><p>{zone || status ? 'Ubah filter untuk melihat pengiriman lainnya.' : 'Isi referensi pesanan dan alamat tujuan untuk menghitung zona pengiriman.'}</p>{(zone || status) && <button className="button secondary" onClick={() => { setZone(''); setStatus(''); }}>Hapus filter</button>}</div>
            : <div className="delivery-list">{deliveries.map(delivery => <article className="delivery-card" key={delivery.id}>
              <div className="delivery-top"><div><h3>{delivery.orderRef}</h3><span className="created">{dateTime(delivery.createdAt)}</span></div><span className={`status status-${delivery.status.toLowerCase()}`}>{statusLabels[delivery.status]}</span></div>
              <p className="destination">{delivery.destAddress}</p>
              <div className="delivery-metrics"><div><span className="metric-label">Zona tujuan</span><ZoneDetails delivery={delivery} /></div><div><span className="metric-label">Jarak</span><strong>{delivery.distanceKm === null ? '—' : `${number.format(delivery.distanceKm)} km`}</strong></div><div><span className="metric-label">Estimasi biaya</span><strong>{delivery.estimate ? money.format(delivery.estimate.costIdr) : '—'}</strong></div><div><span className="metric-label">Estimasi tiba</span><strong>{delivery.estimate ? `${delivery.estimate.minDays}${delivery.estimate.minDays !== delivery.estimate.maxDays ? `–${delivery.estimate.maxDays}` : ''} hari` : '—'}</strong></div></div>
              <div className="delivery-actions">{delivery.zone === 'UNKNOWN' && <button className="text-button retry" disabled={busy} onClick={() => retry(delivery)}>{pendingId === delivery.id ? 'Memproses…' : 'Coba hitung zona'}</button>}<div className="action-spacer" /><button className="text-button" disabled={busy} onClick={() => edit(delivery)} aria-label={`Edit ${delivery.orderRef}`}>Edit</button><button className="text-button danger" disabled={busy} onClick={() => setDeleteTarget(delivery)} aria-label={`Hapus ${delivery.orderRef}`}>Hapus</button></div>
            </article>)}</div>}
          <p className="board-footnote">Biaya dan waktu tiba adalah simulasi per zona. Jarak bukan jarak rute jalan; zona ditentukan sebelum pembulatan jarak.</p>
        </section>
      </div>
    </main>
    <footer><span>Delivery Zone · Case B-002</span><span>Data lokasi © <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap contributors</a> · <a href="https://operations.osmfoundation.org/policies/nominatim/" target="_blank" rel="noreferrer">Kebijakan Nominatim</a></span></footer>
    <dialog ref={deleteDialog} onCancel={event => { if (pendingId !== null) event.preventDefault(); else setDeleteTarget(null); }} aria-labelledby="delete-title"><h2 id="delete-title">Hapus pengiriman?</h2><p><strong>{deleteTarget?.orderRef}</strong> akan dihapus dari daftar. Tindakan ini tidak dapat dibatalkan.</p><div className="dialog-actions"><button className="button secondary" disabled={pendingId !== null} onClick={() => setDeleteTarget(null)}>Batal</button><button className="button destructive" disabled={pendingId !== null} onClick={remove}>{pendingId !== null ? 'Menghapus…' : 'Hapus pengiriman'}</button></div></dialog>
  </div>;
}
