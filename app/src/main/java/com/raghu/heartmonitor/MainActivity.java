package com.raghu.heartmonitor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static String TAG = "MainActivity";
    private MonitorView javaCameraView;
    private BaseLoaderCallback mLoaderCallBack;
    private Mat mrgba;
    private GraphView graphView;
    private LineGraphSeries<DataPoint> rawData;
    private int dataPoints = 0;
    private long timeStarted = 0;
    private double redChannel = 0;
    private double redChannelPrev = 0;
    private int peakCount = 0;
    private double previousSlope = 0;
    private float frameStamp1 = 0;
    private float frameStamp2 = 0;
    private int frameNumber = 0;
    private int truePeak;
    private TextView bmpView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        javaCameraView = (MonitorView) findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        rawData = new LineGraphSeries<>();
        graphView = (GraphView) findViewById(R.id.rawGraph);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(1);
        graphView.getViewport().setMaxX(80);
        graphView.addSeries(rawData);
        mLoaderCallBack = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        javaCameraView.enableView();
                        break;
                    }
                    default: {
                        super.onManagerConnected(status);
                        break;
                    }
                }
            }
        };
        bmpView = (TextView) findViewById(R.id.bmpView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV? loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(TAG, "OpenCV failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mrgba = new Mat(height, width, CvType.CV_8UC4);
        javaCameraView.setFlashOn();
    }

    @Override
    public void onCameraViewStopped() {
        //javaCameraView.setFlashOff();
        mrgba.release();
    }

    public void resetTimer() {
        this.timeStarted = System.currentTimeMillis();
        redChannelPrev = 0;
        frameNumber = 0;
        frameStamp1 = 0;
        frameStamp2 = 0;
        peakCount = 0;
    }

    public long getTimeElapsed() {
        return (System.currentTimeMillis() -  this.timeStarted);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mrgba = inputFrame.rgba();
        MatOfDouble std = new MatOfDouble();
        MatOfDouble mean = new MatOfDouble();
        Core.meanStdDev(mrgba, mean, std);
        if(Core.sumElems(std).val[0] <= 50) {
            redChannel = Core.mean(mrgba).val[0];
            redChannel = lowPassFilter(redChannel, redChannelPrev);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    graphView.getViewport().setMinY(redChannel - 5);
                    graphView.getViewport().setMaxY(redChannel + 5);
                    rawData.appendData(new DataPoint(dataPoints++, redChannel), true, 1000);
                }
            });
            frameNumber++;
            double slope = redChannel - redChannelPrev;
            if(slope * previousSlope < 0) {
                frameStamp2 = frameNumber;
                if(frameStamp2 - frameStamp1 > 8) {
                    peakCount++;
                    frameStamp1 = frameStamp2;
                    Log.i(TAG, "Peak Count" + peakCount);
                }
            }
            truePeak = peakCount / 2;
            previousSlope = slope;
            redChannelPrev = redChannel;
            if(getTimeElapsed() >= 30000) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int stuff =  (truePeak*2) + 15;
                        Toast.makeText(getApplicationContext(), "Your BPM is : " + stuff , Toast.LENGTH_LONG).show();
                        bmpView.setText("" + stuff + " bpm");
                    }
                });
                resetTimer();
            }
        }
        else {
            resetTimer();
        }
        return mrgba;
    }

    public double lowPassFilter(double input, double output) {
        output = output + 0.30 * (input - output);
        return output;
    }
}
