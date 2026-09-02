import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';

class TerimaPelangganPage extends ConsumerStatefulWidget {
  const TerimaPelangganPage({super.key});

  @override
  ConsumerState<TerimaPelangganPage> createState() => _TerimaPelangganPageState();
}

class _TerimaPelangganPageState extends ConsumerState<TerimaPelangganPage> {
  String? _usaId;
  String? _penjualanId;
  final _nominal = TextEditingController();
  final _catatan = TextEditingController();
  bool _saving = false;
  List<Map<String, Object?>> _penjualanList = [];

  @override
  void dispose() {
    _nominal.dispose();
    _catatan.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final id = await ref.read(authRepoProvider).getUsaId();
    if (!mounted || id == null) return;
    final rows = await ref.read(stokRepoProvider).listPenjualanWithSisa(id);
    // Filter hanya yang punya sisa piutang
    final berhutang = rows.where((r) {
      final total = ((r['total'] as num?) ?? 0).toDouble();
      final bayar = ((r['dibayar'] as num?) ?? 0).toDouble();
      return total - bayar > 0;
    }).toList();
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _penjualanList = berhutang;
      if (berhutang.isNotEmpty) {
        _penjualanId = berhutang.first['id'] as String;
      }
    });
  }

  double _sisaFor(String? pid) {
    if (pid == null) return 0;
    final row = _penjualanList.firstWhere((r) => r['id'] == pid, orElse: () => {});
    if (row.isEmpty) return 0;
    final total = ((row['total'] as num?) ?? 0).toDouble();
    final bayar = ((row['dibayar'] as num?) ?? 0).toDouble();
    return total - bayar;
  }

  Future<void> _save() async {
    if (_penjualanId == null) {
      _toast('Pilih transaksi dulu');
      return;
    }
    final nominal = parseDouble(_nominal.text);
    if (nominal == null || nominal <= 0) {
      _toast('Isi nominal bayar');
      return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(stokRepoProvider).terimaBayarPelanggan(
            usaId: _usaId!,
            penjualanId: _penjualanId!,
            nominal: nominal,
            catatan: _catatan.text.isEmpty ? null : _catatan.text,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pembayaran pelanggan tersimpan ✓'), backgroundColor: Colors.green),
        );
        context.pop();
      }
    } catch (e) {
      _toast('Gagal: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: AppTheme.danger),
    );
  }

  @override
  Widget build(BuildContext context) {
    final sisa = _sisaFor(_penjualanId);

    return Scaffold(
      appBar: AppBar(title: const Text('Terima Bayar Pelanggan')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Pilih transaksi yang dibayar',
                  style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              if (_penjualanList.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text('Tidak ada piutang pelanggan. 🎉', textAlign: TextAlign.center),
                )
              else
                ..._penjualanList.map((row) {
                  final isSelected = _penjualanId == row['id'];
                  final total = ((row['total'] as num?) ?? 0).toDouble();
                  final bayar = ((row['dibayar'] as num?) ?? 0).toDouble();
                  final sisaNow = total - bayar;
                  final pelangganNama = (row['pelanggan_nama'] as String?) ?? 'Pelanggan';
                  return Card(
                    color: isSelected ? AppTheme.primary.withOpacity(0.1) : null,
                    child: ListTile(
                      onTap: () => setState(() => _penjualanId = row['id'] as String),
                      title: Text(pelangganNama,
                          style: TextStyle(fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
                      subtitle: Text(
                        '${row['nomor_transaksi']} • ${row['tanggal']}\n'
                        'Total ${formatRupiah(total)} • Sisa ${formatRupiah(sisaNow)}',
                      ),
                      isThreeLine: true,
                      trailing: Icon(isSelected ? Icons.check_circle : Icons.chevron_right,
                          color: isSelected ? AppTheme.primary : Colors.grey),
                    ),
                  );
                }),
              const SizedBox(height: 16),
              if (_penjualanId != null)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.orange.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Sisa piutang saat ini:'),
                      Text(formatRupiah(sisa),
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.orange)),
                    ],
                  ),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _nominal,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [_decimalFormatter],
                decoration: const InputDecoration(labelText: 'Nominal Bayar (Rp)'),
                onChanged: (_) => setState(() {}),
              ),
              if (_penjualanId != null && parseDouble(_nominal.text) != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(
                    'Setelah bayar: ${formatRupiah(sisa - (parseDouble(_nominal.text) ?? 0))}',
                    style: TextStyle(color: Colors.grey.shade700),
                  ),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _catatan,
                decoration: const InputDecoration(labelText: 'Catatan (opsional)'),
                maxLines: 2,
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _saving ? null : _save,
                icon: const Icon(Icons.check),
                label: Text(_saving ? 'Menyimpan...' : 'Simpan Pembayaran'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  final _decimalFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'));
}