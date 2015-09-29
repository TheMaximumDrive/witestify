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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ims.witestify.util.Constants;
import ims.witestify.util.SessionManager;

/**
 * An activity that handles user settings
 */
public class SettingsActivity extends AppCompatActivity {

    /** Tag used for Logging */
    private static final String TAG = SettingsActivity.class.getSimpleName();

    /** Manages a user session */
    private SessionManager session;

    /** Provides an overview about the state of network connectivity */
    private ConnectivityManager mConnectivityManager;

    /** Provides the navigation drawer that opens a {@link NavigationView}
     *  and acts as the root layout containing all activity elements */
    private DrawerLayout mDrawerLayout;
    /** Provides a toggle that allows to open and close {@link SettingsActivity#mDrawerLayout} */
    private ActionBarDrawerToggle mDrawerToggle;
    /** Represents an input field for the current password */
    private EditText mInputOldPassword;
    /** Represents an input field for the new password */
    private EditText mInputNewPassword;
    /** Represents an input field for a confirmation of {@link SettingsActivity#mInputNewPassword} */
    private EditText mInputConfirmNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initToolbar();
        initNavigationView();
        initInstances();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
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
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Intent i;

                switch (menuItem.getItemId()) {
                    case R.id.navItemLibrary:
                        i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);
                        finish();
                        break;
                    case R.id.navItemSettings:
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
        navMenu.findItem(R.id.navItemSettings).setChecked(true);
    }

    /**
     * Initializes the layout relevant instances
     */
    private void initInstances() {
        session = new SessionManager(getApplicationContext());

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(SettingsActivity.this,
                mDrawerLayout, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mInputOldPassword = (EditText) findViewById(R.id.settings_inputOldPassword);
        mInputNewPassword = (EditText) findViewById(R.id.settings_inputNewPassword);
        mInputConfirmNewPassword = (EditText) findViewById(R.id.settings_inputConfirmNewPassword);
    }

    /**
     * Is invoked when the "Save changes" button is clicked.
     */
    public void saveChangesButtonHandler(View view) {
        if(isInternetAvailable()) {
            String oldPassword = mInputOldPassword.getText().toString();
            String newPassword = mInputNewPassword.getText().toString();
            String confirmNewPassword = mInputConfirmNewPassword.getText().toString();

            if (!validatePassword(newPassword, confirmNewPassword)) {
                mInputNewPassword.setText("");
                mInputNewPassword.requestFocus();
            } else {
                saveChanges(oldPassword, newPassword);
            }
        }
    }

    /**
     * Attempts to save the new password.
     */
    private void saveChanges(final String oldPassword, final String newPassword) {
        String tag_string_req = "req_settings";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                Constants.URL_DB_REQUESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    showSnackbar(jObj.getString("msg"));
                    mInputOldPassword.setText("");
                    mInputNewPassword.setText("");
                    mInputOldPassword.requestFocus();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Settings Error: " + error.getMessage());
                showSnackbar(getString(R.string.no_internet));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<>();
                params.put("tag", "settings");
                params.put("name", session.getCurrentUser());
                params.put("oldPassword", oldPassword);
                params.put("newPassword", newPassword);

                return params;
            }
        };

        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Checks if the password input has at least 8 characters and
     * if the password confirmation is identical to the password
     */
    private boolean validatePassword(String password, String confirmPassword) {
        boolean valid = false;
        if (password.length() > 7) {
            if (password.equals(confirmPassword)) {
                valid = true;
            } else {
                showSnackbar(getString(R.string.password_mismatch));
            }
        } else {
            showSnackbar(getString(R.string.short_password));
        }
        return valid;
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
     * Shows a Snackbar with a message and a button.
     */
    private void showSnackbar(String message) {
        Snackbar.make(mDrawerLayout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }
}
