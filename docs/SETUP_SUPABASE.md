# Setup Supabase untuk CATOKAN

Ikuti langkah ini **sekali aja**. Habis itu, app langsung konek ke cloud.

## 1. Bikin project Supabase (gratis)

1. Buka **https://supabase.com** → klik **Start your project** → login/daftar
2. Klik **New Project**
   - Name: `catokan-mama` (atau bebas)
   - Database Password: bikin password kuat, **simpan** (dipakai nanti kalau mau akses langsung DB)
   - Region: **Singapore** (paling deket ke Indonesia)
   - Plan: **Free**
3. Tunggu 1-2 menit sampai project ready

## 2. Setup schema database

1. Di sidebar kiri, klik **SQL Editor**
2. Klik **New query**
3. Copy seluruh isi file `supabase/schema.sql` → paste → klik **Run** (tombol hijau)
4. Kalau success, bikin query baru lagi → copy `supabase/seed.sql` → paste → klik **Run**

## 3. Ambil URL & anon key

1. Sidebar → **Settings** → **API**
2. Di bagian **Project API keys**:
   - **Project URL**: copy (misal `https://abcdefg.supabase.co`)
   - **anon public**: copy (string panjang mulai `eyJ...`)
3. **JANGAN** copy `service_role` (itu untuk server-side, bukan untuk app)

## 4. Masukin ke app Flutter

Buka `app/lib/main.dart`, isi konstanta di paling atas:

```dart
const String _supabaseUrl = 'https://abcdefg.supabase.co'; // paste URL lo
const String _supabaseAnonKey = 'eyJhbGciOi...'; // paste anon key lo
```

Atau kalau mau lebih aman (gak ke-commit), pake `--dart-define` saat build:

```bash
flutter build apk --release \
  --dart-define=SUPABASE_URL=https://abcdefg.supabase.co \
  --dart-define=SUPABASE_ANON_KEY=eyJhbGciOi...
```

## 5. Aktifkan email login (auto-confirm)

Default Supabase: user daftar harus verifikasi email dulu (link ke email). Ini ribet buat Mama.

**Solusi cepat (untuk development):**
1. Sidebar → **Authentication** → **Providers**
2. Klik **Email**
4. **Toggle OFF** "Confirm email" → klik **Save**

Sekarang daftar langsung masuk tanpa verifikasi email.

> ⚠️ Untuk production nanti, balikin ke ON, atau tambahin SMTP custom.

## 6. Invite user kedua (untuk Mama)

Cara paling gampang untuk "undang" Mama ke usaha yang sama:

**Opsi A — Mama daftar sendiri:**
- Mama install APK → buka app → klik "Daftar" → isi email & password
- Setelah login pertama, otomatis dibuat usaha baru (terpisah dari usaha lo)

**Opsi B — Lo undang Mama ke usaha lo:**
- Buka Supabase Dashboard → **Authentication** → **Users** → **Add user** → isi email + cent "Auto confirm"
- Login pakai user ini yang pertama akan otomatis pakai trigger → usaha baru juga

> ⚠️ **PENTING**: saat ini, setiap user yang daftar bikin **usaha baru sendiri**. Jadi kalau lo mau multi-user dalam 1 usaha (lo & Mama lihat data yang sama), perlu setup **invite user ke usaha yang udah ada**.

Ini enhancement v2 — untuk sekarang, Mama pake HP-nya sendiri dengan akun Mama, usaha Mama. Lo pake akun lo, usaha lo. **Data tidak akan nyambung.**

Cara test sync multi-device di setup sekarang:
- Login akun yang sama di 2 HP → otomatis RLS handle per-user, bukan per-usaha
- Saat ini kalau lo & Mama login di akun yang sama di HP berbeda, **data akan tercampur per-user** (bukan per-usaha)

## 7. Cek koneksi

Buka **Table Editor** di sidebar → klik `stok_mentah`, `users_profile`, dll. Kalau sudah ada 1 row `users_profile` (otomatis dibuat trigger saat register), berarti setup sukses.

## Troubleshooting umum

**"Invalid API key"** → cek anon key benar, tanpa spasi di awal/akhir

**"permission denied"** → cek RLS policies sudah ke-create (lihat di Authentication → Policies, harus ada banyak policy)

**Email gak terkirim** → perlu setup SMTP / disable email confirmation (langkah 5)

**App stuck di loading** → cek console log Supabase → biasanya masalah auth atau RLS