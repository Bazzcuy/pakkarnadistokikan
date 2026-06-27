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

public class SalesActivity extends Activity {
    private DbHelper db;
    private StockService service;
    private User user;
    private Spinner pelanggan;
    private Spinner batch;
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
        batch = findViewById(R.id.spBatch);
        kg = findViewById(R.id.edtKg);
        metode = findViewById(R.id.edtMetode);
        bayar = findViewById(R.id.edtBayar);
        metode.setText("Tunai");
        bindSpinner(pelanggan, db.options("pelanggan"));
        bindSpinner(batch, db.options("stok_giling"));
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void save() {
        try {
            OptionItem p = (OptionItem) pelanggan.getSelectedItem();
            OptionItem b = (OptionItem) batch.getSelectedItem();
            String result = service.jualCepat(user, p.id, b.id, toDouble(kg), metode.getText().toString(), toDoubleZero(bayar));
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
