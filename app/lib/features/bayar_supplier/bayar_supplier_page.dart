import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';

class BayarSupplierPage extends ConsumerStatefulWidget {
  const BayarSupplierPage({super.key});

  @override
  ConsumerState<BayarSupplierPage> createState() => _BayarSupplierPageState();
}

class _BayarSupplierPageState extends ConsumerState<BayarSupplierPage> {
  String? _usaId;
  String? _supplierId;
  final _nominal = TextEditingController();
  final _catatan = TextEditingController();
  bool _saving = false;
  List<Map<String, Object?>> _suppliers = [];

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
    final rows = await ref.read(stokRepoProvider).sisaUtangSupplier(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _suppliers = rows;
      // Auto-select supplier dengan utang terbesar
      final berhutang = rows.where((r) => ((r['sisa'] as num?) ?? 0) > 0).toList();
      if (berhutang.isNotEmpty) {
        _supplierId = berhutang.first['id'] as String;
      }
    });
  }

  double _sisaFor(String? sid) {
    if (sid == null) return 0;
    final row = _suppliers.firstWhere((r) => r['id'] == sid, orElse: () => {});
    return ((row['sisa'] as num?) ?? 0).toDouble();
  }

  Future<void> _save() async {
    if (_supplierId == null) {
      _toast('Pilih supplier dulu');
      return;
    }
    final nominal = parseDouble(_nominal.text);
    if (nominal == null || nominal <= 0) {
      _toast('Isi nominal bayar');
      return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(stokRepoProvider).bayarPiutangSupplier(
            usaId: _usaId!,
            supplierId: _supplierId!,
            nominal: nominal,
            catatan: _catatan.text.isEmpty ? null : _catatan.text,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pembayaran utang tersimpan ✓'), backgroundColor: Colors.green),
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
    final sisa = _sisaFor(_supplierId);

    return Scaffold(
      appBar: AppBar(title: const Text('Bayar Utang Supplier')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Supplier', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              ..._suppliers.map((s) {
                final sSisa = ((s['sisa'] as num?) ?? 0).toDouble();
                if (sSisa <= 0) return const SizedBox.shrink();
                final isSelected = _supplierId == s['id'];
                return Card(
                  color: isSelected ? AppTheme.primary.withOpacity(0.1) : null,
                  child: ListTile(
                    onTap: () => setState(() => _supplierId = s['id'] as String),
                    title: Text(s['nama'] as String,
                        style: TextStyle(fontWeight: isSelected ? FontWeight.bold : FontWeight.normal)),
                    subtitle: Text('Utang: ${formatRupiah(sSisa)}'),
                    trailing: Icon(isSelected ? Icons.check_circle : Icons.chevron_right,
                        color: isSelected ? AppTheme.primary : Colors.grey),
                  ),
                );
              }),
              if (_suppliers.every((s) => ((s['sisa'] as num?) ?? 0).toDouble() <= 0))
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text('Tidak ada supplier yang punya utang. 🎉',
                      textAlign: TextAlign.center),
                ),
              const SizedBox(height: 16),
              if (_supplierId != null)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.red.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Sisa utang saat ini:'),
                      Text(formatRupiah(sisa),
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.red)),
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
              if (_supplierId != null && parseDouble(_nominal.text) != null)
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