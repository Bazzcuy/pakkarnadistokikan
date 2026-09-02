# CATOKAN — Catat Stok Ikan Giling untuk Mama

Aplikasi mobile **offline-first** untuk pencatatan stok, produksi, dan penjualan ikan giling. Dibikin biar Mama bisa catat di HP sendiri (walaupun gak ada sinyal), dan Bagas bisa pantau dari HP sendiri/cloud.

📱 **[Panduan Mama (PDF-friendly)](docs/PANDUAN_EMAK.md)**

## Quick Start

1. **Setup Supabase** → [docs/SETUP_SUPABASE.md](docs/SETUP_SUPABASE.md) (~10 menit)
2. **Setup GitHub Actions** → [docs/SETUP_GITHUB.md](docs/SETUP_GITHUB.md) (~5 menit)
3. APK auto-built setiap push ke `main`. Download dari tab **Actions** → **Artifacts**.

## Stack

- **Flutter 3.24+** — UI framework
- **sqflite** — local DB (offline)
- **Supabase** — Postgres cloud sync (gratis tier)
- **Riverpod** — state management

## Fitur Utama

- 🛒 **Beli Ikan** — input stok masuk dengan status Tunai/Utang
- 🍳 **Giling** — input produksi (mentah → giling, FIFO auto)
- 💰 **Jual** — penjualan FIFO (batch terlama keluar dulu)
- 💸 **Bayar Utang Supplier** — cicil/lunasi utang
- 🤝 **Terima Bayar Pelanggan** — catat piutang
- 📊 **Dashboard** — 5 ringkasan angka + 5 tombol aksi besar
- 📝 **Riwayat** — audit trail semua mutasi
- 🔧 **Koreksi Stok** — kalo hitungan sistem vs fisik beda

## Arsitektur

```
┌──────────────┐
│ HP Mama      │ ← input + offline (sqflite)
└──────┬───────┘
       │ background sync (push/pull)
       ▼
┌──────────────┐
│ Supabase    │ ← Postgres cloud (gratis)
│ Postgres    │   + Row Level Security
└──────┬───────┘
       │ realtime + RLS
       ▼
┌──────────────┐
│ HP Bagas    │ ← monitor + input tambahan
└──────────────┘
```

Lihat [docs/SETUP_SUPABASE.md](docs/SETUP_SUPABASE.md) untuk detail multi-tenant + RLS setup.

## Struktur Project

```
├── app/                  # Flutter project
│   ├── lib/
│   │   ├── core/         # theme, format rupiah/kg, generator batch
│   │   ├── data/
│   │   │   ├── local/    # sqflite schema
│   │   │   ├── remote/   # Supabase client
│   │   │   ├── models/   # Data classes
│   │   │   └── repositories/  # Auth, Stok (logic bisnis), Sync
│   │   └── features/     # UI per fitur
│   ├── android/          # Native Android shell
│   └── pubspec.yaml
├── supabase/
│   ├── schema.sql        # CREATE TABLE + RLS + triggers
│   └── seed.sql          # Helper seed_master_data()
├── docs/                 # Setup guides
├── .github/workflows/    # CI/CD build APK
└── README.md
```

## Build Lokal (tanpa GitHub Actions)

```bash
cd app
flutter pub get
flutter run                # debug di HP yang dicolok
flutter build apk --release \
  --dart-define=SUPABASE_URL=https://xxx.supabase.co \
  --dart-define=SUPABASE_ANON_KEY=sb_publishable_xxx
```

APK ada di: `app/build/app/outputs/flutter-apk/app-release.apk`

## License

Internal project, bukan untuk distribusi.