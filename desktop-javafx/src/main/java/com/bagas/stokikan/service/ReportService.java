package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;

public class ReportService {
    public String dashboard() {
        var totalMentah = Database.query("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_mentah").get(0).get("v");
        var totalGiling = Database.query("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_giling").get(0).get("v");
        var totalJual = Database.query("SELECT IFNULL(SUM(total),0) AS v FROM penjualan").get(0).get("v");
        var piutang = Database.query("SELECT IFNULL(SUM(p.total - IFNULL(b.bayar,0)),0) AS v FROM penjualan p LEFT JOIN (SELECT penjualan_id, SUM(jumlah_bayar) AS bayar FROM pembayaran GROUP BY penjualan_id) b ON b.penjualan_id=p.id WHERE p.status_pembayaran='BELUM_LUNAS'").get(0).get("v");
        return "DASHBOARD CATOKAN\n" +
                "================\n\n" +
                String.format("%-26s : %s kg\n", "Total stok mentah", totalMentah) +
                String.format("%-26s : %s kg\n", "Total stok giling", totalGiling) +
                String.format("%-26s : Rp %s\n", "Total nilai penjualan", totalJual) +
                String.format("%-26s : Rp %s\n\n", "Sisa belum lunas", piutang) +
                "Akun demo: admin/admin123, kasir/kasir123, operator/operator123";
    }

    public String laporanRingkas() {
        StringBuilder sb = new StringBuilder(dashboard()).append("\n\nRIWAYAT STOK TERBARU\n====================\n\n");
        var rows = Database.query("SELECT tanggal,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan FROM riwayat_stok ORDER BY id DESC LIMIT 20");
        for (var r : rows) sb.append(String.format("%s | %s | %s | %s kg | %s -> %s | %s\n", r.get("tanggal"), r.get("jenis_transaksi"), r.get("jenis_stok"), r.get("perubahan_kg"), r.get("stok_sebelum"), r.get("stok_sesudah"), r.get("keterangan")));
        return sb.toString();
    }
}
