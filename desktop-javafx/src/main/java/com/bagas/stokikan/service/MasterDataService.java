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

    public List<OptionItem> stokGilingUntukKoreksi() {
        return options("SELECT g.id, j.nama || ' - produksi ' || g.tanggal_produksi || ' (' || g.total_kg || ' kg)' AS nama FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id WHERE g.total_kg>=0 ORDER BY date(g.tanggal_produksi),g.id");
    }

    public List<OptionItem> transaksiBerhasil() {
        return options("SELECT p.id, p.nomor_transaksi || ' - ' || IFNULL(pl.nama,'Pelanggan') || ' - Rp ' || p.total AS nama FROM penjualan p LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id WHERE p.status_pembayaran='LUNAS' ORDER BY p.id DESC");
    }

    public void simpanSupplier(Integer id, String nama, String hp, String alamat, String catatan) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama supplier wajib diisi");
        try (var c = Database.connect()) {
            if (id == null) {
                Database.execute(c, "INSERT INTO suppliers(nama,nomor_hp,alamat,catatan) VALUES(?,?,?,?)", nama.trim(), text(hp), text(alamat), text(catatan));
            } else {
                Database.execute(c, "UPDATE suppliers SET nama=?, nomor_hp=?, alamat=?, catatan=? WHERE id=?", nama.trim(), text(hp), text(alamat), text(catatan), id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan supplier: " + e.getMessage(), e);
        }
    }

    public void hapusSupplier(int id) {
        if (!Database.query("SELECT id FROM stok_masuk WHERE supplier_id=? LIMIT 1", id).isEmpty()) {
            throw new IllegalArgumentException("Supplier sudah dipakai pada stok masuk, tidak bisa dihapus. Ubah datanya saja.");
        }
        try (var c = Database.connect()) {
            Database.execute(c, "DELETE FROM suppliers WHERE id=?", id);
        } catch (Exception e) {
            throw new RuntimeException("Gagal menghapus supplier: " + e.getMessage(), e);
        }
    }

    public void simpanPelanggan(Integer id, String nama, String hp, String alamat, String tipe) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama pelanggan wajib diisi");
        try (var c = Database.connect()) {
            if (id == null) {
                Database.execute(c, "INSERT INTO pelanggan(nama,nomor_hp,alamat,tipe_pelanggan) VALUES(?,?,?,?)", nama.trim(), text(hp), text(alamat), blank(tipe, "Retail"));
            } else {
                Database.execute(c, "UPDATE pelanggan SET nama=?, nomor_hp=?, alamat=?, tipe_pelanggan=? WHERE id=?", nama.trim(), text(hp), text(alamat), blank(tipe, "Retail"), id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan pelanggan: " + e.getMessage(), e);
        }
    }

    public void hapusPelanggan(int id) {
        if (!Database.query("SELECT id FROM penjualan WHERE pelanggan_id=? LIMIT 1", id).isEmpty()) {
            throw new IllegalArgumentException("Pelanggan sudah dipakai pada penjualan, tidak bisa dihapus. Ubah datanya saja.");
        }
        try (var c = Database.connect()) {
            Database.execute(c, "DELETE FROM pelanggan WHERE id=?", id);
        } catch (Exception e) {
            throw new RuntimeException("Gagal menghapus pelanggan: " + e.getMessage(), e);
        }
    }

    public void tambahJenisIkan(String nama, String kategori, String deskripsi, String gambarPath) {
        simpanJenisIkan(null, nama, kategori, deskripsi, gambarPath);
    }

    public void simpanJenisIkan(Integer id, String nama, String kategori, String deskripsi, String gambarPath) {
        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama jenis ikan wajib diisi");
        try (var c = Database.connect()) {
            if (id == null) {
                int rowId = Database.insertAndGetId(c, "INSERT INTO jenis_ikan(nama,kategori,deskripsi,gambar_path,aktif) VALUES(?,?,?,?,1)",
                        nama.trim(), blank(kategori, "Ikan"), blank(deskripsi, "Jenis ikan " + nama.trim()), blank(gambarPath, "/images/catokan_banner.png"));
                Database.execute(c, "INSERT INTO stok_mentah(jenis_ikan_id,total_kg,updated_at) VALUES(?,0,datetime('now'))", rowId);
            } else {
                Database.execute(c, "UPDATE jenis_ikan SET nama=?, kategori=?, deskripsi=?, gambar_path=? WHERE id=?",
                        nama.trim(), blank(kategori, "Ikan"), blank(deskripsi, "Jenis ikan " + nama.trim()), blank(gambarPath, "/images/catokan_banner.png"), id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan jenis ikan: " + e.getMessage(), e);
        }
    }

    public void hapusJenisIkan(int id) {
        boolean dipakai = !Database.query("SELECT id FROM stok_masuk WHERE jenis_ikan_id=? LIMIT 1", id).isEmpty()
                || !Database.query("SELECT id FROM produksi_giling WHERE jenis_ikan_id=? LIMIT 1", id).isEmpty()
                || !Database.query("SELECT id FROM detail_penjualan WHERE jenis_ikan_id=? LIMIT 1", id).isEmpty();
        if (dipakai) {
            throw new IllegalArgumentException("Jenis ikan sudah dipakai pada transaksi, tidak bisa dihapus. Ubah datanya saja.");
        }
        try (var c = Database.connect()) {
            Database.execute(c, "DELETE FROM stok_mentah WHERE jenis_ikan_id=?", id);
            Database.execute(c, "DELETE FROM jenis_ikan WHERE id=?", id);
        } catch (Exception e) {
            throw new RuntimeException("Gagal menghapus jenis ikan: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> jenisIkanRows() {
        return Database.query("SELECT id,nama,kategori,deskripsi,gambar_path FROM jenis_ikan WHERE aktif=1 ORDER BY nama");
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private List<OptionItem> options(String sql) {
        List<OptionItem> list = new ArrayList<>();
        for (Map<String, Object> row : Database.query(sql)) {
            list.add(new OptionItem(((Number) row.get("id")).intValue(), String.valueOf(row.get("nama"))));
        }
        return list;
    }
}
