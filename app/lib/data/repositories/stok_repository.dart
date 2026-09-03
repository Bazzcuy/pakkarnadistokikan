import 'package:sqflite/sqflite.dart';
import 'package:uuid/uuid.dart';

import '../local/db.dart';
import '../models/transaksi.dart';
import '../models/master.dart';

/// Repository pusat untuk SEMUA operasi tulis yang mengubah stok/uang.
/// Prinsip utama: setiap mutasi dilakukan dalam SATU transaksi SQLite,
/// sesuai pola `beginTransaction()` di skripsi Android.
/// Setelah commit, kolom `synced_at` NULL → otomatis queue ke sync nanti.
class StokRepository {
  static const _uuid = Uuid();

  final AppDatabase _db;
  StokRepository(this._db);

  // ============================================================
  // BELI IKAN (stok masuk)
  // ============================================================
  Future<String> inputStokMasuk({
    required String usaId,
    required String jenisIkanId,
    required String supplierId,
    required double beratKg,
    required double hargaBeliPerkg,
    required String statusBayar, // 'LUNAS' atau 'UTANG'
    String? catatan,
  }) async {
    if (beratKg <= 0) throw ArgumentError('Berat harus lebih dari 0');
    if (hargaBeliPerkg <= 0) throw ArgumentError('Harga harus lebih dari 0');

    final db = await _db.getDatabase();
    final id = _uuid.v4();
    final now = DateTime.now();
    final tanggal = now.toIso8601String().substring(0, 10);
    final totalBeli = beratKg * hargaBeliPerkg;

    return await db.transaction((txn) async {
      // 1. Insert stok_masuk
      await txn.insert('stok_masuk', {
        'id': id,
        'usa_id': usaId,
        'tanggal': tanggal,
        'jenis_ikan_id': jenisIkanId,
        'supplier_id': supplierId,
        'berat_kg': beratKg,
        'harga_beli_perkg': hargaBeliPerkg,
        'total_beli': totalBeli,
        'status_bayar': statusBayar,
        'catatan': catatan,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });

      // 2. Update stok_mentah (UPSERT)
      final existing = await txn.query(
        'stok_mentah',
        where: 'usa_id = ? AND jenis_ikan_id = ?',
        whereArgs: [usaId, jenisIkanId],
        limit: 1,
      );

      double stokSebelum;
      double stokSesudah;
      if (existing.isEmpty) {
        stokSebelum = 0;
        stokSesudah = beratKg;
        await txn.insert('stok_mentah', {
          'id': _uuid.v4(),
          'usa_id': usaId,
          'jenis_ikan_id': jenisIkanId,
          'total_kg': stokSesudah,
          'updated_at': now.toIso8601String(),
          'synced_at': null,
        });
      } else {
        stokSebelum = (existing.first['total_kg'] as num).toDouble();
        stokSesudah = stokSebelum + beratKg;
        await txn.update(
          'stok_mentah',
          {
            'total_kg': stokSesudah,
            'updated_at': now.toIso8601String(),
            'synced_at': null,
          },
          where: 'id = ?',
          whereArgs: [existing.first['id']],
        );
      }

      // 3. Insert piutang_supplier kalau status UTANG
      if (statusBayar == 'UTANG') {
        await txn.insert('piutang_supplier', {
          'id': _uuid.v4(),
          'usa_id': usaId,
          'supplier_id': supplierId,
          'tanggal': tanggal,
          'jenis': 'TAMBAH',
          'nominal': totalBeli,
          'stok_masuk_id': id,
          'catatan': catatan ?? 'Utang dari pembelian',
          'updated_at': now.toIso8601String(),
          'synced_at': null,
        });
      }

      // 4. Catat di riwayat_stok
      await txn.insert('riwayat_stok', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'tanggal': now.toIso8601String(),
        'jenis_ikan_id': jenisIkanId,
        'jenis_transaksi': 'STOK_MASUK',
        'jenis_stok': 'MENTAH',
        'referensi': id,
        'perubahan_kg': beratKg,
        'stok_sebelum': stokSebelum,
        'stok_sesudah': stokSesudah,
        'keterangan': catatan,
        'synced_at': null,
      });

      return id;
    });
  }

  // ============================================================
  // GILING (produksi)
  // ============================================================
  Future<String> prosesProduksi({
    required String usaId,
    required String jenisIkanId,
    required double beratMentahKg,
    required double beratHasilKg,
    required double hargaJualPerkg,
    double biayaProduksi = 0,
    String jenisBatch = 'FULL',
    String? catatan,
  }) async {
    if (beratMentahKg <= 0 || beratHasilKg <= 0 || hargaJualPerkg <= 0) {
      throw ArgumentError('Input produksi belum valid');
    }
    if (beratHasilKg > beratMentahKg) {
      throw ArgumentError('Berat hasil tidak boleh melebihi berat mentah');
    }

    final db = await _db.getDatabase();
    final id = _uuid.v4();
    final now = DateTime.now();
    final tanggal = now.toIso8601String().substring(0, 10);

    // Generate batch_no (BG-YYYYMMDD-NNN) — sequence by count today
    final batchCount = Sqflite.firstIntValue(await db.rawQuery(
      "select count(*) from stok_giling where batch_no like 'BG-${tanggal.replaceAll('-', '')}%'",
    )) ?? 0;
    final batchNo = 'BG-${tanggal.replaceAll('-', '')}-${(batchCount + 1).toString().padLeft(3, '0')}';

    return await db.transaction((txn) async {
      // 1. Cek stok mentah cukup
      final stokRows = await txn.query(
        'stok_mentah',
        where: 'usa_id = ? AND jenis_ikan_id = ?',
        whereArgs: [usaId, jenisIkanId],
        limit: 1,
      );
      if (stokRows.isEmpty) {
        throw StateError('Stok mentah belum ada untuk jenis ikan ini');
      }
      final stokSebelum = (stokRows.first['total_kg'] as num).toDouble();
      if (stokSebelum < beratMentahKg) {
        throw StateError('Stok mentah tidak cukup. Tersedia: ${stokSebelum.toStringAsFixed(2)} kg');
      }
      final stokSesudah = stokSebelum - beratMentahKg;

      // 2. Kurangi stok_mentah
      await txn.update(
        'stok_mentah',
        {
          'total_kg': stokSesudah,
          'updated_at': now.toIso8601String(),
          'synced_at': null,
        },
        where: 'id = ?',
        whereArgs: [stokRows.first['id']],
      );

      // 3. Insert stok_giling (batch baru, sisa = berat_hasil)
      await txn.insert('stok_giling', {
        'id': id,
        'usa_id': usaId,
        'batch_no': batchNo,
        'jenis_ikan_id': jenisIkanId,
        'berat_mentah_kg': beratMentahKg,
        'berat_hasil_kg': beratHasilKg,
        'biaya_produksi': biayaProduksi,
        'harga_jual_perkg': hargaJualPerkg,
        'tanggal_produksi': tanggal,
        'jenis_batch': jenisBatch,
        'sisa_kg': beratHasilKg,
        'status': 'TERSEDIA',
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });

      // 4. Riwayat: produksi kurang mentah
      await txn.insert('riwayat_stok', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'tanggal': now.toIso8601String(),
        'jenis_ikan_id': jenisIkanId,
        'jenis_transaksi': 'PRODUKSI_KURANG_MENTAH',
        'jenis_stok': 'MENTAH',
        'referensi': batchNo,
        'perubahan_kg': -beratMentahKg,
        'stok_sebelum': stokSebelum,
        'stok_sesudah': stokSesudah,
        'keterangan': 'Bahan produksi',
        'synced_at': null,
      });

      // 5. Riwayat: produksi tambah giling
      await txn.insert('riwayat_stok', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'tanggal': now.toIso8601String(),
        'jenis_ikan_id': jenisIkanId,
        'jenis_transaksi': 'PRODUKSI_TAMBAH_GILING',
        'jenis_stok': 'GILING',
        'referensi': batchNo,
        'perubahan_kg': beratHasilKg,
        'stok_sebelum': 0,
        'stok_sesudah': beratHasilKg,
        'keterangan': 'Hasil produksi giling',
        'synced_at': null,
      });

      return batchNo;
    });
  }

  // ============================================================
  // JUAL (FIFO)
  // ============================================================
  Future<String> jualFifo({
    required String usaId,
    required String? userId,
    required String jenisIkanId,
    required String? pelangganId,
    required double jumlahKg,
    required String statusBayar,
    String? catatan,
  }) async {
    if (jumlahKg <= 0) throw ArgumentError('Jumlah kg harus lebih dari 0');

    final db = await _db.getDatabase();
    final penjualanId = _uuid.v4();
    final now = DateTime.now();
    final tanggal = now.toIso8601String().substring(0, 10);

    // Generate nomor transaksi
    final trxCount = Sqflite.firstIntValue(await db.rawQuery(
      "select count(*) from penjualan where nomor_transaksi like 'TRX-${tanggal.replaceAll('-', '')}%'",
    )) ?? 0;
    final nomorTrx = 'TRX-${tanggal.replaceAll('-', '')}-${(trxCount + 1).toString().padLeft(3, '0')}';

    return await db.transaction((txn) async {
      // 1. Hitung total tersedia
      final tersediaRows = await txn.rawQuery(
        "select ifnull(sum(sisa_kg),0) as total from stok_giling "
        "where usa_id = ? and jenis_ikan_id = ? and status = 'TERSEDIA'",
        [usaId, jenisIkanId],
      );
      final tersedia = (tersediaRows.first['total'] as num).toDouble();
      if (tersedia < jumlahKg) {
        throw StateError('Stok giling tidak cukup. Tersedia: ${tersedia.toStringAsFixed(2)} kg');
      }

      // 2. FIFO: ambil dari batch terlama
      final batches = await txn.query(
        'stok_giling',
        where: "usa_id = ? and jenis_ikan_id = ? and status = 'TERSEDIA' and sisa_kg > 0",
        whereArgs: [usaId, jenisIkanId],
        orderBy: 'tanggal_produksi asc, id asc',
      );

      double sisaAmbil = jumlahKg;
      double totalHarga = 0;
      final List<Map<String, dynamic>> details = [];

      for (final b in batches) {
        if (sisaAmbil <= 0) break;
        final batchId = b['id'] as String;
        final batchSisa = (b['sisa_kg'] as num).toDouble();
        final harga = (b['harga_jual_perkg'] as num).toDouble();
        final ambil = batchSisa < sisaAmbil ? batchSisa : sisaAmbil;

        totalHarga += ambil * harga;
        sisaAmbil -= ambil;

        details.add({
          'batch_id': batchId,
          'jumlah_kg': ambil,
          'harga_perkg': harga,
          'stok_sebelum': batchSisa,
          'stok_sesudah': batchSisa - ambil,
        });
      }

      // 3. Insert header penjualan
      await txn.insert('penjualan', {
        'id': penjualanId,
        'usa_id': usaId,
        'nomor_transaksi': nomorTrx,
        'tanggal': tanggal,
        'pelanggan_id': pelangganId,
        'user_id': userId,
        'total': totalHarga,
        'status_bayar': statusBayar,
        'catatan': catatan,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });

      // 4. Insert detail + update stok_giling + riwayat
      for (final d in details) {
        await txn.insert('detail_penjualan', {
          'id': _uuid.v4(),
          'usa_id': usaId,
          'penjualan_id': penjualanId,
          'stok_giling_id': d['batch_id'],
          'jenis_ikan_id': jenisIkanId,
          'jumlah_kg': d['jumlah_kg'],
          'harga_perkg': d['harga_perkg'],
          'updated_at': now.toIso8601String(),
          'synced_at': null,
        });

        final newSisa = d['stok_sesudah'] as double;
        await txn.update(
          'stok_giling',
          {
            'sisa_kg': newSisa,
            'status': newSisa <= 0 ? 'HABIS' : 'TERSEDIA',
            'updated_at': now.toIso8601String(),
            'synced_at': null,
          },
          where: 'id = ?',
          whereArgs: [d['batch_id']],
        );

        await txn.insert('riwayat_stok', {
          'id': _uuid.v4(),
          'usa_id': usaId,
          'tanggal': now.toIso8601String(),
          'jenis_ikan_id': jenisIkanId,
          'jenis_transaksi': 'PENJUALAN',
          'jenis_stok': 'GILING',
          'referensi': nomorTrx,
          'perubahan_kg': -(d['jumlah_kg'] as double),
          'stok_sebelum': d['stok_sebelum'] as double,
          'stok_sesudah': d['stok_sesudah'] as double,
          'keterangan': 'Penjualan FIFO',
          'synced_at': null,
        });
      }

      // 5. Jika status UTANG → insert pembayaran_penjualan otomatis 0 (sisa = total)
      // Actually, kalau UTANG berarti belum bayar sama sekali → tidak insert pembayaran.
      // Pembayaran akan diinput terpisah via "Terima Bayar" screen.

      return nomorTrx;
    });
  }

  // ============================================================
  // BAYAR UTANG SUPPLIER
  // ============================================================
  Future<void> bayarPiutangSupplier({
    required String usaId,
    required String supplierId,
    required double nominal,
    String? catatan,
  }) async {
    if (nominal <= 0) throw ArgumentError('Nominal harus lebih dari 0');

    final db = await _db.getDatabase();
    final now = DateTime.now();
    final tanggal = now.toIso8601String().substring(0, 10);

    await db.transaction((txn) async {
      await txn.insert('piutang_supplier', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'supplier_id': supplierId,
        'tanggal': tanggal,
        'jenis': 'BAYAR',
        'nominal': nominal,
        'catatan': catatan ?? 'Pembayaran utang supplier',
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });
    });
  }

  // ============================================================
  // TERIMA BAYAR PELANGGAN
  // ============================================================
  Future<void> terimaBayarPelanggan({
    required String usaId,
    required String penjualanId,
    required double nominal,
    String metode = 'Tunai',
    String? catatan,
  }) async {
    if (nominal <= 0) throw ArgumentError('Nominal harus lebih dari 0');

    final db = await _db.getDatabase();
    final now = DateTime.now();
    final tanggal = now.toIso8601String().substring(0, 10);

    await db.transaction((txn) async {
      await txn.insert('pembayaran_penjualan', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'penjualan_id': penjualanId,
        'tanggal': tanggal,
        'nominal': nominal,
        'metode': metode,
        'catatan': catatan,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });
    });
  }

  // ============================================================
  // KOREKSI STOK
  // ============================================================
  Future<void> koreksiStok({
    required String usaId,
    required String jenisStok, // 'MENTAH' atau 'GILING'
    required String referensiId,
    required double stokFisik,
    required String alasan,
  }) async {
    if (alasan.trim().isEmpty) throw ArgumentError('Alasan koreksi wajib diisi');
    if (stokFisik < 0) throw ArgumentError('Stok fisik tidak boleh negatif');

    final db = await _db.getDatabase();
    final now = DateTime.now();
    final koreksiId = _uuid.v4();

    await db.transaction((txn) async {
      double stokSistem;
      String? jenisIkanId;

      if (jenisStok == 'MENTAH') {
        final r = await txn.query('stok_mentah',
            where: 'id = ? and usa_id = ?', whereArgs: [referensiId, usaId], limit: 1);
        if (r.isEmpty) throw StateError('Stok mentah tidak ditemukan');
        stokSistem = (r.first['total_kg'] as num).toDouble();
        jenisIkanId = r.first['jenis_ikan_id'] as String;
        await txn.update(
          'stok_mentah',
          {
            'total_kg': stokFisik,
            'updated_at': now.toIso8601String(),
            'synced_at': null,
          },
          where: 'id = ?',
          whereArgs: [referensiId],
        );
      } else {
        final r = await txn.query('stok_giling',
            where: 'id = ? and usa_id = ?', whereArgs: [referensiId, usaId], limit: 1);
        if (r.isEmpty) throw StateError('Stok giling tidak ditemukan');
        stokSistem = (r.first['sisa_kg'] as num).toDouble();
        jenisIkanId = r.first['jenis_ikan_id'] as String;
        await txn.update(
          'stok_giling',
          {
            'sisa_kg': stokFisik,
            'status': stokFisik <= 0 ? 'HABIS' : 'TERSEDIA',
            'updated_at': now.toIso8601String(),
            'synced_at': null,
          },
          where: 'id = ?',
          whereArgs: [referensiId],
        );
      }

      final selisih = stokFisik - stokSistem;

      await txn.insert('koreksi_stok', {
        'id': koreksiId,
        'usa_id': usaId,
        'tanggal': now.toIso8601String(),
        'jenis_stok': jenisStok,
        'referensi_id': referensiId,
        'stok_sistem': stokSistem,
        'stok_fisik': stokFisik,
        'alasan': alasan,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });

      await txn.insert('riwayat_stok', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'tanggal': now.toIso8601String(),
        'jenis_ikan_id': jenisIkanId,
        'jenis_transaksi': 'KOREKSI_STOK',
        'jenis_stok': jenisStok,
        'referensi': koreksiId,
        'perubahan_kg': selisih,
        'stok_sebelum': stokSistem,
        'stok_sesudah': stokFisik,
        'keterangan': alasan,
        'synced_at': null,
      });
    });
  }

  // ============================================================
  // MASTER DATA — jenis_ikan, suppliers, pelanggan
  // ============================================================
  Future<List<JenisIkan>> listJenisIkan(String usaId) async {
    final db = await _db.getDatabase();
    final rows = await db.query(
      'jenis_ikan',
      where: 'usa_id = ? and aktif = 1',
      whereArgs: [usaId],
      orderBy: 'nama asc',
    );
    return rows.map(JenisIkan.fromMap).toList();
  }

  Future<List<Supplier>> listSuppliers(String usaId) async {
    final db = await _db.getDatabase();
    final rows = await db.query('suppliers', where: 'usa_id = ?', whereArgs: [usaId], orderBy: 'nama asc');
    return rows.map(Supplier.fromMap).toList();
  }

  Future<List<Pelanggan>> listPelanggan(String usaId) async {
    final db = await _db.getDatabase();
    final rows = await db.query('pelanggan', where: 'usa_id = ?', whereArgs: [usaId], orderBy: 'nama asc');
    return rows.map(Pelanggan.fromMap).toList();
  }

  Future<void> tambahJenisIkan(String usaId, String nama) async {
    final db = await _db.getDatabase();
    final now = DateTime.now();
    final id = _uuid.v4();
    await db.transaction((txn) async {
      await txn.insert('jenis_ikan', {
        'id': id,
        'usa_id': usaId,
        'nama': nama,
        'aktif': 1,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });
      // Inisialisasi stok_mentah = 0
      await txn.insert('stok_mentah', {
        'id': _uuid.v4(),
        'usa_id': usaId,
        'jenis_ikan_id': id,
        'total_kg': 0,
        'updated_at': now.toIso8601String(),
        'synced_at': null,
      });
    });
  }

  Future<void> tambahSupplier(String usaId, String nama, {String? noHp, String? alamat, String? catatan}) async {
    final db = await _db.getDatabase();
    await db.insert('suppliers', {
      'id': _uuid.v4(),
      'usa_id': usaId,
      'nama': nama,
      'no_hp': noHp,
      'alamat': alamat,
      'catatan': catatan,
      'updated_at': DateTime.now().toIso8601String(),
      'synced_at': null,
    });
  }

  Future<void> tambahPelanggan(String usaId, String nama, {String? noHp, String? alamat, String tipe = 'Retail'}) async {
    final db = await _db.getDatabase();
    await db.insert('pelanggan', {
      'id': _uuid.v4(),
      'usa_id': usaId,
      'nama': nama,
      'no_hp': noHp,
      'alamat': alamat,
      'tipe': tipe,
      'updated_at': DateTime.now().toIso8601String(),
      'synced_at': null,
    });
  }

  // ============================================================
  // READ — untuk dashboard & laporan
  // ============================================================
  Future<double> totalStokMentah(String usaId) async {
    final db = await _db.getDatabase();
    final r = await db.rawQuery(
      'select ifnull(sum(total_kg),0) as t from stok_mentah where usa_id = ?',
      [usaId],
    );
    return (r.first['t'] as num).toDouble();
  }

  Future<double> totalStokGiling(String usaId) async {
    final db = await _db.getDatabase();
    final r = await db.rawQuery(
      "select ifnull(sum(sisa_kg),0) as t from stok_giling where usa_id = ? and status = 'TERSEDIA'",
      [usaId],
    );
    return (r.first['t'] as num).toDouble();
  }

  Future<double> totalPenjualanHariIni(String usaId) async {
    final db = await _db.getDatabase();
    final today = DateTime.now().toIso8601String().substring(0, 10);
    final r = await db.rawQuery(
      "select ifnull(sum(total),0) as t from penjualan where usa_id = ? and tanggal = ?",
      [usaId, today],
    );
    return (r.first['t'] as num).toDouble();
  }

  /// Sisa utang per supplier (positif = emak berhutang).
  Future<List<Map<String, Object?>>> sisaUtangSupplier(String usaId) async {
    final db = await _db.getDatabase();
    return await db.rawQuery('''
      select s.id, s.nama,
        coalesce((
          select sum(case when p.jenis='TAMBAH' then p.nominal else -p.nominal end)
          from piutang_supplier p where p.supplier_id = s.id and p.usa_id = s.usa_id
        ), 0) as sisa
      from suppliers s
      where s.usa_id = ?
      order by sisa desc
    ''', [usaId]);
  }

  Future<double> totalSisaUtangSupplier(String usaId) async {
    final rows = await sisaUtangSupplier(usaId);
    double sum = 0;
    for (final r in rows) {
      sum += (r['sisa'] as num).toDouble();
    }
    return sum;
  }

  /// Sisa piutang per pelanggan (positif = pelanggan berhutang ke emak).
  Future<List<Map<String, Object?>>> sisaPiutangPelanggan(String usaId) async {
    final db = await _db.getDatabase();
    return await db.rawQuery('''
      select p.id, p.nama,
        coalesce(p.total, 0) - coalesce((
          select sum(b.nominal)
          from pembayaran_penjualan b
          join penjualan pj on pj.id = b.penjualan_id
          where pj.pelanggan_id = p.id and pj.usa_id = p.usa_id
        ), 0) as sisa
      from pelanggan p
      where p.usa_id = ?
      order by sisa desc
    ''', [usaId]);
  }

  Future<double> totalSisaPiutangPelanggan(String usaId) async {
    final rows = await sisaPiutangPelanggan(usaId);
    double sum = 0;
    for (final r in rows) {
      final v = (r['sisa'] as num).toDouble();
      if (v > 0) sum += v;
    }
    return sum;
  }

  Future<List<Map<String, Object?>>> listPenjualanWithSisa(String usaId) async {
    final db = await _db.getDatabase();
    return await db.rawQuery('''
      select pj.id, pj.nomor_transaksi, pj.tanggal, p.nama as pelanggan_nama, pj.total,
        coalesce((select sum(b.nominal) from pembayaran_penjualan b where b.penjualan_id = pj.id), 0) as dibayar
      from penjualan pj
      left join pelanggan p on p.id = pj.pelanggan_id
      where pj.usa_id = ?
      order by pj.tanggal desc, pj.id desc
      limit 200
    ''', [usaId]);
  }

  Future<List<RiwayatStok>> listRiwayat(String usaId, {int limit = 100}) async {
    final db = await _db.getDatabase();
    final rows = await db.query(
      'riwayat_stok',
      where: 'usa_id = ?',
      whereArgs: [usaId],
      orderBy: 'tanggal desc',
      limit: limit,
    );
    return rows.map(RiwayatStok.fromMap).toList();
  }

  /// Untuk screen koreksi: list semua stok_mentah per jenis ikan.
  Future<List<Map<String, Object?>>> listStokMentahForKoreksi(String usaId) async {
    final db = await _db.getDatabase();
    return await db.rawQuery('''
      select sm.id, ji.nama, sm.total_kg
      from stok_mentah sm
      join jenis_ikan ji on ji.id = sm.jenis_ikan_id
      where sm.usa_id = ?
      order by ji.nama
    ''', [usaId]);
  }

  Future<List<Map<String, Object?>>> listStokGilingForKoreksi(String usaId) async {
    final db = await _db.getDatabase();
    return await db.rawQuery('''
      select sg.id, ji.nama, sg.batch_no, sg.sisa_kg
      from stok_giling sg
      join jenis_ikan ji on ji.id = sg.jenis_ikan_id
      where sg.usa_id = ?
      order by sg.tanggal_produksi desc
    ''', [usaId]);
  }
}