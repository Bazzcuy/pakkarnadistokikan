# Data Dummy Simulasi

Data dummy dibuat agar aplikasi langsung terlihat seperti sistem operasional kecil saat pertama dijalankan. Data ini otomatis dibuat oleh SQLite seed di desktop dan Android.

## Akun Demo

Password disimpan sebagai hash SHA-256 pada database baru, tetapi login tetap memakai kredensial berikut:

| Nama | Username | Password | Login |
|---|---|---|---|
| Pengguna CATOKAN | pengguna | pengguna123 | PENGGUNA |

## Master Data

Jenis ikan:
- Tenggiri
- Gabus
- Kakap
- Patin
- Lele
- Belida
- Nila
- Tongkol

Supplier:
- Supplier Ikan Segar Palembang
- Pasar Ikan 16 Ilir
- Nelayan Sungai Musi
- Jakabaring Fresh Fish
- Agen Ikan Kertapati
- CV Lautan Rasa
- UD Segar Laut Musi
- Depot Ikan Gandus
- Mitra Nelayan Banyuasin
- Pasar Induk Jakabaring

Pelanggan:
- Bu Sari Pempek
- Dapur Pempek Aisyah
- Warung Model Pak Rudi
- Rumah Makan Musi Jaya
- Pelanggan Umum
- Pempek Cek Lina
- Kantin Kampus Sriwijaya
- Frozen Food Bukit
- Pempek Pak Jaya
- Dapur Harian Mama Rina
- Kedai Tekwan 24
- Agen Frozen Kertapati

## Stok Mentah Awal

Stok dibuat bervariasi agar lebih realistis:

| Jenis Ikan | Stok Mentah |
|---|---:|
| Tenggiri | 68.5 kg |
| Gabus | 44.0 kg |
| Kakap | 37.5 kg |
| Patin | 52.0 kg |
| Lele | 31.0 kg |
| Belida | 18.0 kg |
| Nila | 29.5 kg |
| Tongkol | 41.0 kg |

## Batch Giling Awal

| Batch | Jenis | Stok Awal | Harga Jual |
|---|---|---:|---:|
| BG-DEMO-001 | Tenggiri | 12.5 kg | Rp 95.000/kg |
| BG-DEMO-002 | Gabus | 10.0 kg | Rp 80.000/kg |
| BG-DEMO-003 | Kakap | 15.0 kg | Rp 72.000/kg |
| BG-DEMO-004 | Patin | 18.0 kg | Rp 56.000/kg |
| BG-DEMO-005 | Belida | 8.0 kg | Rp 110.000/kg |
| BG-DEMO-006 | Tenggiri | 22.0 kg | Rp 98.000/kg |
| BG-DEMO-007 | Nila | 14.0 kg | Rp 52.000/kg |
| BG-DEMO-008 | Tongkol | 16.0 kg | Rp 60.000/kg |
| BG-DEMO-009 | Lele | 13.0 kg | Rp 42.000/kg |
| BG-DEMO-010 | Kakap | 9.0 kg | Rp 74.000/kg |

## Simulasi Transaksi

Data dummy juga membuat stok masuk, produksi, penjualan, pembayaran, dan riwayat stok. Ada transaksi lunas dan belum lunas agar menu laporan, pembayaran, dan dashboard punya data nyata untuk dijelaskan saat demo.

Contoh transaksi:
- TRX-DEMO-001: Bu Sari Pempek membeli tenggiri dan langsung lunas.
- TRX-DEMO-002: Dapur Pempek Aisyah membeli kakap dengan pembayaran sebagian.
- TRX-DEMO-003: Rumah Makan Musi Jaya membeli patin dan lunas.
- TRX-DEMO-004: Pempek Cek Lina membeli tenggiri dan lunas.
- TRX-DEMO-005: Frozen Food Bukit membeli belida dengan status tempo/belum lunas.
- TRX-DEMO-006 sampai TRX-DEMO-012: transaksi tambahan untuk simulasi dashboard pengguna, laporan, dan pembayaran tempo.
