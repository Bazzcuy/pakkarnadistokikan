/// Model-model master & transaksi.
/// Field `sn` = synced_at (null berarti belum push ke Supabase).

class JenisIkan {
  final String id;
  final String usaId;
  final String nama;
  final bool aktif;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  JenisIkan({
    required this.id,
    required this.usaId,
    required this.nama,
    this.aktif = true,
    required this.updatedAt,
    this.syncedAt,
  });

  factory JenisIkan.fromMap(Map<String, Object?> m) => JenisIkan(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        nama: m['nama'] as String,
        aktif: (m['aktif'] as int? ?? 1) == 1,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'nama': nama,
        'aktif': aktif ? 1 : 0,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class Supplier {
  final String id;
  final String usaId;
  final String nama;
  final String? noHp;
  final String? alamat;
  final String? catatan;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  Supplier({
    required this.id,
    required this.usaId,
    required this.nama,
    this.noHp,
    this.alamat,
    this.catatan,
    required this.updatedAt,
    this.syncedAt,
  });

  factory Supplier.fromMap(Map<String, Object?> m) => Supplier(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        nama: m['nama'] as String,
        noHp: m['no_hp'] as String?,
        alamat: m['alamat'] as String?,
        catatan: m['catatan'] as String?,
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'nama': nama,
        'no_hp': noHp,
        'alamat': alamat,
        'catatan': catatan,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}

class Pelanggan {
  final String id;
  final String usaId;
  final String nama;
  final String? noHp;
  final String? alamat;
  final String tipe;
  final DateTime updatedAt;
  final DateTime? syncedAt;

  Pelanggan({
    required this.id,
    required this.usaId,
    required this.nama,
    this.noHp,
    this.alamat,
    this.tipe = 'Retail',
    required this.updatedAt,
    this.syncedAt,
  });

  factory Pelanggan.fromMap(Map<String, Object?> m) => Pelanggan(
        id: m['id'] as String,
        usaId: m['usa_id'] as String,
        nama: m['nama'] as String,
        noHp: m['no_hp'] as String?,
        alamat: m['alamat'] as String?,
        tipe: m['tipe'] as String? ?? 'Retail',
        updatedAt: DateTime.parse(m['updated_at'] as String),
        syncedAt: m['synced_at'] == null ? null : DateTime.parse(m['synced_at'] as String),
      );

  Map<String, Object?> toMap() => {
        'id': id,
        'usa_id': usaId,
        'nama': nama,
        'no_hp': noHp,
        'alamat': alamat,
        'tipe': tipe,
        'updated_at': updatedAt.toIso8601String(),
        'synced_at': syncedAt?.toIso8601String(),
      };
}