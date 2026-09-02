import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';

class KoreksiPage extends ConsumerStatefulWidget {
  const KoreksiPage({super.key});

  @override
  ConsumerState<KoreksiPage> createState() => _KoreksiPageState();
}

class _KoreksiPageState extends ConsumerState<KoreksiPage> {
  String? _usaId;
  String _jenisStok = 'MENTAH';
  String? _referensiId;
  final _stokFisik = TextEditingController();
  final _alasan = TextEditingController();
  bool _saving = false;

  List<Map<String, Object?>> _rowsMentah = [];
  List<Map<String, Object?>> _rowsGiling = [];

  @override
  void dispose() {
    _stokFisik.dispose();
    _alasan.dispose();
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
    final repo = ref.read(stokRepoProvider);
    final mentah = await repo.listStokMentahForKoreksi(id);
    final giling = await repo.listStokGilingForKoreksi(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _rowsMentah = mentah;
      _rowsGiling = giling;
    });
  }

  Map<String, Object?> get _selected {
    final list = _jenisStok == 'MENTAH' ? _rowsMentah : _rowsGiling;
    if (_referensiId == null) return {};
    return list.firstWhere((r) => r['id'] == _referensiId, orElse: () => {});
  }

  double get _stokSistem {
    final s = _selected;
    if (s.isEmpty) return 0;
    return ((s[_jenisStok == 'MENTAH' ? 'total_kg' : 'sisa_kg'] as num?) ?? 0).toDouble();
  }

  double get _selisih => (parseDouble(_stokFisik.text) ?? 0) - _stokSistem;

  Future<void> _save() async {
    if (_referensiId == null) {
      _toast('Pilih stok yang mau dikoreksi');
      return;
    }
    final fisik = parseDouble(_stokFisik.text);
    if (fisik == null || fisik < 0) {
      _toast('Isi stok fisik');
      return;
    }
    if (_alasan.text.trim().isEmpty) {
      _toast('Isi alasan koreksi');
      return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(stokRepoProvider).koreksiStok(
            usaId: _usaId!,
            jenisStok: _jenisStok,
            referensiId: _referensiId!,
            stokFisik: fisik,
            alasan: _alasan.text.trim(),
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Koreksi tersimpan ✓'), backgroundColor: Colors.green),
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
    final rows = _jenisStok == 'MENTAH' ? _rowsMentah : _rowsGiling;
    final sel = _selected;
    final hasSelected = sel.isNotEmpty;

    return Scaffold(
      appBar: AppBar(title: const Text('Koreksi Stok')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'MENTAH', label: Text('Stok Mentah')),
                  ButtonSegment(value: 'GILING', label: Text('Stok Giling')),
                ],
                selected: {_jenisStok},
                onSelectionChanged: (s) {
                  setState(() {
                    _jenisStok = s.first;
                    _referensiId = null;
                  });
                },
              ),
              const SizedBox(height: 16),
              if (rows.isEmpty)
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text('Tidak ada data stok.'),
                )
              else
                ...rows.map((r) {
                  final isSelected = _referensiId == r['id'];
                  final kg = ((r['total_kg'] as num?) ?? (r['sisa_kg'] as num?) ?? 0).toDouble();
                  final label = _jenisStok == 'MENTAH'
                      ? '${r['nama']} • Sistem ${formatKg(kg)}'
                      : '${r['nama']} • Batch ${r['batch_no']} • Sisa ${formatKg(kg)}';
                  return Card(
                    color: isSelected ? AppTheme.primary.withOpacity(0.1) : null,
                    child: ListTile(
                      onTap: () => setState(() => _referensiId = r['id'] as String),
                      title: Text(label),
                      trailing: Icon(isSelected ? Icons.check_circle : Icons.chevron_right,
                          color: isSelected ? AppTheme.primary : Colors.grey),
                    ),
                  );
                }),
              const SizedBox(height: 16),
              if (hasSelected)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('Stok di sistem:'),
                          Text(formatKg(_stokSistem),
                              style: const TextStyle(fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ],
                  ),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _stokFisik,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [_decimalFormatter],
                decoration: const InputDecoration(labelText: 'Stok Fisik Hasil Hitung Ulang (kg)'),
                onChanged: (_) => setState(() {}),
              ),
              if (hasSelected && parseDouble(_stokFisik.text) != null)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: _selisih >= 0 ? Colors.green.shade50 : Colors.red.shade50,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Selisih:'),
                        Text(
                          '${_selisih >= 0 ? '+' : ''}${formatKg(_selisih)}',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: _selisih >= 0 ? Colors.green.shade800 : Colors.red.shade800,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _alasan,
                decoration: const InputDecoration(
                  labelText: 'Alasan Koreksi',
                  hintText: 'mis: kurang hitung, tumpah, dll',
                ),
                maxLines: 2,
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _saving ? null : _save,
                icon: const Icon(Icons.check),
                label: Text(_saving ? 'Menyimpan...' : 'Simpan Koreksi'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  final _decimalFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'));
}