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
    private String workspace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        db = new DbHelper(this);
        user = AppNav.readUser(this);
        workspace = defaultWorkspace();
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

        bindWorkspaceButtons();
        applyWorkspace(rawInput, production, rawStock, gilingStock, sales, report);

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
        admin.setVisibility(AppNav.allow(user, "ADMIN") ? View.VISIBLE : View.GONE);
        production.setVisibility(AppNav.allow(user, "ADMIN", "OPERATOR") ? View.VISIBLE : View.GONE);
        cashier.setVisibility(AppNav.allow(user, "ADMIN", "KASIR") ? View.VISIBLE : View.GONE);
        admin.setOnClickListener(v -> setWorkspace("ADMIN"));
        production.setOnClickListener(v -> setWorkspace("PRODUKSI"));
        cashier.setOnClickListener(v -> setWorkspace("KASIR"));
        updateWorkspaceLabel();
    }

    private void setWorkspace(String mode) {
        workspace = mode;
        applyWorkspace(findViewById(R.id.btnRawInput), findViewById(R.id.btnProduction), findViewById(R.id.btnRawStock), findViewById(R.id.btnGilingStock), findViewById(R.id.btnSales), findViewById(R.id.btnReport));
        updateWorkspaceLabel();
    }

    private void applyWorkspace(View rawInput, View production, View rawStock, View gilingStock, View sales, View report) {
        boolean admin = "ADMIN".equals(workspace);
        boolean produksi = "PRODUKSI".equals(workspace);
        boolean kasir = "KASIR".equals(workspace);
        rawInput.setVisibility((admin || produksi) && AppNav.allow(user, "ADMIN", "OPERATOR") ? View.VISIBLE : View.GONE);
        production.setVisibility((admin || produksi) && AppNav.allow(user, "ADMIN", "OPERATOR") ? View.VISIBLE : View.GONE);
        rawStock.setVisibility(View.VISIBLE);
        gilingStock.setVisibility(View.VISIBLE);
        sales.setVisibility((admin || kasir) && AppNav.allow(user, "ADMIN", "KASIR") ? View.VISIBLE : View.GONE);
        report.setVisibility((admin || kasir) ? View.VISIBLE : View.GONE);
    }

    private String defaultWorkspace() {
        if (AppNav.allow(user, "KASIR") && !AppNav.allow(user, "ADMIN")) return "KASIR";
        if (AppNav.allow(user, "OPERATOR") && !AppNav.allow(user, "ADMIN")) return "PRODUKSI";
        return "ADMIN";
    }

    private void updateWorkspaceLabel() {
        String text = "ADMIN".equals(workspace) ? "Dashboard Admin" : "PRODUKSI".equals(workspace) ? "Dashboard Produksi" : "Dashboard Kasir";
        ((TextView) findViewById(R.id.txtWorkspace)).setText(text);
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
