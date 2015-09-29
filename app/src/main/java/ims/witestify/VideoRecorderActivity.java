/**
 * Copyright 2015 Wen Chao Chen
 *
 * This file is part of Witestify.
 * Witestify is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Witestify is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Witestify.  If not, see <http://www.gnu.org/licenses/>.
 */

package ims.witestify;

import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import ims.witestify.pojo.DetectedFace;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * An activity that captures a video
 */
@SuppressWarnings("deprecation")
@Deprecated
public class VideoRecorderActivity extends AppCompatActivity {

    /** Tag used for Logging */
    private static final String TAG = VideoRecorderActivity.class.getSimpleName();

    /** Represents aa hardware camera */
    private Camera mCamera;
    /** Represents the surface holder that displays the camera preview */
    private SurfaceHolder mCameraSurfaceHolder;
    /** Represents a recorder that can capture video and audio */
    private MediaRecorder mMediaRecorder;

    /** Represents the video output file */
    private File videoFile;
    /** Provides a variable to track if recording is happening at the moment */
    private boolean isRecording = false;
    /** Represents the time recording started */
    private long recordingStartTime;
    /** Provides a counter to delay face detection */
    private int detectedFaceDelayCounter;
    /** Represents the number of faces detected last */
    private long lastDetectedFaceNumber;
    /** Represents a list containing number of faces detected at different times */
    private ArrayList<Integer> numFaces;
    /** Represents a list containing times where faces where detected */
    private ArrayList<Long> numSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_recorder);

        // install surface callback
        SurfaceView mCameraSurfaceView = (SurfaceView) findViewById(R.id.cameraSurface);
        mCameraSurfaceHolder = mCameraSurfaceView.getHolder();
        mCameraSurfaceHolder.addCallback(cameraSurfaceCallbacks);

        numFaces = new ArrayList<>();
        numSeconds = new ArrayList<>();
    }

    /**
     * Provides a callback object that handles the hardware camera and starts face detection
     */
    private SurfaceHolder.Callback cameraSurfaceCallbacks = new SurfaceHolder.Callback() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if(mCamera == null)return;
            mCamera.stopPreview();
            mCamera.stopFaceDetection();
            mCamera.release();
            mCamera = null;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            try {
                //Try to open front camera if exist...
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                int cameraId = 0;
                int camerasCount = Camera.getNumberOfCameras();
                for ( int camIndex = 0; camIndex < camerasCount; camIndex++ ) {
                    Camera.getCameraInfo(camIndex, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK  ) {
                        cameraId = camIndex;
                        break;
                    }
                }
                mCamera = Camera.open(cameraId);
                mCamera.setPreviewDisplay(holder);
            } catch (Exception exception) {
                Log.e(TAG, "Surface Created Exception", exception);
                if(mCamera == null)return;
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCamera.setFaceDetectionListener(faceDetectionListener);
            mCamera.startPreview();
            mCamera.startFaceDetection();
        }
    };

    /**
     * Provides a listener that responds when at least one face is detected during video capture
     */
    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {
        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            // Gets time of detection in microseconds.
            long duration = ((System.currentTimeMillis() - recordingStartTime) * 1000);

            if(faces.length > 0) {
                if(lastDetectedFaceNumber != faces.length) {
                    lastDetectedFaceNumber = faces.length;
                    detectedFaceDelayCounter = 0;
                    numFaces.add(faces.length);
                    numSeconds.add(duration);
                }
            } else {
                // No faces detected
                lastDetectedFaceNumber = faces.length;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    /**
     * Prepares the camera for video capture
     */
    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();

        // Unlocks and sets camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Sets sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Sets a CamcorderProfile)
        if(CamcorderProfile.hasProfile(0, CamcorderProfile.QUALITY_720P)) {
            CamcorderProfile camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            camProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
            camProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
            mMediaRecorder.setProfile(camProfile);
        } else if(CamcorderProfile.hasProfile(0, CamcorderProfile.QUALITY_480P)) {
            CamcorderProfile camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
            camProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
            camProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
            mMediaRecorder.setProfile(camProfile);
        } else {
            CamcorderProfile camProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
            camProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
            camProfile.videoCodec = MediaRecorder.VideoEncoder.H264;
            mMediaRecorder.setProfile(camProfile);
        }

        // Sets output file
        videoFile = getOutputMediaFile();
        if(videoFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(videoFile.toString());

        // Sets the preview output
        mMediaRecorder.setPreviewDisplay(mCameraSurfaceHolder.getSurface());

        // Prepares configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.v(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.v(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }

        return true;
    }

    /**
     * Is invoked when the recording button is clicked.
     * Starts or stops video capture.
     */
    public void recordVideoButtonHandler(View view) {
        if (isRecording) {
            // Stops recording and release camera
            mMediaRecorder.stop();
            releaseMediaRecorder();
            mCamera.lock();
            releaseCamera();
            isRecording = false;

            finishActivity(packVideoFrames());
        } else {
            // Initializes video camera
            if (prepareVideoRecorder()) {
                // Starts recording
                mMediaRecorder.start();

                recordingStartTime = System.currentTimeMillis();
                detectedFaceDelayCounter = 0;
                lastDetectedFaceNumber = 0;

                setRecordButton();
                isRecording = true;
            } else {
                // Preparing didn't work, release the camera
                Log.v(TAG, "Failed to prepare the MediaRecorder!");
                releaseMediaRecorder();
            }
        }
    }

    /**
     * Returns the data of video frames with detected faces as an {@link ArrayList}
     */
    private ArrayList<DetectedFace> packVideoFrames() {
        ArrayList<Integer> indices = new ArrayList<>();
        int lastConsideredFrame = 0;

        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(videoFile.getPath());
        String time = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = Long.parseLong(time);
        retriever.release();

        for (int i = 1; i < numSeconds.size(); i++) {
            long gap = numSeconds.get(lastConsideredFrame) + (duration / 10);
            if(gap >= numSeconds.get(i)) {
                if(numFaces.get(lastConsideredFrame) < numFaces.get(i)) {
                    lastConsideredFrame = i;
                }
            } else {
                indices.add(i);
                lastConsideredFrame = i;
            }
        }

        ArrayList<DetectedFace> detectedFaces = new ArrayList<>();
        for(int index : indices) {
            detectedFaces.add(new DetectedFace(numSeconds.get(index), numFaces.get(index)));
        }

        return detectedFaces;
    }

    /**
     * Finishes the activity and returns the result to the parent activity.
     */
    private void finishActivity(ArrayList<DetectedFace> detectedFaces) {
        Intent output = getIntent();
        output.putExtra("video_uri", videoFile.getPath());
        output.putParcelableArrayListExtra("detected_faces", detectedFaces);
        setResult(RESULT_OK, output);
        finish();
    }

    /**
     * Sets the icon of the recording button
     */
    private void setRecordButton() {
        ImageButton recordVideoButton = (ImageButton) findViewById(R.id.btn_recordVideo);
        recordVideoButton.setImageResource(R.drawable.ic_stop);
    }

    /**
     * Releases the media recorder
     */
    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    /**
     * Releases the camera
     */
    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * Creates a File for saving a video
     */
    private File getOutputMediaFile(){
        /* Checks if external storage is available for read and write */
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Get the directory for the user's public pictures directory.
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "Witestify");
            if (! mediaStorageDir.exists()){
                if (! mediaStorageDir.mkdirs()){
                    Log.e(TAG, "Directory could not created");
                    return null;
                }
            }

            // Creates a media file name
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN).format(new Date());

            return new File(mediaStorageDir.getPath() + File.separator + timeStamp + ".mp4");
        }
        return null;
    }

}
