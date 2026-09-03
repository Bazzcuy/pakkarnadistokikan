import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/master.dart';

class GilingPage extends ConsumerStatefulWidget {
  const GilingPage({super.key});

  @override
  ConsumerState<GilingPage> createState() => _GilingPageState();
}

class _GilingPageState extends ConsumerState<GilingPage> {
  String? _usaId;
  String? _jenisIkanId;
  final _mentah = TextEditingController();
  final _hasil = TextEditingController();
  final _hargaJual = TextEditingController();
  final _biaya = TextEditingController(text: '0');
  final _catatan = TextEditingController();
  String _jenisBatch = 'FULL';
  bool _saving = false;
  List<JenisIkan> _jenis = [];

  @override
  void dispose() {
    _mentah.dispose();
    _hasil.dispose();
    _hargaJual.dispose();
    _biaya.dispose();
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
    final list = await ref.read(stokRepoProvider).listJenisIkan(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _jenis = list;
    });
  }

  double get _mentahVal => parseDouble(_mentah.text) ?? 0;
  double get _hasilVal => parseDouble(_hasil.text) ?? 0;
  double get _susut => _mentahVal - _hasilVal;
  double get _susutPct => _mentahVal > 0 ? (_susut / _mentahVal) * 100 : 0;

  Future<void> _save() async {
    if (_jenisIkanId == null) {
      _toast('Pilih jenis ikan dulu');
      return;
    }
    if (_mentahVal <= 0 || _hasilVal <= 0) {
      _toast('Isi berat mentah & hasil');
      return;
    }
    if (_hasilVal > _mentahVal) {
      _toast('Hasil tidak boleh lebih dari mentah');
      return;
    }
    final harga = parseDouble(_hargaJual.text);
    if (harga == null || harga <= 0) {
      _toast('Isi harga jual per kg');
      return;
    }

    setState(() => _saving = true);
    try {
      final batchNo = await ref.read(stokRepoProvider).prosesProduksi(
            usaId: _usaId!,
            jenisIkanId: _jenisIkanId!,
            beratMentahKg: _mentahVal,
            beratHasilKg: _hasilVal,
            hargaJualPerkg: harga,
            biayaProduksi: parseDouble(_biaya.text) ?? 0,
            jenisBatch: _jenisBatch,
            catatan: _catatan.text.isEmpty ? null : _catatan.text,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Produksi $batchNo tersimpan ✓'), backgroundColor: Colors.green),
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
      appBar: AppBar(title: const Text('Giling Ikan')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<String>(
                value: _jenisIkanId,
                decoration: const InputDecoration(labelText: 'Jenis Ikan'),
                items: _jenis.map((j) => DropdownMenuItem(value: j.id, child: Text(j.nama))).toList(),
                onChanged: (v) => setState(() => _jenisIkanId = v),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _mentah,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Berat Mentah (kg)'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: _hasil,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Hasil Giling (kg)'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                ],
              ),
              if (_mentahVal > 0 && _hasilVal > 0 && _hasilVal <= _mentahVal)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.orange.shade50,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.orange.shade800, size: 20),
                        const SizedBox(width: 8),
                        Text('Susut: ${formatKg(_susut)} (${_susutPct.toStringAsFixed(1)}%)',
                            style: TextStyle(color: Colors.orange.shade900)),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 12),
              const Text('Jenis Giling', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'FULL', label: Text('Kocin Full')),
                  ButtonSegment(value: 'CAMPUR', label: Text('Campur Batang')),
                ],
                selected: {_jenisBatch},
                onSelectionChanged: (s) => setState(() => _jenisBatch = s.first),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _biaya,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Biaya Produksi (Rp)'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: _hargaJual,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Harga Jual / kg (Rp)'),
                    ),
                  ),
                ],
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
                label: Text(_saving ? 'Menyimpan...' : 'Simpan Produksi'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  final _decimalFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'));
}