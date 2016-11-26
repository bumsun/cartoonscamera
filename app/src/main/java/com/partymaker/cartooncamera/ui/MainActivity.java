package com.partymaker.cartooncamera.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.partymaker.cartooncamera.R;
import com.partymaker.cartooncamera.lib.FilterManager;
import com.partymaker.cartooncamera.lib.FilterType;
import com.partymaker.cartooncamera.lib.Native;
import com.partymaker.cartooncamera.ui.fragments.CameraViewerFragment;
import com.partymaker.cartooncamera.ui.fragments.FilterConfigFragment;
import com.partymaker.cartooncamera.ui.fragments.FilterSelectorFragment;
import com.partymaker.cartooncamera.ui.fragments.PictureViewerFragment;
import com.partymaker.cartooncamera.ui.interfaces.FilterConfigListener;
import com.partymaker.cartooncamera.ui.interfaces.FilterPictureCallback;
import com.partymaker.cartooncamera.ui.interfaces.FilterSelectorListener;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * Main Activity
 */
public class MainActivity extends Activity implements FilterSelectorListener, FilterConfigListener, View.OnClickListener {

    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String TAG = "MainActivity";

    private static final int ORIENTATION_THRESH = 10;

    private CameraViewerFragment mCameraViewerFragment;
    private PictureViewerFragment mPictureViewerFragment;
    private FilterSelectorFragment mFilterSelectorFragment;
    private FilterConfigFragment mFilterConfigFragment;

    private ImageButton mCapturePictureBtn;

    private Handler mHandler;
    private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
    private OrientationEventListener mOrientationListener;

