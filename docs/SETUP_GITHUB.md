# Setup GitHub Repository + Auto-build APK

Workflow di `.github/workflows/android-apk.yml` akan auto-build APK setiap lo push ke branch `main` atau `master`.

## 1. Bikin repo GitHub

1. Buka **https://github.com/new**
2. Repository name: `catokan-mama` (atau bebas)
3. Visibility: **Private** (penting, karena ada secret key)
4. **JANGAN** centang "Add README" atau "Add .gitignore" — kita udah punya
5. Klik **Create repository**

## 2. Push kode ke GitHub

```bash
cd "C:/Users/bagas/Downloads/catatstok"
git init
git branch -M main
git add .
git commit -m "initial commit: CATOKAN mobile app"
git remote add origin https://github.com/USERNAME-LO/catokan-mama.git
git push -u origin main
```

> ⚠️ Pastiin `.gitignore` udah benar — Supabase key di `main.dart` jangan ke-push kalau repo-nya public.

## 3. Setup GitHub Secrets (untuk inject Supabase URL & key saat build)

1. Buka repo GitHub lo → tab **Settings** → sidebar **Secrets and variables** → **Actions**
2. Klik **New repository secret**
3. Tambah **2 secret**:

   **Secret 1:**
   - Name: `SUPABASE_URL`
   - Value: `https://blassyztzvmsvejinqdy.supabase.co`

   **Secret 2:**
   - Name: `SUPABASE_ANON_KEY`
   - Value: `sb_publishable_rJOxklU72bOVTbOnGFCSmg_9lZ99XSP` (publishable key)

> ⚠️ JANGAN masukin `service_role` key di sini. Service_role bisa bypass RLS dan hapus seluruh database. Cuma `anon` atau `publishable` yang aman di app.

## 4. Trigger build pertama

Workflow bakal jalan otomatis saat push pertama kali ke `main`. Tapi untuk mastiin, lo bisa trigger manual:

1. Buka tab **Actions** di repo GitHub
2. Pilih workflow **Build Android APK** di sidebar kiri
3. Klik **Run workflow** → tombol hijau **Run workflow**

Tunggu 3-5 menit sampai selesai.

## 5. Download APK hasil build

1. Setelah workflow selesai (centang ✓ ijo), klik run-nya
2. Scroll ke bawah ke section **Artifacts**
3. Download:
   - `catokan-debug` — APK debug (untuk testing, lebih besar)
   - `catokan-release` — APK release (kecil, buat dikasih ke Mama)
4. Extract file zip → dapet `.apk`
5. Kirim APK ke HP Mama (via WA / Google Drive / email)

## 6. Update workflow kalau perlu

Setiap lo push perubahan ke branch `main`, APK akan auto-rebuild. Tinggal download dari tab Actions.

## Tips

- **Build gagal?** Klik run yang gagal → lihat log → biasanya di step "Run analyzer" atau "Build APK"
- **Mau pake workflow trigger lain?** Edit `.github/workflows/android-apk.yml`:
  - `on.push` — kapan workflow jalan otomatis
  - `paths` — folder/file apa yang trigger build (skip kalau cuma update README)
- **Test di HP lo tanpa push:** Install Flutter SDK di PC → `flutter run` dari `app/` folder