# Menjalankan Delivery Zone

Panduan ini menggunakan **Docker Desktop dan Git Bash di Windows**. Seluruh aplikasi—frontend, backend, dan database—dijalankan dengan Docker Compose. Jadi tidak perlu menginstal Java, Maven, atau Node.js secara terpisah.

Proyek ini menggunakan **PostgreSQL** sebagai relational database dan **Nominatim (OpenStreetMap)** sebagai external API untuk melakukan forward geocoding alamat. Nominatim tidak memerlukan API key.

Hasil geocoding disimpan pada database melalui cache sehingga alamat yang sudah pernah berhasil dicari dapat digunakan kembali tanpa melakukan request API berulang. Jika layanan geocoding sedang tidak tersedia dan koordinat tidak ditemukan di cache, delivery tetap dapat disimpan dengan zona `UNKNOWN`.

## 1. Siapkan Docker Desktop

Buka Docker Desktop dan tunggu sampai Engine berjalan. Pastikan Docker menggunakan **Linux containers**.

Buka Git Bash, lalu jalankan:

```bash
docker version
docker compose version
```

Pastikan `docker version` menampilkan bagian **Client dan Server**. Jika muncul error `dockerDesktopLinuxEngine`, tunggu Docker Desktop siap atau restart Docker Desktop sebelum melanjutkan.

Koneksi internet diperlukan untuk mengunduh image dan dependency saat pertama kali menjalankan proyek, serta untuk mencari koordinat alamat baru. Tidak diperlukan API key.

## 2. Buka folder proyek

Jika mengambil source dari GitHub, salin URL HTTPS repository melalui tombol **Code**, lalu gunakan URL tersebut pada perintah berikut. Ganti `URL_REPOSITORY` dengan URL sebenarnya:

```bash
git clone URL_REPOSITORY delivery-zone
cd delivery-zone
```

Jika sudah mengunduh dan mengekstrak ZIP, cukup buka Git Bash di folder proyek. Contoh:

```bash
cd /c/Projects/delivery-zone
```

Sesuaikan path dengan lokasi foldermu. Jalankan `ls` dan pastikan ada `docker-compose.yml`, `backend`, dan `frontend`.

**Semua perintah berikut dijalankan dari folder yang memuat `docker-compose.yml`.**

## 3. Jalankan aplikasi

```bash
docker compose up
```

Konfigurasi default sudah tersedia sehingga tidak perlu membuat `.env`. Build pertama dapat memerlukan beberapa menit untuk mengunduh dependency dan membangun image. Biarkan terminal ini tetap terbuka selama aplikasi digunakan.

Jika ingin aplikasi berjalan di background, gunakan perintah berikut sebagai alternatif:

```bash
docker compose up -d
```

Untuk memeriksa layanan, buka Git Bash kedua di folder proyek yang sama:

```bash
docker compose ps
```

Pastikan `db`, `backend`, dan `frontend` berjalan. Database dan backend memiliki health check; tunggu keduanya sehat sebelum membuka aplikasi.

## 4. Buka aplikasi

Buka **[http://localhost:3000](http://localhost:3000)** di browser. Aplikasi langsung menampilkan papan pengiriman tanpa halaman login.

Untuk mencoba, isi referensi pesanan, misalnya `DO-2026-001`, dan alamat tujuan publik, misalnya `Monumen Nasional, Jakarta, Indonesia`. Pilih status, lalu klik tombol simpan di bagian bawah form.

Alamat layanan pada konfigurasi default:

| Layanan | Alamat |
| --- | --- |
| Halaman aplikasi | http://localhost:3000 |
| API pengiriman | http://localhost:8080/api/deliveries |
| Health check backend | http://localhost:8080/actuator/health |

Jika pencarian alamat gagal, pengiriman dapat tersimpan sebagai `UNKNOWN`. Periksa koneksi internet dan log backend, kemudian gunakan tombol retry setelah layanan geocoding tersedia kembali.

## 5. Hentikan atau jalankan kembali

Untuk menghentikan aplikasi, jalankan dari terminal lain di folder proyek:

```bash
docker compose down
```

Data tetap tersimpan di volume database. Jangan menambahkan opsi `-v` jika ingin mempertahankan data.

Untuk menjalankan kembali, gunakan folder proyek yang sama:

```bash
docker compose up -d
```

Jika source code berubah dan image perlu dibangun ulang:

```bash
docker compose up -d --build
```

## Jika aplikasi belum bisa dibuka

### Periksa log

```bash
docker compose logs --tail 100 backend
docker compose logs --tail 100 frontend
docker compose logs --tail 100 db
```

Jika muncul `no configuration file provided`, pastikan terminal berada di folder yang memuat `docker-compose.yml`.

### Jika port sudah digunakan aplikasi lain

Port default proyek adalah **3000, 8080, dan 5432**. Untuk memakai port alternatif, jalankan di Git Bash:

```bash
export WEB_PORT=3001
export API_PORT=8081
export DB_PORT=5433
docker compose up -d
```

Buka **http://localhost:3001**. API backend kini di port `8081` dan database di port `5433`. Nilai `export` berlaku pada terminal tersebut; jalankan lagi jika diperlukan dari terminal baru.

## Tambahan

Repository juga menyediakan konfigurasi Docker Swarm sebagai eksplorasi tambahan.

### Jika Delivery Zone sebelumnya dijalankan dengan Swarm

Jika ingin beralih ke Compose, hentikan stack Swarm terlebih dahulu agar port tidak bentrok:

```bash
docker stack rm deliveryzone
```

Tunggu task lama berhenti, kemudian jalankan `docker compose up`. Volume Swarm tetap tersimpan, tetapi **Compose menggunakan volume database berbeda**, sehingga data Swarm tidak otomatis muncul di Compose.

Jika ingin tetap menjalankan versi Swarm, ikuti [panduan Docker Swarm](docs/SWARM-ID.md). Docker Compose sudah cukup untuk menjalankan proyek sesuai brief.
