package com.te.projecttranslate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import te.projecttranslate.R;

public class MainActivity extends AppCompatActivity {

    private Button translateButton;
    private Button ocrButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        translateButton = findViewById(R.id.translateBtn);

        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, TranslateActivity.class);
                startActivity(i);
            }
        });

        ocrButton = findViewById(R.id.ocrBtn);

        ocrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, OcrActivity.class);
                startActivity(i);
            }
        });
    }
}