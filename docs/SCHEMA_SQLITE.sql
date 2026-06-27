-- SQLite schema Sistem Stok Ikan Giling

CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nama TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    status TEXT DEFAULT 'AKTIF'
);

CREATE TABLE jenis_ikan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nama TEXT NOT NULL UNIQUE,
    kategori TEXT,
    deskripsi TEXT,
    aktif INTEGER DEFAULT 1
);

CREATE TABLE suppliers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nama TEXT NOT NULL,
    nomor_hp TEXT,
    alamat TEXT,
    catatan TEXT
);

CREATE TABLE pelanggan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nama TEXT NOT NULL,
    nomor_hp TEXT,
    alamat TEXT,
    tipe_pelanggan TEXT
);

CREATE TABLE stok_mentah (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jenis_ikan_id INTEGER NOT NULL,
    total_kg REAL NOT NULL DEFAULT 0,
    updated_at TEXT,
    FOREIGN KEY (jenis_ikan_id) REFERENCES jenis_ikan(id)
);

CREATE TABLE stok_giling (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    jenis_ikan_id INTEGER NOT NULL,
    batch_no TEXT NOT NULL UNIQUE,
    total_kg REAL NOT NULL DEFAULT 0,
    harga_jual_per_kg REAL NOT NULL DEFAULT 0,
    tanggal_produksi TEXT,
    status_stok TEXT DEFAULT 'TERSEDIA',
    FOREIGN KEY (jenis_ikan_id) REFERENCES jenis_ikan(id)
);

CREATE TABLE stok_masuk (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tanggal TEXT NOT NULL,
    jenis_ikan_id INTEGER NOT NULL,
    supplier_id INTEGER NOT NULL,
    berat_kg REAL NOT NULL,
    harga_beli_per_kg REAL NOT NULL,
    total_beli REAL NOT NULL,
    catatan TEXT,
    FOREIGN KEY (jenis_ikan_id) REFERENCES jenis_ikan(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE produksi_giling (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_no TEXT NOT NULL UNIQUE,
    tanggal TEXT NOT NULL,
    jenis_ikan_id INTEGER NOT NULL,
    berat_mentah_kg REAL NOT NULL,
    berat_hasil_kg REAL NOT NULL,
    penyusutan_kg REAL NOT NULL,
    biaya_produksi REAL DEFAULT 0,
    harga_jual_per_kg REAL NOT NULL,
    catatan TEXT,
    FOREIGN KEY (jenis_ikan_id) REFERENCES jenis_ikan(id)
);

CREATE TABLE penjualan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nomor_transaksi TEXT NOT NULL UNIQUE,
    tanggal TEXT NOT NULL,
    pelanggan_id INTEGER,
    kasir_id INTEGER,
    subtotal REAL NOT NULL,
    diskon REAL DEFAULT 0,
    total REAL NOT NULL,
    status_pembayaran TEXT NOT NULL,
    FOREIGN KEY (pelanggan_id) REFERENCES pelanggan(id),
    FOREIGN KEY (kasir_id) REFERENCES users(id)
);

CREATE TABLE detail_penjualan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    penjualan_id INTEGER NOT NULL,
    stok_giling_id INTEGER NOT NULL,
    jenis_ikan_id INTEGER NOT NULL,
    jumlah_kg REAL NOT NULL,
    harga_per_kg REAL NOT NULL,
    subtotal REAL NOT NULL,
    FOREIGN KEY (penjualan_id) REFERENCES penjualan(id),
    FOREIGN KEY (stok_giling_id) REFERENCES stok_giling(id),
    FOREIGN KEY (jenis_ikan_id) REFERENCES jenis_ikan(id)
);

CREATE TABLE pembayaran (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    penjualan_id INTEGER NOT NULL,
    tanggal TEXT NOT NULL,
    metode TEXT NOT NULL,
    jumlah_bayar REAL NOT NULL,
    sisa_bayar REAL NOT NULL,
    status TEXT NOT NULL,
    catatan TEXT,
    FOREIGN KEY (penjualan_id) REFERENCES penjualan(id)
);

CREATE TABLE penyesuaian_stok (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tanggal TEXT NOT NULL,
    jenis_stok TEXT NOT NULL,
    stok_sistem REAL NOT NULL,
    stok_fisik REAL NOT NULL,
    selisih REAL NOT NULL,
    alasan TEXT
);

CREATE TABLE riwayat_stok (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tanggal TEXT NOT NULL,
    jenis_transaksi TEXT NOT NULL,
    jenis_stok TEXT NOT NULL,
    referensi TEXT,
    perubahan_kg REAL NOT NULL,
    stok_sebelum REAL NOT NULL,
    stok_sesudah REAL NOT NULL,
    keterangan TEXT
);
