package org.sapmentors.nwcloud.innojam.familymatters;

import org.sapmentors.nwcloud.innojam.familymatters.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class NotificationResponseActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_response);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_notification_response, menu);
        return true;
    }
}
