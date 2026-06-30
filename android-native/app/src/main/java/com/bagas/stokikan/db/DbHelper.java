package com.bagas.stokikan.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.util.DateUtil;
import com.bagas.stokikan.util.PasswordUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "stok_ikan_giling_android.db";
    private static final int DB_VERSION = 5;
    private static final String[] TRANSFER_TABLES = {
            "users", "jenis_ikan", "suppliers", "pelanggan", "stok_mentah", "stok_giling",
            "stok_masuk", "produksi_giling", "penjualan", "detail_penjualan", "pembayaran", "riwayat_stok"
    };

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, nama_usaha TEXT, nomor_hp TEXT, alamat TEXT, status TEXT DEFAULT 'AKTIF')");
        db.execSQL("CREATE TABLE jenis_ikan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT UNIQUE NOT NULL, kategori TEXT, deskripsi TEXT, gambar_path TEXT, aktif INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, catatan TEXT)");
        db.execSQL("CREATE TABLE pelanggan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, tipe_pelanggan TEXT)");
        db.execSQL("CREATE TABLE stok_mentah (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, updated_at TEXT)");
        db.execSQL("CREATE TABLE stok_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER NOT NULL, batch_no TEXT UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, harga_jual_per_kg REAL NOT NULL DEFAULT 0, tanggal_produksi TEXT, status_stok TEXT DEFAULT 'TERSEDIA')");
        db.execSQL("CREATE TABLE stok_masuk (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, supplier_id INTEGER NOT NULL, berat_kg REAL NOT NULL, harga_beli_per_kg REAL NOT NULL, total_beli REAL NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE produksi_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_no TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, berat_mentah_kg REAL NOT NULL, berat_hasil_kg REAL NOT NULL, penyusutan_kg REAL NOT NULL, biaya_produksi REAL DEFAULT 0, harga_jual_per_kg REAL NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, nomor_transaksi TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, pelanggan_id INTEGER, kasir_id INTEGER, subtotal REAL NOT NULL, diskon REAL DEFAULT 0, total REAL NOT NULL, status_pembayaran TEXT NOT NULL)");
        db.execSQL("CREATE TABLE detail_penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, stok_giling_id INTEGER NOT NULL, jenis_ikan_id INTEGER NOT NULL, jumlah_kg REAL NOT NULL, harga_per_kg REAL NOT NULL, subtotal REAL NOT NULL)");
        db.execSQL("CREATE TABLE pembayaran (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, tanggal TEXT NOT NULL, metode TEXT NOT NULL, jumlah_bayar REAL NOT NULL, sisa_bayar REAL NOT NULL, status TEXT NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE riwayat_stok (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER, jenis_transaksi TEXT NOT NULL, jenis_stok TEXT NOT NULL, referensi TEXT, perubahan_kg REAL NOT NULL, stok_sebelum REAL NOT NULL, stok_sesudah REAL NOT NULL, keterangan TEXT)");
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            addColumn(db, "riwayat_stok", "jenis_ikan_id", "INTEGER");
            db.execSQL("UPDATE riwayat_stok SET jenis_ikan_id=(SELECT jenis_ikan_id FROM stok_giling WHERE batch_no=riwayat_stok.referensi LIMIT 1) WHERE jenis_ikan_id IS NULL AND referensi LIKE 'BG-%'");
            db.execSQL("UPDATE riwayat_stok SET jenis_ikan_id=(SELECT d.jenis_ikan_id FROM penjualan p JOIN detail_penjualan d ON d.penjualan_id=p.id WHERE p.nomor_transaksi=riwayat_stok.referensi LIMIT 1) WHERE jenis_ikan_id IS NULL AND referensi LIKE 'TRX-%'");
            db.execSQL("UPDATE penjualan SET status_pembayaran='LUNAS' WHERE status_pembayaran='BELUM_LUNAS'");
            db.execSQL("UPDATE pembayaran SET sisa_bayar=0,status='LUNAS',catatan='Pembayaran lunas' WHERE status='BELUM_LUNAS'");
            return;
        }
        if (oldVersion >= 3 && newVersion >= 4) {
            addColumn(db, "riwayat_stok", "jenis_ikan_id", "INTEGER");
            db.execSQL("UPDATE riwayat_stok SET jenis_ikan_id=(SELECT jenis_ikan_id FROM stok_giling WHERE batch_no=riwayat_stok.referensi LIMIT 1) WHERE jenis_ikan_id IS NULL AND referensi LIKE 'BG-%'");
            db.execSQL("UPDATE riwayat_stok SET jenis_ikan_id=(SELECT d.jenis_ikan_id FROM penjualan p JOIN detail_penjualan d ON d.penjualan_id=p.id WHERE p.nomor_transaksi=riwayat_stok.referensi LIMIT 1) WHERE jenis_ikan_id IS NULL AND referensi LIKE 'TRX-%'");
            return;
        }
        db.execSQL("DROP TABLE IF EXISTS pembayaran");
        db.execSQL("DROP TABLE IF EXISTS detail_penjualan");
        db.execSQL("DROP TABLE IF EXISTS penjualan");
        db.execSQL("DROP TABLE IF EXISTS produksi_giling");
        db.execSQL("DROP TABLE IF EXISTS stok_masuk");
        db.execSQL("DROP TABLE IF EXISTS stok_giling");
        db.execSQL("DROP TABLE IF EXISTS stok_mentah");
        db.execSQL("DROP TABLE IF EXISTS pelanggan");
        db.execSQL("DROP TABLE IF EXISTS suppliers");
        db.execSQL("DROP TABLE IF EXISTS jenis_ikan");
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS riwayat_stok");
        onCreate(db);
    }

    private void addColumn(SQLiteDatabase db, String table, String column, String type) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) if (column.equalsIgnoreCase(c.getString(c.getColumnIndexOrThrow("name")))) return;
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    private void seed(SQLiteDatabase db) {
        insertUser(db, "Pengguna CATOKAN", "pengguna", "pengguna123", "PENGGUNA");

        String[] ikan = {"Tenggiri", "Gabus", "Kakap", "Patin", "Lele", "Belida", "Nila", "Tongkol"};
        for (String s : ikan) {
            ContentValues cv = new ContentValues();
            cv.put("nama", s);
            cv.put("kategori", "Ikan");
            cv.put("deskripsi", "Jenis ikan " + s);
            cv.put("gambar_path", "catokan_banner");
            long id = db.insert("jenis_ikan", null, cv);
            ContentValues stok = new ContentValues();
            stok.put("jenis_ikan_id", id);
            double totalKg;
            if (id == 1) totalKg = 68.5;
            else if (id == 2) totalKg = 44.0;
            else if (id == 3) totalKg = 37.5;
            else if (id == 4) totalKg = 52.0;
            else if (id == 5) totalKg = 31.0;
            else if (id == 6) totalKg = 18.0;
            else if (id == 7) totalKg = 29.5;
            else totalKg = 41.0;
            stok.put("total_kg", totalKg);
            stok.put("updated_at", DateUtil.now());
            db.insert("stok_mentah", null, stok);
        }

        String[][] suppliers = {
                {"Supplier Ikan Segar Palembang", "081271001001", "Jl. Demang Lebar Daun, Palembang", "Pasokan tenggiri premium"},
                {"Pasar Ikan 16 Ilir", "081271001002", "Pasar 16 Ilir, Palembang", "Supplier harian"},
                {"Nelayan Sungai Musi", "081271001003", "Seberang Ulu, Palembang", "Pasokan ikan sungai"},
                {"Jakabaring Fresh Fish", "081271001004", "Jakabaring, Palembang", "Pasokan partai besar"},
                {"Agen Ikan Kertapati", "081271001005", "Kertapati, Palembang", "Supplier cadangan"},
                {"CV Lautan Rasa", "081271001006", "Plaju, Palembang", "Pasokan kakap dan patin"},
                {"UD Segar Laut Musi", "081271001007", "Sako, Palembang", "Pasokan tongkol dan nila"},
                {"Depot Ikan Gandus", "081271001008", "Gandus, Palembang", "Supplier ikan sungai"},
                {"Mitra Nelayan Banyuasin", "081271001009", "Banyuasin", "Pasokan luar kota"},
                {"Pasar Induk Jakabaring", "081271001010", "Jakabaring, Palembang", "Supplier grosir"}
        };
        for (String[] s : suppliers) {
            ContentValues cv = new ContentValues();
            cv.put("nama", s[0]);
            cv.put("nomor_hp", s[1]);
            cv.put("alamat", s[2]);
            cv.put("catatan", s[3]);
            db.insert("suppliers", null, cv);
        }

        String[][] pelanggan = {
                {"Bu Sari Pempek", "082171002001", "Ilir Barat I, Palembang", "Retail"},
                {"Dapur Pempek Aisyah", "082171002002", "Bukit Kecil, Palembang", "Grosir"},
                {"Warung Model Pak Rudi", "082171002003", "Sukarami, Palembang", "Retail"},
                {"Rumah Makan Musi Jaya", "082171002004", "Seberang Ulu I, Palembang", "Grosir"},
                {"Pelanggan Umum", "082171002005", "Palembang", "Retail"},
                {"Pempek Cek Lina", "082171002006", "Kemuning, Palembang", "Grosir"},
                {"Kantin Kampus Sriwijaya", "082171002007", "Indralaya", "Retail"},
                {"Frozen Food Bukit", "082171002008", "Bukit Besar, Palembang", "Grosir"},
                {"Pempek Pak Jaya", "082171002009", "Sako, Palembang", "Grosir"},
                {"Dapur Harian Mama Rina", "082171002010", "Plaju, Palembang", "Retail"},
                {"Kedai Tekwan 24", "082171002011", "Alang-Alang Lebar, Palembang", "Retail"},
                {"Agen Frozen Kertapati", "082171002012", "Kertapati, Palembang", "Grosir"}
        };
        for (String[] s : pelanggan) {
            ContentValues cv = new ContentValues();
            cv.put("nama", s[0]);
            cv.put("nomor_hp", s[1]);
            cv.put("alamat", s[2]);
            cv.put("tipe_pelanggan", s[3]);
            db.insert("pelanggan", null, cv);
        }

        ContentValues g1 = new ContentValues();
        g1.put("jenis_ikan_id", 1);
        g1.put("batch_no", "BG-202606-001");
        g1.put("total_kg", 12.5);
        g1.put("harga_jual_per_kg", 95000.0);
        g1.put("tanggal_produksi", DateUtil.today());
        g1.put("status_stok", "TERSEDIA");
        db.insert("stok_giling", null, g1);

        ContentValues g2 = new ContentValues();
        g2.put("jenis_ikan_id", 2);
        g2.put("batch_no", "BG-202606-002");
        g2.put("total_kg", 10.0);
        g2.put("harga_jual_per_kg", 80000.0);
        g2.put("tanggal_produksi", DateUtil.today());
        g2.put("status_stok", "TERSEDIA");
        db.insert("stok_giling", null, g2);

        seedGiling(db, 3, "BG-202606-003", 15.0, 72000.0, "2026-06-21");
        seedGiling(db, 4, "BG-202606-004", 18.0, 56000.0, "2026-06-22");
        seedGiling(db, 6, "BG-202606-005", 8.0, 110000.0, "2026-06-23");
        seedGiling(db, 1, "BG-202606-006", 22.0, 98000.0, "2026-06-24");
        seedGiling(db, 7, "BG-202606-007", 14.0, 52000.0, "2026-06-25");
        seedGiling(db, 8, "BG-202606-008", 16.0, 60000.0, "2026-06-25");
        seedGiling(db, 5, "BG-202606-009", 13.0, 42000.0, "2026-06-26");
        seedGiling(db, 3, "BG-202606-010", 9.0, 74000.0, "2026-06-26");

        seedStockIn(db, "2026-06-18", 1, 1, 45.0, 62000.0, "Pasokan awal tenggiri");
        seedStockIn(db, "2026-06-19", 2, 3, 30.0, 48000.0, "Gabus segar dari Musi");
        seedStockIn(db, "2026-06-21", 4, 4, 55.0, 35000.0, "Patin untuk stok mingguan");
        seedStockIn(db, "2026-06-22", 6, 6, 20.0, 76000.0, "Belida kualitas premium");
        seedStockIn(db, "2026-06-23", 7, 7, 40.0, 28000.0, "Nila untuk produksi ekonomis");
        seedStockIn(db, "2026-06-24", 8, 9, 38.0, 33000.0, "Tongkol luar kota");
        seedStockIn(db, "2026-06-25", 5, 10, 34.0, 24000.0, "Lele partai kecil");
        seedStockIn(db, "2026-06-26", 3, 6, 24.0, 45000.0, "Kakap tambahan");

        seedProduction(db, "BG-202606-001", "2026-06-20", 1, 15.0, 12.5, 85000.0, 95000.0, "Produksi tenggiri");
        seedProduction(db, "BG-202606-002", "2026-06-20", 2, 12.0, 10.0, 65000.0, 80000.0, "Produksi gabus");
        seedProduction(db, "BG-202606-003", "2026-06-21", 3, 18.0, 15.0, 75000.0, 72000.0, "Produksi kakap");
        seedProduction(db, "BG-202606-004", "2026-06-22", 4, 22.0, 18.0, 70000.0, 56000.0, "Produksi patin");
        seedProduction(db, "BG-202606-005", "2026-06-23", 6, 10.0, 8.0, 90000.0, 110000.0, "Produksi belida");
        seedProduction(db, "BG-202606-006", "2026-06-24", 1, 26.0, 22.0, 120000.0, 98000.0, "Produksi tenggiri batch besar");
        seedProduction(db, "BG-202606-007", "2026-06-25", 7, 17.0, 14.0, 60000.0, 52000.0, "Produksi nila ekonomis");
        seedProduction(db, "BG-202606-008", "2026-06-25", 8, 20.0, 16.0, 65000.0, 60000.0, "Produksi tongkol");
        seedProduction(db, "BG-202606-009", "2026-06-26", 5, 16.0, 13.0, 50000.0, 42000.0, "Produksi lele");
        seedProduction(db, "BG-202606-010", "2026-06-26", 3, 11.0, 9.0, 52000.0, 74000.0, "Produksi kakap tambahan");

        seedSale(db, "TRX-202606-001", "2026-06-24", 1, 2, 1, 2.0, 190000.0, "Tunai");
        seedSale(db, "TRX-202606-002", "2026-06-24", 2, 2, 3, 4.5, 200000.0, "Transfer");
        seedSale(db, "TRX-202606-003", "2026-06-25", 4, 2, 4, 6.0, 336000.0, "Tunai");
        seedSale(db, "TRX-202606-004", "2026-06-25", 6, 2, 6, 7.0, 686000.0, "Transfer");
        seedSale(db, "TRX-202606-005", "2026-06-26", 8, 2, 5, 1.5, 63000.0, "Tunai");
        seedSale(db, "TRX-202606-006", "2026-06-26", 9, 2, 2, 3.0, 240000.0, "Tunai");
        seedSale(db, "TRX-202606-007", "2026-06-26", 10, 2, 7, 4.0, 208000.0, "Transfer");
        seedSale(db, "TRX-202606-008", "2026-06-27", 11, 2, 8, 5.0, 300000.0, "Tunai");
        seedSale(db, "TRX-202606-009", "2026-06-27", 12, 2, 9, 2.5, 105000.0, "Transfer");
        seedSale(db, "TRX-202606-010", "2026-06-27", 3, 2, 10, 2.0, 148000.0, "Transfer");
        seedSale(db, "TRX-202606-011", "2026-06-28", 5, 2, 1, 4.0, 380000.0, "Tunai");
        seedSale(db, "TRX-202606-012", "2026-06-28", 7, 2, 6, 3.0, 200000.0, "Transfer");
    }

    private void seedGiling(SQLiteDatabase db, int jenisId, String batch, double kg, double harga, String tanggal) {
        ContentValues cv = new ContentValues();
        cv.put("jenis_ikan_id", jenisId);
        cv.put("batch_no", batch);
        cv.put("total_kg", kg);
        cv.put("harga_jual_per_kg", harga);
        cv.put("tanggal_produksi", tanggal);
        cv.put("status_stok", "TERSEDIA");
        db.insert("stok_giling", null, cv);
    }

    private void seedStockIn(SQLiteDatabase db, String tanggal, int jenisId, int supplierId, double kg, double harga, String catatan) {
        ContentValues cv = new ContentValues();
        cv.put("tanggal", tanggal);
        cv.put("jenis_ikan_id", jenisId);
        cv.put("supplier_id", supplierId);
        cv.put("berat_kg", kg);
        cv.put("harga_beli_per_kg", harga);
        cv.put("total_beli", kg * harga);
        cv.put("catatan", catatan);
        db.insert("stok_masuk", null, cv);
    }

    private void seedProduction(SQLiteDatabase db, String batch, String tanggal, int jenisId, double mentah, double hasil, double biaya, double hargaJual, String catatan) {
        ContentValues cv = new ContentValues();
        cv.put("batch_no", batch);
        cv.put("tanggal", tanggal);
        cv.put("jenis_ikan_id", jenisId);
        cv.put("berat_mentah_kg", mentah);
        cv.put("berat_hasil_kg", hasil);
        cv.put("penyusutan_kg", mentah - hasil);
        cv.put("biaya_produksi", biaya);
        cv.put("harga_jual_per_kg", hargaJual);
        cv.put("catatan", catatan);
        db.insert("produksi_giling", null, cv);
    }

    private void seedSale(SQLiteDatabase db, String nomor, String tanggal, int pelangganId, int kasirId, int stokGilingId, double kg, double bayar, String metode) {
        double stok = scalar(db, "SELECT total_kg FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
        double harga = scalar(db, "SELECT harga_jual_per_kg FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
        int jenisId = (int) scalar(db, "SELECT jenis_ikan_id FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
        double total = kg * harga;
        double bayarFinal = total;
        double sisa = 0;
        String status = "LUNAS";
        ContentValues pj = new ContentValues();
        pj.put("nomor_transaksi", nomor);
        pj.put("tanggal", tanggal);
        pj.put("pelanggan_id", pelangganId);
        pj.put("kasir_id", kasirId);
        pj.put("subtotal", total);
        pj.put("diskon", 0);
        pj.put("total", total);
        pj.put("status_pembayaran", status);
        long penjualanId = db.insert("penjualan", null, pj);

        ContentValues detail = new ContentValues();
        detail.put("penjualan_id", penjualanId);
        detail.put("stok_giling_id", stokGilingId);
        detail.put("jenis_ikan_id", jenisId);
        detail.put("jumlah_kg", kg);
        detail.put("harga_per_kg", harga);
        detail.put("subtotal", total);
        db.insert("detail_penjualan", null, detail);

        ContentValues pay = new ContentValues();
        pay.put("penjualan_id", penjualanId);
        pay.put("tanggal", tanggal);
        pay.put("metode", metode);
        pay.put("jumlah_bayar", bayarFinal);
        pay.put("sisa_bayar", sisa);
        pay.put("status", status);
        pay.put("catatan", "Pembayaran transaksi penjualan");
        db.insert("pembayaran", null, pay);

        double after = stok - kg;
        ContentValues upd = new ContentValues();
        upd.put("total_kg", after);
        upd.put("status_stok", after <= 0 ? "HABIS" : "TERSEDIA");
        db.update("stok_giling", upd, "id=?", new String[]{String.valueOf(stokGilingId)});

        ContentValues r = new ContentValues();
        r.put("tanggal", tanggal + " 10:00:00");
        r.put("jenis_ikan_id", jenisId);
        r.put("jenis_transaksi", "PENJUALAN");
        r.put("jenis_stok", "GILING");
        r.put("referensi", nomor);
        r.put("perubahan_kg", -kg);
        r.put("stok_sebelum", stok);
        r.put("stok_sesudah", after);
        r.put("keterangan", "Penjualan ikan giling");
        db.insert("riwayat_stok", null, r);
    }

    private double scalar(SQLiteDatabase db, String sql, String... args) {
        try (Cursor c = db.rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getDouble(0) : 0.0;
        }
    }

    private void insertUser(SQLiteDatabase db, String nama, String username, String password, String role) {
        ContentValues cv = new ContentValues();
        cv.put("nama", nama);
        cv.put("username", username);
        cv.put("password", PasswordUtil.sha256(password));
        cv.put("role", role);
        cv.put("nama_usaha", "CATOKAN Ikan Giling");
        cv.put("nomor_hp", "081271009999");
        cv.put("alamat", "Palembang");
        db.insert("users", null, cv);
    }

    public User login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,nama,username,role FROM users WHERE username=? AND password IN (?,?) AND status='AKTIF'", new String[]{username, PasswordUtil.sha256(password), password})) {
            if (c.moveToFirst()) return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3));
        }
        return null;
    }

    public long register(String nama, String username, String password, String namaUsaha, String nomorHp, String alamat) {
        if (blank(nama) || blank(username) || blank(password)) throw new IllegalArgumentException("Nama, username, dan password wajib diisi.");
        if (password.length() < 6) throw new IllegalArgumentException("Password minimal 6 karakter.");
        ContentValues cv = new ContentValues();
        cv.put("nama", nama.trim());
        cv.put("username", username.trim());
        cv.put("password", PasswordUtil.sha256(password));
        cv.put("role", "PENGGUNA");
        cv.put("nama_usaha", text(namaUsaha));
        cv.put("nomor_hp", text(nomorHp));
        cv.put("alamat", text(alamat));
        cv.put("status", "AKTIF");
        long id = getWritableDatabase().insert("users", null, cv);
        if (id < 0) throw new IllegalArgumentException("Username sudah dipakai.");
        return id;
    }

    public Cursor profile(int userId) {
        return getReadableDatabase().rawQuery("SELECT id,nama,username,nama_usaha,nomor_hp,alamat FROM users WHERE id=?", new String[]{String.valueOf(userId)});
    }

    public void updateProfile(int userId, String nama, String namaUsaha, String nomorHp, String alamat) {
        if (blank(nama)) throw new IllegalArgumentException("Nama pengguna wajib diisi.");
        ContentValues cv = new ContentValues();
        cv.put("nama", nama.trim());
        cv.put("nama_usaha", text(namaUsaha));
        cv.put("nomor_hp", text(nomorHp));
        cv.put("alamat", text(alamat));
        getWritableDatabase().update("users", cv, "id=?", new String[]{String.valueOf(userId)});
    }

    public void tambahJenisIkan(String nama, String kategori, String deskripsi, String gambarPath) {
        if (blank(nama)) throw new IllegalArgumentException("Nama jenis ikan wajib diisi.");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("nama", nama.trim());
            cv.put("kategori", blank(kategori) ? "Ikan" : kategori.trim());
            cv.put("deskripsi", text(deskripsi));
            cv.put("gambar_path", text(gambarPath));
            long id = db.insert("jenis_ikan", null, cv);
            if (id < 0) throw new IllegalArgumentException("Jenis ikan sudah ada.");
            ContentValues stok = new ContentValues();
            stok.put("jenis_ikan_id", id);
            stok.put("total_kg", 0);
            stok.put("updated_at", DateUtil.now());
            db.insert("stok_mentah", null, stok);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void simpanSupplier(Integer id, String nama, String hp, String alamat, String catatan) {
        if (blank(nama)) throw new IllegalArgumentException("Nama supplier wajib diisi.");
        ContentValues cv = new ContentValues();
        cv.put("nama", nama.trim());
        cv.put("nomor_hp", text(hp));
        cv.put("alamat", text(alamat));
        cv.put("catatan", text(catatan));
        if (id == null) getWritableDatabase().insert("suppliers", null, cv);
        else getWritableDatabase().update("suppliers", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void hapusSupplier(int id) {
        if (scalar("SELECT COUNT(*) FROM stok_masuk WHERE supplier_id=?", String.valueOf(id)) > 0) {
            throw new IllegalArgumentException("Supplier sudah dipakai pada stok masuk, tidak bisa dihapus.");
        }
        getWritableDatabase().delete("suppliers", "id=?", new String[]{String.valueOf(id)});
    }

    public void simpanPelanggan(Integer id, String nama, String hp, String alamat, String tipe) {
        if (blank(nama)) throw new IllegalArgumentException("Nama pelanggan wajib diisi.");
        ContentValues cv = new ContentValues();
        cv.put("nama", nama.trim());
        cv.put("nomor_hp", text(hp));
        cv.put("alamat", text(alamat));
        cv.put("tipe_pelanggan", blank(tipe) ? "Retail" : tipe.trim());
        if (id == null) getWritableDatabase().insert("pelanggan", null, cv);
        else getWritableDatabase().update("pelanggan", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void hapusPelanggan(int id) {
        if (scalar("SELECT COUNT(*) FROM penjualan WHERE pelanggan_id=?", String.valueOf(id)) > 0) {
            throw new IllegalArgumentException("Pelanggan sudah dipakai pada penjualan, tidak bisa dihapus.");
        }
        getWritableDatabase().delete("pelanggan", "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor rawQuery(String sql, String... args) {
        return getReadableDatabase().rawQuery(sql, args);
    }

    public List<OptionItem> options(String table) {
        String label = table.equals("stok_giling") ? "batch_no || ' - ' || total_kg || ' kg'" : "nama";
        String sql = "SELECT id, " + label + " AS label FROM " + table + (table.equals("stok_giling") ? " WHERE total_kg>0" : "") + " ORDER BY id";
        List<OptionItem> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) list.add(new OptionItem(c.getInt(0), c.getString(1)));
        }
        return list;
    }

    public String dashboardText() {
        return "DASHBOARD CATOKAN\n" +
                "================\n\n" +
                "Total stok mentah  : " + scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah") + " kg\n" +
                "Total stok giling  : " + scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling") + " kg\n" +
                "Total penjualan    : Rp " + scalar("SELECT IFNULL(SUM(total),0) FROM penjualan") + "\n" +
                "Stok giling lama   : " + scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')") + " kg\n";
    }

    public String stokMentahText() {
        StringBuilder sb = new StringBuilder("STOK IKAN MENTAH\n================\n\n");
        String sql = "SELECT j.nama, s.total_kg FROM stok_mentah s JOIN jenis_ikan j ON j.id=s.jenis_ikan_id ORDER BY j.nama";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) sb.append(c.getString(0)).append(" : ").append(c.getDouble(1)).append(" kg\n");
        }
        return sb.toString();
    }

    public String stokGilingText() {
        StringBuilder sb = new StringBuilder("STOK IKAN GILING PER BATCH\n==========================\n\n");
        String sql = "SELECT g.id,g.batch_no,j.nama,g.total_kg,g.harga_jual_per_kg FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id ORDER BY g.id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) sb.append("ID ").append(c.getInt(0)).append(" | ").append(c.getString(1)).append(" | ").append(c.getString(2)).append(" | ").append(c.getDouble(3)).append(" kg | Rp ").append(c.getDouble(4)).append("/kg\n");
        }
        return sb.toString();
    }

    public double scalar(String sql, String... args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getDouble(0) : 0.0;
        }
    }

    public void exportJson(OutputStream outputStream) {
        try {
            JSONObject root = new JSONObject();
            root.put("nama_aplikasi", "CATOKAN");
            root.put("versi_data", 1);
            JSONObject tables = new JSONObject();
            SQLiteDatabase db = getReadableDatabase();
            for (String table : TRANSFER_TABLES) {
                JSONArray rows = new JSONArray();
                try (Cursor c = db.rawQuery("SELECT * FROM " + table, null)) {
                    while (c.moveToNext()) {
                        JSONObject row = new JSONObject();
                        for (int i = 0; i < c.getColumnCount(); i++) {
                            if (c.isNull(i)) row.put(c.getColumnName(i), JSONObject.NULL);
                            else if (c.getType(i) == Cursor.FIELD_TYPE_INTEGER) row.put(c.getColumnName(i), c.getLong(i));
                            else if (c.getType(i) == Cursor.FIELD_TYPE_FLOAT) row.put(c.getColumnName(i), c.getDouble(i));
                            else row.put(c.getColumnName(i), c.getString(i));
                        }
                        rows.put(row);
                    }
                }
                tables.put(table, rows);
            }
            root.put("tables", tables);
            outputStream.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception e) {
            throw new RuntimeException("Gagal export data: " + e.getMessage(), e);
        }
    }

    public int importJson(InputStream inputStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) != -1) buffer.write(chunk, 0, read);
            JSONObject root = new JSONObject(buffer.toString(StandardCharsets.UTF_8.name()));
            JSONObject tables = root.getJSONObject("tables");
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            int imported = 0;
            try {
                for (int i = TRANSFER_TABLES.length - 1; i >= 0; i--) db.delete(TRANSFER_TABLES[i], null, null);
                for (String table : TRANSFER_TABLES) {
                    JSONArray rows = tables.optJSONArray(table);
                    if (rows == null) continue;
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        ContentValues values = new ContentValues();
                        JSONArray names = row.names();
                        if (names == null) continue;
                        for (int j = 0; j < names.length(); j++) {
                            String name = names.getString(j);
                            Object value = row.get(name);
                            if (value == JSONObject.NULL) values.putNull(name);
                            else if (value instanceof Integer || value instanceof Long) values.put(name, ((Number) value).longValue());
                            else if (value instanceof Number) values.put(name, ((Number) value).doubleValue());
                            else values.put(name, String.valueOf(value));
                        }
                        db.insert(table, null, values);
                        imported++;
                    }
                }
                db.setTransactionSuccessful();
                return imported;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal import data: " + e.getMessage(), e);
        }
    }

    public int nextId(String table) {
        return (int) scalar("SELECT IFNULL(MAX(id),0)+1 FROM " + table);
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
