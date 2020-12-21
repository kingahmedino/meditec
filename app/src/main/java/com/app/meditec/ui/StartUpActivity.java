package com.app.meditec.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.app.meditec.R;
import com.app.meditec.ui.maps.MapsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class StartUpActivity extends AppCompatActivity {
    private static final String TAG = "StartUpActivity";
    private static final int ERROR_DIALOG_REQUEST = 401;
    private Button mGetStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        mGetStartedButton = findViewById(R.id.get_started_btn);
        if(isGoogleServicesAvailable())
            getStartedButtonClickListener();
    }

    private void getStartedButtonClickListener() {
        mGetStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartUpActivity.this, MapsActivity.class));
            }
        });
    }

    public boolean isGoogleServicesAvailable() {
        Log.d(TAG, "isGoogleServicesAvailable ?");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (available == ConnectionResult.SUCCESS) {
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // resolvable error occured
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(StartUpActivity.this,
                    available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        return false;
    }
}
