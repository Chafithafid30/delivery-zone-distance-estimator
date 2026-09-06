# Panduan penamaan dan desain kode

Tujuan refactor ini adalah membuat alur aplikasi mudah dibaca, dijelaskan, dan
diubah. Clean Code dan SOLID digunakan sesuai kebutuhan kasus B-002, dengan
pendekatan KISS agar setiap pemisahan punya alasan yang jelas.

## Penamaan

Nama memakai istilah bisnis dalam bahasa Inggris secara konsisten. Method yang
melakukan tindakan memakai kata kerja. Variabel menjelaskan data yang disimpan;
satuan seperti kilometer atau nanodetik ditulis jika memengaruhi perhitungan.

| Maksud | Contoh dalam kode |
| --- | --- |
| Membuat pengiriman | `createDelivery` |
| Mengambil pengiriman atau menghasilkan 404 | `findDeliveryOrThrow` |
| Menyelesaikan alamat dengan cache/provider | `resolveAddress` |
| Memanggil pencarian alamat pada provider | `lookupAddress` |
| Menghitung jarak kilometer | `calculateDistanceKm` |
| Alamat sebagai kunci cache | `normalizedAddress` |
| Jarak sebelum pembulatan | `calculatedDistanceKm` |
| Titik koordinat pabrik | `factoryCoordinates` |
| Status proses UI | `isLoading`, `isSaving`, `isBusy` |
| Interaksi pengguna | `handleSaveDelivery`, `handleRetryGeocoding` |

Nama properti JSON seperti `orderRef`, `destAddress`, dan `destLat` mengikuti
kontrak API yang sudah ada. Di domain Java, nama lengkap seperti
`orderReference` dan `destinationLatitude` memperjelas maknanya. DTO menjadi
tempat pemetaan sehingga kontrak API dan kolom database tetap kompatibel.

## Pembagian tanggung jawab

| Bagian | Tanggung jawab |
| --- | --- |
| `DeliveryController` | Menerima HTTP, validasi request, status dan Location header |
| `DeliveryService` | Mengatur urutan proses CRUD, lookup, dan penyimpanan |
| `Delivery` | Menjaga data dan aturan perubahan satu pengiriman |
| `AddressResolver` | Kontrak penyelesaian alamat yang dipakai use case pengiriman |
| `RemoteAddressResolver` | Baca cache bersama, panggil worker HTTP, dan fallback ketika worker gagal |
| `InternalGeocodingController` | Validasi alamat untuk layanan geocoder internal |
| `GeocodingService` | Alur cache, lookup, dan hasil saat provider tidak tersedia |
| `GeocodingRequestPolicy` | Jeda antar-request dan cooldown setelah kegagalan |
| `NominatimClient` | Menyusun request HTTP dan membaca respons provider |
| `Haversine` / `Zone` | Perhitungan jarak dan aturan batas zona |
| `App` | Menghubungkan komponen dan mengatur aksi pada halaman |
| `useDeliveries` | Memuat data, membatalkan request lama, dan refresh daftar |
| `DeliveryForm` | Isian form, hasil validasi, dan submit |
| `DeliveryBoard` / `DeliveryCard` | Daftar, filter, dan tampilan per pengiriman |
| `ZoneDetails` / `DeleteDeliveryDialog` | Detail koordinat dan konfirmasi hapus |
| `api.ts` / `formatters.ts` | Akses HTTP dan format nilai untuk tampilan |

Field entity `Delivery` bersifat private. Perubahan dilakukan lewat method
seperti `updateDetails` dan `updateDestination`, sehingga penggantian alamat,
koordinat, jarak, dan zona bisa dibaca sebagai satu proses domain yang konsisten.
Entity melakukan perhitungan murni; pemanggilan jaringan tetap di layanan.

## Penerapan SOLID

- **Single Responsibility:** cache, aturan jeda request, adapter HTTP, dan UI
  dipisahkan berdasarkan alasan perubahannya. Perubahan cooldown cukup berfokus
  pada `GeocodingRequestPolicy`.
- **Open/Closed:** `GeocodingProvider` menjadi titik perluasan adapter pencarian
  alamat. Jika menambah penyedia sungguhan, konfigurasi dan metadata sumber hasil
  juga harus disesuaikan dengan penyedia tersebut.
- **Liskov Substitution:** adapter mengikuti kontrak yang sama: hasil ditemukan
  menghasilkan `Place`, alamat tidak ditemukan menghasilkan `Optional.empty()`,
  dan kegagalan provider menghasilkan `ProviderUnavailableException`.
- **Interface Segregation:** `GeocodingProvider` memiliki satu operasi pencarian
  alamat. Pemakainya hanya bergantung pada kemampuan yang dibutuhkan.
- **Dependency Inversion:** layanan menerima repository dan provider melalui
  constructor. Adapter HTTP dapat diganti dengan mock saat pengujian.

Untuk Swarm, `DeliveryService` bergantung pada `AddressResolver`, dengan dua
implementasi: lokal dan remote. Pemilihan dilakukan lewat konfigurasi Spring,
sehingga aturan CRUD, jarak, dan zona tidak perlu mengetahui lokasi geocoder.
Antarmuka ini memiliki satu operasi dengan hasil `GeocodeResult` yang sama.
Satu image Java digunakan ulang untuk kedua peran, sehingga tidak ada duplikasi
kode bisnis atau kebutuhan repository microservice baru untuk demo ini.

## Penerapan KISS

- Gunakan guard clause untuk hasil cache, cooldown, data kosong, dan input gagal.
- Simpan aturan zona dalam satu method dan estimasi tarif dalam satu switch.
- Gunakan constructor injection bawaan Spring dan state/hooks bawaan React.
- Pisahkan komponen saat tanggung jawabnya berbeda; pertahankan pemanggilan
  fungsi biasa untuk alur yang sederhana.
- Komentar menjelaskan alasan penting, misalnya zona ditentukan sebelum
  pembulatan dan cache diperiksa lagi setelah mendapatkan lock.
- Pertahankan perilaku yang sudah diuji selama perapian struktur dan nama.

## Pengujian yang menjaga perilaku

Jalankan `mvn verify` dari folder backend dan `npm test` serta `npm run build`
dari folder frontend. Tes memeriksa hasil yang dibutuhkan pengguna: zona benar,
cache tetap tersedia saat API down, input form tetap ada ketika simpan gagal,
dan respons lama tidak menimpa hasil filter terbaru.

Rincian hasil dan batas verifikasi ada pada `VERIFICATION.md`.
