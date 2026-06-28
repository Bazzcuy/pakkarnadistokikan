package com.bagas.stokikan.db;

import java.sql.*;
import java.util.*;

import com.bagas.stokikan.util.PasswordUtil;

public class Database {
    private static final String URL = "jdbc:sqlite:stok_ikan_giling_desktop.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, status TEXT DEFAULT 'AKTIF')");
            s.execute("CREATE TABLE IF NOT EXISTS jenis_ikan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT UNIQUE NOT NULL, kategori TEXT, deskripsi TEXT, aktif INTEGER DEFAULT 1)");
            s.execute("CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, catatan TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS pelanggan (id INTEGER PRIMARY KEY AUTOINCREMENT, nama TEXT NOT NULL, nomor_hp TEXT, alamat TEXT, tipe_pelanggan TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS stok_mentah (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, updated_at TEXT, FOREIGN KEY(jenis_ikan_id) REFERENCES jenis_ikan(id))");
            s.execute("CREATE TABLE IF NOT EXISTS stok_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, jenis_ikan_id INTEGER NOT NULL, batch_no TEXT UNIQUE NOT NULL, total_kg REAL NOT NULL DEFAULT 0, harga_jual_per_kg REAL NOT NULL DEFAULT 0, tanggal_produksi TEXT, status_stok TEXT DEFAULT 'TERSEDIA', FOREIGN KEY(jenis_ikan_id) REFERENCES jenis_ikan(id))");
            s.execute("CREATE TABLE IF NOT EXISTS stok_masuk (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, supplier_id INTEGER NOT NULL, berat_kg REAL NOT NULL, harga_beli_per_kg REAL NOT NULL, total_beli REAL NOT NULL, catatan TEXT, FOREIGN KEY(jenis_ikan_id) REFERENCES jenis_ikan(id), FOREIGN KEY(supplier_id) REFERENCES suppliers(id))");
            s.execute("CREATE TABLE IF NOT EXISTS produksi_giling (id INTEGER PRIMARY KEY AUTOINCREMENT, batch_no TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, jenis_ikan_id INTEGER NOT NULL, berat_mentah_kg REAL NOT NULL, berat_hasil_kg REAL NOT NULL, penyusutan_kg REAL NOT NULL, biaya_produksi REAL DEFAULT 0, harga_jual_per_kg REAL NOT NULL, catatan TEXT, FOREIGN KEY(jenis_ikan_id) REFERENCES jenis_ikan(id))");
            s.execute("CREATE TABLE IF NOT EXISTS penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, nomor_transaksi TEXT UNIQUE NOT NULL, tanggal TEXT NOT NULL, pelanggan_id INTEGER, kasir_id INTEGER, subtotal REAL NOT NULL, diskon REAL DEFAULT 0, total REAL NOT NULL, status_pembayaran TEXT NOT NULL, FOREIGN KEY(pelanggan_id) REFERENCES pelanggan(id), FOREIGN KEY(kasir_id) REFERENCES users(id))");
            s.execute("CREATE TABLE IF NOT EXISTS detail_penjualan (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, stok_giling_id INTEGER NOT NULL, jenis_ikan_id INTEGER NOT NULL, jumlah_kg REAL NOT NULL, harga_per_kg REAL NOT NULL, subtotal REAL NOT NULL, FOREIGN KEY(penjualan_id) REFERENCES penjualan(id), FOREIGN KEY(stok_giling_id) REFERENCES stok_giling(id), FOREIGN KEY(jenis_ikan_id) REFERENCES jenis_ikan(id))");
            s.execute("CREATE TABLE IF NOT EXISTS pembayaran (id INTEGER PRIMARY KEY AUTOINCREMENT, penjualan_id INTEGER NOT NULL, tanggal TEXT NOT NULL, metode TEXT NOT NULL, jumlah_bayar REAL NOT NULL, sisa_bayar REAL NOT NULL, status TEXT NOT NULL, catatan TEXT, FOREIGN KEY(penjualan_id) REFERENCES penjualan(id))");
            s.execute("CREATE TABLE IF NOT EXISTS penyesuaian_stok (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_stok TEXT NOT NULL, stok_sistem REAL NOT NULL, stok_fisik REAL NOT NULL, selisih REAL NOT NULL, alasan TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS riwayat_stok (id INTEGER PRIMARY KEY AUTOINCREMENT, tanggal TEXT NOT NULL, jenis_transaksi TEXT NOT NULL, jenis_stok TEXT NOT NULL, referensi TEXT, perubahan_kg REAL NOT NULL, stok_sebelum REAL NOT NULL, stok_sesudah REAL NOT NULL, keterangan TEXT)");
            seed(c);
        } catch (SQLException e) {
            throw new RuntimeException("Gagal inisialisasi database: " + e.getMessage(), e);
        }
    }

    private static boolean isEmpty(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seed(Connection c) throws SQLException {
        if (isEmpty(c, "users")) {
            execute(c, "INSERT INTO users(nama,username,password,role) VALUES(?,?,?,?)", "Administrator", "admin", PasswordUtil.sha256("admin123"), "ADMIN");
            execute(c, "INSERT INTO users(nama,username,password,role) VALUES(?,?,?,?)", "Kasir", "kasir", PasswordUtil.sha256("kasir123"), "KASIR");
            execute(c, "INSERT INTO users(nama,username,password,role) VALUES(?,?,?,?)", "Operator Produksi", "operator", PasswordUtil.sha256("operator123"), "OPERATOR");
        }
        if (isEmpty(c, "jenis_ikan")) {
            String[] names = {"Tenggiri", "Gabus", "Kakap", "Patin", "Lele", "Belida", "Nila", "Tongkol"};
            for (String n : names) execute(c, "INSERT INTO jenis_ikan(nama,kategori,deskripsi) VALUES(?,?,?)", n, "Ikan", "Jenis ikan " + n);
        }
        if (isEmpty(c, "suppliers")) {
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
            for (String[] s : suppliers) execute(c, "INSERT INTO suppliers(nama,nomor_hp,alamat,catatan) VALUES(?,?,?,?)", s[0], s[1], s[2], s[3]);
        }
        if (isEmpty(c, "pelanggan")) {
            String[][] customers = {
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
            for (String[] p : customers) execute(c, "INSERT INTO pelanggan(nama,nomor_hp,alamat,tipe_pelanggan) VALUES(?,?,?,?)", p[0], p[1], p[2], p[3]);
        }
        if (isEmpty(c, "stok_mentah")) {
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id FROM jenis_ikan")) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    double total = switch (id) {
                        case 1 -> 68.5;
                        case 2 -> 44.0;
                        case 3 -> 37.5;
                        case 4 -> 52.0;
                        case 5 -> 31.0;
                        case 6 -> 18.0;
                        case 7 -> 29.5;
                        default -> 41.0;
                    };
                    execute(c, "INSERT INTO stok_mentah(jenis_ikan_id,total_kg,updated_at) VALUES(?,?,datetime('now'))", id, total);
                }
            }
        }
        if (isEmpty(c, "stok_giling")) {
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 1, "BG-DEMO-001", 12.5, 95000.0, "2026-06-20");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 2, "BG-DEMO-002", 10.0, 80000.0, "2026-06-20");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 3, "BG-DEMO-003", 15.0, 72000.0, "2026-06-21");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 4, "BG-DEMO-004", 18.0, 56000.0, "2026-06-22");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 6, "BG-DEMO-005", 8.0, 110000.0, "2026-06-23");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 1, "BG-DEMO-006", 22.0, 98000.0, "2026-06-24");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 7, "BG-DEMO-007", 14.0, 52000.0, "2026-06-25");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 8, "BG-DEMO-008", 16.0, 60000.0, "2026-06-25");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 5, "BG-DEMO-009", 13.0, 42000.0, "2026-06-26");
            execute(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi) VALUES(?,?,?,?,?)", 3, "BG-DEMO-010", 9.0, 74000.0, "2026-06-26");
        }
        if (isEmpty(c, "stok_masuk")) {
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-18", 1, 1, 45.0, 62000.0, 2790000.0, "Pasokan awal tenggiri");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-19", 2, 3, 30.0, 48000.0, 1440000.0, "Gabus segar dari Musi");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-21", 4, 4, 55.0, 35000.0, 1925000.0, "Patin untuk stok mingguan");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-22", 6, 6, 20.0, 76000.0, 1520000.0, "Belida kualitas premium");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-23", 7, 7, 40.0, 28000.0, 1120000.0, "Nila untuk produksi ekonomis");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-24", 8, 9, 38.0, 33000.0, 1254000.0, "Tongkol luar kota");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-25", 5, 10, 34.0, 24000.0, 816000.0, "Lele partai kecil");
            execute(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", "2026-06-26", 3, 6, 24.0, 45000.0, 1080000.0, "Kakap tambahan");
        }
        if (isEmpty(c, "produksi_giling")) {
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-001", "2026-06-20", 1, 15.0, 12.5, 2.5, 85000.0, 95000.0, "Produksi demo tenggiri");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-002", "2026-06-20", 2, 12.0, 10.0, 2.0, 65000.0, 80000.0, "Produksi demo gabus");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-003", "2026-06-21", 3, 18.0, 15.0, 3.0, 75000.0, 72000.0, "Produksi demo kakap");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-004", "2026-06-22", 4, 22.0, 18.0, 4.0, 70000.0, 56000.0, "Produksi demo patin");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-005", "2026-06-23", 6, 10.0, 8.0, 2.0, 90000.0, 110000.0, "Produksi demo belida");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-006", "2026-06-24", 1, 26.0, 22.0, 4.0, 120000.0, 98000.0, "Produksi tenggiri batch besar");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-007", "2026-06-25", 7, 17.0, 14.0, 3.0, 60000.0, 52000.0, "Produksi nila ekonomis");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-008", "2026-06-25", 8, 20.0, 16.0, 4.0, 65000.0, 60000.0, "Produksi tongkol");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-009", "2026-06-26", 5, 16.0, 13.0, 3.0, 50000.0, 42000.0, "Produksi lele");
            execute(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", "BG-DEMO-010", "2026-06-26", 3, 11.0, 9.0, 2.0, 52000.0, 74000.0, "Produksi kakap tambahan");
        }
        if (isEmpty(c, "penjualan")) {
            seedSale(c, "TRX-DEMO-001", "2026-06-24", 1, 2, 1, 2.0, 190000.0, "Tunai");
            seedSale(c, "TRX-DEMO-002", "2026-06-24", 2, 2, 3, 4.5, 200000.0, "Transfer");
            seedSale(c, "TRX-DEMO-003", "2026-06-25", 4, 2, 4, 6.0, 336000.0, "Tunai");
            seedSale(c, "TRX-DEMO-004", "2026-06-25", 6, 2, 6, 7.0, 686000.0, "Transfer");
            seedSale(c, "TRX-DEMO-005", "2026-06-26", 8, 2, 5, 1.5, 0.0, "Tempo");
            seedSale(c, "TRX-DEMO-006", "2026-06-26", 9, 2, 2, 3.0, 240000.0, "Tunai");
            seedSale(c, "TRX-DEMO-007", "2026-06-26", 10, 2, 7, 4.0, 208000.0, "Transfer");
            seedSale(c, "TRX-DEMO-008", "2026-06-27", 11, 2, 8, 5.0, 300000.0, "Tunai");
            seedSale(c, "TRX-DEMO-009", "2026-06-27", 12, 2, 9, 2.5, 50000.0, "Tempo");
            seedSale(c, "TRX-DEMO-010", "2026-06-27", 3, 2, 10, 2.0, 148000.0, "Transfer");
            seedSale(c, "TRX-DEMO-011", "2026-06-28", 5, 2, 1, 4.0, 380000.0, "Tunai");
            seedSale(c, "TRX-DEMO-012", "2026-06-28", 7, 2, 6, 3.0, 200000.0, "Transfer");
        }
    }

    private static void seedSale(Connection c, String nomor, String tanggal, int pelangganId, int kasirId, int stokGilingId, double kg, double bayar, String metode) throws SQLException {
        double stok = scalarDouble(c, "SELECT total_kg FROM stok_giling WHERE id=?", stokGilingId);
        double harga = scalarDouble(c, "SELECT harga_jual_per_kg FROM stok_giling WHERE id=?", stokGilingId);
        int jenisId = (int) scalarDouble(c, "SELECT jenis_ikan_id FROM stok_giling WHERE id=?", stokGilingId);
        double total = kg * harga;
        double sisa = total - bayar;
        String status = sisa <= 0 ? "LUNAS" : "BELUM_LUNAS";
        int penjualanId = insertAndGetId(c, "INSERT INTO penjualan(nomor_transaksi,tanggal,pelanggan_id,kasir_id,subtotal,diskon,total,status_pembayaran) VALUES(?,?,?,?,?,?,?,?)", nomor, tanggal, pelangganId, kasirId, total, 0, total, status);
        insertAndGetId(c, "INSERT INTO detail_penjualan(penjualan_id,stok_giling_id,jenis_ikan_id,jumlah_kg,harga_per_kg,subtotal) VALUES(?,?,?,?,?,?)", penjualanId, stokGilingId, jenisId, kg, harga, total);
        insertAndGetId(c, "INSERT INTO pembayaran(penjualan_id,tanggal,metode,jumlah_bayar,sisa_bayar,status,catatan) VALUES(?,?,?,?,?,?,?)", penjualanId, tanggal, metode, bayar, sisa, status, "Data dummy simulasi transaksi");
        double after = stok - kg;
        execute(c, "UPDATE stok_giling SET total_kg=?, status_stok=? WHERE id=?", after, after <= 0 ? "HABIS" : "TERSEDIA", stokGilingId);
        execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?)", tanggal + " 10:00:00", "PENJUALAN", "GILING", nomor, -kg, stok, after, "Penjualan dummy simulasi");
    }

    public static void execute(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        }
    }

    public static int insertAndGetId(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public static List<Map<String, Object>> query(String sql, Object... params) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static double scalarDouble(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }

    public static int nextId(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT IFNULL(MAX(id),0)+1 FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 1;
        }
    }
}
