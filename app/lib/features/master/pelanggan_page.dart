import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';
import '../../data/models/master.dart';

class PelangganPage extends ConsumerStatefulWidget {
  const PelangganPage({super.key});

  @override
  ConsumerState<PelangganPage> createState() => _PelangganPageState();
}

class _PelangganPageState extends ConsumerState<PelangganPage> {
  String? _usaId;
  List<Pelanggan> _list = [];
  List<Map<String, Object?>> _piutangRows = [];
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
    final list = await repo.listPelanggan(id);
    final piutang = await repo.sisaPiutangPelanggan(id);
    if (!mounted) return;
    setState(() {
      _usaId = id;
      _list = list;
      _piutangRows = piutang;
      _loading = false;
    });
  }

  Future<void> _tambah() async {
    final nama = TextEditingController();
    final hp = TextEditingController();
    final alamat = TextEditingController();
    String tipeVal = 'Retail';
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => StatefulBuilder(builder: (ctx, setStateDialog) {
        return AlertDialog(
          title: const Text('Tambah Pelanggan'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: nama, decoration: const InputDecoration(labelText: 'Nama')),
                const SizedBox(height: 8),
                TextField(controller: hp, decoration: const InputDecoration(labelText: 'No HP')),
                const SizedBox(height: 8),
                TextField(controller: alamat, decoration: const InputDecoration(labelText: 'Alamat')),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  initialValue: tipeVal,
                  decoration: const InputDecoration(labelText: 'Tipe'),
                  items: const [
                    DropdownMenuItem(value: 'Retail', child: Text('Retail')),
                    DropdownMenuItem(value: 'Grosir', child: Text('Grosir')),
                  ],
                  onChanged: (v) => setStateDialog(() => tipeVal = v ?? 'Retail'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Batal')),
            ElevatedButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Simpan')),
          ],
        );
      }),
    );
    if (ok == true && nama.text.isNotEmpty && _usaId != null) {
      await ref.read(stokRepoProvider).tambahPelanggan(
            _usaId!,
            nama.text,
            noHp: hp.text.isEmpty ? null : hp.text,
            alamat: alamat.text.isEmpty ? null : alamat.text,
            tipe: tipeVal,
          );
      await _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Data Pelanggan')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Data Pelanggan')),
      floatingActionButton: FloatingActionButton(
        onPressed: _tambah,
        backgroundColor: AppTheme.primary,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: _list.isEmpty
          ? const Center(child: Text('Belum ada pelanggan.'))
          : ListView.separated(
              padding: const EdgeInsets.all(12),
              itemCount: _list.length,
              separatorBuilder: (_, __) => const SizedBox(height: 4),
              itemBuilder: (_, i) {
                final p = _list[i];
                final piutangRow = _piutangRows.firstWhere((r) => r['id'] == p.id, orElse: () => {});
                final piutang = ((piutangRow['sisa'] as num?) ?? 0).toDouble();
                return Card(
                  margin: EdgeInsets.zero,
                  child: ListTile(
                    title: Text(p.nama, style: const TextStyle(fontWeight: FontWeight.w600)),
                    subtitle: Text('${p.tipe}${p.noHp != null ? " • ${p.noHp}" : ""}'),
                    trailing: piutang > 0
                        ? Text(
                            'Piutang\n${formatRupiah(piutang)}',
                            textAlign: TextAlign.right,
                            style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.bold, fontSize: 12),
                          )
                        : null,
                  ),
                );
              },
            ),
    );
  }
}