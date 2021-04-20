package com.example.datastoresyncshowstopper;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSDataStorePlugin());
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.configure(AmplifyConfiguration.builder(this).devMenuEnabled(false).build(), this);
            Log.d("test", "Amplify configured.");
        } catch (AmplifyException e) {
            Log.e("test", "Amplify Failed to Configure; this is likely caused by a duplicate call to Amplify.configure().", e);
        }
        setContentView(R.layout.activity_main);
    }
}