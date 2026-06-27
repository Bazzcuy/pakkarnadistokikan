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
}
