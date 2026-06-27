# Desktop JavaFX - Stok Ikan Giling

Aplikasi desktop ini dibuat untuk presentasi di laptop. Data disimpan pada SQLite lokal.

## Run

```bash
mvn clean javafx:run
```

## Distribusi

Untuk presentasi PBO, jalankan via Maven/IDE agar source code dan struktur OOP mudah ditunjukkan. Jika ingin membuat installer Windows, ikuti panduan:

```text
../docs/DESKTOP_DISTRIBUTION.md
```

## Fitur
- Login role
- Dashboard
- Input stok ikan mentah
- Proses produksi ikan giling
- Stok giling per batch
- Transaksi penjualan
- Pembayaran
- Laporan ringkas

## Catatan Sistem
- Database SQLite dibuat otomatis saat aplikasi pertama dijalankan.
- Password demo disimpan sebagai hash SHA-256 pada database baru.
- Service stok, produksi, penjualan, dan pembayaran memakai transaksi database.
