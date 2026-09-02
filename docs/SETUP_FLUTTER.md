# Setup Flutter & Build APK

## 1. Install Flutter SDK

Lo perlu Flutter SDK di PC. Kalau belum ada:

1. Download Flutter SDK: **https://docs.flutter.dev/get-started/install**
3. Pilih **Windows** (sesuai OS lo sekarang)
5. Extract ke `C:\srcflutter` (jangan di path yang ada spasi)
6. Tambahkan ke PATH: `C:\srcflutter\bin`
7. Buka terminal baru → `flutter doctor` → install yang kurang

## 2. Install Android SDK & tools

`flutter doctor` akan kasih tau apa yang kurang. Biasanya:
- **Android Studio** (download dari https://developer.android.com/studio)
- Setelah install, buka Android Studio → **SDK Manager** → install:
  - Android SDK Platform 34
  - Android SDK Build-Tools 34
- Set **ANDROID_HOME** env var (Windows): `C:\Users\bagas\AppData\Local\Android\Sdk`

Cek dengan `flutter doctor -v` — semua harus ijo (centang ✓).

## 3. Setup project

Masuk ke folder `app/`:

```bash
cd C:/Users/bagas/Downloads/catatstok/app
flutter pub get
```

Kalau ada error "Target of URI doesn't exist", coba:
```bash
flutter clean
flutter pub get
```

## 4. Setup Supabase (lihat `SETUP_SUPABASE.md`)

Pastikan `app/lib/main.dart` sudah berisi URL & anon key Supabase lo.

## 5. Test di HP (debug mode)

1. Aktifkan **Developer Options** di HP:
   - Settings → About Phone → tap "Build Number" 7x
   - Kembali → muncul menu "Developer Options"
   - Aktifkan **USB Debugging**
2. Colok HP ke PC pakai kabel USB
3. Cek HP muncul:
   ```bash
   flutter devices
   ```
   Akan muncul device lo (mis: `SM-A515F • 123abc45 • android-arm64`)
4. Run app:
   ```bash
   flutter run
   ```
   App akan install & jalan di HP lo, **dengan hot reload** (bisa edit code → langsung keliatan perubahannya)

## 6. Build APK release (untuk dikasih ke Mama)

```bash
flutter build apk --release \
  --dart-define=SUPABASE_URL=https://xxx.supabase.co \
  --dart-define=SUPABASE_ANON_KEY=eyJ...
```

APK akan muncul di: `app/build/app/outputs/flutter-apk/app-release.apk`

Kirim ke Mama via WA / email / Google Drive.

## 7. Install di HP Mama

1. Mama buka file APK di HP
2. Akan minta izin "Install dari sumber tidak dikenal" → izinkan
3. Install
4. Buka app → login / daftar akun Mama

## 8. Workflow harian setelah setup

**Kalau lo edit code:**
- `flutter run` → lihat di HP lo (debug)
- `flutter build apk --release --dart-define=...` → kasih APK baru ke Mama

**Kalau mau push ke git:**
```bash
cd C:/Users/bagas/Downloads/catatstok
git init
git add .
git commit -m "initial CATOKAN mobile"
```

> ⚠️ **JANGAN** commit `local.properties` (Android SDK path, beda tiap PC) atau kalo lo hardcode Supabase key langsung, pake `.env` atau `--dart-define` aja.

## Catatan ukuran APK: ~30 MB

Untuk app pertama. Kalo mau lebih kecil (15 MB), pakai `--split-per-abi`:
```bash
flutter build apk --split-per-abi --release --dart-define=...
```
Hasilnya ada 3 APK: armeabi-v7a, arm64-v8a, x86_64. Mama pake arm64 (HP Android modern).