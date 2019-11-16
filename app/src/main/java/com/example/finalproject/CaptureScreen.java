package com.example.finalproject;

import android.app.Activity;
import static android.app.Activity.RESULT_OK;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import com.example.finalproject.MainActivity;

import java.nio.ByteBuffer;


public class CaptureScreen {

    private static final String TAG = "CaptureScreen";
    public static final int PERMISSION_CODE = 101;

    private static boolean busy = false;

    private long startTimeMilli = -1;
    private long defineDelayMilli = 0;

    private Activity mainActiv = null;
    private int g_width = 0;
    private int g_height = 0;
    private int g_density = 0;

    private MediaProjection mProjection;
    private ImageReader mImageReader = null;

    private ByteBuffer g_bytebuffer = null;

    private long frameCounter = 0;

    public CaptureScreen(Activity actv , long delayMilli , int ratio , int density)
    {

        //g_metrics = new DisplayMetrics();
        //mainActiv.getWindowManager().getDefaultDisplay().getMetrics(g_metrics);

        if(null == actv || delayMilli <= 0 || ratio <= 0 || density <= 0)
        {
            Log.i(TAG, "Invalid parameters actv=" + actv + ", delayMilli=" + delayMilli + ", ratio=" + ratio + ", density=" + density);
            return;
        }

        mainActiv = actv;
        defineDelayMilli = delayMilli;

        g_width = ratio;//(int)(((double)500/g_metrics.heightPixels)*g_metrics.widthPixels); // 300
        g_height = g_width;//g_metrics.heightPixels;                                    // 300
        g_density = density;//g_metrics.densityDpi;                                              //420
    }


    public void StartCaputre()
    {
        if(null == mainActiv || defineDelayMilli <= 0 || g_width <= 0 || g_height <= 0 || g_density<= 0)
        {
            Log.i(TAG, "Invalid parameters actv=" + mainActiv + ", delayMilli=" + defineDelayMilli + ", width=" + g_width + ", height="
                    + g_height + ", density=" + g_density);
            return;
        }

        frameCounter = 0;

        MediaProjectionManager projectionManager = (MediaProjectionManager) mainActiv.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mainActiv.startActivityForResult(projectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
    }

    public void StopCaputre() {

        mProjection.stop();
        mImageReader.setOnImageAvailableListener(null , null);

        mImageReader = null;
        mProjection = null;
        mainActiv = null;
        g_height = 0;
        g_width = 0;
        g_density = 0;
        defineDelayMilli = 0;
        startTimeMilli = -1;
        frameCounter = 0;
    }

    public ByteBuffer GetLatestFrame(MainActivity.IntObj counter) {

        ByteBuffer buffer = g_bytebuffer;
        counter.value = frameCounter;
        g_bytebuffer = null;

        return buffer;
    }



    public void CaptureScreenActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PERMISSION_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                MediaProjectionManager projectionManager = (MediaProjectionManager) mainActiv.getSystemService
                        (Context.MEDIA_PROJECTION_SERVICE);
                mProjection = projectionManager.getMediaProjection(resultCode, data);

                mImageReader = ImageReader.newInstance(g_width, g_height, PixelFormat.RGBA_8888, 5);

                mProjection.createVirtualDisplay("screen-mirror", g_width, g_height, g_density,
                        android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null);


                mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {

                        Image img = null;
                        img = reader.acquireLatestImage();

                        if(busy) {
                            if (null != img)
                                img.close();
                            return;
                        }

                        busy = true;
                        if (startTimeMilli == -1 || (System.currentTimeMillis() - startTimeMilli) > defineDelayMilli) {
                            Log.i(TAG, "Getting image in the listener");
                            Bitmap bitmap = null;

                            startTimeMilli = System.currentTimeMillis();

                            try {
                                if (img != null) {
                                    Image.Plane[] planes = img.getPlanes();
                                    if (planes[0].getBuffer() == null) {
                                        if (null != img)
                                            img.close();
                                        return;
                                    }

                                    int pixelStride = planes[0].getPixelStride();
                                    int rowStride = planes[0].getRowStride();
                                    int rowPadding = rowStride - pixelStride * g_width;
                                    //byte[] newData = new byte[g_width * g_height * 3];

                                    int offset = 0 , i , j , offset2 = 0;
                                    //bitmap = Bitmap.createBitmap(g_width, g_height, Bitmap.Config.ARGB_8888);
                                    ByteBuffer buffer = planes[0].getBuffer();
                                    ByteBuffer conv_buffer = ByteBuffer.allocateDirect(g_height * g_width * 3);
                                    for (i = 0; i < g_height; ++i) {
                                        for (j = 0; j < g_width; ++j) {
                                            //int pixel = 0;
                                            conv_buffer.put(offset2 , buffer.get(offset));             // R
                                            conv_buffer.put(offset2+1 , buffer.get(offset+1));   // G
                                            conv_buffer.put(offset2+2 , buffer.get(offset+2));   // B

                                            /*pixel |= (conv_buffer.get(offset2) & 0xff) << 16;     // R
                                            pixel |= (conv_buffer.get(offset2 + 1) & 0xff) << 8;  // G
                                            pixel |= (conv_buffer.get(offset2 + 2) & 0xff);       // B
                                            pixel |= 0; // A
                                            bitmap.setPixel(j, i, pixel);*/

                                            offset += pixelStride;
                                            offset2 += pixelStride-1;
                                        }
                                        offset += rowPadding;
                                    }

                                    g_bytebuffer = conv_buffer;
                                    frameCounter++;

                                    // Only for debugging
                                    /*FileOutputStream fos = null;
                                    try {
                                        File file = new File(mainActiv.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/myscreen0.jpeg");
                                        fos = new FileOutputStream(file);
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                    }catch(Exception e) {
                                        e.printStackTrace();
                                        Log.i(TAG, e.toString());
                                    }
                                    finally {
                                        if (null != fos) {
                                            try {
                                                fos.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }*/
                                    //img.close();

                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.i(TAG, e.toString());
                            } finally {
                                /*if (null != bitmap) {
                                    bitmap.recycle();
                                }*/
                                if (null != img) {
                                    img.close();
                                }
                            }
                        }
                        else
                            Log.i(TAG, "Elapsed time less than 200milli");

                        if (null != img)
                            img.close();

                        busy = false;
                    }
                }, null);
            }
        }
    }
}
