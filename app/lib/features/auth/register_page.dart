import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app.dart';
import '../../core/theme.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _nama = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _loading = false;

  @override
  void dispose() {
    _nama.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_nama.text.trim().isEmpty) {
      _toast('Isi nama Mama dulu');
      return;
    }
    if (!_email.text.contains('@')) {
      _toast('Email tidak valid');
      return;
    }
    if (_password.text.length < 6) {
      _toast('Password minimal 6 karakter');
      return;
    }

    setState(() => _loading = true);
    try {
      final auth = ref.read(authRepoProvider);
      await auth.register(
        email: _email.text.trim(),
        password: _password.text,
        nama: _nama.text.trim(),
      );
      // Login otomatis setelah register di Supabase (jika auto-confirm aktif)
      bool loggedIn = false;
      try {
        await auth.login(email: _email.text.trim(), password: _password.text);
        loggedIn = true;
      } catch (_) {
        // Auto-confirm OFF — perlu cek email dulu
      }

      if (!loggedIn) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Cek email Mama untuk verifikasi, lalu login ulang.'),
              backgroundColor: Colors.orange,
              duration: Duration(seconds: 5),
            ),
          );
          context.go('/login');
        }
        return;
      }

      // Seed master data awal (kalau auto-confirm ON, langsung panggil)
      await auth.seedMasterAwal();

      if (mounted) context.go('/');
    } catch (e) {
      _toast(_registerError(e));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _registerError(Object e) {
    final s = e.toString();
    if (s.contains('already registered') || s.contains('User already')) {
      return 'Email sudah terdaftar. Silakan login.';
    }
    if (s.contains('weak password') || s.contains('Password')) {
      return 'Password terlalu lemah.';
    }
    if (s.contains('rate limit')) {
      return 'Terlalu banyak percobaan. Coba lagi nanti.';
    }
    return 'Gagal daftar: ${s.replaceAll('Exception: ', '')}';
  }

  void _toast(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: AppTheme.danger),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Daftar Akun')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Daftar Akun Baru',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                'Sekali daftar, Mama langsung punya usaha sendiri dan Mama Bagas bisa lihat dari HP lain.',
                style: TextStyle(color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 24),
              TextField(
                controller: _nama,
                decoration: const InputDecoration(labelText: 'Nama Mama / Pengguna'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(labelText: 'Email'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _password,
                obscureText: true,
                decoration: const InputDecoration(labelText: 'Password (min. 6 karakter)'),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _loading ? null : _submit,
                child: _loading
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                    : const Text('Daftar & Masuk'),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () => context.go('/login'),
                child: const Text('Sudah punya akun? Masuk'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}