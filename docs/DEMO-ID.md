# Panduan demo dan penjelasan interview

## Menjalankan dan mendemonstrasikan aplikasi

1. Jalankan Docker Desktop, buka terminal di folder proyek, lalu `docker compose up`.
2. Buka `http://localhost:3000` setelah semua layanan sehat.
3. Buat pengiriman dengan alamat publik seperti Monumen Nasional Jakarta,
   Gedung Sate Bandung, atau Tugu Pahlawan Surabaya. Hasil mengikuti data Nominatim;
   buka **Detail** untuk memeriksa alamat yang benar-benar ditemukan.
4. Buat referensi pesanan lain dengan alamat sama. Sumber di detail menjadi cache,
   sementara timestamp pengambilan tetap sama.
5. Filter zona dan status, edit status, lalu ubah alamat untuk menghitung ulang zona.
6. Coba hapus pengiriman melalui dialog konfirmasi.

Tidak ada data contoh yang diam-diam dianggap sebagai hasil API.

## Simulasi API down

1. Buat satu pengiriman dengan alamat yang berhasil dikenali terlebih dahulu.
2. Salin `.env.example` ke `.env`, lalu ubah `GEOCODING_BASE_URL` ke `http://127.0.0.1:9`.
3. Jalankan `docker compose up -d --force-recreate backend`.
4. Muat ulang board: pengiriman lama tetap tampil karena daftar hanya membaca DB.
5. Tambah pengiriman dengan alamat lama: cache tetap menghasilkan koordinat.
6. Tambah alamat baru: pengiriman tersimpan sebagai UNKNOWN tanpa jarak/biaya/ETA.
7. Kembalikan URL ke `https://nominatim.openstreetmap.org`, recreate backend kembali,
   lalu klik **Coba hitung zona** pada pengiriman UNKNOWN.

Alamat `127.0.0.1:9` menunjuk ke container backend sendiri, hanya untuk simulasi
koneksi gagal. Simulasi tidak membanjiri layanan publik.

## Penjelasan singkat untuk interview

**Mengapa monolith?** Brief mengharapkan satu aplikasi Spring Boot dengan batas
modul yang jelas. Geocoding dipisahkan dalam package agar mudah dijelaskan dan
diekstrak menjadi service nanti, tanpa menambah kompleksitas operasional sekarang.

**Mengapa PostgreSQL?** Relasional, mudah dijalankan dengan Compose, serta mendukung
constraint dan migrasi. Oracle tidak diwajibkan. H2 dipakai untuk tes dan mode lokal.

**Mengapa Haversine?** Brief meminta perhitungan sendiri berdasarkan koordinat.
Hasilnya jarak garis lurus, bukan jarak berkendara. Radius bumi yang dipakai
6.371,0088 km. Tepat 50 km dan 300 km termasuk REGIONAL; klasifikasi sebelum pembulatan.

**Bagaimana API down ditangani?** Periksa cache dulu. Bila belum ada dan API gagal,
delivery tetap tersimpan sebagai UNKNOWN. Timeout dan cooldown menjaga agar
pengguna tidak terus menunggu panggilan yang sedang gagal.

**Bagaimana jika DB down?** Database tetap dibutuhkan. Ketahanan yang diminta
adalah terhadap API geocoding; aplikasi tidak mengklaim mode offline tanpa DB.

**Mengapa lock diperlukan?** Dua alamat baru berbeda tetap bisa melewati cache
bersamaan. Lock global menjaga laju API; pengecekan cache kedua di dalam lock
mencegah panggilan ganda untuk alamat sama. Untuk beberapa instance perlu koordinasi
terpusat karena lock Java ini hanya melindungi satu proses.

**Apakah biaya dan ETA tarif sungguhan?** Tidak. Itu asumsi demonstrasi karena
brief tidak memberikan rumus tarif atau SLA. Angkanya dijelaskan dalam README.

## Sebelum dikumpulkan

- Jalankan `mvn verify`, `npm run build`, dan `docker compose up --build` di komputer sendiri.
- Coba alur browser dan simulasi di atas; periksa batas verifikasi pada `VERIFICATION.md`.
- Sesuaikan User-Agent dengan identitas aplikasi/repository milikmu.
- Pahami kode dan keputusan teknis, serta ikuti aturan bantuan alat dari penyelenggara jika ada.
- Gunakan bundle Git yang disertakan agar riwayat commit bertahap ikut dipublikasikan.
