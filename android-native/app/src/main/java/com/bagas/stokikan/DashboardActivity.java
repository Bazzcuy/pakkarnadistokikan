package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.User;

import java.util.Locale;

public class DashboardActivity extends Activity {
    private DbHelper db;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        db = new DbHelper(this);
        user = AppNav.readUser(this);
        bindHeader();
        bindStats();
        bindMenus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) bindStats();
    }

    private void bindHeader() {
        ((TextView) findViewById(R.id.txtGreeting)).setText("Halo, " + user.nama);
        ((TextView) findViewById(R.id.txtRole)).setText("Role: " + user.role);
    }

    private void bindStats() {
        ((TextView) findViewById(R.id.txtStokMentah)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah")));
        ((TextView) findViewById(R.id.txtStokGiling)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling")));
        ((TextView) findViewById(R.id.txtTotalJual)).setText("Rp " + money(db.scalar("SELECT IFNULL(SUM(total),0) FROM penjualan")));
    }

    private void bindMenus() {
        View rawInput = findViewById(R.id.btnRawInput);
        View production = findViewById(R.id.btnProduction);
        View rawStock = findViewById(R.id.btnRawStock);
        View gilingStock = findViewById(R.id.btnGilingStock);
        View sales = findViewById(R.id.btnSales);
        View report = findViewById(R.id.btnReport);

        rawInput.setVisibility(AppNav.allow(user, "ADMIN", "OPERATOR") ? View.VISIBLE : View.GONE);
        production.setVisibility(AppNav.allow(user, "ADMIN", "OPERATOR") ? View.VISIBLE : View.GONE);
        sales.setVisibility(AppNav.allow(user, "ADMIN", "KASIR") ? View.VISIBLE : View.GONE);

        rawInput.setOnClickListener(v -> AppNav.open(this, RawStockActivity.class, user));
        production.setOnClickListener(v -> AppNav.open(this, ProductionActivity.class, user));
        sales.setOnClickListener(v -> AppNav.open(this, SalesActivity.class, user));
        rawStock.setOnClickListener(v -> openText("Stok Ikan Mentah", "raw"));
        gilingStock.setOnClickListener(v -> openText("Stok Ikan Giling", "giling"));
        report.setOnClickListener(v -> openText("Laporan Ringkas", "report"));
        findViewById(R.id.btnBottomHome).setOnClickListener(v -> bindStats());
        findViewById(R.id.btnBottomStock).setOnClickListener(v -> openText("Stok Ikan Giling", "giling"));
        findViewById(R.id.btnBottomReport).setOnClickListener(v -> openText("Laporan Ringkas", "report"));
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void openText(String title, String mode) {
        Intent intent = new Intent(this, TextActivity.class);
        AppNav.putUser(intent, user);
        intent.putExtra("title", title);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    private String formatKg(double value) {
        return String.format(Locale.US, "%,.1f kg", value).replace(',', '.');
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.0f", value).replace(',', '.');
    }
}
