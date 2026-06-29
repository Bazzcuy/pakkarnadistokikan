# Audit Final Struktur CATOKAN

CATOKAN adalah aplikasi PBO Java untuk pencatatan stok ikan giling. Project sengaja dipisah menjadi dua target:

- `desktop-javafx`: aplikasi desktop JavaFX + SQLite lokal untuk presentasi PBO.
- `android-native`: aplikasi Android Java native + SQLite lokal untuk presentasi mobile/offline.

Keduanya tidak saling terhubung jaringan karena rancangan sistem memakai database lokal masing-masing.

## Penerapan PBO

Struktur kode sudah dipisah sesuai tanggung jawab:

- `model`: representasi object sederhana seperti `User` dan `OptionItem`.
- `service`: logika bisnis, validasi, transaksi stok, produksi, penjualan, pembayaran, dan laporan.
- `db`: helper database SQLite, pembuatan tabel, dan seed awal.
- `util`: helper tanggal dan hashing password.
- UI: JavaFX untuk desktop, Activity XML untuk Android.

Pemisahan ini membuat kode mudah dijelaskan sebagai penerapan encapsulation, object modeling, dan separation of concerns.

## Logika Sistem

Alur utama sudah dijaga:

1. Stok mentah masuk menambah `stok_mentah`, mencatat `stok_masuk`, dan menulis `riwayat_stok`.
2. Produksi giling mengurangi `stok_mentah`, menambah batch `stok_giling`, mencatat `produksi_giling`, dan menulis riwayat stok.
3. Penjualan mengambil batch giling tertentu, mengurangi stok batch, membuat header/detail penjualan, mencatat pembayaran awal, dan menulis riwayat stok.
4. Pembayaran lanjutan menolak pembayaran yang melebihi sisa tagihan.
5. Dashboard menghitung total stok, total penjualan, dan sisa belum lunas berdasarkan total transaksi dikurangi pembayaran.

## Validasi Penting

- Berat dan harga wajib lebih dari 0.
- Berat hasil produksi tidak boleh melebihi berat mentah.
- Produksi ditolak jika stok mentah tidak cukup.
- Penjualan ditolak jika stok giling tidak cukup.
- Pembayaran tidak boleh negatif dan tidak boleh melebihi total/sisa tagihan.
- Sistem memakai satu login pengguna agar alur presentasi dan operasional tidak terpecah ke banyak role.
- Setelah login, semua fitur utama tersedia dalam satu dashboard pengguna dengan navigasi terpisah per aktivitas.

## Database

SQLite memakai tabel utama:

- `users`
- `jenis_ikan`
- `suppliers`
- `pelanggan`
- `stok_mentah`
- `stok_giling`
- `stok_masuk`
- `produksi_giling`
- `penjualan`
- `detail_penjualan`
- `pembayaran`
- `riwayat_stok`

Desktop juga memiliki `penyesuaian_stok` untuk pengembangan lanjutan.

## Keamanan presentasi

Password akun awal tetap mudah dipakai untuk presentasi, tetapi database baru menyimpan password dalam bentuk hash SHA-256. Login masih kompatibel dengan database lama yang mungkin sudah berisi password plaintext agar tidak merusak presentasi sebelumnya.

## data awal

data awal sudah diperbanyak:

- 8 jenis ikan.
- Gambar produk ikan pada Data kelola jenis ikan.
- 10 supplier.
- 12 pelanggan.
- Stok mentah bervariasi.
- 10 batch ikan giling.
- Data stok masuk, produksi, penjualan, pembayaran lunas, pembayaran tempo, dan riwayat stok.

Detail ada di `docs/DATA_awal.md`.

## Android

Status: siap dipush ke GitHub.

- Entry point Android adalah `SplashActivity`.
- Flow final memakai `OnboardingActivity`, `LoginActivity`, `DashboardActivity`, `RawStockActivity`, `ProductionActivity`, `SalesActivity`, dan `TextActivity`.
- `MainActivity` lama sudah dihapus agar struktur tidak membingungkan.
- Workflow `.github/workflows/android-debug-apk.yml` membuat APK debug otomatis sebagai artifact.

## Desktop

Status: aplikasi desktop JavaFX utama untuk presentasi PBO.

- UI desktop memakai branding CATOKAN, sidebar, form, card dashboard, tabel data, gambar produk, dan filter laporan.
- Service desktop memakai transaksi database dengan rollback eksplisit.
- Menu laporan desktop mendukung export/import Excel melalui `ExcelService` agar logika file tidak bercampur dengan UI.
- Panduan run dan packaging tersedia di `docs/DESKTOP_DISTRIBUTION.md`.

## Catatan Batasan

Project ini dibuat untuk tugas PBO/offline, bukan sistem produksi multi-user. Karena itu belum memakai server, API, sinkronisasi cloud, atau multi-device realtime. Batasan ini konsisten dengan scope tugas dan justru membuat sistem lebih mudah dipresentasikan.
