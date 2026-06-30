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
        ((TextView) findViewById(R.id.txtRole)).setText("Login: Pengguna");
        ((TextView) findViewById(R.id.txtWorkspace)).setText("Dashboard Pengguna");
    }

    private void bindStats() {
        ((TextView) findViewById(R.id.txtStokMentah)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah")));
        ((TextView) findViewById(R.id.txtStokGiling)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling")));
        ((TextView) findViewById(R.id.txtTotalJual)).setText("Rp " + money(db.scalar("SELECT IFNULL(SUM(total),0) FROM penjualan")));
        ((TextView) findViewById(R.id.txtStokLama)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day')")));
    }

    private void bindMenus() {
        View rawInput = findViewById(R.id.btnRawInput);
        View production = findViewById(R.id.btnProduction);
        View rawStock = findViewById(R.id.btnRawStock);
        View gilingStock = findViewById(R.id.btnGilingStock);
        View sales = findViewById(R.id.btnSales);
        View report = findViewById(R.id.btnReport);

        bindWorkspaceButtons();
        rawInput.setVisibility(View.VISIBLE);
        production.setVisibility(View.VISIBLE);
        rawStock.setVisibility(View.VISIBLE);
        gilingStock.setVisibility(View.VISIBLE);
        sales.setVisibility(View.VISIBLE);
        report.setVisibility(View.VISIBLE);

        findViewById(R.id.btnProfile).setOnClickListener(v -> AppNav.open(this, ProfileActivity.class, user));
        findViewById(R.id.btnFishMaster).setOnClickListener(v -> AppNav.open(this, FishMasterActivity.class, user));
        findViewById(R.id.btnSupplier).setOnClickListener(v -> openPartner("supplier"));
        findViewById(R.id.btnCustomer).setOnClickListener(v -> openPartner("customer"));
        findViewById(R.id.btnTransferData).setOnClickListener(v -> AppNav.open(this, DataTransferActivity.class, user));
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

    private void bindWorkspaceButtons() {
        View admin = findViewById(R.id.btnWorkspaceAdmin);
        View production = findViewById(R.id.btnWorkspaceProduction);
        View cashier = findViewById(R.id.btnWorkspaceCashier);
        admin.setVisibility(View.VISIBLE);
        production.setVisibility(View.VISIBLE);
        cashier.setVisibility(View.VISIBLE);
        admin.setOnClickListener(v -> AppNav.open(this, RawStockActivity.class, user));
        production.setOnClickListener(v -> AppNav.open(this, ProductionActivity.class, user));
        cashier.setOnClickListener(v -> AppNav.open(this, SalesActivity.class, user));
    }

    private void openText(String title, String mode) {
        Intent intent = new Intent(this, TextActivity.class);
        AppNav.putUser(intent, user);
        intent.putExtra("title", title);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }

    private void openPartner(String mode) {
        Intent intent = new Intent(this, PartnerMasterActivity.class);
        AppNav.putUser(intent, user);
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
