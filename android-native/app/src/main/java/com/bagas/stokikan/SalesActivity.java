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
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.service.StockService;

import java.util.List;

public class SalesActivity extends Activity {
    private DbHelper db;
    private StockService service;
    private User user;
    private Spinner pelanggan;
    private Spinner jenis;
    private EditText kg;
    private EditText metode;
    private EditText bayar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales);
        user = AppNav.readUser(this);
        db = new DbHelper(this);
        service = new StockService(db);
        pelanggan = findViewById(R.id.spPelanggan);
        jenis = findViewById(R.id.spBatch);
        kg = findViewById(R.id.edtKg);
        metode = findViewById(R.id.edtMetode);
        bayar = findViewById(R.id.edtBayar);
        metode.setText("Tunai");
        bindSpinner(pelanggan, db.options("pelanggan"));
        bindSpinner(jenis, db.options("jenis_ikan"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void save() {
        try {
            OptionItem p = (OptionItem) pelanggan.getSelectedItem();
            OptionItem j = (OptionItem) jenis.getSelectedItem();
            String result = service.jualFifo(user, p.id, j.id, toDouble(kg), metode.getText().toString(), toDoubleZero(bayar));
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
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
