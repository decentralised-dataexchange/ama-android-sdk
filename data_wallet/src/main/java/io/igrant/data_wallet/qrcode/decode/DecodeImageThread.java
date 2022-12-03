package io.igrant.data_wallet.qrcode.decode;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;

public class DecodeImageThread implements Runnable {
    private int mWidth;
    private int mHeight;
    private String mEncryption;
    private Bitmap mImg;
    private DecodeImageCallback mCallback;
    private Context mContext;

    public DecodeImageThread(String encryption,Bitmap imgPath, DecodeImageCallback callback, Context context) {
       this.mEncryption = encryption;
        this.mImg = imgPath;
        this.mCallback = callback;
        this.mContext = context;
    }

    private int[] bitmapBuffer = new int[0];

    @Override
    public void run() {

        Hashtable<DecodeHintType, String> hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET,mEncryption);


        this.mWidth = mImg.getWidth();
        this.mHeight = mImg.getHeight();

        int size = this.mWidth * this.mHeight;

        if (size > bitmapBuffer.length) {
            bitmapBuffer = new int[size];
        }

        mImg.getPixels(bitmapBuffer, 0, this.mWidth, 0, 0, this.mWidth, this.mHeight);

        RGBLuminanceSource source = new RGBLuminanceSource(this.mWidth, this.mHeight, bitmapBuffer);
        BinaryBitmap bbitmap = new BinaryBitmap(new HybridBinarizer(source));

        QRCodeReader reader = new QRCodeReader();

        try {
            mCallback.decodeSucceed(reader.decode(bbitmap, hints));
        } catch (NotFoundException | FormatException | ChecksumException e) {
            mCallback.decodeFail(0, "");
            e.printStackTrace();
        }
    }
}
