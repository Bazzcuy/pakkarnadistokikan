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

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "stok_ikan_giling_android.db";
    private static final int DB_VERSION = 2;

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, status TEXT DEFAULT 'AKTIF')");
        db.execSQL("CREATE TABLE jenis_ikan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT UNIQUE NOT NULL, kategori TEXT, deskripsi TEXT, aktif INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, catatan TEXT)");
        db.execSQL("CREATE TABLE pelanggan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, tipe_pelanggan TEXT)");
        db.execSQL("CREATE TABLE stok_mentah (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, updated_at TEXT)");
        db.execSQL("CREATE TABLE stok_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER NOT NULL, batch_no TEXT UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, harga_jual_per_kg REAL NOT NULL DEFAULT 0, tanggal_produksi TEXT, status_stok TEXT DEFAULT 'TERSEDIA')");
        db.execSQL("CREATE TABLE stok_masuk (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, supplier_id INTEGER NOT NULL, berat_kg REAL NOT NULL, harga_beli_per_kg REAL NOT NULL, total_beli REAL NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE produksi_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_no TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, berat_mentah_kg REAL NOT NULL, berat_hasil_kg REAL NOT NULL, penyusutan_kg REAL NOT NULL, biaya_produksi REAL DEFAULT 0, harga_jual_per_kg REAL NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, nomor_transaksi TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, pelanggan_id INTEGER, kasir_id INTEGER, subtotal REAL NOT NULL, diskon REAL DEFAULT 0, total REAL NOT NULL, status_pembayaran TEXT NOT NULL)");
        db.execSQL("CREATE TABLE detail_penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, stok_giling_id INTEGER NOT NULL, jenis_ikan_id INTEGER NOT NULL, jumlah_kg REAL NOT NULL, harga_per_kg REAL NOT NULL, subtotal REAL NOT NULL)");
        db.execSQL("CREATE TABLE pembayaran (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, tanggal TEXT NOT NULL, metode TEXT NOT NULL, jumlah_bayar REAL NOT NULL, sisa_bayar REAL NOT NULL, status TEXT NOT NULL, catatan TEXT)");
        db.execSQL("CREATE TABLE riwayat_stok (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_transaksi TEXT NOT NULL, jenis_stok TEXT NOT NULL, referensi TEXT, perubahan_kg REAL NOT NULL, stok_sebelum REAL NOT NULL, stok_sesudah REAL NOT NULL, keterangan TEXT)");
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
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

    private void seed(SQLiteDatabase db) {
        insertUser(db, "Administrator", "admin", "admin123", "ADMIN");
        insertUser(db, "Kasir", "kasir", "kasir123", "KASIR");
        insertUser(db, "Operator Produksi", "operator", "operator123", "OPERATOR");

        String[] ikan = {"Tenggiri", "Gabus", "Kakap", "Patin", "Lele", "Belida", "Nila", "Tongkol"};
        for (String s : ikan) {
            ContentValues cv = new ContentValues();
            cv.put("nama", s);
            cv.put("kategori", "Ikan");
            cv.put("deskripsi", "Jenis ikan " + s);
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
        g1.put("batch_no", "BG-DEMO-001");
        g1.put("total_kg", 12.5);
        g1.put("harga_jual_per_kg", 95000.0);
        g1.put("tanggal_produksi", DateUtil.today());
        g1.put("status_stok", "TERSEDIA");
        db.insert("stok_giling", null, g1);

        ContentValues g2 = new ContentValues();
        g2.put("jenis_ikan_id", 2);
        g2.put("batch_no", "BG-DEMO-002");
        g2.put("total_kg", 10.0);
        g2.put("harga_jual_per_kg", 80000.0);
        g2.put("tanggal_produksi", DateUtil.today());
        g2.put("status_stok", "TERSEDIA");
        db.insert("stok_giling", null, g2);

        seedGiling(db, 3, "BG-DEMO-003", 15.0, 72000.0, "2026-06-21");
        seedGiling(db, 4, "BG-DEMO-004", 18.0, 56000.0, "2026-06-22");
        seedGiling(db, 6, "BG-DEMO-005", 8.0, 110000.0, "2026-06-23");
        seedGiling(db, 1, "BG-DEMO-006", 22.0, 98000.0, "2026-06-24");
        seedGiling(db, 7, "BG-DEMO-007", 14.0, 52000.0, "2026-06-25");
        seedGiling(db, 8, "BG-DEMO-008", 16.0, 60000.0, "2026-06-25");
        seedGiling(db, 5, "BG-DEMO-009", 13.0, 42000.0, "2026-06-26");
        seedGiling(db, 3, "BG-DEMO-010", 9.0, 74000.0, "2026-06-26");

        seedStockIn(db, "2026-06-18", 1, 1, 45.0, 62000.0, "Pasokan awal tenggiri");
        seedStockIn(db, "2026-06-19", 2, 3, 30.0, 48000.0, "Gabus segar dari Musi");
        seedStockIn(db, "2026-06-21", 4, 4, 55.0, 35000.0, "Patin untuk stok mingguan");
        seedStockIn(db, "2026-06-22", 6, 6, 20.0, 76000.0, "Belida kualitas premium");
        seedStockIn(db, "2026-06-23", 7, 7, 40.0, 28000.0, "Nila untuk produksi ekonomis");
        seedStockIn(db, "2026-06-24", 8, 9, 38.0, 33000.0, "Tongkol luar kota");
        seedStockIn(db, "2026-06-25", 5, 10, 34.0, 24000.0, "Lele partai kecil");
        seedStockIn(db, "2026-06-26", 3, 6, 24.0, 45000.0, "Kakap tambahan");

        seedProduction(db, "BG-DEMO-001", "2026-06-20", 1, 15.0, 12.5, 85000.0, 95000.0, "Produksi demo tenggiri");
        seedProduction(db, "BG-DEMO-002", "2026-06-20", 2, 12.0, 10.0, 65000.0, 80000.0, "Produksi demo gabus");
        seedProduction(db, "BG-DEMO-003", "2026-06-21", 3, 18.0, 15.0, 75000.0, 72000.0, "Produksi demo kakap");
        seedProduction(db, "BG-DEMO-004", "2026-06-22", 4, 22.0, 18.0, 70000.0, 56000.0, "Produksi demo patin");
        seedProduction(db, "BG-DEMO-005", "2026-06-23", 6, 10.0, 8.0, 90000.0, 110000.0, "Produksi demo belida");
        seedProduction(db, "BG-DEMO-006", "2026-06-24", 1, 26.0, 22.0, 120000.0, 98000.0, "Produksi tenggiri batch besar");
        seedProduction(db, "BG-DEMO-007", "2026-06-25", 7, 17.0, 14.0, 60000.0, 52000.0, "Produksi nila ekonomis");
        seedProduction(db, "BG-DEMO-008", "2026-06-25", 8, 20.0, 16.0, 65000.0, 60000.0, "Produksi tongkol");
        seedProduction(db, "BG-DEMO-009", "2026-06-26", 5, 16.0, 13.0, 50000.0, 42000.0, "Produksi lele");
        seedProduction(db, "BG-DEMO-010", "2026-06-26", 3, 11.0, 9.0, 52000.0, 74000.0, "Produksi kakap tambahan");

        seedSale(db, "TRX-DEMO-001", "2026-06-24", 1, 2, 1, 2.0, 190000.0, "Tunai");
        seedSale(db, "TRX-DEMO-002", "2026-06-24", 2, 2, 3, 4.5, 200000.0, "Transfer");
        seedSale(db, "TRX-DEMO-003", "2026-06-25", 4, 2, 4, 6.0, 336000.0, "Tunai");
        seedSale(db, "TRX-DEMO-004", "2026-06-25", 6, 2, 6, 7.0, 686000.0, "Transfer");
        seedSale(db, "TRX-DEMO-005", "2026-06-26", 8, 2, 5, 1.5, 0.0, "Tempo");
        seedSale(db, "TRX-DEMO-006", "2026-06-26", 9, 2, 2, 3.0, 240000.0, "Tunai");
        seedSale(db, "TRX-DEMO-007", "2026-06-26", 10, 2, 7, 4.0, 208000.0, "Transfer");
        seedSale(db, "TRX-DEMO-008", "2026-06-27", 11, 2, 8, 5.0, 300000.0, "Tunai");
        seedSale(db, "TRX-DEMO-009", "2026-06-27", 12, 2, 9, 2.5, 50000.0, "Tempo");
        seedSale(db, "TRX-DEMO-010", "2026-06-27", 3, 2, 10, 2.0, 148000.0, "Transfer");
        seedSale(db, "TRX-DEMO-011", "2026-06-28", 5, 2, 1, 4.0, 380000.0, "Tunai");
        seedSale(db, "TRX-DEMO-012", "2026-06-28", 7, 2, 6, 3.0, 200000.0, "Transfer");
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
        double sisa = total - bayar;
        String status = sisa <= 0 ? "LUNAS" : "BELUM_LUNAS";
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
        pay.put("jumlah_bayar", bayar);
        pay.put("sisa_bayar", sisa);
        pay.put("status", status);
        pay.put("catatan", "Data dummy simulasi transaksi");
        db.insert("pembayaran", null, pay);

        double after = stok - kg;
        ContentValues upd = new ContentValues();
        upd.put("total_kg", after);
        upd.put("status_stok", after <= 0 ? "HABIS" : "TERSEDIA");
        db.update("stok_giling", upd, "id=?", new String[]{String.valueOf(stokGilingId)});

        ContentValues r = new ContentValues();
        r.put("tanggal", tanggal + " 10:00:00");
        r.put("jenis_transaksi", "PENJUALAN");
        r.put("jenis_stok", "GILING");
        r.put("referensi", nomor);
        r.put("perubahan_kg", -kg);
        r.put("stok_sebelum", stok);
        r.put("stok_sesudah", after);
        r.put("keterangan", "Penjualan dummy simulasi");
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
        db.insert("users", null, cv);
    }

    public User login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT id,nama,username,role FROM users WHERE username=? AND password IN (?,?) AND status='AKTIF'", new String[]{username, PasswordUtil.sha256(password), password})) {
            if (c.moveToFirst()) return new User(c.getInt(0), c.getString(1), c.getString(2), c.getString(3));
        }
        return null;
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
                "Sisa belum lunas   : Rp " + scalar("SELECT IFNULL(SUM(p.total - IFNULL(b.bayar,0)),0) FROM penjualan p LEFT JOIN (SELECT penjualan_id, SUM(jumlah_bayar) AS bayar FROM pembayaran GROUP BY penjualan_id) b ON b.penjualan_id=p.id WHERE p.status_pembayaran='BELUM_LUNAS'") + "\n";
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

    public int nextId(String table) {
        return (int) scalar("SELECT IFNULL(MAX(id),0)+1 FROM " + table);
    }
}
