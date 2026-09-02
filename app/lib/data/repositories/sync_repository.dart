import 'package:sqflite/sqflite.dart';

import '../local/db.dart';
import '../remote/supabase_client.dart';

/// Sinkronisasi 2 arah antara SQLite lokal ↔ Supabase Postgres.
/// Push: ambil semua row `synced_at IS NULL`, kirim via upsert, set synced_at = now.
/// Pull: ambil semua row dari Supabase, jika belum ada di lokal → insert.
///
/// Untuk v1 strategi konservatif (last-write-wins by updated_at) cocok dipakai
/// karena lo & emak jarang edit di detik yang sama.
class SyncRepository {
  final AppDatabase _db;
  SyncRepository(this._db);

  bool get _isOnline => true; // placeholder, bisa ditambah connectivity_plus nanti

  /// Push semua perubahan lokal ke Supabase.
  /// Returns jumlah row yang berhasil di-sync.
  Future<int> pushAll(String usaId) async {
    if (!_isOnline) return 0;
    final db = await _db.instance();
    int total = 0;

    // Urutan: master dulu, lalu header transaksi, lalu detail.
    final pushOrder = [
      'jenis_ikan',
      'suppliers',
      'pelanggan',
      'stok_masuk',
      'piutang_supplier',
      'stok_giling',
      'penjualan',
      'detail_penjualan',
      'pembayaran_penjualan',
      'koreksi_stok',
      'riwayat_stok',
    ];

    for (final table in pushOrder) {
      try {
        total += await _pushTable(db, table);
      } catch (e) {
        // Skip tabel yang gagal, lanjut ke berikutnya
        // ignore: avoid_print
        print('push $table error: $e');
      }
    }

    return total;
  }

  Future<int> _pushTable(Database db, String table) async {
    final rows = await db.query(table, where: 'synced_at is null');
    if (rows.isEmpty) return 0;

    // Map field dari local naming → Supabase naming.
    // Hapus kolom `synced_at` karena tidak ada di schema Supabase (cuma flag lokal).
    final mapped = rows.map((row) {
      final cleaned = Map<String, Object?>.from(row);
      cleaned.remove('synced_at');
      return _localToRemote(cleaned);
    }).toList();

    // Upsert by primary key
    await supabase.from(table).upsert(mapped, onConflict: 'id');

    // Mark semua sebagai synced di lokal
    final now = DateTime.now().toIso8601String();
    final ids = rows.map((r) => r['id']).toList();
    final placeholders = List.filled(ids.length, '?').join(',');
    await db.rawUpdate(
      'update $table set synced_at = ? where id in ($placeholders)',
      [now, ...ids],
    );

    return rows.length;
  }

  /// Pull semua data dari Supabase untuk user ini.
  Future<int> pullAll(String usaId) async {
    if (!_isOnline) return 0;
    final db = await _db.instance();
    int total = 0;

    final pullOrder = [
      'jenis_ikan',
      'suppliers',
      'pelanggan',
      'stok_masuk',
      'piutang_supplier',
      'stok_giling',
      'penjualan',
      'detail_penjualan',
      'pembayaran_penjualan',
      'koreksi_stok',
      'riwayat_stok',
    ];

    for (final table in pullOrder) {
      try {
        total += await _pullTable(db, table, usaId);
      } catch (e) {
        // ignore: avoid_print
        print('pull $table error: $e');
      }
    }

    return total;
  }

  Future<int> _pullTable(Database db, String table, String usaId) async {
    final remote = await supabase.from(table).select().eq('usa_id', usaId);
    if (remote.isEmpty) return 0;

    int synced = 0;
    final now = DateTime.now().toIso8601String();

    for (final row in remote) {
      final mapped = _remoteToLocal(row);
      final id = mapped['id'] as String;

      // Cek apakah sudah ada lokal dengan updated_at lebih baru
      final local = await db.query(table, where: 'id = ?', whereArgs: [id], limit: 1);

      if (local.isEmpty) {
        mapped['synced_at'] = now;
        await db.insert(table, mapped, conflictAlgorithm: ConflictAlgorithm.replace);
        synced++;
      } else {
        // Kalau remote lebih baru → timpa lokal
        final localUpdated = DateTime.parse(local.first['updated_at'] as String);
        final remoteUpdated = DateTime.parse(mapped['updated_at'] as String);
        if (remoteUpdated.isAfter(localUpdated)) {
          mapped['synced_at'] = now;
          await db.update(table, mapped, where: 'id = ?', whereArgs: [id]);
          synced++;
        }
      }
    }

    return synced;
  }

  /// Full sync: pull dulu (supaya dapat data dari device lain), baru push (kirim lokal).
  Future<({int pushed, int pulled})> syncAll(String usaId) async {
    final pulled = await pullAll(usaId);
    final pushed = await pushAll(usaId);
    return (pushed: pushed, pulled: pulled);
  }

  Map<String, Object?> _localToRemote(Map<String, Object?> row) {
    // Saat ini schema kolom identik. Kalau nanti beda → mapping di sini.
    return row;
  }

  Map<String, Object?> _remoteToLocal(Map<String, Object?> row) {
    // Sama: kolom identik
    return row;
  }
}