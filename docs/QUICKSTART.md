# Setup Project CATOKAN — Quick Start (5 Menit)

**Langkahnya udah disederhanain banget**. Ikutin urutan ini, nanti APK bisa lo download.

## Step 1: Setup Supabase (5 menit)

1. Buka **https://supabase.com/dashboard** → pilih project lo (`catokan-mama`)
2. Sidebar → **SQL Editor**
3. **New query** → Copy seluruh isi `supabase/schema.sql` → paste → klik **Run** (tombol hijau ▶)
4. Kalau success → **New query lagi** → Copy `supabase/seed.sql` → paste → Run
5. Sidebar → **Authentication** → **Providers** → klik **Email** → **OFF** toggle "Confirm email" → Save
6. Sidebar → **Settings** → **API** → copy **Project URL** + **publishable key** (yang `sb_publishable_...`)

## Step 2: Verifikasi (30 detik)

Sidebar → **Table Editor** → pastikan ada **14 tabel**: `usa`, `users_profile`, `jenis_ikan`, `suppliers`, `pelanggan`, `stok_mentah`, `stok_masuk`, `piutang_supplier`, `stok_giling`, `penjualan`, `detail_penjualan`, `pembayaran_penjualan`, `koreksi_stok`, `riwayat_stok`.

Kalau belum ada → schema.sql belum jalan → ulangi Step 1.

## Step 3: Push ke GitHub (2 menit)

```bash
cd "C:/Users/bagas/Downloads/catatstok"
git init
git branch -M main
git add .
git commit -m "initial: CATOKAN mobile app"
git remote add origin https://github.com/Bazzcuy/pakkarnadistokikan.git
git push -u origin main
```

## Step 4: Setup GitHub Secrets (1 menit)

1. Buka repo GitHub lo → **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret**:
   - Name: `SUPABASE_URL` → Value: paste URL dari Step 1
   - Name: `SUPABASE_ANON_KEY` → Value: paste publishable key dari Step 1

## Step 5: Trigger Build APK (5 menit)

1. Buka repo → tab **Actions**
2. Pilih workflow **Build Android APK**
3. Klik **Run workflow** → **Run workflow** (tombol hijau)
4. Tunggu sampai selesai (~3-5 menit)
5. Scroll ke bawah → section **Artifacts** → download `catokan-release`

## Step 6: Install di HP Mama

1. Kirim file `app-release.apk` ke HP Mama (via WA/email/Drive)
2. Mama buka file → izinkan "Install dari sumber tidak dikenal"
3. Install → daftar akun baru atau login

## Kalau Stuck

**SQL Editor error di schema.sql?**
→ Copy paste **per blok** (CREATE TABLE, lalu CREATE TYPE, dst.) biar keliatan error di mana

**Build di GitHub Actions gagal?**
→ Klik run yang gagal → lihat log → biasanya "analyzer found issues" aman, tapi "Build APK failed" perlu dicek. Biasanya masalah Java/SDK version.

**App gak konek ke Supabase?**
→ Cek URL & key di GitHub Secrets sama dengan yang di dashboard Supabase. **URL HARUS sama persis**, termasuk `https://` di awal dan gak ada spasi.

**App force close pas buka?**
→ Buka dengan `flutter run` di PC → lihat log error → biasanya Riverpod/Dart compile issue.

## Next steps

Kalau v1 udah jalan, tambahin fitur v2:
- Batalkan transaksi (refund / retur)
- Export Excel / PDF
- Invite user ke usaha yang sama (supaya lo & Mama lihat data yang sama)
- Push notification untuk reminder bayar utang