package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SalesService {
    public String jualFifo(User kasir, int pelangganId, int jenisIkanId, double jumlahKg, String metode, double jumlahBayar) {
        if (jumlahKg <= 0) throw new IllegalArgumentException("Jumlah kg harus lebih dari 0");
        if (jumlahBayar < 0) throw new IllegalArgumentException("Jumlah bayar tidak boleh negatif");
        if (metode == null || metode.isBlank()) throw new IllegalArgumentException("Metode bayar wajib diisi");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                double tersedia = Database.scalarDouble(c, "SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE jenis_ikan_id=? AND total_kg>0", jenisIkanId);
                if (tersedia < jumlahKg) throw new IllegalArgumentException("Stok giling tidak cukup. Stok tersedia: " + tersedia + " kg");

                List<AlokasiFifo> alokasi = new ArrayList<>();
                double sisaAmbil = jumlahKg;
                double total = 0;
                try (PreparedStatement ps = c.prepareStatement("SELECT id,batch_no,total_kg,harga_jual_per_kg FROM stok_giling WHERE jenis_ikan_id=? AND total_kg>0 ORDER BY date(tanggal_produksi), id")) {
                    ps.setInt(1, jenisIkanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next() && sisaAmbil > 0) {
                            double stokBatch = rs.getDouble("total_kg");
                            double ambil = Math.min(stokBatch, sisaAmbil);
                            double harga = rs.getDouble("harga_jual_per_kg");
                            alokasi.add(new AlokasiFifo(rs.getInt("id"), rs.getString("batch_no"), ambil, harga, stokBatch, stokBatch - ambil));
                            total += ambil * harga;
                            sisaAmbil -= ambil;
                        }
                    }
                }

                if (jumlahBayar > 0 && jumlahBayar < total) throw new IllegalArgumentException("Pembayaran harus lunas. Total transaksi: Rp " + total);
                if (jumlahBayar > total) throw new IllegalArgumentException("Jumlah bayar tidak boleh melebihi total transaksi");
                double bayarFinal = total;
                String nomor = DateUtil.transactionNumber(Database.nextId(c, "penjualan"));
                int idPenjualan = Database.insertAndGetId(c, "INSERT INTO penjualan(nomor_transaksi,tanggal,pelanggan_id,kasir_id,subtotal,diskon,total,status_pembayaran) VALUES(?,?,?,?,?,?,?,?)", nomor, DateUtil.today(), pelangganId, kasir.getId(), total, 0, total, "LUNAS");
                Database.insertAndGetId(c, "INSERT INTO pembayaran(penjualan_id,tanggal,metode,jumlah_bayar,sisa_bayar,status,catatan) VALUES(?,?,?,?,?,?,?)", idPenjualan, DateUtil.today(), metode.trim(), bayarFinal, 0, "LUNAS", "Pembayaran lunas saat transaksi");

                for (AlokasiFifo a : alokasi) {
                    Database.insertAndGetId(c, "INSERT INTO detail_penjualan(penjualan_id,stok_giling_id,jenis_ikan_id,jumlah_kg,harga_per_kg,subtotal) VALUES(?,?,?,?,?,?)", idPenjualan, a.stokGilingId, jenisIkanId, a.jumlahKg, a.hargaPerKg, a.jumlahKg * a.hargaPerKg);
                    Database.execute(c, "UPDATE stok_giling SET total_kg=?, status_stok=? WHERE id=?", a.stokSesudah, a.stokSesudah <= 0 ? "HABIS" : "TERSEDIA", a.stokGilingId);
                    Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "PENJUALAN", "GILING", nomor, -a.jumlahKg, a.stokSebelum, a.stokSesudah, "Penjualan mengambil stok produksi lama lebih dulu");
                }
                c.commit();
                return nomor + " | Total: Rp " + total + " | LUNAS";
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
            if (jumlahBayar > 0 && jumlahBayar < total) throw new IllegalArgumentException("Pembayaran harus lunas. Total transaksi: Rp " + total);
            if (jumlahBayar > total) throw new IllegalArgumentException("Jumlah bayar tidak boleh melebihi total transaksi");
            double jumlahBayarFinal = total;
            double sisa = 0;
            String status = "LUNAS";
            String nomor = DateUtil.transactionNumber(Database.nextId(c, "penjualan"));
            try {
                int idPenjualan = Database.insertAndGetId(c, "INSERT INTO penjualan(nomor_transaksi,tanggal,pelanggan_id,kasir_id,subtotal,diskon,total,status_pembayaran) VALUES(?,?,?,?,?,?,?,?)", nomor, DateUtil.today(), pelangganId, kasir.getId(), total, 0, total, status);
                Database.insertAndGetId(c, "INSERT INTO detail_penjualan(penjualan_id,stok_giling_id,jenis_ikan_id,jumlah_kg,harga_per_kg,subtotal) VALUES(?,?,?,?,?,?)", idPenjualan, stokGilingId, (int) jenis, jumlahKg, harga, total);
                Database.insertAndGetId(c, "INSERT INTO pembayaran(penjualan_id,tanggal,metode,jumlah_bayar,sisa_bayar,status,catatan) VALUES(?,?,?,?,?,?,?)", idPenjualan, DateUtil.today(), metode, jumlahBayarFinal, sisa, status, "Pembayaran lunas saat transaksi");
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
        throw new IllegalArgumentException("Sistem tidak menerima utang. Transaksi penjualan wajib lunas saat dibuat.");
    }

    public String batalkanPenjualan(String nomorTransaksi, String alasan) {
        if (nomorTransaksi == null || nomorTransaksi.isBlank()) throw new IllegalArgumentException("Nomor transaksi wajib diisi");
        if (alasan == null || alasan.isBlank()) throw new IllegalArgumentException("Alasan retur/batal wajib diisi");
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                int penjualanId = (int) Database.scalarDouble(c, "SELECT id FROM penjualan WHERE nomor_transaksi=?", nomorTransaksi.trim());
                if (penjualanId <= 0) throw new IllegalArgumentException("Transaksi tidak ditemukan");
                String status = "";
                try (PreparedStatement ps = c.prepareStatement("SELECT status_pembayaran FROM penjualan WHERE id=?")) {
                    ps.setInt(1, penjualanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) status = rs.getString(1);
                    }
                }
                if ("DIBATALKAN".equals(status)) throw new IllegalArgumentException("Transaksi sudah dibatalkan");

                try (PreparedStatement ps = c.prepareStatement("SELECT stok_giling_id,jenis_ikan_id,jumlah_kg FROM detail_penjualan WHERE penjualan_id=?")) {
                    ps.setInt(1, penjualanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int stokGilingId = rs.getInt("stok_giling_id");
                            int jenisIkanId = rs.getInt("jenis_ikan_id");
                            double kg = rs.getDouble("jumlah_kg");
                            double before = Database.scalarDouble(c, "SELECT total_kg FROM stok_giling WHERE id=?", stokGilingId);
                            double after = before + kg;
                            Database.execute(c, "UPDATE stok_giling SET total_kg=?, status_stok='TERSEDIA' WHERE id=?", after, stokGilingId);
                            Database.execute(c, "INSERT INTO riwayat_stok(tanggal,jenis_ikan_id,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan) VALUES(?,?,?,?,?,?,?,?,?)", DateUtil.now(), jenisIkanId, "RETUR_BATAL", "GILING", nomorTransaksi.trim(), kg, before, after, alasan.trim());
                        }
                    }
                }
                Database.execute(c, "UPDATE penjualan SET subtotal=0,diskon=0,total=0,status_pembayaran='DIBATALKAN' WHERE id=?", penjualanId);
                Database.execute(c, "UPDATE pembayaran SET jumlah_bayar=0,sisa_bayar=0,status='DIBATALKAN',catatan=? WHERE penjualan_id=?", alasan.trim(), penjualanId);
                c.commit();
                return "Transaksi " + nomorTransaksi.trim() + " dibatalkan dan stok dikembalikan.";
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gagal membatalkan transaksi: " + e.getMessage(), e);
        }
    }

    public String batalkanPenjualan(int penjualanId, String alasan) {
        if (penjualanId <= 0) throw new IllegalArgumentException("Pilih transaksi yang akan dibatalkan");
        String nomor = "";
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement("SELECT nomor_transaksi FROM penjualan WHERE id=?")) {
            ps.setInt(1, penjualanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) nomor = rs.getString(1);
            }
        } catch (Exception e) {
            throw new RuntimeException("Gagal membaca transaksi: " + e.getMessage(), e);
        }
        if (nomor.isBlank()) throw new IllegalArgumentException("Transaksi tidak ditemukan");
        return batalkanPenjualan(nomor, alasan);
    }

    public String transaksiText() {
        StringBuilder sb = new StringBuilder("RIWAYAT PENJUALAN\n\n");
        var rows = Database.query("SELECT p.id,p.nomor_transaksi,p.tanggal,pl.nama AS pelanggan,p.total,p.status_pembayaran FROM penjualan p LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id ORDER BY p.id DESC");
        for (var r : rows) sb.append(String.format("ID %s | %s | %s | %s | Rp %s | %s\n", r.get("id"), r.get("nomor_transaksi"), r.get("tanggal"), r.get("pelanggan"), r.get("total"), r.get("status_pembayaran")));
        return sb.toString();
    }

    public String pembayaranText() {
        return "Semua transaksi penjualan wajib lunas saat dibuat.\nTidak ada menu piutang atau pembayaran lanjutan.";
    }

    private record AlokasiFifo(int stokGilingId, String batchNo, double jumlahKg, double hargaPerKg, double stokSebelum, double stokSesudah) {}
}
