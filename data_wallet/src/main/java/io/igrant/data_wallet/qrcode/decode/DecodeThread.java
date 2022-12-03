package io.igrant.data_wallet.qrcode.decode;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;

import io.igrant.data_wallet.qrcode.QRCSActivity;

/**
 * This thread does all the heavy lifting of decoding the images.
 */
final class DecodeThread extends Thread {

    private final QRCSActivity mActivity;
    private final String mEncryption;
    private Handler mHandler;
    private final CountDownLatch mHandlerInitLatch;

    DecodeThread(QRCSActivity activity,String encryption) {
        this.mActivity = activity;
        this.mEncryption = encryption;
        mHandlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return mHandler;
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new DecodeHandler(mActivity,mEncryption);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

}
