/// Generator ID human-readable, port dari DateUtil skripsi Android.
/// BG-YYYYMMDD-NNN untuk batch giling, TRX-YYYYMMDD-NNN untuk transaksi jual.
library;

String batchNumber(int seq) {
  final now = DateTime.now();
  final ds = '${now.year}${_pad(now.month)}${_pad(now.day)}';
  return 'BG-$ds-${_pad3(seq)}';
}

String transactionNumber(int seq) {
  final now = DateTime.now();
  final ds = '${now.year}${_pad(now.month)}${_pad(now.day)}';
  return 'TRX-$ds-${_pad3(seq)}';
}

String _pad(int n) => n.toString().padLeft(2, '0');
String _pad3(int n) => n.toString().padLeft(3, '0');

/// Tipe-tipe mutasi stok (untuk riwayat_stok & validasi).
class StokTransaksi {
  static const String stokMasuk = 'STOK_MASUK';
  static const String produksiKurangMentah = 'PRODUKSI_KURANG_MENTAH';
  static const String produksiTambahGiling = 'PRODUKSI_TAMBAH_GILING';
  static const String penjualan = 'PENJUALAN';
  static const String returBatal = 'RETUR_BATAL';
  static const String koreksiStok = 'KOREKSI_STOK';
}

class JenisStok {
  static const String mentah = 'MENTAH';
  static const String giling = 'GILING';
}

class StatusBayar {
  static const String lunas = 'LUNAS';
  static const String utang = 'UTANG';
}

class StatusStok {
  static const String tersedia = 'TERSEDIA';
  static const String habis = 'HABIS';
}

class JenisBatch {
  static const String full = 'FULL';
  static const String campur = 'CAMPUR';
}