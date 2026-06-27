package com.bagas.stokikan;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.OptionItem;
import com.bagas.stokikan.model.User;
import com.bagas.stokikan.service.StockService;

import java.util.List;

public class RawStockActivity extends Activity {
    private DbHelper db;
    private StockService service;
    private User user;
    private Spinner jenis;
    private Spinner supplier;
    private EditText berat;
    private EditText harga;
    private EditText catatan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_stock);
        user = AppNav.readUser(this);
        db = new DbHelper(this);
        service = new StockService(db);
        jenis = findViewById(R.id.spJenis);
        supplier = findViewById(R.id.spSupplier);
        berat = findViewById(R.id.edtBerat);
        harga = findViewById(R.id.edtHarga);
        catatan = findViewById(R.id.edtCatatan);
        bindSpinner(jenis, db.options("jenis_ikan"));
        bindSpinner(supplier, db.options("suppliers"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void save() {
        try {
            OptionItem j = (OptionItem) jenis.getSelectedItem();
            OptionItem s = (OptionItem) supplier.getSelectedItem();
            service.inputStokMentah(j.id, s.id, toDouble(berat), toDouble(harga), catatan.getText().toString());
            Toast.makeText(this, "Stok mentah berhasil ditambahkan", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void bindSpinner(Spinner spinner, List<OptionItem> items) {
        ArrayAdapter<OptionItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private double toDouble(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Input masih ada yang kosong.");
        return Double.parseDouble(s.replace(",", "."));
    }
}
