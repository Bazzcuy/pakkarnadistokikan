# Stok Ikan Giling - Final Code PBO

Project ini dibuat sesuai laporan final **Rancang Bangun Aplikasi Pengelolaan Stok, Produksi, dan Transaksi Ikan Giling Berbasis Desktop dan Android**.

## Isi Folder

1. `desktop-javafx/`  
   Aplikasi desktop JavaFX + SQLite lokal untuk presentasi di laptop.

2. `android-native/`  
   Aplikasi Android Native Java + SQLiteOpenHelper untuk penggunaan HP/offline.

3. `docs/`  
   Skema database, Akun Awal, dan panduan menjalankan project.

## Akun Awal

| Login | Username | Password |
|---|---|---|
| Pengguna | pengguna | pengguna123 |

## Fitur Utama

- Register dan login satu role pengguna
- Profil pengguna/usaha
- Dashboard visual stok mentah, stok giling, penjualan, dan laporan
- Data kelola jenis ikan lengkap dengan gambar produk
- Data supplier dan pelanggan
- Input stok ikan mentah masuk
- Proses produksi ikan giling
- Stok ikan giling per batch
- Transaksi penjualan
- Pembayaran lunas/belum lunas
- Riwayat stok
- Laporan ringkas/detail dengan filter hari, minggu, bulan
- Import dan export Excel pada aplikasi desktop
- Penyimpanan lokal SQLite

## Catatan

Aplikasi Android dan Desktop sengaja dibuat **tidak saling terhubung**, sesuai rancangan laporan. Masing-masing memakai database SQLite lokal pada perangkatnya sendiri.


## Update UI Branding CATOKAN
- Nama aplikasi diganti menjadi **CATOKAN (Catat Stok Ikan)**.
- Android mendapat refresh UI modern bernuansa biru-tosca, lebih nyaman untuk presentasi dan penggunaan harian.
- Ditambahkan aset visual sendiri: logo aplikasi dan banner dashboard/login.
- Desktop JavaFX juga dirapikan tampilannya agar lebih enak dipresentasikan.


## UI Refresh V2 (CATOKAN Premium)
- Android UI diperhalus lagi dengan layout yang lebih terasa seperti aplikasi komersial: hero banner, branding card, role chip, quick stats, menu card, dan form yang lebih lega.
- Ditambahkan resource drawable untuk background, button, chip, input, dan stat card supaya tampilan lebih konsisten dan rapih.
- Nama branding dipusatkan ke **CATOKAN**.
- Tetap offline dan ringan, tapi tampilannya sekarang jauh lebih presentable.

## Audit Tambahan

- Audit desktop ditambahkan di `docs/AUDIT_DESKTOP.md`.
- Panduan distribusi desktop ada di `docs/DESKTOP_DISTRIBUTION.md`.
- Panduan build APK Android via GitHub Actions ada di `docs/ANDROID_GITHUB_APK.md`.


## Final UI Polish v4
- Android sekarang memakai multi-activity: Splash, Login, Dashboard, Input Stok, Produksi, Penjualan, dan Data/Text viewer.
- Layout utama Android dipindah ke XML agar lebih proper dan mudah diedit di Android Studio.
- UI tetap tidak overkill dari sisi sistem, tapi tampilannya dibuat lebih premium untuk presentasi.
- Catatan detail ada di `docs/UI_FINAL_POLISH.md`.

- Audit struktur final ada di `docs/CODE_AUDIT_FINAL.md`.

## Finalisasi Sistem
- data awal diperbanyak agar terlihat seperti simulasi usaha kecil yang berjalan.
- Setelah login, semua fitur utama tersedia dalam satu dashboard pengguna agar aktivitas lebih ringkas dan tidak bercampur role.
- Password akun awal disimpan dalam bentuk hash SHA-256 pada database baru.
- Logika stok, produksi, penjualan, pembayaran, dan riwayat stok dibuat transactional.
- Android siap dipush ke GitHub dan menghasilkan APK debug lewat workflow Actions.
- Desktop mendukung export laporan Excel, template import Excel, dan import stok masuk dari Excel.
- Icon Android diganti ke vector drawable agar tidak bergantung pada emoji/font perangkat.
- Logo CATOKAN diproses dengan alpha rounded corner agar tidak muncul sudut kotak tajam.
