import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/master.dart';

class SupplierPage extends ConsumerStatefulWidget {
  const SupplierPage({super.key});

  @override
  ConsumerState<SupplierPage> createState() => _SupplierPageState();
}

class _SupplierPageState extends ConsumerState<SupplierPage> {
  String? _usaId;
  List<Supplier> _list = [];
  List<Map<String, Object?>> _sisaRows = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final id = await ref.read(authRepoProvider).getUsaId();
    if (!mounted || id == null) return;
    final repo = ref.read(stokRepoProvider);
    final list = await repo.listSuppliers(id);
    final sisa = await repo.sisaUtangSupplier(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _list = list;
      _sisaRows = sisa;
      _loading = false;
    });
  }

  Future<void> _tambah() async {
    final nama = TextEditingController();
    final hp = TextEditingController();
    final alamat = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Tambah Supplier'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: nama, decoration: const InputDecoration(labelText: 'Nama')),
              const SizedBox(height: 8),
              TextField(controller: hp, decoration: const InputDecoration(labelText: 'No HP')),
              const SizedBox(height: 8),
              TextField(controller: alamat, decoration: const InputDecoration(labelText: 'Alamat')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Batal')),
          ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text('Simpan')),
        ],
      ),
    );
    if (ok == true && nama.text.isNotEmpty && _usaId != null) {
      await ref.read(stokRepoProvider).tambahSupplier(
            _usaId!,
            nama.text,
            noHp: hp.text.isEmpty ? null : hp.text,
            alamat: alamat.text.isEmpty ? null : alamat.text,
          );
      await _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Data Supplier')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Data Supplier')),
      floatingActionButton: FloatingActionButton(
        onPressed: _tambah,
        backgroundColor: AppTheme.primary,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: _list.isEmpty
          ? const Center(child: Text('Belum ada supplier.'))
          : ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: _list.length,
              separatorBuilder: (_, __) => const SizedBox(height: 4),
              itemBuilder: (_, i) {
                final s = _list[i];
                final sisaRow = _sisaRows.firstWhere((r) => r['id'] == s.id, orElse: () => {});
                final sisa = ((sisaRow['sisa'] as num?) ?? 0).toDouble();
                return Card(
                  margin: EdgeInsets.zero,
                  child: ListTile(
                    title: Text(s.nama, style: const TextStyle(fontWeight: FontWeight.w600)),
                    subtitle: Text([
                      if (s.noHp != null) s.noHp,
                      if (s.alamat != null) s.alamat,
                    ].whereType<String>().join(' • ')),
                    trailing: sisa > 0
                        ? Text(
                            'Utang\n${formatRupiah(sisa)}',
                            textAlign: TextAlign.right,
                            style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold, fontSize: 12),
                          )
                        : null,
                  ),
                );
              },
            ),
    );
  }
}