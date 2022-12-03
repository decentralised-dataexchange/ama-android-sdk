package io.igrant.data_wallet.qrcode.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.igrant.data_wallet.qrcode.ImageProcessingActivity;
import io.igrant.data_wallet.qrcode.QRCSActivity;

public class QRScanner {

    private static final int REQUEST_QRSCANNER_SDK = 101;

    private Intent mQRScannerIntent;

    private Bundle mQRScannerOptionsBundle;

    private static final String EXTRA_PREFIX = "io.igrant.qrcodescanner.util.QRScanner";

    public static final String EXTRA_URI = EXTRA_PREFIX + ".uri";
    public static final String EXTRA_TYPE = EXTRA_PREFIX + ".type";
    public static final String EXTRA_LOCALE = EXTRA_PREFIX + ".locale";
    public static final String EXTRA_ENCRYPTION = EXTRA_PREFIX + ".encryption";

    private String mType = QRType.TYPE_SCAN;

    public QRScanner() {
        mQRScannerIntent = new Intent();
        mQRScannerOptionsBundle = new Bundle();
        mQRScannerOptionsBundle.putString(EXTRA_LOCALE, "en");
        mQRScannerOptionsBundle.putString(EXTRA_ENCRYPTION, EncryptionType.ENCRYPTION_UTF);
    }

    /**
     * Set type of the function, Image processing or image scan.
     */
    public QRScanner withType(String type) {
        mType = type;
        mQRScannerOptionsBundle.putString(EXTRA_TYPE, type);
        return this;
    }

    /**
     * Set image URI.
     */
    public QRScanner withImageUri(Uri uri) {
        mQRScannerOptionsBundle.putParcelable(EXTRA_URI, uri);
        return this;
    }

    /**
     * Set locale for the qr scanner Sdk.
     */
    public QRScanner withLocale(String locale) {
        mQRScannerOptionsBundle.putString(EXTRA_LOCALE, locale);
        return this;
    }

    /**
     * Set encryption for the qr scanner Sdk.
     */
    public QRScanner withEncryption(String encryption) {
        mQRScannerOptionsBundle.putString(EXTRA_ENCRYPTION, encryption);
        return this;
    }

    /**
     * Send the Intent from an Activity
     *
     * @param activity Activity to receive result
     */
    public void start(@NonNull Activity activity) {
        start(activity, REQUEST_QRSCANNER_SDK);
    }

    /**
     * Send the Intent from an Activity with a custom request code
     *
     * @param activity    Activity to receive result
     * @param requestCode requestCode for result
     */
    public void start(@NonNull Activity activity, int requestCode) {
        activity.startActivityForResult(getIntent(activity), requestCode);
    }

    /**
     * Send the Intent from a Fragment
     *
     * @param fragment Fragment to receive result
     */
    public void start(@NonNull Context context, @NonNull Fragment fragment) {
        start(context, fragment, REQUEST_QRSCANNER_SDK);
    }

    /**
     * Send the Intent with a custom request code
     *
     * @param fragment    Fragment to receive result
     * @param requestCode requestCode for result
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void start(@NonNull Context context, @NonNull Fragment fragment, int requestCode) {
        fragment.startActivityForResult(getIntent(context), requestCode);
    }

    /**
     * Get Intent to start {@link QRCSActivity}
     *
     * @return Intent for {@link QRCSActivity}
     */
    public Intent getIntent(@NonNull Context context) {
        mQRScannerIntent.setClass(context, mType.equals(QRType.TYPE_SCAN) ? QRCSActivity.class :
                ImageProcessingActivity.class);
        mQRScannerIntent.putExtras(mQRScannerOptionsBundle);
        return mQRScannerIntent;
    }

}
