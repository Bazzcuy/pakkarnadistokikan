package com.bagas.stokikan;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;

public class PartnerMasterActivity extends Activity {
    private DbHelper db;
    private LinearLayout list;
    private EditText id;
    private EditText nama;
    private EditText hp;
    private EditText alamat;
    private EditText info;
    private boolean supplierMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        supplierMode = "supplier".equals(getIntent().getStringExtra("mode"));
        setContentView(view());
        load();
    }

    private ScrollView view() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = column(16);
        scroll.addView(root);

        LinearLayout form = card();
        root.addView(form);
        title(form, supplierMode ? "Data Supplier" : "Data Pelanggan");
        subtitle(form, supplierMode ? "Supplier dipakai saat input stok ikan mentah masuk." : "Pelanggan dipakai saat transaksi penjualan.");
        id = input(form, "ID", "");
        id.setEnabled(false);
        nama = input(form, supplierMode ? "Nama Supplier" : "Nama Pelanggan", "Nama");
        hp = input(form, "Nomor HP", "0812...");
        alamat = input(form, "Alamat", "Alamat");
        info = input(form, supplierMode ? "Catatan" : "Tipe Pelanggan", supplierMode ? "Catatan supplier" : "Retail/Grosir");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        Button baru = button("Baru", false);
        baru.setOnClickListener(v -> clear());
        Button save = button("Simpan", true);
        save.setOnClickListener(v -> save());
        Button delete = button("Hapus", false);
        delete.setOnClickListener(v -> delete());
        actions.addView(baru, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(delete, new LinearLayout.LayoutParams(0, -2, 1));
        form.addView(actions);

        LinearLayout card = card();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        card.setLayoutParams(lp);
        root.addView(card);
        title(card, supplierMode ? "Daftar Supplier" : "Daftar Pelanggan");
        list = column(0);
        card.addView(list);
        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        return scroll;
    }

    private void load() {
        list.removeAllViews();
        String sql = supplierMode
                ? "SELECT id,nama,nomor_hp,alamat,catatan FROM suppliers ORDER BY nama"
                : "SELECT id,nama,nomor_hp,alamat,tipe_pelanggan FROM pelanggan ORDER BY nama";
        try (Cursor c = db.rawQuery(sql)) {
            while (c.moveToNext()) {
                int rowId = c.getInt(0);
                String rowNama = c.getString(1);
                String rowHp = c.getString(2);
                String rowAlamat = c.getString(3);
                String rowInfo = c.getString(4);
                LinearLayout row = miniCard();
                row.setOnClickListener(v -> fill(rowId, rowNama, rowHp, rowAlamat, rowInfo));
                row.addView(text(rowNama, 17, 0xff103b52, true));
                row.addView(text((rowHp == null ? "-" : rowHp) + " | " + (rowInfo == null ? "-" : rowInfo), 13, 0xff076b9d, true));
                row.addView(text(rowAlamat == null ? "" : rowAlamat, 13, 0xff5f7d90, false));
                list.addView(row);
            }
        }
    }

    private void fill(int rowId, String rowNama, String rowHp, String rowAlamat, String rowInfo) {
        id.setText(String.valueOf(rowId));
        nama.setText(rowNama == null ? "" : rowNama);
        hp.setText(rowHp == null ? "" : rowHp);
        alamat.setText(rowAlamat == null ? "" : rowAlamat);
        info.setText(rowInfo == null ? "" : rowInfo);
    }

    private void save() {
        try {
            Integer rowId = id.getText().toString().trim().isEmpty() ? null : Integer.parseInt(id.getText().toString());
            if (supplierMode) db.simpanSupplier(rowId, value(nama), value(hp), value(alamat), value(info));
            else db.simpanPelanggan(rowId, value(nama), value(hp), value(alamat), value(info));
            clear();
            load();
            Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void delete() {
        try {
            if (id.getText().toString().trim().isEmpty()) throw new IllegalArgumentException("Pilih data dari daftar dulu.");
            int rowId = Integer.parseInt(id.getText().toString());
            if (supplierMode) db.hapusSupplier(rowId);
            else db.hapusPelanggan(rowId);
            clear();
            load();
            Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clear() {
        id.setText("");
        nama.setText("");
        hp.setText("");
        alamat.setText("");
        info.setText("");
    }

    private String value(EditText e) {
        return e.getText().toString().trim();
    }

    private LinearLayout miniCard() {
        LinearLayout row = column(12);
        row.setBackgroundResource(R.drawable.bg_soft_panel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    private EditText input(LinearLayout parent, String label, String hint) {
        parent.addView(text(label, 13, 0xff103b52, true));
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(false);
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        parent.addView(e);
        return e;
    }

    private LinearLayout card() {
        LinearLayout l = column(18);
        l.setBackgroundResource(R.drawable.bg_card);
        return l;
    }

    private LinearLayout column(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return l;
    }

    private void title(LinearLayout parent, String value) {
        parent.addView(text(value, 22, 0xff103b52, true));
    }

    private void subtitle(LinearLayout parent, String value) {
        TextView v = text(value, 14, 0xff5f7d90, false);
        v.setPadding(0, dp(4), 0, dp(8));
        parent.addView(v);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value == null ? "" : value);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, 1);
        v.setPadding(0, dp(5), 0, dp(3));
        return v;
    }

    private Button button(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xffffffff : 0xff076b9d);
        b.setTypeface(null, 1);
        b.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        return b;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
