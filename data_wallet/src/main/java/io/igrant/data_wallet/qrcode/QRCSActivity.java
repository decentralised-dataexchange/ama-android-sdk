package io.igrant.data_wallet.qrcode;

import static io.igrant.data_wallet.qrcode.utils.QRScanner.EXTRA_ENCRYPTION;
import static io.igrant.data_wallet.qrcode.utils.QRScanner.EXTRA_LOCALE;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;

import java.io.IOException;
import java.lang.ref.WeakReference;

import io.igrant.data_wallet.R;
import io.igrant.data_wallet.qrcode.camera.CameraManager;
import io.igrant.data_wallet.qrcode.decode.CaptureActivityHandler;
import io.igrant.data_wallet.qrcode.decode.DecodeManager;
import io.igrant.data_wallet.qrcode.decode.InactivityTimer;
import io.igrant.data_wallet.qrcode.utils.EncryptionType;
import io.igrant.data_wallet.qrcode.utils.LocaleHelper;
import io.igrant.data_wallet.qrcode.view.QrCodeFinderView;

public class QRCSActivity extends AppCompatActivity implements Callback, OnClickListener {

    private static final int REQUEST_SYSTEM_PICTURE = 0;
    private static final int REQUEST_PICTURE = 1;
    public static final int MSG_DECODE_SUCCEED = 1;
    public static final int MSG_DECODE_FAIL = 2;
    private CaptureActivityHandler mCaptureActivityHandler;
    private boolean mHasSurface;
    private boolean mPermissionOk;
    private InactivityTimer mInactivityTimer;
    private QrCodeFinderView mQrCodeFinderView;
    private SurfaceView mSurfaceView;
    private View mLlFlashLight;
    private final DecodeManager mDecodeManager = new DecodeManager();

    private static final float BEEP_VOLUME = 0.10f;
    private static final long VIBRATE_DURATION = 200L;
    private MediaPlayer mMediaPlayer;
    private boolean mPlayBeep;
    private boolean mVibrate;
    private boolean mNeedFlashLightOpen = true;
    private ImageView mIvFlashLight;
    private TextView mTvFlashLightText;
//    private Executor mQrCodeExecutor;
    private Handler mHandler;

    private final String GOT_RESULT = "com.blikoon.qrcodescanner.got_qr_scan_relult";
    private final String ERROR_DECODING_IMAGE = "com.blikoon.qrcodescanner.error_decoding_image";
    private final String LOGTAG = "QRScannerQRCodeActivity";
    private Context mApplicationContext;

    private String mEncryption = EncryptionType.ENCRYPTION_UTF;
    private String mLocale = "en";
    private static Intent createIntent(Context context) {
        Intent i = new Intent(context, QRCSActivity.class);
        return i;
    }

    public static void launch(Context context) {
        Intent i = createIntent(context);
        context.startActivity(i);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        newBase = LocaleHelper.onAttach(newBase);
        super.attachBaseContext(newBase);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
        getIntentData();
        initView();
        initListener();
        initData();
        mApplicationContext = getApplicationContext();
    }

    private void getIntentData() {
        try {
            mEncryption = getIntent().getStringExtra(EXTRA_ENCRYPTION);
            mLocale = getIntent().getStringExtra(EXTRA_LOCALE);
            if (!mLocale.equals("")) {
                LocaleHelper.setLocale(this, mLocale);
            }
        } catch (Exception e) {

        }
    }

