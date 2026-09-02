import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/format.dart';
import '../../core/theme.dart';

/// Dashboard utama — 5 tombol gede + ringkasan angka.
/// Dirancang supaya Mama bisa langsung "mau ngapain hari ini?" dalam 1 tap.
class DashboardPage extends ConsumerStatefulWidget {
  const DashboardPage({super.key});

  @override
  ConsumerState<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends ConsumerState<DashboardPage> {
  String? _usaId;
  bool _loading = true;

  // Angka dashboard
  double _stokMentah = 0;
  double _stokGiling = 0;
  double _jualHariIni = 0;
  double _utangSupplier = 0;
  double _piutangPelanggan = 0;
  bool _syncing = false;
  String? _syncHint;

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    final auth = ref.read(authRepoProvider);
    final id = await auth.getUsaId();
    if (!mounted) return;
    setState(() => _usaId = id);
    if (id != null) {
      await _refresh();
      // Sync otomatis saat buka dashboard
      _syncAll();
    } else {
      setState(() => _loading = false);
    }
  }

  Future<void> _refresh() async {
    if (_usaId == null) return;
    final repo = ref.read(stokRepoProvider);
    final results = await Future.wait([
      repo.totalStokMentah(_usaId!),
      repo.totalStokGiling(_usaId!),
      repo.totalPenjualanHariIni(_usaId!),
      repo.totalSisaUtangSupplier(_usaId!),
      repo.totalSisaPiutangPelanggan(_usaId!),
    ]);
    if (!mounted) return;
    setState(() {
      _stokMentah = results[0];
      _stokGiling = results[1];
      _jualHariIni = results[2];
      _utangSupplier = results[3];
      _piutangPelanggan = results[4];
      _loading = false;
    });
  }

  Future<void> _syncAll() async {
    if (_syncing || _usaId == null) return;
    setState(() {
      _syncing = true;
      _syncHint = null;
    });
    try {
      final result = await ref.read(syncRepoProvider).syncAll(_usaId!);
      if (mounted) {
        setState(() {
          _syncHint = 'Sinkron selesai (kirim ${result.pushed}, ambil ${result.pulled})';
        });
        await _refresh();
      }
    } catch (e) {
      if (mounted) setState(() => _syncHint = 'Sinkron gagal: $e');
    } finally {
      if (mounted) setState(() => _syncing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    if (_usaId == null) {
      return Scaffold(
        appBar: AppBar(title: const Text('CATOKAN')),
        body: const Center(child: Text('Gagal memuat usaha. Coba login ulang.')),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('CATOKAN'),
        actions: [
          IconButton(
            tooltip: 'Sinkron',
            onPressed: _syncing ? null : _syncAll,
            icon: _syncing
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                : const Icon(Icons.sync),
          ),
          IconButton(
            tooltip: 'Riwayat',
            onPressed: () => context.push('/riwayat'),
            icon: const Icon(Icons.history),
          ),
          IconButton(
            tooltip: 'Keluar',
            onPressed: () async {
              await ref.read(authRepoProvider).logout();
            },
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await _syncAll();
          await _refresh();
        },
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_syncHint != null)
                Container(
                  padding: const EdgeInsets.all(12),
                  margin: const EdgeInsets.only(bottom: 12),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(_syncHint!, style: TextStyle(color: Colors.blue.shade800)),
                ),
              _statCard('Stok Ikan Mentah', formatKg(_stokMentah), Icons.water, Colors.blue),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(child: _statCard('Stok Giling', formatKg(_stokGiling), Icons.inventory, Colors.white)),
                  const SizedBox(width: 8),
                  Expanded(child: _statCard('Jual Hari Ini', formatRupiah(_jualHariIni), Icons.payments, Colors.green.shade50)),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(child: _statCard('Utang ke Supplier', formatRupiah(_utangSupplier), Icons.outbox, Colors.red.shade50)),
                  const SizedBox(width: 8),
                  Expanded(child: _statCard('Piutang Pelanggan', formatRupiah(_piutangPelanggan), Icons.inbox, Colors.orange.shade50)),
                ],
              ),
              const SizedBox(height: 24),
              const Text('Mau ngapain hari ini?',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              _bigAction('Beli Ikan', 'Catat pembelian dari supplier', Icons.shopping_basket, Colors.blue, () => context.push('/beli')),
              const SizedBox(height: 8),
              _bigAction('Giling', 'Catat produksi ikan giling', Icons.kitchen, Colors.purple, () => context.push('/giling')),
              const SizedBox(height: 8),
              _bigAction('Jual', 'Catat penjualan ke pelanggan', Icons.point_of_sale, Colors.green, () => context.push('/jual')),
              const SizedBox(height: 8),
              _bigAction('Bayar Utang Supplier', 'Mencicil / melunasi utang', Icons.account_balance_wallet, Colors.red, () => context.push('/bayar-supplier')),
              const SizedBox(height: 8),
              _bigAction('Terima Bayar Pelanggan', 'Mencatat pembayaran piutang', Icons.payments, Colors.orange, () => context.push('/terima-pelanggan')),
              const SizedBox(height: 24),
              const Text('Lain-lain', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(child: _smallAction('Data Supplier', Icons.people, () => context.push('/supplier'))),
                  const SizedBox(width: 8),
                  Expanded(child: _smallAction('Data Pelanggan', Icons.person, () => context.push('/pelanggan'))),
                ],
              ),
              const SizedBox(height: 8),
              _smallAction('Koreksi Stok (Hitung Ulang)', Icons.calculate, () => context.push('/koreksi')),
            ],
          ),
        ),
      ),
    );
  }

  Widget _statCard(String label, String value, IconData icon, Color color) {
    return Card(
      color: color,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: AppTheme.textSecondary),
                const SizedBox(width: 6),
                Expanded(child: Text(label, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
              ],
            ),
            const SizedBox(height: 8),
            Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppTheme.textPrimary)),
          ],
        ),
      ),
    );
  }

  Widget _bigAction(String label, String subtitle, IconData icon, Color color, VoidCallback onTap) {
    return Material(
      color: color,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: Colors.white.withOpacity(0.3),
                radius: 26,
                child: Icon(icon, size: 28, color: AppTheme.textPrimary),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(label, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 2),
                    Text(subtitle, style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary)),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppTheme.textPrimary),
            ],
          ),
        ),
      ),
    );
  }

  Widget _smallAction(String label, IconData icon, VoidCallback onTap) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              Icon(icon, color: AppTheme.primary),
              const SizedBox(width: 8),
              Expanded(child: Text(label, style: const TextStyle(fontSize: 14))),
              const Icon(Icons.chevron_right, size: 18, color: AppTheme.textSecondary),
            ],
          ),
        ),
      ),
    );
  }
}