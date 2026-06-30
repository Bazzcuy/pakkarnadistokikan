package com.bagas.stokikan;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.service.StockService;

import java.util.List;

public class ProductionActivity extends Activity {
    private DbHelper db;
    private StockService service;
    private Spinner jenis;
    private EditText mentah;
    private EditText hasil;
    private EditText biaya;
    private EditText hargaJual;
    private EditText catatan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_production);
        db = new DbHelper(this);
        service = new StockService(db);
        jenis = findViewById(R.id.spJenis);
        mentah = findViewById(R.id.edtMentah);
        hasil = findViewById(R.id.edtHasil);
        biaya = findViewById(R.id.edtBiaya);
        hargaJual = findViewById(R.id.edtHargaJual);
        catatan = findViewById(R.id.edtCatatan);
        bindSpinner(jenis, db.options("jenis_ikan"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void save() {
        try {
            OptionItem j = (OptionItem) jenis.getSelectedItem();
            service.prosesProduksi(j.id, toDouble(mentah), toDouble(hasil), toDoubleZero(biaya), toDouble(hargaJual), catatan.getText().toString());
            Toast.makeText(this, "Produksi ikan giling berhasil disimpan.", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void bindSpinner(Spinner spinner, List<OptionItem> items) {
        ArrayAdapter<OptionItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) showSearchDialog(spinner, adapter, items);
            return true;
        });
    }

    private void showSearchDialog(Spinner spinner, ArrayAdapter<OptionItem> mainAdapter, List<OptionItem> items) {
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

    private double toDouble(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Input masih ada yang kosong.");
        return Double.parseDouble(s.replace(",", "."));
    }

    private double toDoubleZero(EditText e) {
        String s = e.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s.replace(",", "."));
    }
}
