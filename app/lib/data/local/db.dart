import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

/// Database lokal untuk offline-first.
/// Schema HARUS mirror Postgres di `supabase/schema.sql`, dengan tambahan `synced_at` & `local_id`.
class AppDatabase {
  static AppDatabase? _instance;
  static Database? _db;

  static const _dbName = 'catokan_local.db';
  static const _dbVersion = 1;

  AppDatabase._();

  static AppDatabase get instance {
    _instance ??= AppDatabase._();
    return _instance!;
  }

  Future<Database> getDatabase() async {
    if (_db != null) return _db!;
    final dir = await getDatabasesPath();
    final path = join(dir, _dbName);
    _db = await openDatabase(
      path,
      version: _dbVersion,
      onCreate: (db, version) async {
        await _createSchema(db);
      },
      onUpgrade: (db, oldV, newV) async {
        // Untuk v2+: tambahkan migrasi di sini
      },
    );
    return _db!;
  }

  Future<void> close() async {
    await _db?.close();
    _db = null;
  }

  static Future<void> _createSchema(Database db) async {
    final batch = db.batch();

    // Semua tabel bisnis punya: id (UUID), local_id (auto), updated_at, synced_at (null = perlu push)
    batch.execute('''
      create table jenis_ikan (
        id text, usa_id text not null,
        nama text not null, aktif integer not null default 1,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table suppliers (
        id text, usa_id text not null,
        nama text not null, no_hp text, alamat text, catatan text,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table pelanggan (
        id text, usa_id text not null,
        nama text not null, no_hp text, alamat text, tipe text default 'Retail',
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table stok_mentah (
        id text, usa_id text not null,
        jenis_ikan_id text not null, total_kg real not null default 0,
        updated_at text not null, synced_at text,
        primary key (id), unique (usa_id, jenis_ikan_id)
      )
    ''');

    batch.execute('''
      create table stok_masuk (
        id text, usa_id text not null,
        tanggal text not null,
        jenis_ikan_id text not null, supplier_id text not null,
        berat_kg real not null, harga_beli_perkg real not null, total_beli real not null,
        status_bayar text not null default 'LUNAS',
        catatan text,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table piutang_supplier (
        id text, usa_id text not null,
        supplier_id text not null,
        tanggal text not null,
        jenis text not null, nominal real not null,
        stok_masuk_id text, catatan text,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table stok_giling (
        id text, usa_id text not null,
        batch_no text not null,
        jenis_ikan_id text not null,
        berat_mentah_kg real not null, berat_hasil_kg real not null,
        biaya_produksi real default 0, harga_jual_perkg real not null,
        tanggal_produksi text not null,
        jenis_batch text default 'FULL',
        sisa_kg real not null, status text default 'TERSEDIA',
        updated_at text not null, synced_at text,
        primary key (id), unique (usa_id, batch_no)
      )
    ''');

    batch.execute('''
      create table penjualan (
        id text, usa_id text not null,
        nomor_transaksi text not null,
        tanggal text not null,
        pelanggan_id text, user_id text,
        total real not null default 0,
        status_bayar text not null default 'LUNAS',
        catatan text,
        updated_at text not null, synced_at text,
        primary key (id), unique (usa_id, nomor_transaksi)
      )
    ''');

    batch.execute('''
      create table detail_penjualan (
        id text, usa_id text not null,
        penjualan_id text not null,
        stok_giling_id text not null,
        jenis_ikan_id text not null,
        jumlah_kg real not null, harga_perkg real not null,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table pembayaran_penjualan (
        id text, usa_id text not null,
        penjualan_id text not null,
        tanggal text not null, nominal real not null,
        metode text default 'Tunai', catatan text,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table koreksi_stok (
        id text, usa_id text not null,
        tanggal text not null,
        jenis_stok text not null, referensi_id text not null,
        stok_sistem real not null, stok_fisik real not null,
        alasan text not null,
        updated_at text not null, synced_at text,
        primary key (id)
      )
    ''');

    batch.execute('''
      create table riwayat_stok (
        id text, usa_id text not null,
        tanggal text not null,
        jenis_ikan_id text,
        jenis_transaksi text not null, jenis_stok text not null,
        referensi text,
        perubahan_kg real not null,
        stok_sebelum real not null, stok_sesudah real not null,
        keterangan text,
        synced_at text,
        primary key (id)
      )
    ''');

    // Index untuk FIFO query
    batch.execute('create index idx_stok_giling_fifo on stok_giling(usa_id, jenis_ikan_id, tanggal_produksi, id)');
    batch.execute('create index idx_riwayat_ago on riwayat_stok(usa_id, tanggal desc)');
    batch.execute('create index idx_penjualan_ago on penjualan(usa_id, tanggal desc)');

    await batch.commit(noResult: true);
  }
}