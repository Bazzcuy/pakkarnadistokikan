# CATOKAN Android Native

Aplikasi Android Native Java + SQLite lokal untuk sistem pencatatan stok ikan giling.

## Cara buka
1. Buka Android Studio.
2. Pilih `Open`.
3. Pilih folder `android-native`.
4. Tunggu Gradle sync.
5. Run ke emulator atau HP.

## Build APK via GitHub

Project root sudah punya workflow:

```text
.github/workflows/android-debug-apk.yml
```

Setelah dipush ke GitHub, buka tab `Actions`, jalankan `Build Android Debug APK`, lalu download artifact `CATOKAN-debug-apk`.

## Akun Awal
- Pengguna: `pengguna / pengguna123`

## Struktur UI
- `SplashActivity`: pembuka aplikasi.
- `OnboardingActivity`: pengenalan singkat aplikasi.
- `LoginActivity`: login pengguna dan akses daftar akun.
- `DashboardActivity`: statistik, menu utama, export/import data, dan shortcut bottom navigation sederhana.
- `RawStockActivity`: input stok mentah.
- `ProductionActivity`: produksi ikan giling.
- `SalesActivity`: penjualan FIFO wajib lunas.
- `TextActivity`: tampilan stok/laporan detail.

## Catatan UI
UI sudah dibuat lebih proper menggunakan XML layout dan drawable resource. Sistem tetap tidak overkill karena fitur bisnisnya masih fokus pada stok, produksi, transaksi, dan laporan ringkas.

## Catatan Sistem
- Database dibuat otomatis saat aplikasi pertama dibuka.
- Password pengguna disimpan sebagai hash SHA-256 pada database baru.
- Data awal dibuat cukup banyak untuk simulasi stok, produksi, penjualan, dan laporan.
