package org.dpppt.android.sdk.internal.hms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;

import androidx.core.util.Consumer;

import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration;
import com.google.android.gms.nearby.exposurenotification.ExposureInformation;
import com.google.android.gms.nearby.exposurenotification.ExposureSummary;
import com.google.android.gms.nearby.exposurenotification.ExposureWindow;
import com.google.android.gms.nearby.exposurenotification.ScanInstance;
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey;
import com.google.android.gms.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.contactshield.ContactDetail;
import com.huawei.hms.contactshield.ContactShield;
import com.huawei.hms.contactshield.ContactShieldEngine;
import com.huawei.hms.contactshield.ContactShieldSetting;
import com.huawei.hms.contactshield.ContactSketch;
import com.huawei.hms.contactshield.ContactWindow;
import com.huawei.hms.contactshield.DiagnosisConfiguration;
import com.huawei.hms.contactshield.PeriodicKey;
import com.huawei.hms.contactshield.ScanInfo;
import com.huawei.hms.contactshield.SharedKeyFileProvider;
import com.huawei.hms.contactshield.StatusCode;
import com.huawei.hms.utils.HMSPackageManager;

import org.dpppt.android.sdk.internal.logger.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ContactShieldWrapper {
    private static final String TAG = "ContactShieldWrapper";

    private static final String EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET = "either exception or result must be set";

    private static volatile ContactShieldWrapper instance;

    private WeakReference<Context> mContextWeakRef;

    private ContactShieldEngine mContactShieldEngine;

    private ContactShieldWrapper(Context context) {
        mContactShieldEngine = ContactShield.getContactShieldEngine(context);
        mContextWeakRef = new WeakReference<>(context);
    }

    public static ContactShieldWrapper getInstance(Context context) {
        if (instance == null) {
            synchronized (ContactShieldWrapper.class) {
                if (instance == null) {
                    instance = new ContactShieldWrapper(context);
                }
            }
        }
        return instance;
    }

    public void start(Activity activity, int resolutionRequestCode, Runnable successCallback, Consumer<Exception> errorCallback) {
        if (!(mContextWeakRef.get() instanceof Activity)) {
            mContactShieldEngine = ContactShield.getContactShieldEngine(activity);
            mContextWeakRef.clear();
            mContextWeakRef = new WeakReference<>(activity);
        }
        mContactShieldEngine.startContactShield(ContactShieldSetting.DEFAULT)
                .addOnSuccessListener(nothing -> {
                    Logger.i(TAG, "start: started successfully");
                    successCallback.run();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        ResolvableApiException apiException = (ResolvableApiException) e;
                        if (apiException.getResolution() != null) {
                            try {
                                Logger.i(TAG, "start: resolution required");
                                apiException.startResolutionForResult(activity, resolutionRequestCode);
                                return;
                            } catch (IntentSender.SendIntentException ex) {
                                Logger.e(TAG, "start: error calling startResolutionForResult()");
                            }
                        }
                    } else if (e instanceof ApiException&&((ApiException)e).getStatusCode() == StatusCode.STATUS_MISSING_PERMISSION_LOCATION) {
                        Logger.i(TAG, "start: resolution required");
                        initDialog(activity);
                        return;
                    }
                    Logger.e(TAG, "start", e);
                    errorCallback.accept(e);
                });
    }

    private void initDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setMessage("Please open the location permission in Settings - Apps - Apps - HMS Core - Permissions - Location - Allow all the time")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + HMSPackageManager.getInstance(activity).getHMSPackageName()));
                    activity.startActivity(intent);
                })
                .create()
                .show();
    }

    public void stop() {
        mContactShieldEngine.stopContactShield()
                .addOnSuccessListener(nothing -> Logger.i(TAG, "stop: stopped successfully"))
                .addOnFailureListener(e -> Logger.e(TAG, "stop", e));
    }

    public Task<Boolean> isEnabled() {
        return mContactShieldEngine.isContactShieldRunning();
    }


    public void getTemporaryExposureKeyHistory(Activity activity, int resolutionRequestCode,
                                               OnSuccessListener<List<TemporaryExposureKey>> successCallback, Consumer<Exception> errorCallback) {
        if (!(mContextWeakRef.get() instanceof Activity)) {
            mContactShieldEngine = ContactShield.getContactShieldEngine(activity);
            mContextWeakRef.clear();
            mContextWeakRef = new WeakReference<>(activity);
        }
        mContactShieldEngine.getPeriodicKey()
                .addOnSuccessListener(list -> {
                    Logger.d(TAG, "getTemporaryExposureKeyHistory: success");
                    successCallback.onSuccess(getTemporaryExposureKeyList(list));
                })
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        ResolvableApiException apiException = (ResolvableApiException) e;
                        if (apiException.getResolution() != null) {
                            try {
                                Logger.i(TAG, "getTemporaryExposureKeyHistory: resolution required");
                                apiException.startResolutionForResult(activity, resolutionRequestCode);
                                return;
                            } catch (IntentSender.SendIntentException ex) {
                                Logger.e(TAG, "getTemporaryExposureKeyHistory: error calling startResolutionForResult()");
                            }
                        }
                    }
                    Logger.e(TAG, "getTemporaryExposureKeyHistory", e);
                    errorCallback.accept(e);
                });
    }

    public List<TemporaryExposureKey> getTemporaryExposureKeyHistorySynchronous() throws Exception {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Object[] results = new Object[] { null };

        mContactShieldEngine.getPeriodicKey()
                .addOnSuccessListener(list -> {
                    List<TemporaryExposureKey> keys = getTemporaryExposureKeyList(list);
                    results[0] = keys;
                    countDownLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    results[0] = e;
                    countDownLatch.countDown();
                });

        countDownLatch.await();

        if (results[0] instanceof Exception) {
            throw (Exception) results[0];
        } else if (results[0] instanceof List) {
            return (List<TemporaryExposureKey>) results[0];
        } else {
            throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
        }
    }

    public void provideDiagnosisKeys(List<File> keys) throws Exception {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Exception[] exceptions = new Exception[] { null };
        Context context = mContextWeakRef.get();
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                new Intent(context, BackgroundContackShieldIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mContactShieldEngine.putSharedKeyFiles(pendingIntent,new SharedKeyFileProvider(keys))
                .addOnSuccessListener(nothing -> {
                    Logger.d(TAG, "provideDiagnosisKeys: inserted keys successfully");
                    countDownLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    Logger.e(TAG, "provideDiagnosisKeys", e);
                    exceptions[0] = e;
                    countDownLatch.countDown();
                });
        countDownLatch.await();
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void provideDiagnosisKeys(List<File> files, ExposureConfiguration configuration, String token) throws Exception {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (configuration == null) {
            throw new IllegalStateException("must call setParams()");
        }
        DiagnosisConfiguration diagnosisConfiguration = new DiagnosisConfiguration.Builder()
                .setMinimumRiskValueThreshold(configuration.getMinimumRiskScore())
                .setAttenuationRiskValues(configuration.getAttenuationScores())
                .setDaysAfterContactedRiskValues(configuration.getDaysSinceLastExposureScores())
                .setDurationRiskValues(configuration.getDurationScores())
                .setInitialRiskLevelRiskValues(configuration.getTransmissionRiskScores())
                .setAttenuationDurationThresholds(configuration.getDurationAtAttenuationThresholds())
                .build();
        Context context = mContextWeakRef.get();
        PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                new Intent(context, BackgroundContackShieldIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);

        final Object syncObject = new Object();
        Exception[] exceptions = new Exception[]{null};
        synchronized (syncObject) {
            mContactShieldEngine.putSharedKeyFiles(pendingIntent, files, diagnosisConfiguration, token)
                    .addOnSuccessListener(nothing -> {
                        Logger.d(TAG, "provideDiagnosisKeys: inserted keys successfully for token " + token);
                        synchronized (syncObject) {
                            syncObject.notifyAll();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Logger.e(TAG, "provideDiagnosisKeys for token " + token, e);
                        exceptions[0] = e;
                        synchronized (syncObject) {
                            syncObject.notifyAll();
                        }
                    });

            syncObject.wait();
        }
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Task<ContactSketch> getExposureSummary(String token) {
        return mContactShieldEngine.getContactSketch(token);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public Task<List<ContactDetail>> getExposureInformation(String token) {
        return mContactShieldEngine.getContactDetail(token);
    }

    public List<ExposureWindow> getExposureWindows() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Object[] results = new Object[] { null };

        mContactShieldEngine.getContactWindow(ContactShieldEngine.TOKEN_A)
                .addOnSuccessListener(list -> {
                    List<ExposureWindow> keys = getExposureWindowList(list);
                    results[0] = keys;
                    countDownLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    results[0] = e;
                    countDownLatch.countDown();
                });
        countDownLatch.await();
        if (results[0] instanceof Exception) {
            throw (Exception) results[0];
        } else if (results[0] instanceof List) {
            return (List<ExposureWindow>) results[0];
        } else {
            throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
        }
    }

    public void getVersion(com.huawei.hmf.tasks.OnSuccessListener<Long> onSuccessListener, com.huawei.hmf.tasks.OnFailureListener onFailureListener) {
        mContactShieldEngine.getContactShieldVersion()
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener);
    }

    public Integer getCalibrationConfidence() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Object[] results = new Object[] { null };

        mContactShieldEngine.getDeviceCalibrationConfidence()
                .addOnSuccessListener(confidence -> {
                    results[0] = confidence;
                    countDownLatch.countDown();
                })
                .addOnFailureListener(e -> {
                    results[0] = e;
                    countDownLatch.countDown();
                });
        countDownLatch.await();
        if (results[0] instanceof Exception) {
            throw (Exception) results[0];
        } else if (results[0] instanceof Integer) {
            return (Integer) results[0];
        } else {
            throw new IllegalStateException(EITHER_EXCEPTION_OR_RESULT_MUST_BE_SET);
        }
    }

    public List<TemporaryExposureKey> getTemporaryExposureKeyList(List<PeriodicKey> periodicKeyList) {
        List<TemporaryExposureKey> temporaryExposureKeyList = new ArrayList<>();
        for (PeriodicKey periodicKey : periodicKeyList) {
            TemporaryExposureKey temporaryExposureKey =
                    new TemporaryExposureKey.TemporaryExposureKeyBuilder()
                            .setKeyData(periodicKey.getContent())
                            .setRollingStartIntervalNumber((int) periodicKey.getPeriodicKeyValidTime())
                            .setRollingPeriod((int) periodicKey.getPeriodicKeyLifeTime())
                            .setTransmissionRiskLevel(periodicKey.getInitialRiskLevel())
                            .build();
            temporaryExposureKeyList.add(temporaryExposureKey);
        }
        return temporaryExposureKeyList;
    }

    public List<ExposureWindow> getExposureWindowList(List<ContactWindow> contactWindowList) {
        List<ExposureWindow> exposureWindowList = new ArrayList<>();
        for (ContactWindow item : contactWindowList) {
            ExposureWindow exposureWindow =
                    new ExposureWindow.Builder()
                            .setCalibrationConfidence(item.getCalibrationConfidence())
                            .setInfectiousness(item.getContagiousness())
                            .setDateMillisSinceEpoch(item.getDateMillis())
                            .setReportType(item.getReportType())
                            .setScanInstances(getScanInstances(item.getScanInfos()))
                            .build();
            exposureWindowList.add(exposureWindow);
        }
        return exposureWindowList;
    }

    private List<ScanInstance> getScanInstances(List<ScanInfo> scanInfos) {
        List<ScanInstance> scanInstanceList = new ArrayList<>();
        for(ScanInfo item:scanInfos){
            ScanInstance scanInstance =
                    new ScanInstance.Builder()
                            .setTypicalAttenuationDb(item.getAverageAttenuation())
                            .setMinAttenuationDb(item.getMinimumAttenuation())
                            .setSecondsSinceLastScan(item.getSecondsSinceLastScan())
                            .build();
            scanInstanceList.add(scanInstance);
        }
        return scanInstanceList;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public ExposureSummary getExposureSummary(ContactSketch contactSketch) {
        return new ExposureSummary.ExposureSummaryBuilder()
                .setDaysSinceLastExposure(contactSketch.getDaysSinceLastHit())
                .setMatchedKeyCount(contactSketch.getNumberOfHits())
                .setMaximumRiskScore(contactSketch.getMaxRiskValue())
                .setSummationRiskScore(contactSketch.getSummationRiskValue())
                .setAttenuationDurations(contactSketch.getAttenuationDurations())
                .build();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public List<ExposureInformation> getExposureInformationList(List<ContactDetail> contactDetailList) {
        List<ExposureInformation> exposureInformationList = new ArrayList<>();
        for (ContactDetail detail : contactDetailList) {
            ExposureInformation exposureInformation =
                    new ExposureInformation.ExposureInformationBuilder()
                            .setDateMillisSinceEpoch(detail.getDayNumber())
                            .setAttenuationValue(detail.getAttenuationRiskValue())
                            .setTransmissionRiskLevel(detail.getInitialRiskLevel())
                            .setDurationMinutes(detail.getDurationMinutes())
                            .setAttenuationDurations(detail.getAttenuationDurations())
                            .setTotalRiskScore(detail.getTotalRiskValue())
                            .build();
            exposureInformationList.add(exposureInformation);
        }
        return exposureInformationList;
    }

}