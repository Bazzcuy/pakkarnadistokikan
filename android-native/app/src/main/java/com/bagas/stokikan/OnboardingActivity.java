package com.bagas.stokikan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class OnboardingActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        findViewById(R.id.btnStart).setOnClickListener(v -> openLogin());
        findViewById(R.id.btnSkip).setOnClickListener(v -> openLogin());
    }

    private void openLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
