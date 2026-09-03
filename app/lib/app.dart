import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme.dart';
import 'data/local/db.dart';
import 'data/remote/supabase_client.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/stok_repository.dart';
import 'data/repositories/sync_repository.dart';
import 'router.dart';

// =========================================================
// Riverpod providers — dipakai global oleh semua UI
// =========================================================
final appDbProvider = Provider<AppDatabase>((ref) => AppDatabase.instance);
final authRepoProvider = Provider<AuthRepository>((ref) => AuthRepository(supabase));
final stokRepoProvider = Provider<StokRepository>((ref) => StokRepository(ref.watch(appDbProvider)));
final syncRepoProvider = Provider<SyncRepository>((ref) => SyncRepository(ref.watch(appDbProvider)));

// State apakah user login atau belum. Router baca ini untuk redirect.
final authStateProvider = StreamProvider<bool>((ref) {
  return ref.watch(authRepoProvider).authState.map((_) => supabase.auth.currentUser != null);
});

class CatokanApp extends StatelessWidget {
  const CatokanApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'CATOKAN',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      routerConfig: appRouter,
    );
  }
}