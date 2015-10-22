package com.afollestad.digitussample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.digitus.Digitus;
import com.afollestad.digitus.DigitusCallback;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends AppCompatActivity implements DigitusCallback {

    private TextView mStatus;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatus = (TextView) findViewById(R.id.status);
        mButton = (Button) findViewById(R.id.beginAuthentication);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Digitus.init(this, getString(R.string.app_name), 6969);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Digitus.beginAuthentication();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Digitus.deinit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Digitus.handleResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onDigitusReady() {
        mStatus.setText(R.string.status_ready);
        mButton.setEnabled(true);
    }

    @Override
    public void onDigitusRegistrationNeeded() {
        mStatus.setText(R.string.status_registration_needed);
        mButton.setText(R.string.open_security_settings);
        mButton.setEnabled(true);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.setText(R.string.begin_authentication);
                Digitus.openSecuritySettings();
            }
        });
    }

    @Override
    public void onDigitusAuthenticated() {
        mStatus.setText(R.string.status_authenticated);
        mButton.setEnabled(true);
    }

    @Override
    public void onDigitusError(Exception e) {
        mStatus.setText(getString(R.string.status_error, e.getMessage()));
    }

    @Override
    public void onDigitusValidatePassword(String password) {
        Digitus.notifyPasswordValidation(password.equals("password"));
    }
}
