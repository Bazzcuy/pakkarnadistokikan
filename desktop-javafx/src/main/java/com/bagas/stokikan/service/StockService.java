package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.util.DateUtil;

import java.sql.Connection;

public class StockService {
    public void inputStokMentah(int jenisIkanId, int supplierId, double beratKg, double hargaBeli, String catatan) {
        if (beratKg <= 0 || hargaBeli <= 0) throw new IllegalArgumentException("Berat dan harga harus lebih dari 0");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                double before = Database.scalarDouble(c, "SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=?", jenisIkanId);
                double after = before + beratKg;
                Database.insertAndGetId(c, "INSERT INTO stok_masuk(tanggal,jenis_ikan_id,supplier_id,berat_kg,harga_beli_per_kg,total_beli,catatan) VALUES(?,?,?,?,?,?,?)", DateUtil.today(), jenisIkanId, supplierId, beratKg, hargaBeli, beratKg * hargaBeli, catatan);
                Database.execute(c, "INSERT INTO stok_mentah(jenis_ikan_id,total_kg,updated_at) VALUES(?,?,?) ON CONFLICT(jenis_ikan_id) DO UPDATE SET total_kg=?, updated_at=?", jenisIkanId, after, DateUtil.now(), after, DateUtil.now());
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "STOK_MASUK", "MENTAH", "stok_masuk", beratKg, before, after, catatan);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan stok mentah: " + e.getMessage(), e);
        }
    }

    public String stokMentahText() {
        StringBuilder sb = new StringBuilder("DAFTAR STOK IKAN MENTAH\n\n");
        var rows = Database.query("SELECT j.nama AS jenis_ikan, s.total_kg, s.updated_at FROM stok_mentah s JOIN jenis_ikan j ON j.id=s.jenis_ikan_id ORDER BY j.nama");
        for (var r : rows) sb.append(String.format("%-15s : %s kg  | update: %s\n", r.get("jenis_ikan"), r.get("total_kg"), r.get("updated_at")));
        return sb.toString();
    }

    public String sesuaikanStokMentah(int jenisIkanId, double stokFisik, String alasan) {
        if (stokFisik < 0) throw new IllegalArgumentException("Stok fisik tidak boleh negatif");
        if (alasan == null || alasan.isBlank()) throw new IllegalArgumentException("Alasan penyesuaian wajib diisi");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                double before = Database.scalarDouble(c, "SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=?", jenisIkanId);
                double selisih = stokFisik - before;
                Database.execute(c, "UPDATE stok_mentah SET total_kg=?, updated_at=? WHERE jenis_ikan_id=?", stokFisik, DateUtil.now(), jenisIkanId);
                Database.insertAndGetId(c, "INSERT INTO penyesuaian_stok(tanggal,jenis_stok,stok_sistem,stok_fisik,selisih,alasan) VALUES(?,?,?,?,?,?)", DateUtil.now(), "MENTAH", before, stokFisik, selisih, alasan.trim());
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "PENYESUAIAN_STOK", "MENTAH", "opname", selisih, before, stokFisik, alasan.trim());
                c.commit();
                return "Stok mentah disesuaikan. Selisih: " + selisih + " kg";
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyesuaikan stok mentah: " + e.getMessage(), e);
        }
    }

    public String sesuaikanStokGiling(int stokGilingId, double stokFisik, String alasan) {
        if (stokFisik < 0) throw new IllegalArgumentException("Stok fisik tidak boleh negatif");
        if (alasan == null || alasan.isBlank()) throw new IllegalArgumentException("Alasan penyesuaian wajib diisi");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                double before = Database.scalarDouble(c, "SELECT total_kg FROM stok_giling WHERE id=?", stokGilingId);
                int jenisIkanId = (int) Database.scalarDouble(c, "SELECT jenis_ikan_id FROM stok_giling WHERE id=?", stokGilingId);
                double selisih = stokFisik - before;
                Database.execute(c, "UPDATE stok_giling SET total_kg=?, status_stok=? WHERE id=?", stokFisik, stokFisik <= 0 ? "HABIS" : "TERSEDIA", stokGilingId);
                Database.insertAndGetId(c, "INSERT INTO penyesuaian_stok(tanggal,jenis_stok,stok_sistem,stok_fisik,selisih,alasan) VALUES(?,?,?,?,?,?)", DateUtil.now(), "GILING", before, stokFisik, selisih, alasan.trim());
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "PENYESUAIAN_STOK", "GILING", "opname", selisih, before, stokFisik, alasan.trim());
                c.commit();
                return "Stok giling disesuaikan. Selisih: " + selisih + " kg";
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyesuaikan stok giling: " + e.getMessage(), e);
        }
    }

    public String stokGilingText() {
        StringBuilder sb = new StringBuilder("DAFTAR STOK IKAN GILING PER BATCH\n\n");
        var rows = Database.query("SELECT g.id, g.batch_no, j.nama AS jenis_ikan, g.total_kg, g.harga_jual_per_kg, g.tanggal_produksi, g.status_stok FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id ORDER BY g.id DESC");
        for (var r : rows) sb.append(String.format("ID %s | %s | %-10s | %s kg | Rp %s/kg | %s | %s\n", r.get("id"), r.get("batch_no"), r.get("jenis_ikan"), r.get("total_kg"), r.get("harga_jual_per_kg"), r.get("tanggal_produksi"), r.get("status_stok")));
        return sb.toString();
    }
}
