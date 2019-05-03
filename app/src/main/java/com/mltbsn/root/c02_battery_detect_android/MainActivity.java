package com.mltbsn.root.c02_battery_detect_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Button mBtnBluetooth;
    private EditText mEditTextUsername;
    private EditText mEditTextPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnBluetooth = findViewById(R.id.btn_bluetooth);
        mEditTextPassword = findViewById(R.id.editTextPassword);
        mEditTextUsername = findViewById(R.id.editTextUsername);
        setListeners();

    }

    private void setListeners() {
        OnClick onClick = new OnClick();
        mBtnBluetooth.setOnClickListener(onClick);
    }

    private class OnClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = null;
            switch (v.getId()) {

                case R.id.btn_bluetooth:
                    intent = new Intent(MainActivity.this, BluetoothBleActivity.class);
                    startActivity(intent);
                    break;
            }

        }
    }

}
