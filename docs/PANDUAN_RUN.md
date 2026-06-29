# Panduan Menjalankan Project

## A. Menjalankan Desktop JavaFX

Syarat:
- Java 17 atau lebih baru
- Maven

Langkah:

```bash
cd desktop-javafx
mvn clean javafx:run
```

Database SQLite desktop akan otomatis dibuat di folder project dengan nama:

```text
stok_ikan_giling_desktop.db
```

## B. Menjalankan Android Native Java

Syarat:
- Android Studio
- JDK bawaan Android Studio
- Emulator atau HP Android dengan USB Debugging aktif

Langkah:
1. Buka Android Studio.
2. Pilih **Open**.
3. Arahkan ke folder `android-native`.
4. Tunggu Gradle Sync selesai.
5. Jalankan aplikasi ke emulator/HP.

Database SQLite Android dibuat otomatis oleh `DbHelper` saat aplikasi pertama dibuka.

## C. Akun Login

- Pengguna: `pengguna / pengguna123`
- Pengguna baru juga bisa dibuat lewat tab **Daftar** pada aplikasi desktop.

## D. Alur presentasi Singkat

1. Login sebagai Pengguna.
2. Lengkapi profil usaha.
3. Cek dashboard visual, stok mentah, stok giling, dan data Data kelola jenis ikan.
4. Tambah jenis ikan beserta gambar produk jika diperlukan.
5. Input stok mentah masuk.
6. Proses produksi giling dari stok mentah.
7. Input transaksi penjualan.
8. Lihat riwayat dan laporan dengan filter hari, minggu, atau bulan.
9. Coba export/import Excel pada aplikasi desktop.

## E. Build APK dari GitHub

Workflow GitHub Actions sudah tersedia di `.github/workflows/android-debug-apk.yml`.

Setelah project dipush ke GitHub:
1. Buka tab `Actions`.
2. Jalankan workflow `Build Android Debug APK`.
3. Download artifact `CATOKAN-debug-apk`.

Detail ada di `docs/ANDROID_GITHUB_APK.md`.

## F. Distribusi Desktop

Untuk tugas PBO, desktop JavaFX paling aman dijalankan via Maven/IDE agar source code dan struktur OOP mudah ditunjukkan. Jika ingin membuat installer Windows, ikuti `docs/DESKTOP_DISTRIBUTION.md`.
