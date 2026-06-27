package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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
        setContentView(R.layout.activity_login);
        db = new DbHelper(this);
        db.getWritableDatabase();
        username = findViewById(R.id.edtUsername);
        password = findViewById(R.id.edtPassword);
        username.setText("admin");
        password.setText("admin123");

        findViewById(R.id.btnLogin).setOnClickListener(v -> login(username.getText().toString().trim(), password.getText().toString().trim()));
        findViewById(R.id.btnAdmin).setOnClickListener(v -> login("admin", "admin123"));
        findViewById(R.id.btnKasir).setOnClickListener(v -> login("kasir", "kasir123"));
        findViewById(R.id.btnOperator).setOnClickListener(v -> login("operator", "operator123"));
    }

    private void login(String u, String p) {
        User user = db.login(u, p);
        if (user == null) {
            Toast.makeText(this, "Login gagal. Periksa username dan password.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(this, DashboardActivity.class);
        AppNav.putUser(intent, user);
        startActivity(intent);
        finish();
    }
}
