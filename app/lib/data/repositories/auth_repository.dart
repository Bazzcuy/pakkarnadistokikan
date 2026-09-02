import 'package:supabase_flutter/supabase_flutter.dart';

import '../remote/supabase_client.dart';

/// Auth + session management.
/// Saat register: otomatis bikin usaha + user_profile via trigger DB.
class AuthRepository {
  final SupabaseClient _client;
  AuthRepository(this._client);

  User? get currentUser => _client.auth.currentUser;

  String? get userId => currentUser?.id;
  String? get email => currentUser?.email;

  Stream<AuthState> get authState => _client.auth.onAuthStateChange;

  Future<AuthResponse> register({
    required String email,
    required String password,
    required String nama,
  }) async {
    return await _client.auth.signUp(
      email: email,
      password: password,
      data: {'nama': nama},
    );
  }

  Future<AuthResponse> login({
    required String email,
    required String password,
  }) async {
    return await _client.auth.signInWithPassword(email: email, password: password);
  }

  Future<void> logout() async {
    await _client.auth.signOut();
  }

  /// Get usaha_id user saat ini. Dipakai sebagai foreign key di semua tabel.
  Future<String?> getUsaId() async {
    final uid = userId;
    if (uid == null) return null;
    final r = await _client
        .from('users_profile')
        .select('usa_id')
        .eq('user_id', uid)
        .maybeSingle();
    return r?['usa_id'] as String?;
  }

  /// Setelah register pertama kali, panggil ini untuk seed master data awal.
  Future<void> seedMasterAwal() async {
    final usId = await getUsaId();
    if (usId == null) return;
    // Panggil RPC yang ada di schema.sql → seed_master_data
    await _client.rpc('seed_master_data', params: {'target_usa': usId});
  }
}