    private FilterManager mFilterManager;

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("image_filters");
    }

    /**
     * Picture capture callback implementation
     */
    private FilterPictureCallback mPictureCallback = new FilterPictureCallback() {
        @Override
        public void onPictureCaptured(Bitmap pictureBitmap) {
            saveBitmap(pictureBitmap);
            resultIV.setImageBitmap(pictureBitmap);
            resultIV.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(),"Click image to close",Toast.LENGTH_LONG).show();
        }
    };
    private ImageView resultIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        checkAndRequestPermissions();

        setContentView(R.layout.activity_main);

        mFilterManager = FilterManager.getInstance();
        mCameraViewerFragment = new CameraViewerFragment();
        mPictureViewerFragment = new PictureViewerFragment();
        mFilterSelectorFragment = new FilterSelectorFragment();

        mCapturePictureBtn = (ImageButton) findViewById(R.id.capturePictureBtn);
        resultIV = (ImageView) findViewById(R.id.resultIV);
        resultIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultIV.setVisibility(View.GONE);
            }
        });

        mHandler = new Handler();


        mCapturePictureBtn.setOnClickListener(this);
        findViewById(R.id.filterViewer).setOnClickListener(this);

        // Load sketch texture
        loadSketchTexture(getApplicationContext().getResources(),
                R.drawable.sketch_texture);

        // Set camera viewer as default
        getFragmentManager()
                .beginTransaction()
                .add(R.id.filterViewer, mCameraViewerFragment)
                .commit();

        /**
         * Device orientation listener implementation to appropriately rotate button and filter icons on orientation change
         */
        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientation) {
                if(isOrientationLandscape(orientation) && mOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    mOrientation = Configuration.ORIENTATION_LANDSCAPE;
                    mCapturePictureBtn.setRotation(90);

                    if(mFilterSelectorFragment.isVisible())
                        mFilterSelectorFragment.changeOrientation(mOrientation);
                }
                else if(isOrientationPortrait(orientation) && mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mOrientation = Configuration.ORIENTATION_PORTRAIT;
                    mCapturePictureBtn.setRotation(0);
                    if(mFilterSelectorFragment.isVisible())
                        mFilterSelectorFragment.changeOrientation(mOrientation);
                }
            }
        };
    }


    @Override
    public void onResume() {
        super.onResume();
        mOrientationListener.enable();
    }


    @Override
    public void onPause() {
        super.onPause();
        mOrientationListener.disable();
    }


    @Override
    public void onClick(View v) {
        // Detect clicked view, and execute actions accordingly
        switch(v.getId()) {
            case R.id.capturePictureBtn:
                if(mFilterManager.getCurrentFilter()!=null) {
                    if (mCameraViewerFragment.isVisible())
                        mCameraViewerFragment.capturePicture(mPictureCallback);
                    else
                        mPictureViewerFragment.capturePicture(mPictureCallback);
                    break;
                }
            case R.id.filterViewer:
                closeCurrentFilterConfig();
                closeFilterSelector();
        }
    }

    public void checkAndRequestPermissions() {
        if(Build.VERSION.SDK_INT > 22) {
            String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };
            if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions,REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    return;
                } else {
                    finish();
                }
            }
        }
    }


    public int getOrientation() {
        return mOrientation;
    }


    private void openPictureFilterViewer(String pictureFilePath) {
        mFilterManager.reset();
        if(!mPictureViewerFragment.isVisible()) {
            Bundle args = new Bundle();
            args.putString("pictureFilePath", pictureFilePath);
            mPictureViewerFragment.setArguments(args);
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.filterViewer, mPictureViewerFragment)
                    .commit();
            mCapturePictureBtn.setImageResource(R.drawable.icon_btn_save);
        } else {
            mPictureViewerFragment.loadPicture(pictureFilePath);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };

                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                // Move to first row
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                // Get selected picture filepath
                String pictureFilePath = cursor.getString(columnIndex);
                cursor.close();
                Log.d(TAG, "Picture picked- " + pictureFilePath);

                // Switch to picture view mode, loading the selected picture
                openPictureFilterViewer(pictureFilePath);
            }
        }
    }


    private void closeFilterSelector() {
        if(mFilterSelectorFragment.isVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(mFilterSelectorFragment)
                    .commit();
            Log.d(TAG, "filter selector closed");
        }
    }


    @Override
    public void onFilterSelect(FilterType filterType) {
        if(mFilterManager.getCurrentFilter()==null || filterType != mFilterManager.getCurrentFilter().getType()) {
            mFilterManager.setCurrentFilter(filterType);
            Log.d(TAG, "current filter set to " + filterType.toString());
            if (mPictureViewerFragment.isVisible()) {
                mPictureViewerFragment.updatePicture();
            }
            // Display selected filter name as Toast
        }
    }

    private boolean isFilterConfigVisible() {
        if(mFilterConfigFragment!=null && mFilterConfigFragment.isVisible())
            return true;
        else
            return false;
    }

    private void openCurrentFilterConfig() {
        if (mFilterManager.getCurrentFilter()!=null && !isFilterConfigVisible()) {

            mFilterConfigFragment = new FilterConfigFragment();
            mFilterConfigFragment.setFilter(mFilterManager.getCurrentFilter());


            Log.d(TAG, "filter config opened");
        }
    }


    private void closeCurrentFilterConfig() {
        if (isFilterConfigVisible()) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(mFilterConfigFragment)
                    .commit();
            Log.d(TAG,"filter config closed");
        }
    }


    @Override
    public void onFilterConfigChanged() {
        if(mPictureViewerFragment.isVisible())
            mPictureViewerFragment.updatePicture();
    }


    private void saveBitmap(Bitmap bitmap) {
        try {
            String savedPicturePath = Utils.saveBitmap(this, bitmap);
            Log.d(TAG, "Saved picture at "+savedPicturePath);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error: Unable to save picture", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isOrientationLandscape(int orientation) {
        return orientation >= (270 - ORIENTATION_THRESH) && orientation <= (270 + ORIENTATION_THRESH);
    }


    private boolean isOrientationPortrait(int orientation) {
        return (orientation >= (360 - ORIENTATION_THRESH) && orientation <= 360) || (orientation >= 0 && orientation <= ORIENTATION_THRESH);
    }


    private void loadSketchTexture(Resources res, int sketchTexRes) {
        Mat mat, tempMat;
        Bitmap bmp = BitmapFactory.decodeResource(res, sketchTexRes);
        tempMat = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        org.opencv.android.Utils.bitmapToMat(bmp, tempMat);
        mat = new Mat(tempMat.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(tempMat, mat, Imgproc.COLOR_RGBA2GRAY);
        Native.setSketchTexture(mat.getNativeObjAddr());
    }
}

