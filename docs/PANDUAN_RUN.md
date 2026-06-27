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

- Admin: `admin / admin123`
- Kasir: `kasir / kasir123`
- Operator: `operator / operator123`

## D. Alur Demo Singkat

1. Login sebagai Admin.
2. Cek dashboard dan data master.
3. Input stok mentah masuk.
4. Proses produksi giling dari stok mentah.
5. Cek stok ikan giling per batch.
6. Input transaksi penjualan.
7. Input pembayaran.
8. Lihat laporan ringkas.

## E. Build APK dari GitHub

Workflow GitHub Actions sudah tersedia di `.github/workflows/android-debug-apk.yml`.

Setelah project dipush ke GitHub:
1. Buka tab `Actions`.
2. Jalankan workflow `Build Android Debug APK`.
3. Download artifact `CATOKAN-debug-apk`.

Detail ada di `docs/ANDROID_GITHUB_APK.md`.

## F. Distribusi Desktop

Untuk tugas PBO, desktop JavaFX paling aman dijalankan via Maven/IDE agar source code dan struktur OOP mudah ditunjukkan. Jika ingin membuat installer Windows, ikuti `docs/DESKTOP_DISTRIBUTION.md`.
