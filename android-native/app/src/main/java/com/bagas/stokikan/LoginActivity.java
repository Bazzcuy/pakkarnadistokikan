package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.User;

public class LoginActivity extends Activity {
    private DbHelper db;
    private EditText username;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildView());
        db = new DbHelper(this);
        username.setText("pengguna");
        password.setText("pengguna123");
    }

    private ScrollView buildView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundResource(R.drawable.bg_screen);

        LinearLayout root = column(16);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(root);

        LinearLayout card = column(18);
        card.setBackgroundResource(R.drawable.bg_card);
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.catokan_logo);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(92), dp(92));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(logo, logoParams);

        TextView title = text("CATOKAN", 28, 0xff082b4d, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView subtitle = text("Catat Stok Ikan", 15, 0xff6a8197, false);
        subtitle.setGravity(Gravity.CENTER);
        card.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        TextView loginTitle = text("Masuk ke Akun Anda", 23, 0xff082b4d, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(18), 0, 0);
        card.addView(loginTitle, titleParams);
        card.addView(text("Silakan login untuk membuka dashboard stok, produksi, transaksi, dan laporan.", 14, 0xff6a8197, false));

        card.addView(label("Username"));
        username = input("Masukkan username", false);
        card.addView(username);

        card.addView(label("Password"));
        password = input("Masukkan password", true);
        card.addView(password);

        card.addView(button("Masuk", true, v -> login(username.getText().toString().trim(), password.getText().toString().trim())));
        TextView quick = text("Atau login cepat", 13, 0xff6a8197, false);
        quick.setGravity(Gravity.CENTER);
        card.addView(quick, new LinearLayout.LayoutParams(-1, -2));
        card.addView(button("Login sebagai Pengguna", false, v -> login("pengguna", "pengguna123")));
        card.addView(button("Isi Akun Awal", false, v -> {
            username.setText("pengguna");
            password.setText("pengguna123");
            Toast.makeText(this, "Akun awal pengguna sudah diisi.", Toast.LENGTH_SHORT).show();
        }));
        card.addView(button("Daftar Akun Pengguna", false, v -> startActivity(new Intent(this, RegisterActivity.class))));
        return scroll;
    }

    private LinearLayout column(int padding) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return layout;
    }

    private TextView label(String value) {
        TextView label = text(value, 14, 0xff082b4d, true);
        label.setPadding(0, dp(14), 0, dp(6));
        return label;
    }

    private EditText input(String hint, boolean passwordMode) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(16), 0, dp(16), 0);
        input.setTextColor(0xff082b4d);
        input.setHintTextColor(0xff6a8197);
        if (passwordMode) input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        input.setLayoutParams(params);
        return input;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(null, 1);
        text.setLineSpacing(dp(2), 1f);
        return text;
    }

    private TextView button(String value, boolean primary, android.view.View.OnClickListener listener) {
        TextView button = text(value, 15, primary ? 0xffffffff : 0xff078fe1, true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(primary ? 56 : 50));
        params.setMargins(0, dp(primary ? 18 : 8), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void login(String u, String p) {
        User user;
        try {
            user = db.login(u, p);
        } catch (Exception e) {
            Toast.makeText(this, "Data aplikasi belum siap: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (user == null) {
            Toast.makeText(this, "Login gagal. Periksa username dan password.", Toast.LENGTH_LONG).show();
            return;
        }
        DbHelper.setCurrentUserId(user.id);
        Intent intent = new Intent(this, DashboardActivity.class);
        AppNav.putUser(intent, user);
        startActivity(intent);
        finish();
    }
}
