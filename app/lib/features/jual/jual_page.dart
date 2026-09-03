import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/master.dart';

class JualPage extends ConsumerStatefulWidget {
  const JualPage({super.key});

  @override
  ConsumerState<JualPage> createState() => _JualPageState();
}

class _JualPageState extends ConsumerState<JualPage> {
  String? _usaId;
  String? _pelangganId;
  String? _jenisIkanId;
  final _kg = TextEditingController();
  String _statusBayar = 'LUNAS';
  bool _saving = false;

  List<Pelanggan> _pelanggan = [];
  List<JenisIkan> _jenis = [];
  List<Map<String, Object?>> _stokPerJenis = []; // {jenis_ikan_id, total_tersedia, harga_rata}

  @override
  void dispose() {
    _kg.dispose();
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
    final pel = await repo.listPelanggan(id);
    final jenis = await repo.listJenisIkan(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _pelanggan = pel;
      _jenis = jenis;
    });
    await _refreshStok();
  }

  Future<void> _refreshStok() async {
    if (_usaId == null) return;
    final db = await ref.read(appDbProvider).getDatabase();
    final rows = await db.rawQuery('''
      select ji.id as jenis_id, ji.nama as jenis_nama,
        ifnull(sum(sg.sisa_kg),0) as total_kg,
        ifnull(sum(sg.sisa_kg * sg.harga_jual_perkg) / nullif(sum(sg.sisa_kg),0), 0) as harga_rata
      from jenis_ikan ji
      left join stok_giling sg on sg.jenis_ikan_id = ji.id and sg.status = 'TERSEDIA' and sg.usa_id = ji.usa_id
      where ji.usa_id = ? and ji.aktif = 1
      group by ji.id
      order by ji.nama
    ''', [_usaId]);
    if (!mounted) return;
    setState(() => _stokPerJenis = rows);
  }

  double get _kgVal => parseDouble(_kg.text) ?? 0;

  Map<String, Object?>? get _selectedStok {
    if (_jenisIkanId == null) return null;
    return _stokPerJenis.firstWhere(
      (r) => r['jenis_id'] == _jenisIkanId,
      orElse: () => {},
    );
  }

  double get _hargaPerkg {
    final s = _selectedStok;
    if (s == null || s.isEmpty) return 0;
    return (s['harga_rata'] as num).toDouble();
  }

  double get _total => _kgVal * _hargaPerkg;

  double get _stokTersedia {
    final s = _selectedStok;
    if (s == null || s.isEmpty) return 0;
    return (s['total_kg'] as num).toDouble();
  }

  Future<void> _save() async {
    if (_pelangganId == null) {
      _toast('Pilih pelanggan dulu');
      return;
    }
    if (_jenisIkanId == null) {
      _toast('Pilih jenis ikan dulu');
      return;
    }
    if (_kgVal <= 0) {
      _toast('Isi jumlah kg');
      return;
    }
    if (_kgVal > _stokTersedia) {
      _toast('Stok tidak cukup. Tersedia: ${formatKg(_stokTersedia)}');
      return;
    }
    setState(() => _saving = true);
    try {
      final user = ref.read(authRepoProvider).currentUser;
      final nomor = await ref.read(stokRepoProvider).jualFifo(
            usaId: _usaId!,
            userId: user?.id,
            jenisIkanId: _jenisIkanId!,
            pelangganId: _pelangganId,
            jumlahKg: _kgVal,
            statusBayar: _statusBayar,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Penjualan $nomor tersimpan ✓'), backgroundColor: Colors.green),
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
    return Scaffold(
      appBar: AppBar(title: const Text('Jual Ikan')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<String>(
                value: _pelangganId,
                decoration: const InputDecoration(labelText: 'Pelanggan'),
                items: _pelanggan.map((p) => DropdownMenuItem(value: p.id, child: Text(p.nama))).toList(),
                onChanged: (v) => setState(() => _pelangganId = v),
              ),
              const SizedBox(height: 12),
              const Text('Pilih Ikan', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              ..._jenis.map((j) {
                final stok = _stokPerJenis.firstWhere(
                  (r) => r['jenis_id'] == j.id,
                  orElse: () => {'total_kg': 0.0, 'harga_rata': 0.0},
                );
                final tersedia = (stok['total_kg'] as num).toDouble();
                final harga = (stok['harga_rata'] as num).toDouble();
                final selected = _jenisIkanId == j.id;
                final disabled = tersedia <= 0;
                return Card(
                  color: selected ? AppTheme.primary.withOpacity(0.1) : null,
                  child: ListTile(
                    onTap: disabled
                        ? null
                        : () => setState(() {
                              _jenisIkanId = j.id;
                            }),
                    title: Text(j.nama, style: TextStyle(fontWeight: selected ? FontWeight.bold : FontWeight.normal)),
                    subtitle: Text(
                      disabled
                          ? 'Stok habis'
                          : 'Tersedia ${formatKg(tersedia)} • ${formatRupiah(harga)}/kg',
                    ),
                    trailing: Icon(
                      selected ? Icons.check_circle : Icons.chevron_right,
                      color: selected ? AppTheme.primary : Colors.grey,
                    ),
                  ),
                );
              }),
              const SizedBox(height: 12),
              TextField(
                controller: _kg,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                inputFormatters: [_decimalFormatter],
                decoration: InputDecoration(
                  labelText: 'Jumlah (kg)',
                  hintText: 'mis: 2,5',
                  suffixText: _stokTersedia > 0 ? 'tersedia ${formatKg(_stokTersedia)}' : null,
                ),
                onChanged: (_) => setState(() {}),
              ),
              const SizedBox(height: 12),
              if (_kgVal > 0 && _hargaPerkg > 0)
                Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.green.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Total:', style: TextStyle(fontSize: 15)),
                      Text(formatRupiah(_total),
                          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                    ],
                  ),
                ),
              const SizedBox(height: 16),
              const Text('Bayar?', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'LUNAS', label: Text('Tunai / Lunas')),
                  ButtonSegment(value: 'UTANG', label: Text('Utang')),
                ],
                selected: {_statusBayar},
                onSelectionChanged: (s) => setState(() => _statusBayar = s.first),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: _saving ? null : _save,
                icon: const Icon(Icons.check),
                label: Text(_saving ? 'Menyimpan...' : 'Simpan Penjualan'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  final _decimalFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'));
}