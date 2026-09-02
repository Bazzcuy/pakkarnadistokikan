import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'app.dart';
import 'data/remote/supabase_client.dart';
import 'features/auth/login_page.dart';
import 'features/auth/register_page.dart';
import 'features/home/dashboard_page.dart';
import 'features/beli/beli_page.dart';
import 'features/giling/giling_page.dart';
import 'features/jual/jual_page.dart';
import 'features/bayar_supplier/bayar_supplier_page.dart';
import 'features/terima_pelanggan/terima_pelanggan_page.dart';
import 'features/koreksi/koreksi_page.dart';
import 'features/riwayat/semua_catatan_page.dart';
import 'features/master/supplier_page.dart';
import 'features/master/pelanggan_page.dart';

final appRouter = GoRouter(
  initialLocation: '/login',
  refreshListenable: _AuthRefresh(),
  redirect: (context, state) {
    final loggedIn = supabase.auth.currentUser != null;
    final goingToAuth = state.matchedLocation == '/login' || state.matchedLocation == '/register';
    if (!loggedIn && !goingToAuth) return '/login';
    if (loggedIn && goingToAuth) return '/';
    return null;
  },
  routes: [
    GoRoute(path: '/login', builder: (_, __) => const LoginPage()),
    GoRoute(path: '/register', builder: (_, __) => const RegisterPage()),
    GoRoute(path: '/', builder: (_, __) => const DashboardPage()),
    GoRoute(path: '/beli', builder: (_, __) => const BeliPage()),
    GoRoute(path: '/giling', builder: (_, __) => const GilingPage()),
    GoRoute(path: '/jual', builder: (_, __) => const JualPage()),
    GoRoute(path: '/bayar-supplier', builder: (_, __) => const BayarSupplierPage()),
    GoRoute(path: '/terima-pelanggan', builder: (_, __) => const TerimaPelangganPage()),
    GoRoute(path: '/koreksi', builder: (_, __) => const KoreksiPage()),
    GoRoute(path: '/riwayat', builder: (_, __) => const SemuaCatatanPage()),
    GoRoute(path: '/supplier', builder: (_, __) => const SupplierPage()),
    GoRoute(path: '/pelanggan', builder: (_, __) => const PelangganPage()),
  ],
);

class _AuthRefresh extends ChangeNotifier {
  _AuthRefresh() {
    supabase.auth.onAuthStateChange.listen((_) => notifyListeners());
  }
}