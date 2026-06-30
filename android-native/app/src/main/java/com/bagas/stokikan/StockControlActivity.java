package com.bagas.stokikan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
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
    private Spinner transaksi;
    private EditText alasanBatal;
    private Spinner jenisMentah;
    private EditText fisikMentah;
    private EditText alasanMentah;
    private Spinner stokGiling;
    private EditText fisikGiling;
    private EditText alasanGiling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppNav.readUser(this);
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

        transaksi = spinner(db.transaksiBerhasil());
        alasanBatal = input("Alasan pembatalan");
        Button batal = button("Batalkan Penjualan", true);
        batal.setOnClickListener(v -> {
            OptionItem item = (OptionItem) transaksi.getSelectedItem();
            run(() -> {
                if (item == null) throw new IllegalArgumentException("Belum ada transaksi berhasil yang bisa dibatalkan");
                return service.batalkanPenjualan(item.id, alasanBatal.getText().toString());
            });
        });
        root.addView(card("Batalkan Penjualan", "Pilih transaksi yang sudah berhasil. Stok akan dikembalikan otomatis.", transaksi, alasanBatal, batal));

        jenisMentah = spinner(db.options("jenis_ikan"));
        fisikMentah = input("Jumlah hasil hitung kg");
        alasanMentah = input("Alasan perubahan stok mentah");
        Button cekMentah = button("Simpan Cek Ulang Stok Mentah", false);
        cekMentah.setOnClickListener(v -> {
            OptionItem item = (OptionItem) jenisMentah.getSelectedItem();
            run(() -> {
                if (item == null) throw new IllegalArgumentException("Pilih jenis ikan dulu");
                return service.sesuaikanStokMentah(item.id, toDouble(fisikMentah), alasanMentah.getText().toString());
            });
        });
        root.addView(card("Cek Ulang Stok Mentah", "Masukkan jumlah stok hasil hitung di tempat. Selisihnya akan masuk riwayat.", jenisMentah, fisikMentah, alasanMentah, cekMentah));

        stokGiling = spinner(db.options("stok_giling"));
        fisikGiling = input("Jumlah hasil hitung kg");
        alasanGiling = input("Alasan perubahan stok giling");
        Button cekGiling = button("Simpan Cek Ulang Stok Giling", false);
        cekGiling.setOnClickListener(v -> {
            OptionItem item = (OptionItem) stokGiling.getSelectedItem();
            run(() -> {
                if (item == null) throw new IllegalArgumentException("Pilih stok giling dulu");
                return service.sesuaikanStokGiling(item.id, toDouble(fisikGiling), alasanGiling.getText().toString());
            });
        });
        root.addView(card("Cek Ulang Stok Giling", "Pilih stok giling yang dihitung ulang agar perubahan stok tetap jelas.", stokGiling, fisikGiling, alasanGiling, cekGiling));

        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());
        root.addView(back);
        return scroll;
    }

    private LinearLayout header() {
        LinearLayout box = column(18);
        box.setBackgroundResource(R.drawable.bg_gradient_card);
        box.addView(text("Kontrol Stok", 23, 0xffffffff, true));
        box.addView(text("Batalkan penjualan dan perbaiki jumlah stok setelah dihitung ulang.", 14, 0xffdff7ff, false));
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
        ArrayAdapter<OptionItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(0, dp(8), 0, dp(8));
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) showSearchDialog(spinner, adapter, items);
            return true;
        });
        return spinner;
    }

    private void showSearchDialog(Spinner spinner, ArrayAdapter<OptionItem> mainAdapter, java.util.List<OptionItem> items) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        SearchView search = new SearchView(this);
        search.setQueryHint("Cari data...");
        ListView list = new ListView(this);
        ArrayAdapter<OptionItem> dialogAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(dialogAdapter);
        box.addView(search);
        box.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Pilih Data").setView(box).create();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String text) {
                dialogAdapter.getFilter().filter(text);
                return true;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            OptionItem selected = dialogAdapter.getItem(position);
            int index = mainAdapter.getPosition(selected);
            if (index >= 0) spinner.setSelection(index);
            dialog.dismiss();
        });
        dialog.show();
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
        if (value.isEmpty()) throw new IllegalArgumentException("Jumlah hasil hitung wajib diisi");
        return Double.parseDouble(value.replace(",", "."));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface Action {
        String execute();
    }
}
