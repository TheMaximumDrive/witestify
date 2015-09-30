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

package ims.witestify.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import ims.witestify.AppController;
import ims.witestify.MainActivity;
import ims.witestify.R;
import ims.witestify.pojo.DetectedFace;
import ims.witestify.util.Constants;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * An {@link IntentService} subclass for handling asynchronous file
 * upload requests in a service on a separate handler thread.
 */
public class FileUploadIntentService extends IntentService {

    /** Tag used for Logging */
    private static final String TAG = FileUploadIntentService.class.getSimpleName();

    /** Provides the receiver where results are forwarded to from this service. */
    private ResultReceiver mReceiver;

    /** Provides a notification manager */
    private NotificationManager mNotifyManager;

    /** Provides a notification builder */
    private NotificationCompat.Builder mBuilder;

    /** Represents the currently signed in user */
    private String user;

    /** Represents the local storage path of the video */
    private Uri mVideoUri;

    /** Represents a list that contains the faces detected during video capture */
    private ArrayList<DetectedFace> mDetectedFaces;

    /** Represents the total size of all files to upload */
    private long totalFilesSize;

    /** Represents the currently transferred bytes */
    private long transferredBytes;

    /** Represents the number of bytes to transfer one at a time */
    private static final byte[] bytesIn = new byte[4096];

