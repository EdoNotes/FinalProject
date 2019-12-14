package com.example.finalproject;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.lang.Object;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class FrameClassifier {

    private static final String TAG = "FrameClassifier";

    // Only return this many results.
    private static final int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;

    private Interpreter tfLite;
    private ArrayList<Recognition> g_recognitions = null;

    private DisplayMetrics g_metrics;

    private float scaleX = 0 , scaleY = 0 , offsetX = 0 , offsetY = 0;


    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private Integer sensorOrientation;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private FrameClassifier detector;

    public FrameClassifier(Activity act , int definedRatio) {

        //TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 81}, DataType.UINT8);
        if (definedRatio <= 0 )
            return;

        g_metrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(g_metrics);
        float ratio = (float)definedRatio;
        float width = (float)g_metrics.widthPixels;
        float height = (float)g_metrics.heightPixels;
        if(width == height) {
            offsetX = 0;
            offsetY = 0;
            scaleX = scaleY = width/ratio;
        }
        else if(width > height) {
            offsetX = 0;
            scaleY = scaleX = width / ratio;
            offsetY = (width - height)/(2*scaleY);
        }
        else {
            offsetY = 0;
            scaleY = scaleX = height / ratio;
            offsetX = (height - width)/(2*scaleX);
        }

        // Initialise the model
        try{
            //AssetManager assetManager = act.getAssets();
            //String[] files = assetManager.list("");
            //MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(act, "mobilenet_v1_1.0_224_quant.tflite");
            tfLite = new Interpreter(loadModelFile(act.getAssets(), TF_OD_API_MODEL_FILE),2);

        } catch (Exception e){
            Log.i("FrameClassifier", "Error reading model", e);
        }
    }

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public int PredictFrame( ByteBuffer imgBuffer , float Threshold)
    {
        if(null == imgBuffer || Threshold > 1 || Threshold < 0)
            return -1;

        imgData = imgBuffer;

        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        try {
            long startTimeMilli = System.currentTimeMillis();
            // Run the inference call.
            tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
            Log.i(TAG, "RunningDetector time:" + (System.currentTimeMillis() - startTimeMilli));

            // Show the best detections.
            // after scaling them back to the input size.
            ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
            for (int i = 0; i < NUM_DETECTIONS; ++i) {
                if(outputScores[0][i] >= Threshold && outputClasses[0][i] == 0.0f /*Only person class*/) {
                    final RectF detection =
                            new RectF(
                                    ((outputLocations[0][i][1] * TF_OD_API_INPUT_SIZE)-offsetX) * scaleX,
                                    /*((*/outputLocations[0][i][0] * TF_OD_API_INPUT_SIZE/*)-offsetY)*/ * scaleY,
                                    ((outputLocations[0][i][3] * TF_OD_API_INPUT_SIZE)-offsetX) * scaleX,
                                    /*((*/outputLocations[0][i][2] * TF_OD_API_INPUT_SIZE/*)-(2*offsetY))*/ * scaleY);
                    // SSD Mobilenet V1 Model assumes class 0 is background class
                    // in label file and class labels start from 1 to number_of_classes+1,
                    // while outputClasses correspond to class index from 0 to number_of_classes
                    recognitions.add(
                            new Recognition(
                                    outputScores[0][i],
                                    detection));
                }
            }

            g_recognitions = recognitions;
        }catch (Exception e){
            e.printStackTrace();
            Log.i(TAG, e.toString());
            return -2;
        }

        return 0;
    }

    public ArrayList<int[]> GetCoordinates(){

        int i;
        ArrayList<int[]> al = null;

        for(i=0; i<g_recognitions.size(); i++)
        {
            if(null == al)
                al = new ArrayList<int[]>();
            int loc[] = {g_recognitions.get(i).location.top < 0 ? 0 : (g_recognitions.get(i).location.top > (float)g_metrics.heightPixels ? Math.round(g_metrics.heightPixels) : Math.round(g_recognitions.get(i).location.top)),
                    g_recognitions.get(i).location.left < 0 ? 0 : (g_recognitions.get(i).location.left > (float)g_metrics.widthPixels ? Math.round(g_metrics.widthPixels) : Math.round(g_recognitions.get(i).location.left)),
                    g_recognitions.get(i).location.right < 0 ? 0 : (g_recognitions.get(i).location.right > (float)g_metrics.widthPixels ? Math.round(g_metrics.widthPixels) : Math.round(g_recognitions.get(i).location.right)),
                    g_recognitions.get(i).location.bottom < 0 ? 0 : (g_recognitions.get(i).location.bottom > (float)g_metrics.heightPixels ? Math.round(g_metrics.heightPixels) : Math.round(g_recognitions.get(i).location.bottom))};
            al.add(i,loc);
        }

        //g_recognitions = null;

        return al;
    }


    public class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private String id;

        /** Display name for the recognition. */
        private String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        public final Float confidence;

        /** Optional location within the source image for the location of the recognized object. */
        public RectF location;

        public Recognition(
                final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public Recognition(
                final Float confidence, final RectF location) {
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (confidence != null) {
                resultString += String.format("(%.1f%%) ", confidence * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }
    }
}
