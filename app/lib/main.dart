import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'data/remote/supabase_client.dart';

/// CARA ISI SUPABASE URL & KEY:
/// Opsi A — edit langsung di file ini (ganti string kosong di bawah).
/// Opsi B — build dengan flag:
///   flutter run --dart-define=SUPABASE_URL=https://xxx.supabase.co --dart-define=SUPABASE_ANON_KEY=eyJ...
const String _supabaseUrl = String.fromEnvironment(
  'SUPABASE_URL',
  defaultValue: 'https://blassyztvmsvejinqdy.supabase.co',
);
const String _supabaseAnonKey = String.fromEnvironment(
  'SUPABASE_ANON_KEY',
  defaultValue: 'sb_publishable_rJOxklU72bOVTbOnGFCSmg_9lZ99XSP',
);

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  SupabaseConfig.url = _supabaseUrl.isEmpty ? 'https://YOUR-PROJECT.supabase.co' : _supabaseUrl;
  SupabaseConfig.anonKey = _supabaseAnonKey.isEmpty ? 'YOUR-ANON-KEY' : _supabaseAnonKey;

  await initSupabase();

  runApp(const ProviderScope(child: CatokanApp()));
}