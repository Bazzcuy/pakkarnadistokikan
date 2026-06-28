package com.bagas.stokikan.service;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.util.DateUtil;

public class StockService {
    private final DbHelper dbh;

    public StockService(DbHelper dbh) { this.dbh = dbh; }

    public void inputStokMentah(int jenisIkanId, int supplierId, double beratKg, double hargaBeli, String catatan) {
        if (beratKg <= 0 || hargaBeli <= 0) throw new IllegalArgumentException("Berat dan harga beli harus lebih dari 0");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double before = dbh.scalar("SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=?", String.valueOf(jenisIkanId));
            double after = before + beratKg;

            ContentValues sm = new ContentValues();
            sm.put("tanggal", DateUtil.today());
            sm.put("jenis_ikan_id", jenisIkanId);
            sm.put("supplier_id", supplierId);
            sm.put("berat_kg", beratKg);
            sm.put("harga_beli_per_kg", hargaBeli);
            sm.put("total_beli", beratKg * hargaBeli);
            sm.put("catatan", catatan);
            db.insert("stok_masuk", null, sm);

            ContentValues stok = new ContentValues();
            stok.put("total_kg", after);
            stok.put("updated_at", DateUtil.now());
            db.update("stok_mentah", stok, "jenis_ikan_id=?", new String[]{String.valueOf(jenisIkanId)});

            riwayat(db, jenisIkanId, "STOK_MASUK", "MENTAH", "stok_masuk", beratKg, before, after, catatan);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String prosesProduksi(int jenisIkanId, double mentah, double hasil, double biaya, double hargaJual, String catatan) {
        if (mentah <= 0 || hasil <= 0 || hargaJual <= 0) throw new IllegalArgumentException("Input produksi belum valid");
        if (hasil > mentah) throw new IllegalArgumentException("Berat hasil tidak boleh melebihi berat mentah");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double before = dbh.scalar("SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=?", String.valueOf(jenisIkanId));
            if (before < mentah) throw new IllegalArgumentException("Stok mentah tidak cukup. Stok tersedia: " + before + " kg");
            String batch = DateUtil.batchNo(dbh.nextId("produksi_giling"));
            double after = before - mentah;
            double susut = mentah - hasil;

            ContentValues prod = new ContentValues();
            prod.put("batch_no", batch);
            prod.put("tanggal", DateUtil.today());
            prod.put("jenis_ikan_id", jenisIkanId);
            prod.put("berat_mentah_kg", mentah);
            prod.put("berat_hasil_kg", hasil);
            prod.put("penyusutan_kg", susut);
            prod.put("biaya_produksi", biaya);
            prod.put("harga_jual_per_kg", hargaJual);
            prod.put("catatan", catatan);
            db.insert("produksi_giling", null, prod);

            ContentValues stockUpdate = new ContentValues();
            stockUpdate.put("total_kg", after);
            stockUpdate.put("updated_at", DateUtil.now());
            db.update("stok_mentah", stockUpdate, "jenis_ikan_id=?", new String[]{String.valueOf(jenisIkanId)});

            ContentValues giling = new ContentValues();
            giling.put("jenis_ikan_id", jenisIkanId);
            giling.put("batch_no", batch);
            giling.put("total_kg", hasil);
            giling.put("harga_jual_per_kg", hargaJual);
            giling.put("tanggal_produksi", DateUtil.today());
            giling.put("status_stok", "TERSEDIA");
            db.insert("stok_giling", null, giling);

            riwayat(db, jenisIkanId, "PRODUKSI_KURANG_MENTAH", "MENTAH", batch, -mentah, before, after, "Bahan produksi");
            riwayat(db, jenisIkanId, "PRODUKSI_TAMBAH_GILING", "GILING", batch, hasil, 0, hasil, "Hasil produksi giling");
            db.setTransactionSuccessful();
            return batch;
        } finally {
            db.endTransaction();
        }
    }

    public String jualCepat(User user, int pelangganId, int stokGilingId, double kg, String metode, double bayar) {
        if (kg <= 0) throw new IllegalArgumentException("Jumlah kg harus lebih dari 0");
        if (bayar < 0) throw new IllegalArgumentException("Pembayaran tidak boleh negatif");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double stok = dbh.scalar("SELECT total_kg FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
            if (stok < kg) throw new IllegalArgumentException("Stok giling tidak cukup. Tersedia: " + stok + " kg");
            double harga = dbh.scalar("SELECT harga_jual_per_kg FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
            int jenisId = (int) dbh.scalar("SELECT jenis_ikan_id FROM stok_giling WHERE id=?", String.valueOf(stokGilingId));
            double total = kg * harga;
            if (bayar > total) throw new IllegalArgumentException("Pembayaran tidak boleh melebihi total transaksi");
            double sisa = Math.max(total - bayar, 0);
            String status = sisa <= 0 ? "LUNAS" : "BELUM_LUNAS";
            String nomor = DateUtil.trxNo(dbh.nextId("penjualan"));

            ContentValues pj = new ContentValues();
            pj.put("nomor_transaksi", nomor);
            pj.put("tanggal", DateUtil.today());
            pj.put("pelanggan_id", pelangganId);
            pj.put("kasir_id", user.id);
            pj.put("subtotal", total);
            pj.put("diskon", 0);
            pj.put("total", total);
            pj.put("status_pembayaran", status);
            long idPenjualan = db.insert("penjualan", null, pj);

            ContentValues detail = new ContentValues();
            detail.put("penjualan_id", idPenjualan);
            detail.put("stok_giling_id", stokGilingId);
            detail.put("jenis_ikan_id", jenisId);
            detail.put("jumlah_kg", kg);
            detail.put("harga_per_kg", harga);
            detail.put("subtotal", total);
            db.insert("detail_penjualan", null, detail);

            ContentValues pay = new ContentValues();
            pay.put("penjualan_id", idPenjualan);
            pay.put("tanggal", DateUtil.today());
            pay.put("metode", metode);
            pay.put("jumlah_bayar", bayar);
            pay.put("sisa_bayar", sisa);
            pay.put("status", status);
            pay.put("catatan", "Pembayaran saat transaksi");
            db.insert("pembayaran", null, pay);

            double after = stok - kg;
            ContentValues upd = new ContentValues();
            upd.put("total_kg", after);
            upd.put("status_stok", after <= 0 ? "HABIS" : "TERSEDIA");
            db.update("stok_giling", upd, "id=?", new String[]{String.valueOf(stokGilingId)});
            riwayat(db, jenisId, "PENJUALAN", "GILING", nomor, -kg, stok, after, "Penjualan ikan giling");
            db.setTransactionSuccessful();
            return nomor + " | Total Rp " + total + " | " + status;
        } finally {
            db.endTransaction();
        }
    }

    private void riwayat(SQLiteDatabase db, int jenisIkanId, String transaksi, String jenisStok, String ref, double perubahan, double before, double after, String ket) {
        ContentValues r = new ContentValues();
        r.put("tanggal", DateUtil.now());
        r.put("jenis_ikan_id", jenisIkanId);
        r.put("jenis_transaksi", transaksi);
        r.put("jenis_stok", jenisStok);
        r.put("referensi", ref);
        r.put("perubahan_kg", perubahan);
        r.put("stok_sebelum", before);
        r.put("stok_sesudah", after);
        r.put("keterangan", ket);
        db.insert("riwayat_stok", null, r);
    }
}
