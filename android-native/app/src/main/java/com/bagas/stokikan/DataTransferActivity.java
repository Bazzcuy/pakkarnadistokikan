package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;

import java.io.InputStream;
import java.io.OutputStream;

public class DataTransferActivity extends Activity {
    private static final int REQ_EXPORT = 301;
    private static final int REQ_IMPORT = 302;
    private DbHelper db;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppNav.readUser(this);
        db = new DbHelper(this);
        setContentView(buildView());
    }

    private ScrollView buildView() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundResource(R.drawable.bg_screen);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(root);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_card);
        card.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Cadangkan / Pulihkan Data");
        title.setTextSize(23);
        title.setTypeface(null, 1);
        title.setTextColor(getColor(R.color.catokan_text));

        TextView subtitle = new TextView(this);
        subtitle.setText("Gunakan cadangkan data sebelum pindah HP. Gunakan pulihkan data untuk memasukkan kembali file cadangan CATOKAN.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getColor(R.color.catokan_muted));
        subtitle.setPadding(0, dp(8), 0, dp(16));
        subtitle.setLineSpacing(dp(2), 1f);

        Button export = button("Cadangkan Data", true);
        export.setOnClickListener(v -> startExport());
        Button importBtn = button("Pulihkan Data", false);
        importBtn.setOnClickListener(v -> startImport());
        status = new TextView(this);
        status.setText("Belum ada aksi. Pilih Cadangkan Data atau Pulihkan Data.");
        status.setTextSize(13);
        status.setTextColor(getColor(R.color.catokan_muted));
        status.setPadding(0, dp(14), 0, dp(4));
        Button back = button("Kembali", false);
        back.setOnClickListener(v -> finish());

        card.addView(title);
        card.addView(subtitle);
        card.addView(export);
        card.addView(importBtn);
        card.addView(status);
        card.addView(back);
        root.addView(card);
        return scroll;
    }

    private Button button(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, 1);
        button.setTextColor(primary ? 0xffffffff : getColor(R.color.catokan_primary_dark));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_btn : R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(10), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "cadangan-catokan.json");
        startActivityForResult(intent, REQ_EXPORT);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_EXPORT) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    db.exportJson(out);
                }
                status.setText("Data berhasil dicadangkan ke file pilihan.");
                Toast.makeText(this, "Data berhasil dicadangkan.", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQ_IMPORT) {
                int rows;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    rows = db.importJson(in);
                }
                status.setText(rows + " baris data berhasil dipulihkan.");
                Toast.makeText(this, "Data berhasil dipulihkan.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            status.setText("Gagal: " + e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
