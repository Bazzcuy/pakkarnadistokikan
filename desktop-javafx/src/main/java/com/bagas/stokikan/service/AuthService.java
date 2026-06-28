package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {
    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return null;
        String sql = "SELECT id,nama,username,role FROM users WHERE username=? AND password IN (?,?) AND status='AKTIF'";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordUtil.sha256(password.trim()));
            ps.setString(3, password.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new User(rs.getInt("id"), rs.getString("nama"), rs.getString("username"), rs.getString("role"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Login gagal: " + e.getMessage(), e);
        }
        return null;
    }

    public User register(String nama, String username, String password, String namaUsaha, String nomorHp, String alamat) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama pengguna wajib diisi");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username wajib diisi");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password minimal 6 karakter");
        try (Connection c = Database.connect()) {
            int id = Database.insertAndGetId(c, "INSERT INTO users(nama,username,password,role,nama_usaha,nomor_hp,alamat,status) VALUES(?,?,?,?,?,?,?,'AKTIF')",
                    nama.trim(), username.trim(), PasswordUtil.sha256(password.trim()), "PENGGUNA", blank(namaUsaha), blank(nomorHp), blank(alamat));
            return new User(id, nama.trim(), username.trim(), "PENGGUNA");
        } catch (Exception e) {
            throw new RuntimeException("Gagal daftar akun: " + e.getMessage(), e);
        }
    }

    public void updateProfile(int userId, String nama, String namaUsaha, String nomorHp, String alamat) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama pengguna wajib diisi");
        try (Connection c = Database.connect()) {
            Database.execute(c, "UPDATE users SET nama=?, nama_usaha=?, nomor_hp=?, alamat=? WHERE id=?",
                    nama.trim(), blank(namaUsaha), blank(nomorHp), blank(alamat), userId);
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan profil: " + e.getMessage(), e);
        }
    }

    public java.util.Map<String, Object> profile(int userId) {
        var rows = Database.query("SELECT id,nama,username,role,nama_usaha,nomor_hp,alamat FROM users WHERE id=?", userId);
        return rows.isEmpty() ? java.util.Map.of() : rows.get(0);
    }

    private String blank(String value) {
        return value == null ? "" : value.trim();
    }
}
