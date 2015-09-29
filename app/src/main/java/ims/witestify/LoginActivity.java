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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request.Method;
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
 * An activity that handles user sign in
 */
public class LoginActivity extends AppCompatActivity {

    /** Tag used for Logging */
    private static final String TAG = LoginActivity.class.getSimpleName();

    /**
     * Tracks whether the user has requested a sign in.
     */
    private boolean mSignInRequested;

    /** Provides the root layout that contains all the acitivity elements */
    private LinearLayout mSignInLinearLayout;
    /** Represents a progress bar that is shown during the sign in process */
    private ProgressBar mProgressSignIn;
    /** Represents a text that is shown alongside {@link LoginActivity#mProgressSignIn}
     * during the sign in process
     */
    private TextView mSignInInfo;
    /** Represents an input field for the username */
    private EditText mInputName;
    /** Represents an input field for the password */
    private EditText mInputPassword;
    /** Provides a button that tries to sign in a user with the provided input */
    private Button mSignInButton;
    /** Provides a button that links to {@link SignUpActivity} */
    private Button mLinkToSignUpButton;

    /** Manages a user session */
    private SessionManager session;

    /** Provides an overview about the state of network connectivity */
    private ConnectivityManager mConnectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mSignInLinearLayout = (LinearLayout) findViewById(R.id.loginLinearLayout);

        mInputName = (EditText) findViewById(R.id.signin_inputUsername);
        mInputPassword = (EditText) findViewById(R.id.signin_inputPassword);

        mProgressSignIn = (ProgressBar) findViewById(R.id.progress_signIn);
        mSignInInfo = (TextView) findViewById(R.id.txtSignInInfo);
        mSignInButton = (Button) findViewById(R.id.btn_signIn);
        mLinkToSignUpButton = (Button) findViewById(R.id.btn_linkToSignUp);

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        session = new SessionManager(getApplicationContext());

        // Checks if user has already signed in the last session
        if (session.isLoggedIn()) {
            if(isInternetAvailable()) {
                // Direct already signed in user to the main acitvity
                launchMainActivity();
            } else {
                // Remain in this activity, as there is no internet connectivity
                session.setLogin(false);
                session.setUser("");
            }
        }

        mSignInRequested = false;
        updateSignInProgressUI();

        // Displays Snackbar, if an user has returned to the sign in activity after signing up
        Intent intent = getIntent();
        if(intent.getBooleanExtra("user_signed_up", false)) {
            showSnackbar(getString(R.string.user_created));
        }
    }

    /**
     * Updates the visibility of the input fields, buttons and the progress bar.
     * Hides the input fields and buttons, and displays the progress bar during a sign in attempt.
     * Hides the progress bar, and displays the input fields and buttons otherwise.
     */
    private void updateSignInProgressUI() {
        if (mSignInRequested) {
            mProgressSignIn.setVisibility(ProgressBar.VISIBLE);
            mSignInInfo.setVisibility(TextView.VISIBLE);
            mInputName.setVisibility(EditText.GONE);
            mInputPassword.setVisibility(EditText.GONE);
            mSignInButton.setVisibility(Button.GONE);
            mLinkToSignUpButton.setVisibility(Button.GONE);
        } else {
            mProgressSignIn.setVisibility(ProgressBar.GONE);
            mSignInInfo.setVisibility(TextView.GONE);
            mInputName.setVisibility(EditText.VISIBLE);
            mInputPassword.setVisibility(EditText.VISIBLE);
            mSignInButton.setVisibility(Button.VISIBLE);
            mLinkToSignUpButton.setVisibility(Button.VISIBLE);
        }
    }

    /**
     * Is invoked when the "Sign in" button is clicked, validates the inputs
     * and starts an attempt to sign in
     */
    public void signInButtonHandler(View view) {
        hideKeyboard(view);

        String username = mInputName.getText().toString();
        String password = mInputPassword.getText().toString();

        if(validateUsername(username) && validatePassword(password)) {
            mSignInRequested = true;
            updateSignInProgressUI();
            tryToLogin(username, password);
        } else {
            mSignInRequested = false;
            updateSignInProgressUI();
            showSnackbar(getString(R.string.invalid_creds));
        }
    }

    /**
     * Attempts to sign in the user using a {@link StringRequest}.
     * Directs to {@link MainActivity}, if the sign in was successful.
     */
    private void tryToLogin(final String name, final String password) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

        StringRequest strReq = new StringRequest(Method.POST, Constants.URL_DB_REQUESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Login Response: " + response);
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // user successfully logged in
                        // Create login session
                        session.setLogin(true);
                        session.setUser(name);

                        mSignInRequested = false;
                        updateSignInProgressUI();

                        // Launch main activity
                        launchMainActivity();
                    } else {
                        mSignInRequested = false;
                        updateSignInProgressUI();
                        // Error in login. Get the error message
                        mInputPassword.setText("");
                        mInputPassword.requestFocus();
                        showSnackbar(jObj.getString("error_msg"));
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                showSnackbar(getString(R.string.no_internet));
                mSignInRequested = false;
                updateSignInProgressUI();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                params.put("tag", "login");
                params.put("name", name);
                params.put("password", password);

                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Is invoked when the button to go to the sign up page is clicked.
     * Directs to {@link SignUpActivity}.
     */
    public void signUpButtonHandler(View view) {
        Intent i = new Intent(view.getContext(), SignUpActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * Hides the soft keyboard
     */
    private void hideKeyboard(View view) {
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * Checks if the user name input is at least one character long
     */
    private boolean validateUsername(String username) {
        return username.trim().length() > 0;
    }

    /**
     * Checks if the password input is at least one character long
     */
    private boolean validatePassword(String password) {
        return password.trim().length() > 0;
    }

    /**
     * Checks and returs wether internet connection is available.
     */
    private boolean isInternetAvailable() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Shows a Snackbar with a message and a button.
     */
    private void showSnackbar(String message) {
        Snackbar.make(mSignInLinearLayout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }

    /**
     * Launches the main activity
     */
    private void launchMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
