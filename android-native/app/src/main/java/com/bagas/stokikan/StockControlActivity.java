package com.bagas.stokikan;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.service.StockService;

public class StockControlActivity extends Activity {
    private DbHelper db;
    private StockService service;
    private EditText nomor;
    private EditText alasanBatal;
    private Spinner jenisMentah;
    private EditText fisikMentah;
    private EditText alasanMentah;
    private Spinner batchGiling;
    private EditText fisikGiling;
    private EditText alasanGiling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        service = new StockService(db);
        setContentView(buildView());
    }

    private ScrollView buildView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = column(16);
        scroll.addView(root);
        root.addView(header());

        nomor = input("Nomor transaksi");
        alasanBatal = input("Alasan retur/batal");
        Button batal = button("Batalkan / Retur Transaksi", true);
        batal.setOnClickListener(v -> run(() -> service.batalkanPenjualan(nomor.getText().toString(), alasanBatal.getText().toString())));
        root.addView(card("Retur / Batal Transaksi", "Stok giling dikembalikan sesuai detail penjualan.", nomor, alasanBatal, batal));

        jenisMentah = spinner(db.options("jenis_ikan"));
        fisikMentah = input("Stok fisik mentah kg");
        alasanMentah = input("Alasan opname mentah");
        Button opnameMentah = button("Simpan Opname Mentah", false);
        opnameMentah.setOnClickListener(v -> {
            OptionItem item = (OptionItem) jenisMentah.getSelectedItem();
            run(() -> service.sesuaikanStokMentah(item.id, toDouble(fisikMentah), alasanMentah.getText().toString()));
        });
        root.addView(card("Opname Stok Mentah", "Masukkan hasil hitung fisik agar selisih tercatat di riwayat.", jenisMentah, fisikMentah, alasanMentah, opnameMentah));

        batchGiling = spinner(db.options("stok_giling"));
        fisikGiling = input("Stok fisik giling kg");
        alasanGiling = input("Alasan opname giling");
        Button opnameGiling = button("Simpan Opname Giling", false);
        opnameGiling.setOnClickListener(v -> {
            OptionItem item = (OptionItem) batchGiling.getSelectedItem();
            run(() -> service.sesuaikanStokGiling(item.id, toDouble(fisikGiling), alasanGiling.getText().toString()));
        });
        root.addView(card("Opname Stok Giling", "Pilih batch agar koreksi stok tetap bisa diaudit.", batchGiling, fisikGiling, alasanGiling, opnameGiling));

        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        return scroll;
    }

    private LinearLayout header() {
        LinearLayout box = column(18);
        box.setBackgroundResource(R.drawable.bg_gradient_card);
        box.addView(text("Kontrol Stok", 23, 0xffffffff, true));
        box.addView(text("Retur, batal transaksi, dan opname stok untuk menjaga data tetap sesuai kondisi fisik.", 14, 0xffdff7ff, false));
        return box;
    }

    private LinearLayout card(String title, String subtitle, android.view.View... views) {
        LinearLayout box = column(16);
        box.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(14), 0, 0);
        box.setLayoutParams(lp);
        box.addView(text(title, 19, 0xff103b52, true));
        box.addView(text(subtitle, 13, 0xff5f7d90, false));
        for (android.view.View view : views) box.addView(view);
        return box;
    }

    private Spinner spinner(java.util.List<OptionItem> items) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items));
        spinner.setPadding(0, dp(8), 0, dp(8));
        return spinner;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(10), 0, 0);
        input.setLayoutParams(lp);
        return input;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, 1);
        button.setTextColor(primary ? 0xffffffff : getColor(R.color.catokan_primary_dark));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(lp);
        return button;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, 1);
        return text;
    }

    private LinearLayout column(int padding) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return box;
    }

    private void run(Action action) {
        try {
            Toast.makeText(this, action.execute(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private double toDouble(EditText input) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Input stok fisik wajib diisi");
        return Double.parseDouble(value.replace(",", "."));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface Action {
        String execute();
    }
}
