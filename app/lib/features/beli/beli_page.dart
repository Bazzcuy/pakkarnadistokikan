import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/master.dart';

/// Quick input: BELI IKAN dari supplier.
/// Field seminimal mungkin: pilih supplier → pilih ikan → kg → harga → status bayar → simpan.
class BeliPage extends ConsumerStatefulWidget {
  const BeliPage({super.key});

  @override
  ConsumerState<BeliPage> createState() => _BeliPageState();
}

class _BeliPageState extends ConsumerState<BeliPage> {
  String? _usaId;
  String? _supplierId;
  String? _jenisIkanId;
  final _berat = TextEditingController();
  final _harga = TextEditingController();
  String _statusBayar = 'LUNAS';
  final _catatan = TextEditingController();
  bool _saving = false;

  List<Supplier> _suppliers = [];
  List<JenisIkan> _jenis = [];

  @override
  void dispose() {
    _berat.dispose();
    _harga.dispose();
    _catatan.dispose();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final auth = ref.read(authRepoProvider);
    final id = await auth.getUsaId();
    if (!mounted || id == null) return;
    final repo = ref.read(stokRepoProvider);
    final suppliers = await repo.listSuppliers(id);
    final jenis = await repo.listJenisIkan(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _suppliers = suppliers;
      _jenis = jenis;
    });
  }

  double get _total {
    final b = parseDouble(_berat.text) ?? 0;
    final h = parseDouble(_harga.text) ?? 0;
    return b * h;
  }

  Future<void> _save() async {
    final berat = parseDouble(_berat.text);
    final harga = parseDouble(_harga.text);
    if (_supplierId == null) {
      _toast('Pilih supplier dulu');
      return;
    }
    if (_jenisIkanId == null) {
      _toast('Pilih jenis ikan dulu');
      return;
    }
    if (berat == null || berat <= 0) {
      _toast('Isi berat ikan (kg)');
      return;
    }
    if (harga == null || harga <= 0) {
      _toast('Isi harga per kg');
      return;
    }
    setState(() => _saving = true);
    try {
      await ref.read(stokRepoProvider).inputStokMasuk(
            usaId: _usaId!,
            jenisIkanId: _jenisIkanId!,
            supplierId: _supplierId!,
            beratKg: berat,
            hargaBeliPerkg: harga,
            statusBayar: _statusBayar,
            catatan: _catatan.text.isEmpty ? null : _catatan.text,
          );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pembelian tersimpan ✓'), backgroundColor: Colors.green),
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
      appBar: AppBar(title: const Text('Beli Ikan')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<String>(
                initialValue: _supplierId,
                decoration: const InputDecoration(labelText: 'Supplier'),
                items: _suppliers
                    .map((s) => DropdownMenuItem(value: s.id, child: Text(s.nama)))
                    .toList(),
                onChanged: (v) => setState(() => _supplierId = v),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _jenisIkanId,
                decoration: const InputDecoration(labelText: 'Jenis Ikan'),
                items: _jenis
                    .map((j) => DropdownMenuItem(value: j.id, child: Text(j.nama)))
                    .toList(),
                onChanged: (v) => setState(() => _jenisIkanId = v),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _berat,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Berat (kg)', hintText: 'mis: 5'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: _harga,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      inputFormatters: [_decimalFormatter],
                      decoration: const InputDecoration(labelText: 'Harga / kg (Rp)', hintText: 'mis: 60000'),
                      onChanged: (_) => setState(() {}),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withOpacity(0.08),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Total beli:', style: TextStyle(fontSize: 15)),
                    Text(formatRupiah(_total),
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              const Text('Bayar pakai apa?', style: TextStyle(fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: 'LUNAS', label: Text('Tunai / Lunas'), icon: Icon(Icons.payments)),
                  ButtonSegment(value: 'UTANG', label: Text('Utang'), icon: Icon(Icons.receipt_long)),
                ],
                selected: {_statusBayar},
                onSelectionChanged: (s) => setState(() => _statusBayar = s.first),
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
                label: Text(_saving ? 'Menyimpan...' : 'Simpan Pembelian'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  final _decimalFormatter = FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'));
}