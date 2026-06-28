# Distribusi Aplikasi Desktop CATOKAN

Desktop CATOKAN adalah aplikasi JavaFX berbasis Java 17 dan SQLite lokal. Untuk tugas PBO, cara paling aman saat presentasi adalah menjalankan project desktop langsung dari Maven atau IDE karena source code, class, object, service, model, dan database bisa ditunjukkan ke dosen.

## Opsi 1 - Run untuk presentasi

Syarat:
- Java 17 atau lebih baru
- Maven

Perintah:

```bash
cd desktop-javafx
mvn clean javafx:run
```

Database akan dibuat otomatis:

```text
desktop-javafx/stok_ikan_giling_desktop.db
```

Jika ingin mengulang dari data dummy awal, tutup aplikasi lalu hapus file database tersebut. Saat aplikasi dibuka lagi, sistem akan membuat database dan dummy data baru.

## Opsi 2 - Jadikan installer Windows

Syarat:
- Windows
- Java JDK 17 atau lebih baru
- Maven

Langkah umum:

```bash
cd desktop-javafx
mvn clean package dependency:copy-dependencies
jpackage --type exe --name CATOKAN --app-version 1.0.0 --input target --main-jar stok-ikan-giling-desktop-1.0.0.jar --main-class com.bagas.stokikan.MainApp --java-options "--module-path target/dependency --add-modules javafx.controls" --win-menu --win-shortcut
```

Output installer akan muncul di folder `desktop-javafx`.

Catatan: kalau fokusnya tugas PBO, opsi run via Maven lebih mudah dijelaskan karena dosen bisa langsung melihat struktur OOP: model, service, database helper, dan UI JavaFX.

## Fitur Excel

Menu `Laporan` pada aplikasi desktop menyediakan:

- `Export Laporan Excel`: membuat workbook `.xlsx` berisi ringkasan, stok mentah, stok giling, penjualan, dan riwayat stok.
- `Template Import`: membuat template `.xlsx` untuk input stok masuk.
- `Import Stok Excel`: membaca sheet pertama template dan memasukkan stok masuk ke database.

Format kolom import:

```text
Jenis Ikan | Supplier | Berat Kg | Harga Beli per Kg | Catatan
```

Nama jenis ikan dan supplier harus sama dengan data master agar validasi tetap jelas.
