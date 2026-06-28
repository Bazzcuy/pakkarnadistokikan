package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.util.DateUtil;

import java.sql.Connection;

public class SalesService {
    public String jualCepat(User kasir, int pelangganId, int stokGilingId, double jumlahKg, String metode, double jumlahBayar) {
        if (jumlahKg <= 0) throw new IllegalArgumentException("Jumlah kg harus lebih dari 0");
        if (jumlahBayar < 0) throw new IllegalArgumentException("Jumlah bayar tidak boleh negatif");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            double stok = Database.scalarDouble(c, "SELECT total_kg FROM stok_giling WHERE id=?", stokGilingId);
            if (stok < jumlahKg) throw new IllegalArgumentException("Stok giling tidak cukup. Stok tersedia: " + stok + " kg");
            double harga = Database.scalarDouble(c, "SELECT harga_jual_per_kg FROM stok_giling WHERE id=?", stokGilingId);
            double jenis = Database.scalarDouble(c, "SELECT jenis_ikan_id FROM stok_giling WHERE id=?", stokGilingId);
            double total = jumlahKg * harga;
            if (jumlahBayar > total) throw new IllegalArgumentException("Jumlah bayar tidak boleh melebihi total transaksi");
            double sisa = Math.max(total - jumlahBayar, 0);
            String status = sisa <= 0 ? "LUNAS" : "BELUM_LUNAS";
            String nomor = DateUtil.transactionNumber(Database.nextId(c, "penjualan"));
            try {
                int idPenjualan = Database.insertAndGetId(c, "INSERT INTO penjualan(nomor_transaksi,tanggal,pelanggan_id,kasir_id,subtotal,diskon,total,status_pembayaran) VALUES(?,?,?,?,?,?,?,?)", nomor, DateUtil.today(), pelangganId, kasir.getId(), total, 0, total, status);
                Database.insertAndGetId(c, "INSERT INTO detail_penjualan(penjualan_id,stok_giling_id,jenis_ikan_id,jumlah_kg,harga_per_kg,subtotal) VALUES(?,?,?,?,?,?)", idPenjualan, stokGilingId, (int) jenis, jumlahKg, harga, total);
                Database.insertAndGetId(c, "INSERT INTO pembayaran(penjualan_id,tanggal,metode,jumlah_bayar,sisa_bayar,status,catatan) VALUES(?,?,?,?,?,?,?)", idPenjualan, DateUtil.today(), metode, jumlahBayar, sisa, status, "Pembayaran saat transaksi");
                double stokSesudah = stok - jumlahKg;
                Database.execute(c, "UPDATE stok_giling SET total_kg=?, status_stok=? WHERE id=?", stokSesudah, stokSesudah <= 0 ? "HABIS" : "TERSEDIA", stokGilingId);
                Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), (int) jenis, "PENJUALAN", "GILING", nomor, -jumlahKg, stok, stokSesudah, "Penjualan ikan giling");
                c.commit();
                return nomor + " | Total: Rp " + total + " | Status: " + status;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan penjualan: " + e.getMessage(), e);
        }
    }

    public void bayarTransaksi(int penjualanId, double bayar, String metode) {
        if (bayar <= 0) throw new IllegalArgumentException("Pembayaran harus lebih dari 0");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            double total = Database.scalarDouble(c, "SELECT total FROM penjualan WHERE id=?", penjualanId);
            if (total <= 0) throw new IllegalArgumentException("ID penjualan tidak ditemukan");
            double sudahBayar = Database.scalarDouble(c, "SELECT IFNULL(SUM(jumlah_bayar),0) FROM pembayaran WHERE penjualan_id=?", penjualanId);
            double sisaSebelum = total - sudahBayar;
            if (sisaSebelum <= 0) throw new IllegalArgumentException("Transaksi sudah lunas");
            if (bayar > sisaSebelum) throw new IllegalArgumentException("Pembayaran melebihi sisa tagihan: " + sisaSebelum);
            double sisa = Math.max(total - sudahBayar - bayar, 0);
            String status = sisa <= 0 ? "LUNAS" : "BELUM_LUNAS";
            try {
                Database.insertAndGetId(c, "INSERT INTO pembayaran(penjualan_id,tanggal,metode,jumlah_bayar,sisa_bayar,status,catatan) VALUES(?,?,?,?,?,?,?)", penjualanId, DateUtil.today(), metode, bayar, sisa, status, "Pembayaran lanjutan");
                Database.execute(c, "UPDATE penjualan SET status_pembayaran=? WHERE id=?", status, penjualanId);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal menyimpan pembayaran: " + e.getMessage(), e);
        }
    }

    public String transaksiText() {
        StringBuilder sb = new StringBuilder("RIWAYAT PENJUALAN\n\n");
        var rows = Database.query("SELECT p.id,p.nomor_transaksi,p.tanggal,pl.nama AS pelanggan,p.total,p.status_pembayaran FROM penjualan p LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id ORDER BY p.id DESC");
        for (var r : rows) sb.append(String.format("ID %s | %s | %s | %s | Rp %s | %s\n", r.get("id"), r.get("nomor_transaksi"), r.get("tanggal"), r.get("pelanggan"), r.get("total"), r.get("status_pembayaran")));
        return sb.toString();
    }

    public String pembayaranText() {
        StringBuilder sb = new StringBuilder("TRANSAKSI BELUM LUNAS\n\n");
        var rows = Database.query("SELECT id,nomor_transaksi,tanggal,total,status_pembayaran FROM penjualan WHERE status_pembayaran='BELUM_LUNAS' ORDER BY id DESC");
        for (var r : rows) sb.append(String.format("ID %s | %s | %s | Total Rp %s | %s\n", r.get("id"), r.get("nomor_transaksi"), r.get("tanggal"), r.get("total"), r.get("status_pembayaran")));
        return sb.toString();
    }
}
