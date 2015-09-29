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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ims.witestify.adapter.RecyclerViewAdapter;
import ims.witestify.dialog.LocationDialogFragment;
import ims.witestify.dialog.RenameVideoDialogFragment;
import ims.witestify.dialog.SortVideosDialogFragment;
import ims.witestify.pojo.DetectedFace;
import ims.witestify.pojo.Video;
import ims.witestify.service.FetchAddressIntentService;
import ims.witestify.service.FileUploadIntentService;
import ims.witestify.util.Constants;
import ims.witestify.util.SessionManager;
import wseemann.media.FFmpegMediaMetadataRetriever;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
                   GoogleApiClient.OnConnectionFailedListener,
                   RenameVideoDialogFragment.RenameVideoListener,
                   SortVideosDialogFragment.SortVideosListener {

    /** Tag used for Logging */
    private static final String TAG = MainActivity.class.getSimpleName();
    /** Represents a tag that is used to save and restore a fetch address request */
    private static final String ADDRESS_REQUESTED_KEY = "ADDRESS_REQUEST_PENDING";
    /** Represents a tag that is used to save and restore a location address */
    private static final String LOCATION_ADDRESS_KEY = "LOCATION_ADDRESS";
    /** Represents a tag that is used to save and restore the state of a LayoutManager */
    private static final String LAYOUT_MANAGER_KEY = "LAYOUT_MANAGER";
    /** Represents a tag that is used to save and restore the last selected video sort function */
    private static final String LAST_VIDEO_LIST_SORT_TYPE = "VIDEO_LIST_SORT_TYPE";

    /** The minimum distance to change Updates in meters */
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    /** The minimum time between updates in milliseconds */
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    /** Provides an overview about the state of network connectivity */
    private ConnectivityManager mConnectivityManager;
    /** Provides access to the system location services */
    private LocationManager mLocationManager;
    /** Provides an entry point to all Google Play services  */
    private GoogleApiClient mGoogleApiClient;
    /** Represents the last found location */
    private Location mLastLocation;
    /** Represents a tracker that determines whether fetching the address has been requested */
    private boolean mAddressRequested;
    /** Represents a location written as String */
    private String mAddressOutput;
    /** Provides a receiver for data sent from {@link FetchAddressIntentService}. */
    private AddressResultReceiver mAddressResultReceiver;
    /** Provides a receiver for data sent from {@link FileUploadIntentService}. */
    private FileUploadResultReceiver mFileUploadResultReceiver;

    /** Provides the navigation drawer that opens a {@link NavigationView} */
    private DrawerLayout mDrawerLayout;
    /** Provides the root layout that contains all the acitivity elements */
    private CoordinatorLayout mRootLayout;
    /** Provides a layout that allows to refresh the page by swiping from the top */
    private SwipeRefreshLayout mSwipeRefreshLayout;
    /** Provides a view to hold video items */
    private RecyclerView mRecyclerView;
    /** Provides an adapter that populates {@link MainActivity#mRecyclerView} with video items */
    private RecyclerViewAdapter mRecyclerViewAdapter;
    /** Provides a toggle that allows to open and close {@link MainActivity#mDrawerLayout} */
    private ActionBarDrawerToggle mDrawerToggle;

    /** Manages a user session */
    private SessionManager session;

    /** Represents the local storage path of the video */
    private Uri mVideoUri = null;
    /** Represents the last sort function used to sort the video list */
    private String mLastSortType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initNavigationView();
        initLocationInstances(savedInstanceState);
        initInstances(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns true
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Show the sort dialog
        if (item.getItemId() == R.id.action_sortBy) {
            if (isInternetAvailable()) {
                DialogFragment dialogFragment = new SortVideosDialogFragment();
                Bundle args = new Bundle();
                args.putString("sortType", mLastSortType);
                dialogFragment.setArguments(args);
                dialogFragment.show(getSupportFragmentManager(), "SortVideosDialogFragment");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
        outState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        outState.putParcelable(LAYOUT_MANAGER_KEY,
                mRecyclerView.getLayoutManager().onSaveInstanceState());
        outState.putString(LAST_VIDEO_LIST_SORT_TYPE, mLastSortType);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     * Gets the best and most recent location currently available.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        updateLocation();
    }

    /**
     * GoogleApiClient object failed to connect. In this case, the
     * android.location framework is used for location detection.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        updateLocation();
    }

    /**
     * Attempts to re-establish the connection to Google Play services
     */
    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /**
     * Handles the result of {@link VideoRecorderActivity} and {@link LocationDialogFragment}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Constants.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                handleCameraVideo(data);
            } else if (resultCode == RESULT_CANCELED) {
                Log.v(TAG, "User cancelled the video capture");
            } else {
                showSnackbar(getString(R.string.video_capture_failed));
            }
        } else if(requestCode == Constants.ENABLE_LOCATION_REQUEST_CODE) {
            isLocationProviderAvailable();
        }
    }

    /**
     * Changes the video title after being called with
     * {@link RenameVideoDialogFragment#mCallback}
     */
    @Override
    public void onRenameVideoDialogSave(DialogFragment dialog, final String oldTitle, final String newTitle) {
        String tag_string_req = "req_update_entry";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_DB_REQUESTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if(!error) {
                        showSnackbar(getString(R.string.video_renamed_success));
                    } else {
                        showSnackbar(getString(R.string.video_renamed_failed));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showSnackbar(getString(R.string.no_internet));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tag", "update_entry");
                params.put("name", session.getCurrentUser());
                params.put("oldTitle", oldTitle);
                params.put("newTitle", newTitle);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Reloads the video list with the chosen sort function after being
     * called with {@link SortVideosDialogFragment#mCallback}
     */
    @Override
    public void onSortTypeSelected(DialogFragment dialog, final String sortType) {
        mLastSortType = sortType;
        loadVideoEntries(sortType);
    }

    /**
     * Creates an intent, adds location data to it as an extra,
     * and starts the intent service for fetching an address.
     */
    private void startFetchAddressIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mAddressResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    /**
     * Creates an intent, adds an array of detected faces from the video recording
     * and starts the intent service for starting file uploading.
     */
    private void startFileUploadIntentService(ArrayList<DetectedFace> detectedFaces) {
        Intent intent = new Intent(this, FileUploadIntentService.class);
        intent.putExtra(Constants.RECEIVER, mFileUploadResultReceiver);
        intent.putExtra("user", session.getCurrentUser());
        intent.putExtra("video_uri", mVideoUri);
        intent.putParcelableArrayListExtra("detected_faces", detectedFaces);
        startService(intent);
    }

    /**
     * Initializes the Action Bar, so that the home button and the title are shown
     */
    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        final ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    /**
     * Initializes the Navigation View and defines its behaviour, when an item is selected
     */
    private void initNavigationView() {
        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        Intent i;

                        switch (menuItem.getItemId()) {
                            case R.id.navItemLibrary:
                                break;
                            case R.id.navItemSettings:
                                i = new Intent(getApplicationContext(), SettingsActivity.class);
                                startActivity(i);
                                finish();
                                break;
                            case R.id.navItemLogout:
                                session.setLogin(false);
                                session.setUser("");
                                i = new Intent(getApplicationContext(), LoginActivity.class);
                                startActivity(i);
                                finish();
                                break;
                            default:
                                break;
                        }

                        menuItem.setChecked(true);
                        mDrawerLayout.closeDrawers();
                        return true;
                    }
                });

        Menu navMenu = mNavigationView.getMenu();
        navMenu.setGroupCheckable(R.id.group_main, true, true);
        navMenu.setGroupCheckable(R.id.group_extra, true, true);

        navMenu.findItem(R.id.navItemLibrary).setChecked(true);
    }

    /**
     * Initializes the instances necessary for location detection
     */
    private void initLocationInstances(Bundle savedInstanceState) {
        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mAddressResultReceiver = new AddressResultReceiver(new Handler());
        mFileUploadResultReceiver = new FileUploadResultReceiver(new Handler());
        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = false;
        mAddressOutput = "";

        updateLocationValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    /**
     * Initializes the layout relevant instances and loads video entries
     */
    private void initInstances(Bundle savedInstanceState) {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(MainActivity.this,
                mDrawerLayout, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mRootLayout = (CoordinatorLayout) findViewById(R.id.root_layout);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.main_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadVideoEntries(mLastSortType);
            }
        });

        FloatingActionButton mfabButton = (FloatingActionButton) findViewById(R.id.fab);
        mfabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLocationProviderAvailable();
            }
        });

        session = new SessionManager(getApplicationContext());

        initRecyclerView();
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LAYOUT_MANAGER_KEY)) {
                Parcelable recyclerLayout = savedInstanceState.getParcelable(LAYOUT_MANAGER_KEY);
                mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerLayout);
            }
        }

        mLastSortType = Constants.SORT_VIDEOS_BY_MOST_RECENT;
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LAYOUT_MANAGER_KEY)) {
                mLastSortType = savedInstanceState.getString(LAST_VIDEO_LIST_SORT_TYPE);
            }
        }
        loadVideoEntries(mLastSortType);
    }

    /**
     * Initializes the Recycler View
     */
    private void initRecyclerView() {
        if(mRecyclerView == null) {
            mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }
    }

    /**
     * Loads the video list ordered by a sort function and populates the list into a Recycler View.
     */
    private void loadVideoEntries(final String sortType) {
        if(!isInternetAvailable()) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        initRecyclerView();

        String tag_string_req = "req_load_entries";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_DB_REQUESTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    List<Video> videos = new ArrayList<>();

                    JSONObject jObj = new JSONObject(response);
                    JSONArray entries = jObj.getJSONArray("entry");
                    for (int i = 0; i < entries.length(); i++) {
                        JSONObject entry = entries.getJSONObject(i);
                        Video vid = new Video();
                        vid.setTitle(entry.getString("title"));
                        vid.setLocation(entry.getString("location"));
                        vid.setTimestamp(entry.getString("timestamp"));
                        vid.setDuration(entry.getLong("duration"));
                        vid.setKeyframe(entry.getString("keyframe"));
                        vid.setUrl(entry.getString("url"));
                        videos.add(vid);
                    }

                    mRecyclerViewAdapter = new RecyclerViewAdapter(MainActivity.this,
                            getSupportFragmentManager(), videos);
                    mRecyclerView.setAdapter(mRecyclerViewAdapter);
                    mRecyclerViewAdapter.setOnItemClickListener(
                            new RecyclerViewAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View v, Video video) {
                                    Intent intent = new Intent(MainActivity.this,
                                            KeyframesActivity.class);
                                    intent.putExtra(KeyframesActivity.VIDEO_EXTRA_PARAM, video);
                                    startActivity(intent);
                                }
                    });
                    mSwipeRefreshLayout.setRefreshing(false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                showSnackbar(getString(R.string.no_internet));
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<>();
                params.put("tag", "load_entries");
                params.put("name", session.getCurrentUser());
                params.put("sortType", sortType);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Gets the location address by starting {@link FetchAddressIntentService}.
     */
    private void updateLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLastLocation = mLocationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(mLastLocation == null) {
                    mLastLocation = mLocationManager
                            .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } else {
                mLastLocation = mLocationManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (mLastLocation != null) {
            if (!Geocoder.isPresent()) {
                showSnackbar(getString(R.string.no_geocoder_available));
                return;
            }

            if (mAddressRequested) {
                startFetchAddressIntentService();
            }
        } else {
            System.out.println("mLastLocation null");
        }
    }

    /**
     * Updates fields based on location data stored in the bundle.
     */
    private void updateLocationValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
            }
        }
    }

    /**
     * Checks and returs wether internet connection is available.
     */
    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        if(!isConnected)
            showSnackbar(getString(R.string.no_internet));
        return isConnected;
    }

    /**
     * Checks if both Network Location Provider and GPS Provider or one of them is available
     */
    private void isLocationProviderAvailable() {
        mAddressRequested = true;

        boolean networkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        NetworkInfo mActiveNetwork  = mConnectivityManager.getActiveNetworkInfo();
        boolean isConnected = mActiveNetwork != null && mActiveNetwork.isConnected();

        if(!isConnected) {
            mAddressRequested = false;
            showSnackbar(getString(R.string.no_internet));
        } else {
            if (networkEnabled || gpsEnabled) {
                if (mGoogleApiClient.isConnected() && mLastLocation != null) {
                    startFetchAddressIntentService();
                }
                startRecording();
            } else {
                DialogFragment mDialogFragment = new LocationDialogFragment();
                mDialogFragment.show(getSupportFragmentManager(), "location");
                mAddressRequested = false;
            }
        }
    }

    /**
     * Gets a location update and starts the {@link VideoRecorderActivity} to capture a video
     */
    private void startRecording() {
        updateLocation();

        final Intent intent = new Intent(this, VideoRecorderActivity.class);
        startActivityForResult(intent, Constants.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    /**
     * Gets the video Uri, the list of detected faces from the intent and prepares for file upload
     */
    private void handleCameraVideo(Intent intent) {
        String videoPath = intent.getStringExtra("video_uri");
        mVideoUri = Uri.parse(videoPath);

        ArrayList<DetectedFace> detectedFaces = intent.getParcelableArrayListExtra("detected_faces");

        startFileUploadIntentService(detectedFaces);
    }

    /**
     * Creates a video object for upload using the video Uri
     */
    private void postUpload(Uri videoUri) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN);
        File localFile = new File(videoUri.getPath());
        String remoteFile = localFile.getName();
        String title = remoteFile.substring(0, remoteFile.lastIndexOf("."));
        Date videoRecordDate = new Date();
        try {
            videoRecordDate = sdf.parse(title);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String timeStamp =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN).format(videoRecordDate);
        FFmpegMediaMetadataRetriever retriever = new FFmpegMediaMetadataRetriever();
        retriever.setDataSource(videoUri.toString());
        String time = retriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        final long duration = Long.parseLong(time) / 1000;
        retriever.release();

        Video video = new Video();
        String currentUser = session.getCurrentUser();
        video.setUser(currentUser);
        video.setTitle(title);
        video.setDuration(duration);
        video.setLocation(mAddressOutput);
        video.setTimestamp(timeStamp);
        video.setKeyframe(Constants.URL_UPLOADS + currentUser + "/" + title + "/");
        video.setUrl(Constants.URL_UPLOADS + currentUser + "/" + title + "/" + title + ".mp4");

        addDBEntry(video);
    }

    /**
     * Creates a new video entry in the server database
     */
    private void addDBEntry(final Video video) {
        String tag_string_req = "req_add_entry";

        StringRequest strReq = new StringRequest(Request.Method.POST, Constants.URL_DB_REQUESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    // Check for error node in json
                    if (!error) {
                        File localVideoFile = new File(mVideoUri.getPath());
                        localVideoFile.delete();
                        mLastSortType = Constants.SORT_VIDEOS_BY_MOST_RECENT;
                        loadVideoEntries(mLastSortType);
                    } else {
                        showSnackbar("Entry insert failed: " + jObj.getString("error_msg"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showSnackbar(getString(R.string.no_internet));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("tag", "add_entry");
                params.put("name", video.getUser());
                params.put("title", video.getTitle());
                params.put("location", video.getLocation());
                params.put("timestamp", video.getTimestamp());
                params.put("duration", String.valueOf(video.getDuration()));
                params.put("keyframe", video.getKeyframe());
                params.put("url", video.getUrl());

                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Receiver for data sent from {@link FetchAddressIntentService}.
     */
    private class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from {@link FetchAddressIntentService}.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Save the address string or an error message sent from the intent service.
            mAddressOutput = null;

            // Show a toast message if an address was found.
            if (resultCode == Constants.FAILURE_RESULT) {
                Log.v(TAG, "Address could not be found");
            } else if(resultCode == Constants.SUCCESS_RESULT) {
                mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            }

            mAddressRequested = false;
        }
    }

    /**
     * Receiver for data sent from {@link FileUploadIntentService}.
     */
    private class FileUploadResultReceiver extends ResultReceiver {
        public FileUploadResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Receives data sent from {@link FileUploadIntentService}.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Show a toast message if an address was found.
            if (resultCode == Constants.FAILURE_RESULT) {
                showSnackbar("Files could not be uploaded properly");
            } else if(resultCode == Constants.SUCCESS_RESULT) {
                Uri videoUri = resultData.getParcelable("video_uri");
                postUpload(videoUri);
            }
        }
    }

    /**
     * Shows a Snackbar with a message and a button.
     */
    private void showSnackbar(String message) {
        Snackbar.make(mRootLayout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }
}
