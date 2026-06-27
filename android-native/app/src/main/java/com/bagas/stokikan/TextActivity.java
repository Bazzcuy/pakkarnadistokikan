package com.bagas.stokikan;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.bagas.stokikan.db.DbHelper;

public class TextActivity extends Activity {
    private DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        db = new DbHelper(this);
        String title = getIntent().getStringExtra("title");
        String mode = getIntent().getStringExtra("mode");
        if (title == null) title = "Data";
        ((TextView) findViewById(R.id.txtTitle)).setText(title);
        ((TextView) findViewById(R.id.txtContent)).setText(content(mode));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private String content(String mode) {
        if ("raw".equals(mode)) return db.stokMentahText();
        if ("giling".equals(mode)) return db.stokGilingText();
        return db.dashboardText() + "\n" + db.stokMentahText() + "\n" + db.stokGilingText();
    }
}
