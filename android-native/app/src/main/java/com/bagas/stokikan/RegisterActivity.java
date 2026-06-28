package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;
import com.bagas.stokikan.model.User;

public class RegisterActivity extends Activity {
    private DbHelper db;
    private EditText nama;
    private EditText username;
    private EditText password;
    private EditText usaha;
    private EditText hp;
    private EditText alamat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setContentView(view());
    }

    private ScrollView view() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = column(16);
        scroll.addView(root);

        LinearLayout card = card();
        root.addView(card);
        title(card, "Daftar Pengguna");
        subtitle(card, "Buat akun pengguna CATOKAN. Setelah daftar, kamu langsung masuk ke dashboard.");
        nama = input(card, "Nama Pengguna", "Contoh: Bagas");
        username = input(card, "Username", "Contoh: penggunabaru");
        password = input(card, "Password", "Minimal 6 karakter");
        password.setInputType(0x00000081);
        usaha = input(card, "Nama Usaha", "Contoh: CATOKAN Ikan Giling");
        hp = input(card, "Nomor HP", "Contoh: 0812...");
        alamat = input(card, "Alamat", "Contoh: Palembang");

        Button save = button("Daftar dan Masuk", true);
        save.setOnClickListener(v -> register());
        card.addView(save);
        Button back = button("Kembali ke Login", false);
        back.setOnClickListener(v -> finish());
        card.addView(back);
        return scroll;
    }

    private void register() {
        try {
            db.register(text(nama), text(username), text(password), text(usaha), text(hp), text(alamat));
            User user = db.login(text(username), text(password));
            Intent intent = new Intent(this, DashboardActivity.class);
            AppNav.putUser(intent, user);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout column(int padding) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(padding), dp(padding), dp(padding), dp(padding));
        return l;
    }

    private LinearLayout card() {
        LinearLayout l = column(18);
        l.setBackgroundResource(R.drawable.bg_card);
        return l;
    }

    private void title(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(22);
        v.setTextColor(0xff103b52);
        v.setTypeface(null, 1);
        parent.addView(v);
    }

    private void subtitle(LinearLayout parent, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setTextColor(0xff5f7d90);
        v.setPadding(0, dp(4), 0, dp(8));
        parent.addView(v);
    }

    private EditText input(LinearLayout parent, String label, String hint) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(0xff103b52);
        l.setTypeface(null, 1);
        l.setPadding(0, dp(10), 0, dp(5));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        parent.addView(e);
        return e;
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xffffffff : 0xff076b9d);
        b.setTypeface(null, 1);
        b.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private String text(EditText e) {
        return e.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
