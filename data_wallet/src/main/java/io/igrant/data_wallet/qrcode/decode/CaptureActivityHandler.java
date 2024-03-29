package io.igrant.data_wallet.qrcode.decode;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.zxing.Result;

import io.igrant.data_wallet.R;
import io.igrant.data_wallet.qrcode.QRCSActivity;
import io.igrant.data_wallet.qrcode.camera.CameraManager;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 */
public final class CaptureActivityHandler extends Handler {
    private static final String TAG = CaptureActivityHandler.class.getName();

    private final QRCSActivity mActivity;
    private final DecodeThread mDecodeThread;
    private State mState;

    private enum State {
        PREVIEW, SUCCESS, DONE
    }

    public CaptureActivityHandler(QRCSActivity activity,String encryption) {
        this.mActivity = activity;
        mDecodeThread = new DecodeThread(activity,encryption);
        mDecodeThread.start();
        mState = State.SUCCESS;
        // Start ourselves capturing previews and decoding.
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {

        if(message.what == R.id.auto_focus)
        {
               Log.d(TAG, "Got auto-focus message");
                // When one auto focus pass finishes, start another. This is the closest thing to
                // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
                if (mState == State.PREVIEW) {
                    CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
                }
        }else if(message.what ==R.id.decode_succeeded)
        {
            Log.e(TAG, "Got decode succeeded message");
                mState = State.SUCCESS;
                mActivity.handleDecode((Result) message.obj);
        }else if(message.what ==R.id.decode_failed)
        {
            // We're decoding as fast as possible, so when one decode fails, start another.
                mState = State.PREVIEW;
                CameraManager.get().requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
        }
    }

    public void quitSynchronously() {
        mState = State.DONE;
        CameraManager.get().stopPreview();
        Message quit = Message.obtain(mDecodeThread.getHandler(), R.id.quit);
        quit.sendToTarget();
        try {
            mDecodeThread.join();
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded);
        removeMessages(R.id.decode_failed);
    }

    public void restartPreviewAndDecode() {
        if (mState != State.PREVIEW) {
            CameraManager.get().startPreview();
            mState = State.PREVIEW;
            CameraManager.get().requestPreviewFrame(mDecodeThread.getHandler(), R.id.decode);
            CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
        }
    }

}
