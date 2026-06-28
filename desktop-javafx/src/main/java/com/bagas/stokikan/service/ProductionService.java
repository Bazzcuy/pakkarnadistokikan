package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.util.DateUtil;

import java.sql.Connection;

public class ProductionService {
    public String prosesProduksi(int jenisIkanId, double beratMentah, double beratHasil, double biayaProduksi, double hargaJual, String catatan) {
        if (beratMentah <= 0 || beratHasil <= 0 || hargaJual <= 0) throw new IllegalArgumentException("Berat dan harga jual harus valid");
        if (beratHasil > beratMentah) throw new IllegalArgumentException("Berat hasil tidak boleh melebihi berat mentah");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                double stokSebelum = Database.scalarDouble(c, "SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=?", jenisIkanId);
                if (stokSebelum < beratMentah) throw new IllegalArgumentException("Stok mentah tidak cukup. Stok tersedia: " + stokSebelum + " kg");
                String batch = DateUtil.batchNumber(Database.nextId(c, "produksi_giling"));
                double penyusutan = beratMentah - beratHasil;
                double stokMentahSesudah = stokSebelum - beratMentah;
                Database.execute(c, "UPDATE stok_mentah SET total_kg=?, updated_at=? WHERE jenis_ikan_id=?", stokMentahSesudah, DateUtil.now(), jenisIkanId);
                Database.insertAndGetId(c, "INSERT INTO produksi_giling(batch_no,tanggal,jenis_ikan_id,berat_mentah_kg,berat_hasil_kg,penyusutan_kg,biaya_produksi,harga_jual_per_kg,catatan) VALUES(?,?,?,?,?,?,?,?,?)", batch, DateUtil.today(), jenisIkanId, beratMentah, beratHasil, penyusutan, biayaProduksi, hargaJual, catatan);
                Database.insertAndGetId(c, "INSERT INTO stok_giling(jenis_ikan_id,batch_no,total_kg,harga_jual_per_kg,tanggal_produksi,status_stok) VALUES(?,?,?,?,?,?)", jenisIkanId, batch, beratHasil, hargaJual, DateUtil.today(), "TERSEDIA");
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "PRODUKSI_KURANG_MENTAH", "MENTAH", batch, -beratMentah, stokSebelum, stokMentahSesudah, "Bahan produksi");
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "PRODUKSI_TAMBAH_GILING", "GILING", batch, beratHasil, 0, beratHasil, "Hasil produksi giling");
                c.commit();
                return batch;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal memproses produksi: " + e.getMessage(), e);
        }
    }
}
