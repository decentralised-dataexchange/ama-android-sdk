package io.igrant.data_wallet.qrcode;

import static io.igrant.data_wallet.qrcode.utils.EncryptionType.ENCRYPTION_UTF;
import static io.igrant.data_wallet.qrcode.utils.QRScanner.EXTRA_ENCRYPTION;
import static io.igrant.data_wallet.qrcode.utils.QRScanner.EXTRA_TYPE;
import static io.igrant.data_wallet.qrcode.utils.QrUtils.decodeUriToBitmap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.igrant.data_wallet.R;
import io.igrant.data_wallet.qrcode.decode.DecodeImageCallback;
import io.igrant.data_wallet.qrcode.decode.DecodeImageThread;
import io.igrant.data_wallet.qrcode.utils.QRScanner;
import io.igrant.data_wallet.qrcode.utils.QRType;

public class ImageProcessingActivity extends AppCompatActivity {

    private CropImageView cropImageView;
    private Button btnApply;

    private Bitmap mBitmap;

    private Executor mQrCodeExecutor;

    private final String GOT_RESULT = "com.blikoon.qrcodescanner.got_qr_scan_relult";
    private final String ERROR_DECODING_IMAGE = "com.blikoon.qrcodescanner.error_decoding_image";
    private final String LOGTAG = "QRScannerQRCodeActivity";
    private String mEncryption = ENCRYPTION_UTF;
    private String mType = QRType.TYPE_IMAGE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);
        initData();
        initView();
        handleIntent();
    }

    private void initData() {
        mQrCodeExecutor = Executors.newSingleThreadExecutor();
    }

    private void handleIntent() {
        if (getIntent().hasExtra(EXTRA_ENCRYPTION))
            mEncryption = getIntent().getStringExtra(EXTRA_ENCRYPTION);

        if (getIntent().hasExtra(EXTRA_TYPE))
            mType = getIntent().getStringExtra(EXTRA_TYPE);

        if (getIntent().hasExtra(QRScanner.EXTRA_URI)) {
            Uri uri = getIntent().getParcelableExtra(QRScanner.EXTRA_URI);
            if (mType.equals(QRType.TYPE_IMAGE))
                mBitmap = decodeUriToBitmap(this, uri);
            else {
                try {
                    mBitmap = convert(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setImage();
            mQrCodeExecutor.execute(new DecodeImageThread(mEncryption, mBitmap, mDecodeImageCallback, ImageProcessingActivity.this));

        }
    }

    private void setImage() {
        cropImageView.setImageBitmap(mBitmap);
    }

    private File getFileFromUri(Uri uri) {
        return new File(uri.getPath());
    }

    public String getPath(Uri uri) {
        final ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null)
            return null;

        // Create file path inside app's data dir
        String filePath = getApplicationInfo().dataDir + File.separator
                + System.currentTimeMillis();

        File file = new File(filePath);
        try {
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null)
                return null;

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                outputStream.write(buf, 0, len);

            outputStream.close();
            inputStream.close();
        } catch (IOException ignore) {
            return null;
        }

        return file.getAbsolutePath();
    }

    private Bitmap convert(Uri documentFile) {
        Bitmap bitmap = null;

        try {
            // Create the page renderer for the PDF document.
            ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(documentFile,"r");
            PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

            // Open the page to be rendered.
            PdfRenderer.Page page = pdfRenderer.openPage(0);

            // Render the page to the bitmap.
            bitmap = Bitmap.createBitmap(getResources().getDisplayMetrics().densityDpi * page.getWidth() / 72,
                    getResources().getDisplayMetrics().densityDpi * page.getHeight() / 72, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

            // Close the page when you are done with it.
            page.close();

            // Close the `PdfRenderer` when you are done with it.
            pdfRenderer.close();
        } catch (IOException e) {
            Log.d("milna", "convert: " + e.getMessage());
        }
        return bitmap;
    }

    private void initView() {
        cropImageView = findViewById(R.id.cropImageView);
        btnApply = findViewById(R.id.btnApply);

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mQrCodeExecutor.execute(new DecodeImageThread(mEncryption, cropImageView.getCroppedImage(), mDecodeImageCallback, ImageProcessingActivity.this));
            }
        });

        ImageView ivClose = findViewById(R.id.ivClose);
        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        setResult(Activity.RESULT_CANCELED, data);
        finish();
    }

    private DecodeImageCallback mDecodeImageCallback = new DecodeImageCallback() {
        @Override
        public void decodeSucceed(Result result) {
            //Got scan result from scaning an image loaded by the user
            Log.d(LOGTAG, "Decoded the image successfully :" + result.getText());
            Intent data = new Intent();
            data.putExtra(GOT_RESULT, result.getText());
            setResult(Activity.RESULT_OK, data);
            finish();
        }

        @Override
        public void decodeFail(int type, String reason) {
            Log.d(LOGTAG, "Something went wrong decoding the image :" + reason);
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(ImageProcessingActivity.this, "couldn't process the image, please try after cropping", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
}
