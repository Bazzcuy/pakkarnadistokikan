package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;

public class FishMasterActivity extends Activity {
    private static final int PICK_IMAGE = 21;
    private DbHelper db;
    private LinearLayout list;
    private EditText nama;
    private EditText kategori;
    private EditText deskripsi;
    private String gambarPath = "";
    private ImageView preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setContentView(view());
        loadList();
    }

    private ScrollView view() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = column(16);
        scroll.addView(root);

        LinearLayout form = card();
        root.addView(form);
        title(form, "Jenis Ikan");
        subtitle(form, "Tambah master jenis ikan, kategori, deskripsi, dan gambar produk.");
        preview = new ImageView(this);
        preview.setImageResource(R.drawable.catokan_banner);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        form.addView(preview, new LinearLayout.LayoutParams(-1, dp(145)));
        nama = input(form, "Nama Jenis Ikan", "Contoh: Tenggiri Super");
        kategori = input(form, "Kategori", "Ikan");
        deskripsi = input(form, "Deskripsi", "Keterangan kualitas atau asal ikan");
        Button pick = button("Pilih Gambar Ikan", false);
        pick.setOnClickListener(v -> pickImage());
        form.addView(pick);
        Button save = button("Tambah Jenis Ikan", true);
        save.setOnClickListener(v -> save());
        form.addView(save);

        LinearLayout listCard = card();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        listCard.setLayoutParams(lp);
        root.addView(listCard);
        title(listCard, "Daftar Jenis Ikan");
        subtitle(listCard, "Jenis ini otomatis muncul pada input stok mentah dan produksi.");
        list = column(0);
        listCard.addView(list);
        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        return scroll;
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Pilih gambar ikan"), PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            gambarPath = uri.toString();
            preview.setImageURI(uri);
        }
    }

    private void save() {
        try {
            db.tambahJenisIkan(text(nama), text(kategori), text(deskripsi), gambarPath);
            nama.setText("");
            kategori.setText("");
            deskripsi.setText("");
            gambarPath = "";
            preview.setImageResource(R.drawable.catokan_banner);
            loadList();
            Toast.makeText(this, "Jenis ikan berhasil ditambahkan", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadList() {
        list.removeAllViews();
        String sql = "SELECT j.nama,j.kategori,j.deskripsi,j.gambar_path,IFNULL(s.total_kg,0) stok FROM jenis_ikan j LEFT JOIN stok_mentah s ON s.jenis_ikan_id=j.id ORDER BY j.nama";
        try (Cursor c = db.rawQuery(sql)) {
            while (c.moveToNext()) {
                LinearLayout row = rowCard();
                ImageView img = new ImageView(this);
                setImage(img, c.getString(3));
                row.addView(img, new LinearLayout.LayoutParams(dp(82), dp(82)));
                LinearLayout text = column(0);
                text.setPadding(dp(12), 0, 0, 0);
                row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
                smallTitle(text, c.getString(0));
                body(text, c.getString(1) + " | Stok mentah " + kg(c.getDouble(4)));
                body(text, c.getString(2));
                list.addView(row);
            }
        }
    }

    private LinearLayout rowCard() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(12), dp(12), dp(12));
        r.setBackgroundResource(R.drawable.bg_soft_panel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        r.setLayoutParams(lp);
        return r;
    }

    private void setImage(ImageView img, String path) {
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            if (path != null && path.startsWith("content:")) {
                img.setImageURI(Uri.parse(path));
            } else {
                img.setImageResource(R.drawable.catokan_banner);
            }
        } catch (Exception e) {
            img.setImageResource(R.drawable.catokan_banner);
        }
    }

    private LinearLayout column(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return l;
    }

    private LinearLayout card() {
        LinearLayout l = column(18);
        l.setBackgroundResource(R.drawable.bg_card);
        return l;
    }

    private void title(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(22);
        v.setTextColor(0xff103b52);
        v.setTypeface(null, 1);
        parent.addView(v);
    }

    private void smallTitle(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(17);
        v.setTextColor(0xff103b52);
        v.setTypeface(null, 1);
        parent.addView(v);
    }

    private void subtitle(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setTextColor(0xff5f7d90);
        v.setPadding(0, dp(4), 0, dp(8));
        parent.addView(v);
    }

    private void body(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text == null ? "" : text);
        v.setTextSize(13);
        v.setTextColor(0xff5f7d90);
        parent.addView(v);
    }

    private EditText input(LinearLayout parent, String label, String hint) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(0xff103b52);
        l.setTypeface(null, 1);
        l.setPadding(0, dp(10), 0, dp(5));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(false);
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        parent.addView(e);
        return e;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xffffffff : 0xff076b9d);
        b.setTypeface(null, 1);
        b.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private String text(EditText e) {
        return e.getText().toString().trim();
    }

    private String kg(double v) {
        return String.format(java.util.Locale.US, "%,.1f kg", v).replace(',', '.');
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
