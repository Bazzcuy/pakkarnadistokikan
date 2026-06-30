package com.bagas.stokikan.service;

import android.content.ContentValues;
import android.database.Cursor;
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
            double before = dbh.scalar("SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=? AND owner_user_id=?", String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId()));
            double after = before + beratKg;

            ContentValues sm = new ContentValues();
            sm.put("tanggal", DateUtil.today());
            sm.put("jenis_ikan_id", jenisIkanId);
            sm.put("supplier_id", supplierId);
            sm.put("berat_kg", beratKg);
            sm.put("harga_beli_per_kg", hargaBeli);
            sm.put("total_beli", beratKg * hargaBeli);
            sm.put("catatan", catatan);
            db.insert("stok_masuk", null, owned(sm));

            ContentValues stok = new ContentValues();
            stok.put("total_kg", after);
            stok.put("updated_at", DateUtil.now());
            db.update("stok_mentah", stok, "jenis_ikan_id=? AND owner_user_id=?", new String[]{String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId())});

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
            double before = dbh.scalar("SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=? AND owner_user_id=?", String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId()));
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
            db.insert("produksi_giling", null, owned(prod));

            ContentValues stockUpdate = new ContentValues();
            stockUpdate.put("total_kg", after);
            stockUpdate.put("updated_at", DateUtil.now());
            db.update("stok_mentah", stockUpdate, "jenis_ikan_id=? AND owner_user_id=?", new String[]{String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId())});

            ContentValues giling = new ContentValues();
            giling.put("jenis_ikan_id", jenisIkanId);
            giling.put("batch_no", batch);
            giling.put("total_kg", hasil);
            giling.put("harga_jual_per_kg", hargaJual);
            giling.put("tanggal_produksi", DateUtil.today());
            giling.put("status_stok", "TERSEDIA");
            db.insert("stok_giling", null, owned(giling));

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
            double stok = dbh.scalar("SELECT total_kg FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
            if (stok < kg) throw new IllegalArgumentException("Stok giling tidak cukup. Tersedia: " + stok + " kg");
            double harga = dbh.scalar("SELECT harga_jual_per_kg FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
            int jenisId = (int) dbh.scalar("SELECT jenis_ikan_id FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
            double total = kg * harga;
            if (bayar > 0 && bayar < total) throw new IllegalArgumentException("Pembayaran harus lunas. Total transaksi: Rp " + total);
            if (bayar > total) throw new IllegalArgumentException("Pembayaran tidak boleh melebihi total transaksi");
            double bayarFinal = total;
            double sisa = 0;
            String status = "LUNAS";
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
            long idPenjualan = db.insert("penjualan", null, owned(pj));

            ContentValues detail = new ContentValues();
            detail.put("penjualan_id", idPenjualan);
            detail.put("stok_giling_id", stokGilingId);
            detail.put("jenis_ikan_id", jenisId);
            detail.put("jumlah_kg", kg);
            detail.put("harga_per_kg", harga);
            detail.put("subtotal", total);
            db.insert("detail_penjualan", null, owned(detail));

            ContentValues pay = new ContentValues();
            pay.put("penjualan_id", idPenjualan);
            pay.put("tanggal", DateUtil.today());
            pay.put("metode", metode);
            pay.put("jumlah_bayar", bayarFinal);
            pay.put("sisa_bayar", sisa);
            pay.put("status", status);
            pay.put("catatan", "Pembayaran saat transaksi");
            db.insert("pembayaran", null, owned(pay));

            double after = stok - kg;
            ContentValues upd = new ContentValues();
            upd.put("total_kg", after);
            upd.put("status_stok", after <= 0 ? "HABIS" : "TERSEDIA");
            db.update("stok_giling", upd, "id=? AND owner_user_id=?", new String[]{String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId())});
            riwayat(db, jenisId, "PENJUALAN", "GILING", nomor, -kg, stok, after, "Penjualan ikan giling");
            db.setTransactionSuccessful();
            return nomor + " | Total Rp " + total + " | " + status;
        } finally {
            db.endTransaction();
        }
    }

    public String jualFifo(User user, int pelangganId, int jenisIkanId, double kg, String metode, double bayar) {
        if (kg <= 0) throw new IllegalArgumentException("Jumlah kg harus lebih dari 0");
        if (bayar < 0) throw new IllegalArgumentException("Pembayaran tidak boleh negatif");
        if (metode == null || metode.trim().isEmpty()) throw new IllegalArgumentException("Metode bayar wajib diisi");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double tersedia = dbh.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE jenis_ikan_id=? AND total_kg>0 AND owner_user_id=?", String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId()));
            if (tersedia < kg) throw new IllegalArgumentException("Stok giling tidak cukup. Tersedia: " + tersedia + " kg");
            String nomor = DateUtil.trxNo(dbh.nextId("penjualan"));
            double sisaAmbil = kg;
            double total = 0;

            Cursor cursor = db.rawQuery("SELECT id,batch_no,total_kg,harga_jual_per_kg FROM stok_giling WHERE jenis_ikan_id=? AND total_kg>0 AND owner_user_id=? ORDER BY date(tanggal_produksi), id", new String[]{String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId())});
            try {
                while (cursor.moveToNext() && sisaAmbil > 0) {
                    double stokBatch = cursor.getDouble(2);
                    double ambil = Math.min(stokBatch, sisaAmbil);
                    double harga = cursor.getDouble(3);
                    total += ambil * harga;
                    sisaAmbil -= ambil;
                }
            } finally {
                cursor.close();
            }

            if (bayar > 0 && bayar < total) throw new IllegalArgumentException("Pembayaran harus lunas. Total transaksi: Rp " + total);
            if (bayar > total) throw new IllegalArgumentException("Pembayaran tidak boleh melebihi total transaksi");

            ContentValues pj = new ContentValues();
            pj.put("nomor_transaksi", nomor);
            pj.put("tanggal", DateUtil.today());
            pj.put("pelanggan_id", pelangganId);
            pj.put("kasir_id", user.id);
            pj.put("subtotal", total);
            pj.put("diskon", 0);
            pj.put("total", total);
            pj.put("status_pembayaran", "LUNAS");
            long idPenjualan = db.insert("penjualan", null, owned(pj));

            ContentValues pay = new ContentValues();
            pay.put("penjualan_id", idPenjualan);
            pay.put("tanggal", DateUtil.today());
            pay.put("metode", metode.trim());
            pay.put("jumlah_bayar", total);
            pay.put("sisa_bayar", 0);
            pay.put("status", "LUNAS");
            pay.put("catatan", "Pembayaran lunas saat transaksi");
            db.insert("pembayaran", null, owned(pay));

            int jumlahBatch = 0;
            sisaAmbil = kg;
            cursor = db.rawQuery("SELECT id,batch_no,total_kg,harga_jual_per_kg FROM stok_giling WHERE jenis_ikan_id=? AND total_kg>0 AND owner_user_id=? ORDER BY date(tanggal_produksi), id", new String[]{String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId())});
            try {
                while (cursor.moveToNext() && sisaAmbil > 0) {
                    int stokGilingId = cursor.getInt(0);
                    String batchNo = cursor.getString(1);
                    double stokBatch = cursor.getDouble(2);
                    double harga = cursor.getDouble(3);
                    double ambil = Math.min(stokBatch, sisaAmbil);
                    double after = stokBatch - ambil;

                    ContentValues detail = new ContentValues();
                    detail.put("penjualan_id", idPenjualan);
                    detail.put("stok_giling_id", stokGilingId);
                    detail.put("jenis_ikan_id", jenisIkanId);
                    detail.put("jumlah_kg", ambil);
                    detail.put("harga_per_kg", harga);
                    detail.put("subtotal", ambil * harga);
                    db.insert("detail_penjualan", null, owned(detail));

                    ContentValues upd = new ContentValues();
                    upd.put("total_kg", after);
                    upd.put("status_stok", after <= 0 ? "HABIS" : "TERSEDIA");
                    db.update("stok_giling", upd, "id=? AND owner_user_id=?", new String[]{String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId())});
                    riwayat(db, jenisIkanId, "PENJUALAN", "GILING", nomor, -ambil, stokBatch, after, "Penjualan mengambil stok produksi lama lebih dulu");
                    sisaAmbil -= ambil;
                    jumlahBatch++;
                }
            } finally {
                cursor.close();
            }
            db.setTransactionSuccessful();
            return nomor + " | Total Rp " + total + " | LUNAS";
        } finally {
            db.endTransaction();
        }
    }

    public String batalkanPenjualan(String nomor, String alasan) {
        if (nomor == null || nomor.trim().isEmpty()) throw new IllegalArgumentException("Nomor transaksi wajib diisi");
        if (alasan == null || alasan.trim().isEmpty()) throw new IllegalArgumentException("Alasan retur/batal wajib diisi");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            int penjualanId = (int) dbh.scalar("SELECT id FROM penjualan WHERE nomor_transaksi=? AND owner_user_id=?", nomor.trim(), String.valueOf(DbHelper.currentUserId()));
            if (penjualanId <= 0) throw new IllegalArgumentException("Transaksi tidak ditemukan");
            String status = "";
            try (Cursor statusCursor = db.rawQuery("SELECT status_pembayaran FROM penjualan WHERE id=? AND owner_user_id=?", new String[]{String.valueOf(penjualanId), String.valueOf(DbHelper.currentUserId())})) {
                if (statusCursor.moveToFirst()) status = statusCursor.getString(0);
            }
            if ("DIBATALKAN".equals(status)) throw new IllegalArgumentException("Transaksi sudah dibatalkan");

            try (Cursor c = db.rawQuery("SELECT stok_giling_id,jenis_ikan_id,jumlah_kg FROM detail_penjualan WHERE penjualan_id=? AND owner_user_id=?", new String[]{String.valueOf(penjualanId), String.valueOf(DbHelper.currentUserId())})) {
                while (c.moveToNext()) {
                    int stokGilingId = c.getInt(0);
                    int jenisIkanId = c.getInt(1);
                    double kg = c.getDouble(2);
                    double before = dbh.scalar("SELECT total_kg FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
                    double after = before + kg;
                    ContentValues update = new ContentValues();
                    update.put("total_kg", after);
                    update.put("status_stok", "TERSEDIA");
                    db.update("stok_giling", update, "id=? AND owner_user_id=?", new String[]{String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId())});
                    riwayat(db, jenisIkanId, "RETUR_BATAL", "GILING", nomor.trim(), kg, before, after, alasan.trim());
                }
            }

            ContentValues penjualan = new ContentValues();
            penjualan.put("subtotal", 0);
            penjualan.put("diskon", 0);
            penjualan.put("total", 0);
            penjualan.put("status_pembayaran", "DIBATALKAN");
            db.update("penjualan", penjualan, "id=? AND owner_user_id=?", new String[]{String.valueOf(penjualanId), String.valueOf(DbHelper.currentUserId())});
            ContentValues pembayaran = new ContentValues();
            pembayaran.put("jumlah_bayar", 0);
            pembayaran.put("sisa_bayar", 0);
            pembayaran.put("status", "DIBATALKAN");
            pembayaran.put("catatan", alasan.trim());
            db.update("pembayaran", pembayaran, "penjualan_id=? AND owner_user_id=?", new String[]{String.valueOf(penjualanId), String.valueOf(DbHelper.currentUserId())});
            db.setTransactionSuccessful();
            return "Transaksi " + nomor.trim() + " dibatalkan dan stok dikembalikan.";
        } finally {
            db.endTransaction();
        }
    }

    public String batalkanPenjualan(int penjualanId, String alasan) {
        if (penjualanId <= 0) throw new IllegalArgumentException("Pilih transaksi yang akan dibatalkan");
        String nomor = "";
        SQLiteDatabase db = dbh.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT nomor_transaksi FROM penjualan WHERE id=? AND owner_user_id=?", new String[]{String.valueOf(penjualanId), String.valueOf(DbHelper.currentUserId())})) {
            if (c.moveToFirst()) nomor = c.getString(0);
        }
        if (nomor.isEmpty()) throw new IllegalArgumentException("Transaksi tidak ditemukan");
        return batalkanPenjualan(nomor, alasan);
    }

    public String sesuaikanStokMentah(int jenisIkanId, double stokFisik, String alasan) {
        if (stokFisik < 0) throw new IllegalArgumentException("Jumlah stok hasil hitung tidak boleh negatif");
        if (alasan == null || alasan.trim().isEmpty()) throw new IllegalArgumentException("Alasan perubahan stok wajib diisi");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double before = dbh.scalar("SELECT total_kg FROM stok_mentah WHERE jenis_ikan_id=? AND owner_user_id=?", String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId()));
            double selisih = stokFisik - before;
            ContentValues update = new ContentValues();
            update.put("total_kg", stokFisik);
            update.put("updated_at", DateUtil.now());
            db.update("stok_mentah", update, "jenis_ikan_id=? AND owner_user_id=?", new String[]{String.valueOf(jenisIkanId), String.valueOf(DbHelper.currentUserId())});
            penyesuaian(db, "MENTAH", before, stokFisik, selisih, alasan.trim());
            riwayat(db, jenisIkanId, "PERBAIKAN_STOK", "MENTAH", "cek ulang stok", selisih, before, stokFisik, alasan.trim());
            db.setTransactionSuccessful();
            return "Stok mentah diperbarui. Selisih: " + selisih + " kg";
        } finally {
            db.endTransaction();
        }
    }

    public String sesuaikanStokGiling(int stokGilingId, double stokFisik, String alasan) {
        if (stokFisik < 0) throw new IllegalArgumentException("Jumlah stok hasil hitung tidak boleh negatif");
        if (alasan == null || alasan.trim().isEmpty()) throw new IllegalArgumentException("Alasan perubahan stok wajib diisi");
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.beginTransaction();
        try {
            double before = dbh.scalar("SELECT total_kg FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
            int jenisIkanId = (int) dbh.scalar("SELECT jenis_ikan_id FROM stok_giling WHERE id=? AND owner_user_id=?", String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId()));
            double selisih = stokFisik - before;
            ContentValues update = new ContentValues();
            update.put("total_kg", stokFisik);
            update.put("status_stok", stokFisik <= 0 ? "HABIS" : "TERSEDIA");
            db.update("stok_giling", update, "id=? AND owner_user_id=?", new String[]{String.valueOf(stokGilingId), String.valueOf(DbHelper.currentUserId())});
            penyesuaian(db, "GILING", before, stokFisik, selisih, alasan.trim());
            riwayat(db, jenisIkanId, "PERBAIKAN_STOK", "GILING", "cek ulang stok", selisih, before, stokFisik, alasan.trim());
            db.setTransactionSuccessful();
            return "Stok giling diperbarui. Selisih: " + selisih + " kg";
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
        db.insert("riwayat_stok", null, owned(r));
    }

    private void penyesuaian(SQLiteDatabase db, String jenisStok, double sistem, double fisik, double selisih, String alasan) {
        ContentValues p = new ContentValues();
        p.put("tanggal", DateUtil.now());
        p.put("jenis_stok", jenisStok);
        p.put("stok_sistem", sistem);
        p.put("stok_fisik", fisik);
        p.put("selisih", selisih);
        p.put("alasan", alasan);
        db.insert("penyesuaian_stok", null, owned(p));
    }

    private ContentValues owned(ContentValues values) {
        values.put("owner_user_id", DbHelper.currentUserId());
        return values;
    }
}
