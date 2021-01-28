package org.dpppt.android.sdk.internal.hms;

import android.app.IntentService;
import android.content.Intent;

import com.huawei.hms.contactshield.ContactShield;
import com.huawei.hms.contactshield.ContactShieldCallback;
import com.huawei.hms.contactshield.ContactShieldEngine;

import org.dpppt.android.sdk.BuildConfig;
import org.dpppt.android.sdk.internal.logger.Logger;
import org.dpppt.android.sdk.internal.nearby.ExposureWindowMatchingWorker;

public class BackgroundContackShieldIntentService extends IntentService {
    private static final String TAG = "ContactShieldPendIntent";

    private ContactShieldEngine contactEngine;

    public BackgroundContackShieldIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        contactEngine = ContactShield.getContactShieldEngine(BackgroundContackShieldIntentService.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            contactEngine.handleIntent(intent, new ContactShieldCallback() {
                @Override
                public void onHasContact(String token) {
                    if (BuildConfig.FLAVOR.equals("calibration")) {
                        Logger.i(TAG, "received update for " + intent.toString());
                    }

                    ExposureWindowMatchingWorker.startMatchingWorker(BackgroundContackShieldIntentService.this);
                }

                @Override
                public void onNoContact(String token) {
                }
            });
        }
    }
}
