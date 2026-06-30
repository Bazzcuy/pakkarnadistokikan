package com.bagas.stokikan;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bagas.stokikan.db.DbHelper;

import java.util.Locale;

public class TextActivity extends Activity {
    private DbHelper db;
    private LinearLayout content;
    private Spinner periode;
    private Spinner jenis;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        mode = getIntent().getStringExtra("mode");
        setContentView(view());
        render();
    }

    private ScrollView view() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = column(16);
        scroll.addView(root);

        LinearLayout hero = column(18);
        hero.setBackgroundResource(R.drawable.bg_gradient_card);
        root.addView(hero);
        TextView title = text(getIntent().getStringExtra("title") == null ? "Data" : getIntent().getStringExtra("title"), 23, 0xffffffff, true);
        hero.addView(title);
        hero.addView(text(subtitle(), 14, 0xffdff7ff, false));

        LinearLayout filter = card();
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.setMargins(0, dp(14), 0, 0);
        filter.setLayoutParams(flp);
        root.addView(filter);
        filter.addView(text("Filter Data", 18, 0xff103b52, true));
        periode = new Spinner(this);
        periode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Semua", "Hari Ini", "Minggu Ini", "Bulan Ini"}));
        filter.addView(label("Periode"));
        filter.addView(periode);
        jenis = new Spinner(this);
        jenis.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, jenisItems()));
        filter.addView(label("Jenis Ikan"));
        filter.addView(jenis);
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { render(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        periode.setOnItemSelectedListener(listener);
        jenis.setOnItemSelectedListener(listener);

        content = column(0);
        root.addView(content);
        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        return scroll;
    }

    private void render() {
        if (content == null) return;
        content.removeAllViews();
        if ("raw".equals(mode)) renderRaw();
        else if ("giling".equals(mode)) renderGiling();
        else renderReport();
    }

    private void renderRaw() {
        summary("Total Stok Mentah", kg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah")), "Jenis ikan", String.valueOf(count("jenis_ikan")));
        String sql = "SELECT j.nama,j.kategori,j.deskripsi,j.gambar_path,s.total_kg,s.updated_at FROM stok_mentah s JOIN jenis_ikan j ON j.id=s.jenis_ikan_id WHERE 1=1 " + jenisSql("j.nama") + " ORDER BY s.total_kg DESC";
        try (Cursor c = db.rawQuery(sql)) {
            while (c.moveToNext()) {
                LinearLayout row = imageCard(c.getString(3));
                LinearLayout body = (LinearLayout) row.getChildAt(1);
                body.addView(text(c.getString(0), 18, 0xff103b52, true));
                body.addView(text(c.getString(1) + " | " + kg(c.getDouble(4)), 14, 0xff076b9d, true));
                body.addView(text(c.getString(2), 13, 0xff5f7d90, false));
                body.addView(text("Update: " + c.getString(5), 12, 0xff5f7d90, false));
                content.addView(row);
            }
        }
        historyBlock("Riwayat Masuk/Keluar Stok Mentah", "MENTAH");
    }

    private void renderGiling() {
        summary("Total Stok Giling", kg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling")), "Data tersedia", String.valueOf(countWhere("stok_giling", "total_kg>0")));
        String sql = "SELECT j.nama,j.gambar_path,g.total_kg,g.harga_jual_per_kg,g.tanggal_produksi,g.status_stok FROM stok_giling g JOIN jenis_ikan j ON j.id=g.jenis_ikan_id WHERE 1=1 " + jenisSql("j.nama") + periodSql("g.tanggal_produksi") + " ORDER BY date(g.tanggal_produksi),g.id";
        try (Cursor c = db.rawQuery(sql)) {
            while (c.moveToNext()) {
                LinearLayout row = imageCard(c.getString(1));
                LinearLayout body = (LinearLayout) row.getChildAt(1);
                body.addView(text(c.getString(0), 18, 0xff103b52, true));
                body.addView(text(kg(c.getDouble(2)) + " | Rp " + money(c.getDouble(3)) + "/kg", 14, 0xff076b9d, true));
                body.addView(text("Produksi: " + c.getString(4) + " | " + c.getString(5) + " | Dijual lebih dulu jika lebih lama", 13, 0xff5f7d90, false));
                content.addView(row);
            }
        }
        historyBlock("Riwayat Masuk/Keluar Stok Giling", "GILING");
    }

    private void renderReport() {
        double mentah = db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah");
        double giling = db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling");
        String salesWhere = salesFilterSql();
        double jual = db.scalar("SELECT IFNULL(SUM(p.total),0) FROM penjualan p WHERE 1=1 " + salesWhere);
        double stokLama = db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')");
        summary("Stok Mentah", kg(mentah), "Stok Giling", kg(giling));
        summary("Penjualan Lunas", "Rp " + money(jual), "Stok Perlu Dijual Dulu", kg(stokLama));
        section("Transaksi Penjualan");
        String sales = "SELECT p.nomor_transaksi,p.tanggal,IFNULL(pl.nama,'Pelanggan Umum') pelanggan,j.nama jenis_ikan,d.jumlah_kg,p.total,p.status_pembayaran FROM penjualan p JOIN detail_penjualan d ON d.penjualan_id=p.id JOIN jenis_ikan j ON j.id=d.jenis_ikan_id LEFT JOIN pelanggan pl ON pl.id=p.pelanggan_id WHERE 1=1 " + salesWhere + " ORDER BY p.tanggal DESC,p.id DESC LIMIT 25";
        try (Cursor c = db.rawQuery(sales)) {
            while (c.moveToNext()) {
                LinearLayout row = miniCard();
                row.addView(text(c.getString(0) + " | " + c.getString(1), 16, 0xff103b52, true));
                row.addView(text(c.getString(2) + " | " + c.getString(3) + " " + kg(c.getDouble(4)) + " | Rp " + money(c.getDouble(5)) + " | " + c.getString(6), 13, 0xff5f7d90, false));
                content.addView(row);
            }
        }
        historyBlock("Riwayat Stok Detail", null);
    }

    private void historyBlock(String title, String jenisStok) {
        section(title);
        String stok = jenisStok == null ? "" : " AND r.jenis_stok='" + jenisStok + "' ";
        String sql = "SELECT r.tanggal,IFNULL(j.nama,'-') jenis_ikan,r.jenis_transaksi,r.jenis_stok,r.referensi,r.perubahan_kg,r.stok_sebelum,r.stok_sesudah,r.keterangan FROM riwayat_stok r LEFT JOIN jenis_ikan j ON j.id=r.jenis_ikan_id WHERE 1=1 " + stok + jenisHistorySql() + periodSql("r.tanggal") + " ORDER BY r.tanggal DESC,r.id DESC LIMIT 30";
        try (Cursor c = db.rawQuery(sql)) {
            while (c.moveToNext()) {
                LinearLayout row = miniCard();
                row.addView(text(c.getString(0) + " | " + c.getString(1) + " | " + c.getString(2) + " | " + c.getString(3), 15, 0xff103b52, true));
                row.addView(text("Ref: " + c.getString(4) + " | " + kg(c.getDouble(5)) + " | " + kg(c.getDouble(6)) + " -> " + kg(c.getDouble(7)), 13, 0xff076b9d, true));
                row.addView(text(c.getString(8), 13, 0xff5f7d90, false));
                content.addView(row);
            }
        }
    }

    private void summary(String leftTitle, String leftValue, String rightTitle, String rightValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        row.setLayoutParams(lp);
        row.addView(stat(leftTitle, leftValue, R.drawable.bg_stat_teal), new LinearLayout.LayoutParams(0, dp(96), 1));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(dp(10), 1);
        TextView spacer = new TextView(this);
        row.addView(spacer, gap);
        row.addView(stat(rightTitle, rightValue, R.drawable.bg_stat_blue), new LinearLayout.LayoutParams(0, dp(96), 1));
        content.addView(row);
    }

    private LinearLayout stat(String title, String value, int bg) {
        LinearLayout s = column(14);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setBackgroundResource(bg);
        s.addView(text(title, 12, 0xff5f7d90, false));
        s.addView(text(value, 19, 0xff103b52, true));
        return s;
    }

    private LinearLayout imageCard(String path) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        row.setLayoutParams(lp);
        ImageView image = new ImageView(this);
        setImage(image, path);
        row.addView(image, new LinearLayout.LayoutParams(dp(96), dp(96)));
        LinearLayout body = column(0);
        body.setPadding(dp(12), 0, 0, 0);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private LinearLayout miniCard() {
        LinearLayout row = column(12);
        row.setBackgroundResource(R.drawable.bg_soft_panel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(lp);
        return row;
    }

    private void section(String title) {
        TextView s = text(title, 19, 0xff103b52, true);
        s.setPadding(0, dp(18), 0, dp(4));
        content.addView(s);
    }

    private String[] jenisItems() {
        java.util.ArrayList<String> items = new java.util.ArrayList<>();
        items.add("Semua Ikan");
        try (Cursor c = db.rawQuery("SELECT nama FROM jenis_ikan ORDER BY nama")) {
            while (c.moveToNext()) items.add(c.getString(0));
        }
        return items.toArray(new String[0]);
    }

    private String jenisSql(String column) {
        if (jenis == null || jenis.getSelectedItem() == null) return "";
        String value = jenis.getSelectedItem().toString();
        if ("Semua Ikan".equals(value)) return "";
        return " AND " + column + "='" + value.replace("'", "''") + "' ";
    }

    private String periodSql(String column) {
        if (periode == null || periode.getSelectedItem() == null) return "";
        String p = periode.getSelectedItem().toString();
        if ("Hari Ini".equals(p)) return " AND date(" + column + ")=date('now','localtime') ";
        if ("Minggu Ini".equals(p)) return " AND strftime('%Y-%W'," + column + ")=strftime('%Y-%W','now','localtime') ";
        if ("Bulan Ini".equals(p)) return " AND strftime('%Y-%m'," + column + ")=strftime('%Y-%m','now','localtime') ";
        return "";
    }

    private String salesFilterSql() {
        String sql = periodSql("p.tanggal");
        if (jenis == null || jenis.getSelectedItem() == null) return sql;
        String value = jenis.getSelectedItem().toString();
        if ("Semua Ikan".equals(value)) return sql;
        return sql + " AND EXISTS (SELECT 1 FROM detail_penjualan dx JOIN jenis_ikan jx ON jx.id=dx.jenis_ikan_id WHERE dx.penjualan_id=p.id AND jx.nama='" + value.replace("'", "''") + "') ";
    }

    private String jenisHistorySql() {
        if (jenis == null || jenis.getSelectedItem() == null) return "";
        String value = jenis.getSelectedItem().toString();
        if ("Semua Ikan".equals(value)) return "";
        return " AND r.jenis_ikan_id=(SELECT id FROM jenis_ikan WHERE nama='" + value.replace("'", "''") + "' LIMIT 1) ";
    }

    private String subtitle() {
        if ("raw".equals(mode)) return "Stok mentah per jenis ikan lengkap dengan gambar, jumlah, dan riwayat.";
        if ("giling".equals(mode)) return "Stok ikan giling lengkap dengan gambar, harga, tanggal produksi, dan riwayat.";
        return "Laporan operasional dengan ringkasan, transaksi, dan riwayat stok.";
    }

    private long count(String table) {
        return (long) db.scalar("SELECT COUNT(*) FROM " + table);
    }

    private long countWhere(String table, String where) {
        return (long) db.scalar("SELECT COUNT(*) FROM " + table + " WHERE " + where);
    }

    private void setImage(ImageView img, String path) {
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            if (path != null && path.startsWith("content:")) img.setImageURI(Uri.parse(path));
            else img.setImageResource(R.drawable.catokan_banner);
        } catch (Exception e) {
            img.setImageResource(R.drawable.catokan_banner);
        }
    }

    private LinearLayout card() {
        LinearLayout l = column(16);
        l.setBackgroundResource(R.drawable.bg_card);
        return l;
    }

    private LinearLayout column(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return l;
    }

    private TextView label(String text) {
        TextView v = text(text, 13, 0xff103b52, true);
        v.setPadding(0, dp(10), 0, dp(5));
        return v;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value == null ? "" : value);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, 1);
        v.setLineSpacing(dp(2), 1f);
        return v;
    }

    private Button button(String value, boolean primary) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xffffffff : 0xff076b9d);
        b.setTypeface(null, 1);
        b.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private String kg(double v) {
        return String.format(Locale.US, "%,.1f kg", v).replace(',', '.');
    }

    private String money(double v) {
        return String.format(Locale.US, "%,.0f", v).replace(',', '.');
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
