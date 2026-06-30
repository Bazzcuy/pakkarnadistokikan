package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bagas.stokikan.db.DbHelper;

import java.io.InputStream;
import java.io.OutputStream;

public class DataTransferActivity extends Activity {
    private static final int REQ_EXPORT = 301;
    private static final int REQ_IMPORT = 302;
    private DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        setContentView(buildView());
    }

    private LinearLayout buildView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundResource(R.drawable.bg_screen);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(28, 28, 28, 28);
        card.setBackgroundResource(R.drawable.bg_card);

        TextView title = new TextView(this);
        title.setText("Export / Import Data");
        title.setTextSize(22);
        title.setTypeface(null, 1);
        title.setTextColor(getColor(R.color.catokan_text));

        TextView subtitle = new TextView(this);
        subtitle.setText("Export menyimpan seluruh data aplikasi. Import memuat ulang data dari file backup CATOKAN.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(getColor(R.color.catokan_muted));
        subtitle.setPadding(0, 10, 0, 20);

        Button export = button("Export Data Aplikasi");
        export.setOnClickListener(v -> startExport());
        Button importBtn = button("Import Data Aplikasi");
        importBtn.setOnClickListener(v -> startImport());
        Button back = button("Kembali");
        back.setOnClickListener(v -> finish());

        card.addView(title);
        card.addView(subtitle);
        card.addView(export);
        card.addView(importBtn);
        card.addView(back);
        root.addView(card);
        return root;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(getColor(R.color.catokan_primary_dark));
        button.setBackgroundResource(R.drawable.bg_secondary_btn);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52);
        params.setMargins(0, 10, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "catokan-data.json");
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
                Toast.makeText(this, "Data aplikasi berhasil diexport.", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQ_IMPORT) {
                int rows;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    rows = db.importJson(in);
                }
                Toast.makeText(this, rows + " baris data berhasil diimport.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
