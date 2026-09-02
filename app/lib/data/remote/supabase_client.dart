import 'package:supabase_flutter/supabase_flutter.dart';

/// Supabase client singleton.
/// `supabaseUrl` dan `supabaseAnonKey` di-inject dari main.dart (env / compile-time).
class SupabaseConfig {
  static String? _url;
  static String? _anonKey;

  static String get url {
    final v = _url;
    if (v == null || v.isEmpty) {
      throw StateError(
        'Supabase URL belum di-set. Isi di main.dart atau pakai --dart-define=SUPABASE_URL=... saat build.',
      );
    }
    return v;
  }

  static String get anonKey {
    final v = _anonKey;
    if (v == null || v.isEmpty) {
      throw StateError(
        'Supabase anon key belum di-set. Isi di main.dart atau pakai --dart-define=SUPABASE_ANON_KEY=... saat build.',
      );
    }
    return v;
  }

  static set url(String value) => _url = value;
  static set anonKey(String value) => _anonKey = value;
}

Future<void> initSupabase() async {
  await Supabase.initialize(
    url: SupabaseConfig.url,
    anonKey: SupabaseConfig.anonKey,
  );
}

SupabaseClient get supabase => Supabase.instance.client;