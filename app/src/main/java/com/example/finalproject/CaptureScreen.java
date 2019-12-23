package com.example.finalproject;
import android.app.Activity;
import static android.app.Activity.RESULT_OK;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
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
    private static volatile ImageReader mImageReader = null;

    private ByteBuffer g_bytebuffer = null;

    private long frameCounter = 0;

    public int PermissionGranted = -1;
    public int Per_requestCode;
    public int Per_resultCode;
    public Intent Per_data;


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
        g_density = density;//g_metrics.densityDpi;                                          // 420

        //frameCounter = 0;

        MediaProjectionManager projectionManager = (MediaProjectionManager) mainActiv.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mainActiv.startActivityForResult(projectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
    }


    public void StartCaputre()
    {
        if(null == mainActiv || defineDelayMilli <= 0 || g_width <= 0 || g_height <= 0 || g_density<= 0)
        {
            Log.i(TAG, "Invalid parameters actv=" + mainActiv + ", delayMilli=" + defineDelayMilli + ", width=" + g_width + ", height="
                    + g_height + ", density=" + g_density);
            return;
        }

        if(PermissionGranted == 1) {
            //CaptureScreenActivityResult(Per_requestCode, Per_resultCode, Per_data);
            mImageReader = ImageReader.newInstance(g_width, g_height, PixelFormat.RGBA_8888, 2);

            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    frameCounter++;
                }
            }, null);
        }
        else if(PermissionGranted == 0)
            Per_requestCode = 0;

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

        Image img = null;
        try {



            long prev_counter = frameCounter;

            CaptureScreenActivityResult(Per_requestCode, Per_resultCode, Per_data);

            long startTimeMilli = System.currentTimeMillis();
            try {
                while(prev_counter >= frameCounter)
                    Thread.sleep(1);
            } catch (Exception e) {}
            Log.i(TAG, "AcquireLatestImage time:" + (System.currentTimeMillis() - startTimeMilli));

            img = mImageReader.acquireLatestImage();
            mProjection.stop();


            if (img != null) {
                startTimeMilli = System.currentTimeMillis();

                Image.Plane[] planes = img.getPlanes();
                if (planes[0].getBuffer() == null) {
                    if (null != img)
                        img.close();
                    return null;
                }

                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * g_width;
                //byte[] newData = new byte[g_width * g_height * 3];

                int offset = 0, i, j, offset2 = 0, redDotsbase = 12308;
                boolean BlurredFrame = true;
                // Debug
                //Bitmap bitmap = Bitmap.createBitmap(g_width, g_height, Bitmap.Config.ARGB_8888);
                //ByteBuffer buffer = planes[0].getBuffer();
                // Debug end
                ByteBuffer conv_buffer = ByteBuffer.allocateDirect(g_height * g_width * 3);

                byte[] arr = new byte[planes[0].getBuffer().remaining()];
                planes[0].getBuffer().get(arr);

                for (i = 0; i < 3; i++) {
                    for(j=0; j<2; j++) {
                        if ((arr[redDotsbase + (i * 4) + (1200*j)] != -1) ||
                                (arr[redDotsbase + 1 + (i * 4) + (1200*j)] != 0) ||
                                (arr[redDotsbase + 2 + (i * 4) + (1200*j)] != 0)) {
                            BlurredFrame = false;
                        }
                    }
                }

                if(BlurredFrame) {
                    if (null != img)
                        img.close();
                    counter.value = -1;
                    g_bytebuffer = null;
                    return null;
                }

                for (i = 0; i < g_height; ++i) {
                    for (j = 0; j < g_width; ++j) {
                        //conv_buffer.put(offset2, buffer.get(offset));           // R
                        //conv_buffer.put(offset2 + 1, buffer.get(offset + 1));   // G
                        //conv_buffer.put(offset2 + 2, buffer.get(offset + 2));   // B

                        conv_buffer.put(offset2 , arr[offset]);
                        conv_buffer.put(offset2+1 , arr[offset+1]);
                        conv_buffer.put(offset2+2 , arr[offset+2]);

                        /*if(arr[offset] == -1 && arr[offset+1] == 0 && arr[offset+2] == 0)
                            redDotsCount++;
                        else
                            redDotsCount = 0;*/

                        // Debug - start
                        /*int pixel = 0;
                        pixel |= (buffer.get(offset) & 0xff) << 16;     // R
                        pixel |= (buffer.get(offset + 1) & 0xff) << 8;  // G
                        pixel |= (buffer.get(offset + 2) & 0xff);       // B
                        pixel |= (buffer.get(offset + 3) & 0xff) << 24; // A
                        bitmap.setPixel(j, i, pixel);*/
                        // Debug - end

                        offset += pixelStride;
                        offset2 += pixelStride - 1;
                    }
                    offset += rowPadding;
                }


                g_bytebuffer = conv_buffer;
                //frameCounter++;

                // Debug - start
                /*FileOutputStream fos = null;
                try {
                    File file = new File(mainActiv.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "/myscreen"+frameCounter+".jpeg");
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
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }*/
                // Debug - end

                Log.i(TAG, "ConvertImageToBuffer time:" + (System.currentTimeMillis() - startTimeMilli));
            }
            else
                return null;

        } catch (Exception e) {
            e.printStackTrace();
            //Log.i(TAG, e.toString());
        } finally {
            if (null != img) {
                img.close();
            }
        }


        ByteBuffer buffer = g_bytebuffer;
        if(null != counter)
            counter.value = frameCounter;
        g_bytebuffer = null;

        return buffer;
    }

    public void CleanFrameQueue(){
        Image img;
        try {
            img = mImageReader.acquireLatestImage();
            if (null != img)
                img.close();
        }catch (Exception e){}
    }

    public long GetFrameCounter(){
        return frameCounter;
    }



    public void CaptureScreenActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == PERMISSION_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {

                MediaProjectionManager projectionManager = (MediaProjectionManager) mainActiv.getSystemService
                        (Context.MEDIA_PROJECTION_SERVICE);
                mProjection = projectionManager.getMediaProjection(resultCode, data);

                //mImageReader = ImageReader.newInstance(g_width, g_height, PixelFormat.RGBA_8888, 2);

                mProjection.createVirtualDisplay("screen-mirror", g_width, g_height, g_density,
                        android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null);

                /*mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        frameCounter++;
                    }
                    }, null);*/
            }
        }
    }
}
