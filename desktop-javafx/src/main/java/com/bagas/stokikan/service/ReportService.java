package com.bagas.stokikan.service;

import com.bagas.stokikan.db.Database;

public class ReportService {
    public String dashboard() {
        var totalMentah = Database.query("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_mentah").get(0).get("v");
        var totalGiling = Database.query("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_giling").get(0).get("v");
        var totalJual = Database.query("SELECT IFNULL(SUM(total),0) AS v FROM penjualan").get(0).get("v");
        var stokLama = Database.query("SELECT IFNULL(SUM(total_kg),0) AS v FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')").get(0).get("v");
        return "DASHBOARD CATOKAN\n" +
                "================\n\n" +
                String.format("%-26s : %s kg\n", "Total stok mentah", totalMentah) +
                String.format("%-26s : %s kg\n", "Total stok giling", totalGiling) +
                String.format("%-26s : Rp %s\n", "Total penjualan lunas", totalJual) +
                String.format("%-26s : %s kg\n\n", "Stok lama FIFO", stokLama) +
                "Akun awal pengguna: pengguna/pengguna123";
    }

    public String laporanRingkas() {
        StringBuilder sb = new StringBuilder(dashboard()).append("\n\nRIWAYAT STOK TERBARU\n====================\n\n");
        var rows = Database.query("SELECT tanggal,jenis_transaksi,jenis_stok,referensi,perubahan_kg,stok_sebelum,stok_sesudah,keterangan FROM riwayat_stok ORDER BY id DESC LIMIT 20");
        for (var r : rows) sb.append(String.format("%s | %s | %s | %s kg | %s -> %s | %s\n", r.get("tanggal"), r.get("jenis_transaksi"), r.get("jenis_stok"), r.get("perubahan_kg"), r.get("stok_sebelum"), r.get("stok_sesudah"), r.get("keterangan")));
        return sb.toString();
    }
}
