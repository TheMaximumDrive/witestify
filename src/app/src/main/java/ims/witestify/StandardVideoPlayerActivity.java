package ims.witestify;

import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class StandardVideoPlayerActivity extends AppCompatActivity {

    private Uri contentUri;
    private VideoView mVideoView;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standard_video_player);

        contentUri = getIntent().getData();
        mVideoView = (VideoView) findViewById(R.id.standard_video_player_videoView);

        new VideoPlaybackAsyncTask().execute(contentUri);
    }

    private class VideoPlaybackAsyncTask extends AsyncTask<Uri, Uri, Void> {
        ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = new ProgressDialog(StandardVideoPlayerActivity.this);
            dialog.setMessage("Loading, Please Wait...");
            dialog.setCancelable(true);
            dialog.show();
        }

        protected void onProgressUpdate(final Uri... uri) {

            try {

                mediaController = new MediaController(StandardVideoPlayerActivity.this);
                mVideoView.setMediaController(mediaController);
                mediaController.setPrevNextListeners(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // next button clicked
                    }
                }, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                mediaController.show(10000);

                mVideoView.setVideoURI(uri[0]);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer arg0) {
                        mVideoView.start();
                        dialog.dismiss();
                    }
                });


            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Uri... params) {
            try {
                publishProgress(params[0]);
            } catch (Exception e) {
                e.printStackTrace();

            }

            return null;
        }
    }
}