    public FileUploadIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }

        initUploadNotification();
        user = intent.getStringExtra("user");
        mVideoUri = intent.getParcelableExtra("video_uri");
        mDetectedFaces = intent.getParcelableArrayListExtra("detected_faces");

        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (user == null || mVideoUri == null) {
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, "no user and/or video uri passed");
        } else {
            getFtpLogin();
        }
    }

    /**
     * Sends a resultCode and message to the receiver.
     */
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("video_uri", mVideoUri);
        bundle.putString("msg", message);
        mReceiver.send(resultCode, bundle);
    }

    /**
     * Initializes the notification that is displayed during the upload task
     */
    private void initUploadNotification() {
        Intent intent = new Intent (this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        mBuilder.setContentTitle("Witestify Video Upload")
                .setContentText("Upload in progress")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(bm)
                .setColor(getResources().getColor(R.color.app_colorPrimary))
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setPriority(Notification.PRIORITY_HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mBuilder.setOnlyAlertOnce(true)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setVibrate(new long[] {1000 });
        }
    }

    /**
     * Requests the login data of the Ftp host and starts a
     * {@link ims.witestify.service.FileUploadIntentService.FilesUploader}
     */
    private void getFtpLogin() {
        String tag_string_req = "req_ftp_config";

        StringRequest strReq = new StringRequest(Request.Method.POST, Constants.URL_DB_REQUESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    JSONObject ftpObj = jObj.getJSONObject("ftp");
                    new FilesUploader().execute(ftpObj.getString("url"),
                            ftpObj.getString("user"), ftpObj.getString("password"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v(TAG, getString(R.string.no_internet));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tag", "ftp_config");

                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * A {@link AsyncTask} subclass that uploads files
     */
    private class FilesUploader extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            if(uploadFiles(params[0], params[1], params[2])) {
                deliverResultToReceiver(Constants.SUCCESS_RESULT, "good");
            } else {
                deliverResultToReceiver(Constants.FAILURE_RESULT, "no user and/or video uri passed");
            }
            return null;
        }
    }

    /**
     * Uploads the video and the keyframes
     */
    private Boolean uploadFiles(String ftpUrl, String username, String password) {
        mBuilder.setContentText("Video upload in progress");
        mBuilder.setProgress(100, 1, false);
        mNotifyManager.notify(TAG, 1, mBuilder.build());

        System.out.println("begin FTP");

        boolean completed = false;
        FTPClient mFtpClient = new FTPClient();

        Collections.sort(mDetectedFaces);

        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(mVideoUri.toString());
        int duration = Integer.parseInt(retriever.extractMetadata(
                FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        Bitmap tmpBitmap;

        if(mDetectedFaces.size() < 3) {

            ArrayList<Long> muSeconds = new ArrayList<>();

            for(int i = 0; i < mDetectedFaces.size(); i++) {
                muSeconds.add(mDetectedFaces.get(i).getMuSeconds());
                tmpBitmap = retriever.getFrameAtTime(mDetectedFaces.get(i).getMuSeconds(),
                        FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(tmpBitmap == null){
                    long lowerBound = mDetectedFaces.get(i).getMuSeconds() - 1000000;
                    lowerBound = (lowerBound < 1) ? 1 : lowerBound;
                    long upperBound = mDetectedFaces.get(i).getMuSeconds() - 1000000;
                    upperBound = (upperBound > duration) ? duration : upperBound;
                    for(int j = (int)lowerBound; j <= upperBound; j++) {
                        tmpBitmap = retriever.getFrameAtTime(mDetectedFaces.get(i).getMuSeconds(),
                                FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if(tmpBitmap != null)
                            break;
                    }
                } else {
                    bitmaps.add(tmpBitmap);
                }
            }

            while(bitmaps.size() <= 3) {
                Random rnd = new Random();
                int rndTime = rnd.nextInt(duration + 1);

                tmpBitmap = retriever.getFrameAtTime(rndTime,
                        FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(tmpBitmap == null){
                    continue;
                } else {
                    System.out.println(rndTime + " muSeconds");
                    bitmaps.add(tmpBitmap);
                }
            }
        } else {
            bitmaps.add(retriever.getFrameAtTime(mDetectedFaces.get(0).getMuSeconds(),
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC));
            bitmaps.add(retriever.getFrameAtTime(mDetectedFaces.get(1).getMuSeconds(),
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC));
            bitmaps.add(retriever.getFrameAtTime(mDetectedFaces.get(2).getMuSeconds(),
                    FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC));
        }

        retriever.release();

        File localImageFile_01 = getOutputMediaFile("01.jpg");
        File localImageFile_02 = getOutputMediaFile("02.jpg");
        File localImageFile_03 = getOutputMediaFile("03.jpg");

        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(localImageFile_01));
            tmpBitmap = bitmaps.get(0);
            tmpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.close();
            OutputStream os2 = new BufferedOutputStream(new FileOutputStream(localImageFile_02));
            tmpBitmap = bitmaps.get(1);
            tmpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os2);
            os2.close();
            OutputStream os3 = new BufferedOutputStream(new FileOutputStream(localImageFile_03));
            tmpBitmap = bitmaps.get(2);
            tmpBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os3);
            os3.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException" + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "IOException" +  e.toString());
        }

        try {
            // Connect ftp client
            mFtpClient.addProtocolCommandListener(new PrintCommandListener(new
                    PrintWriter(System.out)));
            mFtpClient.setConnectTimeout(300);
            mFtpClient.connect(InetAddress.getByName(ftpUrl));
            mFtpClient.login(username, password);
            if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                mFtpClient.setFileType(FTP.BINARY_FILE_TYPE);
                mFtpClient.enterLocalPassiveMode();

                System.out.println("mVideoUri: " + mVideoUri.toString());
                File localVideoFile = new File(mVideoUri.toString());
                String remoteVideoFile = localVideoFile.getName();
                String videoTitle = remoteVideoFile.substring(0, remoteVideoFile.lastIndexOf("."));

                mFtpClient.makeDirectory("/html/witestify/uploads/" + user + "/" + videoTitle);
                mFtpClient.changeWorkingDirectory("/html/witestify/uploads/" + user + "/" + videoTitle);

                String remoteImageFile_01 = "01.jpg";
                String remoteImageFile_02 = "02.jpg";
                String remoteImageFile_03 = "03.jpg";

                totalFilesSize = localVideoFile.length()+ localImageFile_01.length() +
                        localImageFile_02.length() + localImageFile_03.length();
                transferredBytes = 0;

                // Upload video
                uploadFile(mFtpClient, localVideoFile, remoteVideoFile);

                // Upload keyframes
                uploadFile(mFtpClient, localImageFile_01, remoteImageFile_01);
                uploadFile(mFtpClient, localImageFile_02, remoteImageFile_02);
                completed = uploadFile(mFtpClient, localImageFile_03, remoteImageFile_03);
            }

            mFtpClient.logout();

            // Finalize notification
            if(completed) {
                mBuilder.setContentText("Upload complete")
                        .setProgress(0, 0, false);
            } else {
                mBuilder.setContentText("Upload failed")
                        .setProgress(0, 0, false);
            }
            mNotifyManager.notify(TAG, 1, mBuilder.build());
            mNotifyManager.cancelAll();

            // Disconnect ftp client
            if (mFtpClient.isConnected()) {
                try {
                    mFtpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "FTP SocketException" + e.toString());
            return false;
        } catch (UnknownHostException e) {
            Log.e(TAG, "FTP UnknownHostException" + e.toString());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "FTP IOException" + e.toString());
            return false;
        } finally {
            if(localImageFile_01 != null) {
                localImageFile_01.delete();
            }
            if(localImageFile_02 != null) {
                localImageFile_02.delete();
            }
            if(localImageFile_03 != null) {
                localImageFile_03.delete();
            }
        }

        return completed;
    }

    /**
     * Uploads a single file using {@link} FTPClient}
     * and updates the notification progress
     */
    private Boolean uploadFile(FTPClient mFtpClient,
                               File localFile,
                               String remoteFile){
        int read;

        try {
            InputStream inputStream = new FileInputStream(localFile);
            OutputStream outputStream = mFtpClient.storeFileStream(remoteFile);

            while ((read = inputStream.read(bytesIn)) != -1) {
                outputStream.write(bytesIn, 0, read);
                transferredBytes += 4096;
                transferredBytes = (transferredBytes > totalFilesSize) ? totalFilesSize : transferredBytes;
                mBuilder.setProgress(100, (int) ((transferredBytes / (float) totalFilesSize) * 100), false);
                mNotifyManager.notify(TAG, 1, mBuilder.build());
            }
            inputStream.close();
            outputStream.close();

            return mFtpClient.completePendingCommand();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Creates a File for saving a video
     */
    private File getOutputMediaFile(String filename){
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

            return new File(mediaStorageDir.getPath() + File.separator + filename);
        }
        return null;
    }
}
