/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ims.witestify.exoplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.MediaController;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;
import com.google.android.exoplayer.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Map;

import ims.witestify.R;
import ims.witestify.exoplayer.player.DemoPlayer;
import ims.witestify.exoplayer.player.ExtractorRendererBuilder;

/**
 * An activity that plays media using {@link DemoPlayer}.
 */
public class ExoVideoPlayerActivity extends Activity
        implements SurfaceHolder.Callback, DemoPlayer.Listener,
                   DemoPlayer.Id3MetadataListener, AudioCapabilitiesReceiver.Listener {

    private static final String TAG = "ExoVideoPlayerActivity";

    private static final CookieManager defaultCookieManager;
    static {
        defaultCookieManager = new CookieManager();
        defaultCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private EventLogger eventLogger;
    private MediaController mediaController;
    private AspectRatioFrameLayout videoFrame;
    private SurfaceView surfaceView;

    private DemoPlayer player;
    private boolean playerNeedsPrepare;
    private long playerPosition;

    private Uri contentUri;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    // Activity lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exo_video_player);

        hideStatusActionBars();

        Intent intent = getIntent();
        contentUri = intent.getData();

        View root = findViewById(R.id.root);
        root.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                return true;
            }
        });
        root.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                        mediaController.dispatchKeyEvent(event);
            }
        });
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getApplicationContext(), this);

        videoFrame = (AspectRatioFrameLayout) findViewById(R.id.video_frame);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);

        mediaController = new MediaController(this);
        mediaController.setAnchorView(root);

        CookieHandler currentHandler = CookieHandler.getDefault();
        if (currentHandler != defaultCookieManager) {
            CookieHandler.setDefault(defaultCookieManager);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // The player will be prepared on receiving audio capabilities.
        audioCapabilitiesReceiver.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
        audioCapabilitiesReceiver.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    // Internal methods

    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(this, "ExoPlayerDemo");
        return new ExtractorRendererBuilder(this, userAgent, contentUri);
    }

    private void preparePlayer() {
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder());
            player.addListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            mediaController.setMediaPlayer(player.getPlayerControl());
            mediaController.setEnabled(true);
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            resetVideo();
            //finish();
        } else if (playbackState == ExoPlayer.STATE_BUFFERING) {
            showControls();
        }
        /*String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
          case ExoPlayer.STATE_BUFFERING:
            text += "buffering";
            break;
          case ExoPlayer.STATE_ENDED:
            text += "ended";
            break;
          case ExoPlayer.STATE_IDLE:
            text += "idle";
            break;
          case ExoPlayer.STATE_PREPARING:
            text += "preparing";
            break;
          case ExoPlayer.STATE_READY:
            text += "ready";
            break;
          default:
            text += "unknown";
            break;
        }
        playerStateTextView.setText(text);
        updateButtonVisibilities();*/
    }

    @Override
    public void onError(Exception e) {
        if (e instanceof UnsupportedDrmException) {
        // Special case DRM failures.
        UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            String stringId = Util.SDK_INT < 18 ? "Protected content not supported on API levels below 18"
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? "This device does not support the required DRM scheme" : "An unknown DRM error occurred";
            Snackbar.make(findViewById(R.id.root), stringId, Snackbar.LENGTH_LONG).show();
            Log.e(TAG, stringId);
        }
        playerNeedsPrepare = true;
        showControls();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        videoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    // User controls

    private void toggleControlsVisibility()  {
        if (mediaController.isShowing()) {
            mediaController.hide();
            hideStatusActionBars();
        } else {
            showControls();
        }
    }

    private void showControls() {
        mediaController.show(0);
    }

    private void resetVideo() {
        mediaController.dispatchKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP));
        player.seekTo(0);
        showControls();
    }

    private void hideStatusActionBars() {
        // Hide the status bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Hide the action bar.
        if(getActionBar() != null) {
            getActionBar().hide();
        }
    }

    // DemoPlayer.MetadataListener implementation

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }

    // SurfaceHolder.Callback implementation

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (player != null) {
            player.setSurface(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (player != null) {
            player.blockingClearSurface();
        }
    }
}
