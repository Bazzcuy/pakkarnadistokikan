/// Model untuk transaksi bisnis (stok masuk, produksi, jual, dll).

class StokMasuk {
  final String id;
  final String usaId;
  final DateTime tanggalTr;
  final String jenisIkanId;
  final String supplierId;
  final double beratKg;
  final double hargaBeliPerkg;
  final double totalBeli;
  final String statusBayar;
  final String? catatan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  StokMasuk({
    required this.id,
    required this.usaId,
    required this.tanggalTr,
    required this.jenisIkanId,
    required this.supplierId,
    required this.beratKg,
    required this.hargaBeliPerkg,
    required this.totalBeli,
    required this.statusBayar,
    this.catatan,
    required this.updatedAt,
    this.syncedAt,
  });

  factory StokMasuk.fromMap(Map<String, Object?> m) => StokMasuk(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        tanggalTr: DateTime.parse(m['tanggal'] as String),
        jenisIkanId: m['jenis_ikan_id'] as String,
        supplierId: m['supplier_id'] as String,
        beratKg: (m['berat_kg'] as num).toDouble(),
        hargaBeliPerkg: (m['harga_beli_perkg'] as num).toDouble(),
        totalBeli: (m['total_beli'] as num).toDouble(),
        statusBayar: m['status_bayar'] as String,
        catatan: m['catatan'] as String?,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'tanggal': tanggalTr.toIso8601String().substring(0, 10),
        'jenis_ikan_id': jenisIkanId,
        'supplier_id': supplierId,
        'berat_kg': beratKg,
        'harga_beli_perkg': hargaBeliPerkg,
        'total_beli': totalBeli,
        'status_bayar': statusBayar,
        'catatan': catatan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class PiutangSupplier {
  final String id;
  final String usaId;
  final String supplierId;
  final DateTime tanggal;
  final String jenis; // 'TAMBAH' atau 'BAYAR'
  final double nominal;
  final String? stokMasukId;
  final String? catatan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  PiutangSupplier({
    required this.id,
    required this.usaId,
    required this.supplierId,
    required this.tanggal,
    required this.jenis,
    required this.nominal,
    this.stokMasukId,
    this.catatan,
    required this.updatedAt,
    this.syncedAt,
  });

  factory PiutangSupplier.fromMap(Map<String, Object?> m) => PiutangSupplier(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        supplierId: m['supplier_id'] as String,
        tanggal: DateTime.parse(m['tanggal'] as String),
        jenis: m['jenis'] as String,
        nominal: (m['nominal'] as num).toDouble(),
        stokMasukId: m['stok_masuk_id'] as String?,
        catatan: m['catatan'] as String?,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'supplier_id': supplierId,
        'tanggal': tanggal.toIso8601String().substring(0, 10),
        'jenis': jenis,
        'nominal': nominal,
        'stok_masuk_id': stokMasukId,
        'catatan': catatan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class StokGiling {
  final String id;
  final String usaId;
  final String batchNo;
  final String jenisIkanId;
  final double beratMentahKg;
  final double beratHasilKg;
  final double biayaProduksi;
  final double hargaJualPerkg;
  final DateTime tanggalProduksi;
  final String jenisBatch;
  final double sisaKg;
  final String status;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  StokGiling({
    required this.id,
    required this.usaId,
    required this.batchNo,
    required this.jenisIkanId,
    required this.beratMentahKg,
    required this.beratHasilKg,
    this.biayaProduksi = 0,
    required this.hargaJualPerkg,
    required this.tanggalProduksi,
    this.jenisBatch = 'FULL',
    required this.sisaKg,
    this.status = 'TERSEDIA',
    required this.updatedAt,
    this.syncedAt,
  });

  double get penyusutanKg => beratMentahKg - beratHasilKg;

  factory StokGiling.fromMap(Map<String, Object?> m) => StokGiling(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        batchNo: m['batch_no'] as String,
        jenisIkanId: m['jenis_ikan_id'] as String,
        beratMentahKg: (m['berat_mentah_kg'] as num).toDouble(),
        beratHasilKg: (m['berat_hasil_kg'] as num).toDouble(),
        biayaProduksi: (m['biaya_produksi'] as num?)?.toDouble() ?? 0,
        hargaJualPerkg: (m['harga_jual_perkg'] as num).toDouble(),
        tanggalProduksi: DateTime.parse(m['tanggal_produksi'] as String),
        jenisBatch: m['jenis_batch'] as String? ?? 'FULL',
        sisaKg: (m['sisa_kg'] as num).toDouble(),
        status: m['status'] as String? ?? 'TERSEDIA',
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'batch_no': batchNo,
        'jenis_ikan_id': jenisIkanId,
        'berat_mentah_kg': beratMentahKg,
        'berat_hasil_kg': beratHasilKg,
        'biaya_produksi': biayaProduksi,
        'harga_jual_perkg': hargaJualPerkg,
        'tanggal_produksi': tanggalProduksi.toIso8601String().substring(0, 10),
        'jenis_batch': jenisBatch,
        'sisa_kg': sisaKg,
        'status': status,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class Penjualan {
  final String id;
  final String usaId;
  final String nomorTransaksi;
  final DateTime tanggal;
  final String? pelangganId;
  final String? userId;
  final double total;
  final String statusBayar;
  final String? catatan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  Penjualan({
    required this.id,
    required this.usaId,
    required this.nomorTransaksi,
    required this.tanggal,
    this.pelangganId,
    this.userId,
    required this.total,
    required this.statusBayar,
    this.catatan,
    required this.updatedAt,
    this.syncedAt,
  });

  factory Penjualan.fromMap(Map<String, Object?> m) => Penjualan(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        nomorTransaksi: m['nomor_transaksi'] as String,
        tanggal: DateTime.parse(m['tanggal'] as String),
        pelangganId: m['pelanggan_id'] as String?,
        userId: m['user_id'] as String?,
        total: (m['total'] as num).toDouble(),
        statusBayar: m['status_bayar'] as String,
        catatan: m['catatan'] as String?,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'nomor_transaksi': nomorTransaksi,
        'tanggal': tanggal.toIso8601String().substring(0, 10),
        'pelanggan_id': pelangganId,
        'user_id': userId,
        'total': total,
        'status_bayar': statusBayar,
        'catatan': catatan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class DetailPenjualan {
  final String id;
  final String usaId;
  final String penjualanId;
  final String stokGilingId;
  final String jenisIkanId;
  final double jumlahKg;
  final double hargaPerkg;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  DetailPenjualan({
    required this.id,
    required this.usaId,
    required this.penjualanId,
    required this.stokGilingId,
    required this.jenisIkanId,
    required this.jumlahKg,
    required this.hargaPerkg,
    required this.updatedAt,
    this.syncedAt,
  });

  double get subtotal => jumlahKg * hargaPerkg;

  factory DetailPenjualan.fromMap(Map<String, Object?> m) => DetailPenjualan(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        penjualanId: m['penjualan_id'] as String,
        stokGilingId: m['stok_giling_id'] as String,
        jenisIkanId: m['jenis_ikan_id'] as String,
        jumlahKg: (m['jumlah_kg'] as num).toDouble(),
        hargaPerkg: (m['harga_perkg'] as num).toDouble(),
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'penjualan_id': penjualanId,
        'stok_giling_id': stokGilingId,
        'jenis_ikan_id': jenisIkanId,
        'jumlah_kg': jumlahKg,
        'harga_perkg': hargaPerkg,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class PembayaranPenjualan {
  final String id;
  final String usaId;
  final String penjualanId;
  final DateTime tanggal;
  final double nominal;
  final String metode;
  final String? catatan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  PembayaranPenjualan({
    required this.id,
    required this.usaId,
    required this.penjualanId,
    required this.tanggal,
    required this.nominal,
    this.metode = 'Tunai',
    this.catatan,
    required this.updatedAt,
    this.syncedAt,
  });

  factory PembayaranPenjualan.fromMap(Map<String, Object?> m) => PembayaranPenjualan(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        penjualanId: m['penjualan_id'] as String,
        tanggal: DateTime.parse(m['tanggal'] as String),
        nominal: (m['nominal'] as num).toDouble(),
        metode: m['metode'] as String? ?? 'Tunai',
        catatan: m['catatan'] as String?,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'penjualan_id': penjualanId,
        'tanggal': tanggal.toIso8601String().substring(0, 10),
        'nominal': nominal,
        'metode': metode,
        'catatan': catatan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class RiwayatStok {
  final String id;
  final String usaId;
  final DateTime tanggal;
  final String? jenisIkanId;
  final String jenisTransaksi;
  final String jenisStok;
  final String? referensi;
  final double perubahanKg;
  final double stokSebelum;
  final double stokSesudah;
  final String? keterangan;
  final DateTime? syncedAt;

  RiwayatStok({
    required this.id,
    required this.usaId,
    required this.tanggal,
    this.jenisIkanId,
    required this.jenisTransaksi,
    required this.jenisStok,
    this.referensi,
    required this.perubahanKg,
    required this.stokSebelum,
    required this.stokSesudah,
    this.keterangan,
    this.syncedAt,
  });

  factory RiwayatStok.fromMap(Map<String, Object?> m) => RiwayatStok(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        tanggal: DateTime.parse(m['tanggal'] as String),
        jenisIkanId: m['jenis_ikan_id'] as String?,
        jenisTransaksi: m['jenis_transaksi'] as String,
        jenisStok: m['jenis_stok'] as String,
        referensi: m['referensi'] as String?,
        perubahanKg: (m['perubahan_kg'] as num).toDouble(),
        stokSebelum: (m['stok_sebelum'] as num).toDouble(),
        stokSesudah: (m['stok_sesudah'] as num).toDouble(),
        keterangan: m['keterangan'] as String?,
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'tanggal': tanggal.toIso8601String(),
        'jenis_ikan_id': jenisIkanId,
        'jenis_transaksi': jenisTransaksi,
        'jenis_stok': jenisStok,
        'referensi': referensi,
        'perubahan_kg': perubahanKg,
        'stok_sebelum': stokSebelum,
        'stok_sesudah': stokSesudah,
        'keterangan': keterangan,
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class KoreksiStok {
  final String id;
  final String usaId;
  final DateTime tanggal;
  final String jenisStok;
  final String referensiId;
  final double stokSistem;
  final double stokFisik;
  final String alasan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  KoreksiStok({
    required this.id,
    required this.usaId,
    required this.tanggal,
    required this.jenisStok,
    required this.referensiId,
    required this.stokSistem,
    required this.stokFisik,
    required this.alasan,
    required this.updatedAt,
    this.syncedAt,
  });

  double get selisih => stokFisik - stokSistem;

  factory KoreksiStok.fromMap(Map<String, Object?> m) => KoreksiStok(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        tanggal: DateTime.parse(m['tanggal'] as String),
        jenisStok: m['jenis_stok'] as String,
        referensiId: m['referensi_id'] as String,
        stokSistem: (m['stok_sistem'] as num).toDouble(),
        stokFisik: (m['stok_fisik'] as num).toDouble(),
        alasan: m['alasan'] as String,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'tanggal': tanggal.toIso8601String(),
        'jenis_stok': jenisStok,
        'referensi_id': referensiId,
        'stok_sistem': stokSistem,
        'stok_fisik': stokFisik,
        'alasan': alasan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}