package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.OptionItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MasterDataService {
    public List<OptionItem> jenisIkan() {
        return options("SELECT id, nama FROM jenis_ikan WHERE aktif=1 ORDER BY nama");
    }

    public List<OptionItem> suppliers() {
        return options("SELECT id, nama FROM suppliers ORDER BY nama");
    }

    public List<OptionItem> pelanggan() {
        return options("SELECT id, nama FROM pelanggan ORDER BY nama");
    }

    public List<OptionItem> batchGiling() {
        return options("SELECT id, batch_no || ' - ' || (SELECT nama FROM jenis_ikan WHERE id=stok_giling.jenis_ikan_id) || ' (' || total_kg || ' kg)' AS nama FROM stok_giling WHERE total_kg>0 ORDER BY id DESC");
    }

    public void tambahJenisIkan(String nama, String kategori, String deskripsi, String gambarPath) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama jenis ikan wajib diisi");
        try (var c = Database.connect()) {
            Database.execute(c, "INSERT INTO jenis_ikan(nama,kategori,deskripsi,gambar_path,aktif) VALUES(?,?,?,?,1)",
                    nama.trim(), blank(kategori, "Ikan"), blank(deskripsi, "Jenis ikan " + nama.trim()), blank(gambarPath, "/images/catokan_banner.png"));
            Database.execute(c, "INSERT INTO stok_mentah(jenis_ikan_id,total_kg,updated_at) VALUES((SELECT id FROM jenis_ikan WHERE nama=?),0,datetime('now'))", nama.trim());
        } catch (Exception e) {
            throw new RuntimeException("Gagal menambah jenis ikan: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> jenisIkanRows() {
        return Database.query("SELECT id,nama,kategori,deskripsi,gambar_path FROM jenis_ikan WHERE aktif=1 ORDER BY nama");
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<OptionItem> options(String sql) {
        List<OptionItem> list = new ArrayList<>();
        for (Map<String, Object> row : Database.query(sql)) {
            list.add(new OptionItem(((Number) row.get("id")).intValue(), String.valueOf(row.get("nama"))));
        }
        return list;
    }
}