    private void initListener() {
        ImageView ivClose = findViewById(R.id.ivClose);
        ivClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent data = new Intent();
                setResult(Activity.RESULT_CANCELED, data);
                finish();
            }
        });
    }

    private void checkPermission() {
        boolean hasHardware = checkCameraHardWare(this);
        if (hasHardware) {
            if (!hasCameraPermission()) {
                findViewById(R.id.qr_code_view_background).setVisibility(View.VISIBLE);
                mQrCodeFinderView.setVisibility(View.GONE);
                mPermissionOk = false;
            } else {
                mPermissionOk = true;
            }
        } else {
            mPermissionOk = false;
            finish();
        }
    }

    private void initView() {
//        TextView tvPic = (TextView) findViewById(R.id.qr_code_header_black_pic);
        mIvFlashLight = (ImageView) findViewById(R.id.qr_code_iv_flash_light);
        mTvFlashLightText = (TextView) findViewById(R.id.qr_code_tv_flash_light);
        mQrCodeFinderView = (QrCodeFinderView) findViewById(R.id.qr_code_view_finder);
        mSurfaceView = (SurfaceView) findViewById(R.id.qr_code_preview_view);
        mLlFlashLight = findViewById(R.id.qr_code_ll_flash_light);
        mHasSurface = false;
        mIvFlashLight.setOnClickListener(this);
//        tvPic.setOnClickListener(this);
    }

    private void initData() {
        CameraManager.init(this);
        mInactivityTimer = new InactivityTimer(QRCSActivity.this);
//        mQrCodeExecutor = Executors.newSingleThreadExecutor();
        mHandler = new WeakHandler(this);
    }

    private boolean hasCameraPermission() {
        PackageManager pm = getPackageManager();
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.CAMERA", getPackageName());
    }

    @Override
    protected void onResume() {
        super.onResume();

        String lang = LocaleHelper.getLanguage(this);
        LocaleHelper.setLocale(this, lang);

        checkPermission();
        if (!mPermissionOk) {
            mDecodeManager.showPermissionDeniedDialog(this);
            return;
        }
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        turnFlashLightOff();
        if (mHasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        mPlayBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            mPlayBeep = false;
        }
        initBeepSound();
        mVibrate = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCaptureActivityHandler != null) {
            mCaptureActivityHandler.quitSynchronously();
            mCaptureActivityHandler = null;
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer.shutdown();
        }
        super.onDestroy();
    }


    public void handleDecode(Result result) {
        mInactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            String resultString = result.getText();

            handleResult(resultString);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.qr_code_camera_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (RuntimeException re) {
            re.printStackTrace();
            mDecodeManager.showPermissionDeniedDialog(this);
            return;
        }
        mQrCodeFinderView.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.VISIBLE);
        mSurfaceView.setFocusable(true);
        mLlFlashLight.setVisibility(View.VISIBLE);
        findViewById(R.id.qr_code_view_background).setVisibility(View.GONE);
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = new CaptureActivityHandler(this,mEncryption);
        }
    }

    private void restartPreview() {
        if (null != mCaptureActivityHandler) {
            mCaptureActivityHandler.restartPreviewAndDecode();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    private boolean checkCameraHardWare(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!mHasSurface) {
            mHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
    }

    public Handler getCaptureActivityHandler() {
        return mCaptureActivityHandler;
    }

    private void initBeepSound() {
//        if (mPlayBeep && mMediaPlayer == null) {
//            // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
//            // so we now play on the music stream.
//            setVolumeControlStream(AudioManager.STREAM_MUSIC);
//            mMediaPlayer = new MediaPlayer();
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mMediaPlayer.setOnCompletionListener(mBeepListener);
//
//            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
//            try {
//                mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
//                file.close();
//                mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
//                mMediaPlayer.prepare();
//            } catch (IOException e) {
//                mMediaPlayer = null;
//            }
//        }
    }

    private void playBeepSoundAndVibrate() {
//        if (mPlayBeep && mMediaPlayer != null) {
//            mMediaPlayer.start();
//        }
//        if (mVibrate) {
//            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//            vibrator.vibrate(VIBRATE_DURATION);
//        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener mBeepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.qr_code_iv_flash_light) {
            if (mNeedFlashLightOpen) {
                turnFlashlightOn();
            } else {
                turnFlashLightOff();
            }

        }
//        else if(v.getId() == R.id.qr_code_header_black_pic)
//        {
//            if (!hasCameraPermission()) {
//                    mDecodeManager.showPermissionDeniedDialog(this);
//                } else {
//                    openSystemAlbum();
//                }
//
//        }

    }

//    private void openSystemAlbum() {
//        Intent intent = new Intent();
//        intent.setType("image/*");
//        intent.setAction(Intent.ACTION_PICK);
//        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//        startActivityForResult(intent, REQUEST_SYSTEM_PICTURE);
//    }

    private void turnFlashlightOn() {
        mNeedFlashLightOpen = false;
        mTvFlashLightText.setText(getString(R.string.qr_code_close_flash_light));
        mIvFlashLight.setBackgroundResource(R.drawable.ic_flash_off_white);
        CameraManager.get().setFlashLight(true);
    }

    private void turnFlashLightOff() {
        mNeedFlashLightOpen = true;
        mTvFlashLightText.setText(getString(R.string.qr_code_open_flash_light));
        mIvFlashLight.setBackgroundResource(R.drawable.ic_flash_on_white);
        CameraManager.get().setFlashLight(false);
    }

    private void handleResult(String resultString) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(this, new DecodeManager.OnRefreshCameraListener() {
                @Override
                public void refresh() {
                    restartPreview();
                }
            });
        } else {
            //Got result from scanning QR Code with users camera
            Log.d(LOGTAG, "Got scan result from user loaded image :" + resultString);
            Intent data = new Intent();
            data.putExtra(GOT_RESULT, resultString);
            setResult(Activity.RESULT_OK, data);
            finish();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_PICTURE:
                finish();
                break;
            case REQUEST_SYSTEM_PICTURE:
//                Uri uri = data.getData();
//                Log.d("milna", "onActivityResult: "+uri);
//                mQrCodeExecutor.execute(new DecodeImageThread(uri, mDecodeImageCallback, this));
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

//    private DecodeImageCallback mDecodeImageCallback = new DecodeImageCallback() {
//        @Override
//        public void decodeSucceed(Result result) {
//            //Got scan result from scaning an image loaded by the user
//            Log.d(LOGTAG, "Decoded the image successfully :" + result.getText());
//            Intent data = new Intent();
//            data.putExtra(GOT_RESULT, result.getText());
//            setResult(Activity.RESULT_OK, data);
//            finish();
//        }
//
//        @Override
//        public void decodeFail(int type, String reason) {
//            Log.d(LOGTAG, "Something went wrong decoding the image :" + reason);
//            Intent data = new Intent();
//            data.putExtra(ERROR_DECODING_IMAGE, reason);
//            setResult(Activity.RESULT_CANCELED, data);
//            finish();
//        }
//    };

    private static class WeakHandler extends Handler {
        private WeakReference<QRCSActivity> mWeakQrCodeActivity;
        private DecodeManager mDecodeManager = new DecodeManager();

        public WeakHandler(QRCSActivity imagePickerActivity) {
            super();
            this.mWeakQrCodeActivity = new WeakReference<>(imagePickerActivity);
        }


        @Override
        public void handleMessage(Message msg) {
            QRCSActivity qrCodeActivity = mWeakQrCodeActivity.get();
            switch (msg.what) {
                case MSG_DECODE_SUCCEED:
                    Result result = (Result) msg.obj;
                    if (null == result) {
                        mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    } else {
                        String resultString = result.getText();
                        handleResult(resultString);
                    }
                    break;
                case MSG_DECODE_FAIL:
                    mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    break;
            }
            super.handleMessage(msg);
        }

        private void handleResult(String resultString) {
            QRCSActivity imagePickerActivity = mWeakQrCodeActivity.get();

            mDecodeManager.showResultDialog(imagePickerActivity, resultString, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }

    }
}
