package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.User;

import java.util.Locale;

public class DashboardActivity extends Activity {
    private DbHelper db;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_dashboard);
            db = new DbHelper(this);
            user = AppNav.readUser(this);
            bindHeader();
            bindStats();
            bindMenus();
        } catch (Throwable error) {
            showFallback(error);
        }
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
        try {
            String owner = " owner_user_id=" + DbHelper.currentUserId();
            ((TextView) findViewById(R.id.txtStokMentah)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_mentah WHERE" + owner)));
            ((TextView) findViewById(R.id.txtStokGiling)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE" + owner)));
            ((TextView) findViewById(R.id.txtTotalJual)).setText("Rp " + money(db.scalar("SELECT IFNULL(SUM(total),0) FROM penjualan WHERE" + owner)));
            ((TextView) findViewById(R.id.txtStokLama)).setText(formatKg(db.scalar("SELECT IFNULL(SUM(total_kg),0) FROM stok_giling WHERE total_kg>0 AND date(tanggal_produksi)<=date('now','-5 day') AND owner_user_id=" + DbHelper.currentUserId())));
        } catch (Throwable error) {
            Toast.makeText(this, "Dashboard belum bisa membaca data: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        findViewById(R.id.btnStockControl).setOnClickListener(v -> AppNav.open(this, StockControlActivity.class, user));
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

    private void showFallback(Throwable error) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundResource(R.drawable.bg_screen);
        TextView title = new TextView(this);
        title.setText("Dashboard belum bisa dibuka");
        title.setTextSize(22);
        title.setTextColor(0xff082b4d);
        title.setTypeface(null, 1);
        TextView message = new TextView(this);
        message.setText("Aplikasi tidak berhenti, tapi ada data/tampilan yang perlu diperbaiki: " + error.getMessage());
        message.setTextSize(14);
        message.setTextColor(0xff6a8197);
        message.setPadding(0, dp(10), 0, dp(16));
        TextView back = new TextView(this);
        back.setText("Kembali ke Login");
        back.setGravity(android.view.Gravity.CENTER);
        back.setTextColor(0xffffffff);
        back.setTypeface(null, 1);
        back.setBackgroundResource(R.drawable.bg_primary_btn);
        back.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        root.addView(title);
        root.addView(message);
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(54)));
        setContentView(root);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatKg(double value) {
        return String.format(Locale.US, "%,.1f kg", value).replace(',', '.');
    }

    private String money(double value) {
        return String.format(Locale.US, "%,.0f", value).replace(',', '.');
    }
}
