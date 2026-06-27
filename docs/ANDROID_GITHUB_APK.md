# Build APK Android dari GitHub

Workflow GitHub Actions sudah disiapkan di:

```text
.github/workflows/android-debug-apk.yml
```

Cara pakai:

1. Push folder project ini ke GitHub.
2. Buka tab `Actions`.
3. Pilih workflow `Build Android Debug APK`.
4. Jalankan manual dengan `Run workflow`, atau tunggu otomatis saat push ke branch `main`/`master`.
5. Setelah selesai, buka hasil run dan download artifact `CATOKAN-debug-apk`.

File APK berada di dalam artifact:

```text
app-debug.apk
```

APK ini cocok untuk demo dan pengujian. Untuk rilis publik di Play Store, perlu signing key release sendiri.
