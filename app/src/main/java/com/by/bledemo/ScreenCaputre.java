package com.by.bledemo;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.FaceDetector;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;


import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.FaceDetector.Face.CONFIDENCE_THRESHOLD;
import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

public class ScreenCaputre extends Service {

    private static final String TAG = ScreenCaputre.class.getSimpleName();
    private static MainActivity mainActivity;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public interface ScreenCaputreListener {
        // void onImageData(byte[] buf);
        void onImageBitmap(Bitmap bitmap);

    }

    private ScreenCaputreListener screenCaputreListener;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader imageReader;


    public int width;
    public int height;
    public int x, cx, y, cy;
    public float isFaceConfidence = 0.4f;


    public ScreenCaputre(int width, int height, MediaProjection mMediaProjection, MainActivity mainActivity) {

        //自动获取屏幕宽高
        this.width = width;
        this.height = height;
        this.cx = width / 2;
        this.cy = height / 2;

        this.mMediaProjection = mMediaProjection;
        this.mainActivity = mainActivity;


    }

    public void setScreenCaputreListener(ScreenCaputreListener screenCaputreListener) {
        this.screenCaputreListener = screenCaputreListener;
    }

    public void start() {
        try {

            prepareVideoEncoder();

            //startVideoEncode();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        videoEncoderLoop = false;
        if (null != vEncoder) {
            vEncoder.stop();

        }
        if (mVirtualDisplay != null) mVirtualDisplay.release();
        if (mMediaProjection != null) mMediaProjection.stop();
        vEncoder = null;

    }


    private MediaCodec.BufferInfo vBufferInfo = new MediaCodec.BufferInfo();
    private MediaCodec vEncoder;
    private Thread videoEncoderThread;

    private boolean videoEncoderLoop;

    public void prepareVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(KEY_BIT_RATE, width * height);
        format.setInteger(KEY_FRAME_RATE, 30);
        format.setInteger(KEY_I_FRAME_INTERVAL, 1);

        MediaCodec vencoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        //vencoder.configure(format, null, null, CONFIGURE_FLAG_ENCODE);

        // PixelFormat.RGBA_8888
        // ImageFormat.JPEG
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("-display", width, height, 1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);

        vEncoder = vencoder;
        imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);

    }


    public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    Log.v(TAG, "有图片数据");

                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                    // Log.v(TAG, "w:" + width + "h:" + height);

                    image.close();

                    if (null != screenCaputreListener) {
                        screenCaputreListener.onImageBitmap(bitmap);
                    }

                    // 设定最大可查的人脸数量
                    int MAX_FACES = 1;

                    if (true) {
                        FaceDetector faceDet = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), MAX_FACES);
                        // Log.d(TAG, "onImageAvailable: 宽w:" +bitmap.getWidth() + "高h：" +bitmap.getHeight());
                        // 将人脸数据存储到faceArray 中
                        FaceDetector.Face[] faceArray = new FaceDetector.Face[MAX_FACES];
                        // 返回找到图片中人脸的数量，同时把返回的脸部位置信息放到faceArray中，过程耗时
                        bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                        // 构造实例并解析人脸
                        int findFaceCount = faceDet.findFaces(bitmap, faceArray);


                        if (findFaceCount == 0) {
                            return;
                        }

                        //获取的人脸位置
                        PointF mid = new PointF();
                        faceArray[0].getMidPoint(mid);

                        isFaceConfidence = faceArray[0].confidence();
                        Log.d(TAG, "isFace the confidence?" +isFaceConfidence);
                        x = (int) mid.x;
                        y = (int) mid.y;

                        int x2 = cx - x;
                        int y2 = cy - y;

                        Log.d(TAG, "差值: " + "x:" + x2 + ",y:" + y2);
                        if (isFaceConfidence > 0.51f) {
                            mainActivity.sendFollowData(x2, y2, 25);
                        }


                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
