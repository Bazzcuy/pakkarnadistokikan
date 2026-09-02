import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/transaksi.dart';

class SemuaCatatanPage extends ConsumerStatefulWidget {
  const SemuaCatatanPage({super.key});

  @override
  ConsumerState<SemuaCatatanPage> createState() => _SemuaCatatanPageState();
}

class _SemuaCatatanPageState extends ConsumerState<SemuaCatatanPage> {
  String? _usaId;
  List<RiwayatStok> _data = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final id = await ref.read(authRepoProvider).getUsaId();
    if (!mounted || id == null) return;
    final rows = await ref.read(stokRepoProvider).listRiwayat(id, limit: 200);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _data = rows;
      _loading = false;
    });
  }

  IconData _iconFor(String jenis) {
    switch (jenis) {
      case 'STOK_MASUK':
        return Icons.shopping_basket;
      case 'PENJUALAN':
        return Icons.point_of_sale;
      case 'PRODUKSI_TAMBAH_GILING':
        return Icons.kitchen;
      case 'PRODUKSI_KURANG_MENTAH':
        return Icons.kitchen;
      case 'KOREKSI_STOK':
        return Icons.calculate;
      case 'RETUR_BATAL':
        return Icons.undo;
      default:
        return Icons.info;
    }
  }

  Color _colorFor(String jenis) {
    switch (jenis) {
      case 'STOK_MASUK':
        return Colors.blue;
      case 'PENJUALAN':
        return Colors.green;
      case 'PRODUKSI_TAMBAH_GILING':
        return Colors.purple;
      case 'PRODUKSI_KURANG_MENTAH':
        return Colors.purple;
      case 'KOREKSI_STOK':
        return Colors.orange;
      case 'RETUR_BATAL':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  String _labelFor(String jenis) {
    switch (jenis) {
      case 'STOK_MASUK':
        return 'Beli Ikan';
      case 'PENJUALAN':
        return 'Penjualan';
      case 'PRODUKSI_TAMBAH_GILING':
        return 'Giling (hasil)';
      case 'PRODUKSI_KURANG_MENTAH':
        return 'Giling (bahan)';
      case 'KOREKSI_STOK':
        return 'Koreksi Stok';
      case 'RETUR_BATAL':
        return 'Retur / Batal';
      default:
        return jenis;
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Semua Catatan')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Semua Catatan')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _data.isEmpty
            ? const Center(child: Text('Belum ada catatan.'))
            : ListView.separated(
                padding: const EdgeInsets.all(8),
                itemCount: _data.length,
                separatorBuilder: (_, __) => const SizedBox(height: 4),
                itemBuilder: (_, i) {
                  final r = _data[i];
                  return Card(
                    margin: EdgeInsets.zero,
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: _colorFor(r.jenisTransaksi).withOpacity(0.2),
                        child: Icon(_iconFor(r.jenisTransaksi),
                            color: _colorFor(r.jenisTransaksi)),
                      ),
                      title: Text(_labelFor(r.jenisTransaksi)),
                      subtitle: Text(
                        '${DateFormat('d MMM, HH:mm', 'id_ID').format(r.tanggal)}\n'
                        '${r.jenisStok} • ${r.referensi ?? '-'}',
                      ),
                      isThreeLine: true,
                      trailing: Text(
                        '${r.perubahanKg >= 0 ? '+' : ''}${formatKg(r.perubahanKg)}',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: r.perubahanKg >= 0 ? Colors.green : Colors.red,
                          fontSize: 14,
                        ),
                      ),
                    ),
                  );
                },
              ),
        ),
    );
  }
}