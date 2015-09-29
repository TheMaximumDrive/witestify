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
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ims.witestify.util.Constants;
import ims.witestify.util.SQLiteHandler;

/**
 * An activity that handles user sign up
 */
public class SignUpActivity extends AppCompatActivity {

    /** Represents a pattern that e-mail inputs should have */
    private static final String EMAIL_PATTERN = "^[a-zA-Z0-9#_~!$&'()*+,;=:.\"(),:;<>@\\[\\]\\\\]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*$";
    private Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    /** Tag used for Logging */
    private static final String TAG = SignUpActivity.class.getSimpleName();

    /** Provides an overview about the state of network connectivity */
    private ConnectivityManager mConnectivityManager;

    /** Provides a local database handler */
    private SQLiteHandler db;

    /** Tracks whether the user has requested a sign up. */
    private boolean mSignUpRequested;

    /** Provides the root layout that contains all the acitivity elements */
    private LinearLayout mSignUpLinearLayout;
    /** Represents an input field for the username */
    private EditText mInputName;
    /** Represents an input field for the e-mail address */
    private EditText mInputEmail;
    /** Represents an input field for the password */
    private EditText mInputPassword;
    /** Represents an input field for a confirmation of {@link SignUpActivity#mInputPassword} */
    private EditText mInputConfirmPassword;
    /** Represents a progress bar that is shown during the sign up process */
    private ProgressBar mProgressSignUp;
    /** Represents a text that is shown alongside {@link SignUpActivity#mProgressSignUp}
     * during the sign in process
     */
    private TextView mSignUpInfo;
    /** Provides a button that tries to sign up a user with the provided input */
    private Button mSignUpButton;
    /** Provides a button that links to {@link LoginActivity} */
    private Button mLinkToSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mConnectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        mSignUpLinearLayout = (LinearLayout) findViewById(R.id.signUpLinearLayout);

        mProgressSignUp = (ProgressBar) findViewById(R.id.progress_signUp);
        mSignUpInfo = (TextView) findViewById(R.id.txtSignUpInfo);

        mInputName = (EditText) findViewById(R.id.sign_up_name);
        mInputEmail = (EditText) findViewById(R.id.sign_up_email);
        mInputPassword = (EditText) findViewById(R.id.sign_up_password);
        mInputConfirmPassword = (EditText) findViewById(R.id.sign_up_confirmPassword);
        mSignUpButton = (Button) findViewById(R.id.btn_signUp);
        mLinkToSignInButton = (Button) findViewById(R.id.btn_linkToSignIn);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        mSignUpRequested = false;
        updateSignUpProgressUI();
    }

    /**
     * Is invoked when "Sign up" button is clicked.
     * Checks if all input data is valid and attempts a registration.
     */
    public void registerButtonHandler(View view) {
        String name = mInputName.getText().toString();
        String email = mInputEmail.getText().toString();
        String password = mInputPassword.getText().toString();
        String confirmPassword = mInputConfirmPassword.getText().toString();

        if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {
            hideKeyboard(view);
            if(!validatePassword(password, confirmPassword)) {
                mInputPassword.setText("");
                mInputConfirmPassword.setText("");
                mInputPassword.requestFocus();
            } else if(!validateEmail(email)) {
                mInputEmail.setText("");
                mInputEmail.requestFocus();
                showSnackbar(getString(R.string.invalid_email));
            } else {
                if(isInternetAvailable()) {
                    registerUser(name, email, password);
                }
            }
        } else {
            showSnackbar(getString(R.string.missing_fields));
        }
    }

    /**
     * Is invoked when the button to go to the sign in page is clicked.
     * Directs to {@link LoginActivity}.
     */
    public void registerLinkToLoginButtonHandler(View view) {
        Intent i = new Intent(view.getContext(),LoginActivity.class);
        startActivity(i);
        finish();
    }

    /**
     * Attempts to register a new user with the provided name, e-mail and password.
     * Directs to {@link LoginActivity} after a new database entry has been created for the user.
     */
    private void registerUser(final String name, final String email, final String password) {
        String tag_string_req = "req_register";

        StringRequest strReq = new StringRequest(Method.POST,
                Constants.URL_DB_REQUESTS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {
                        // User successfully stored in MySQL, next store the user in SQLite
                        String uid = jObj.getString("uid");

                        JSONObject user = jObj.getJSONObject("user");
                        String name = user.getString("name");
                        String email = user.getString("email");
                        String created_at = user
                                .getString("created_at");
                        db.addUser(name, email, uid, created_at);

                        // Create upload folder for the new user;
                        preCreateDir();

                        mSignUpRequested = false;
                        updateSignUpProgressUI();

                        // Launch login activity
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        intent.putExtra("user_signed_up", true);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error occurred in registration. Get the error
                        // message
                        showSnackbar(jObj.getString("error_msg"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                showSnackbar("A network error occurred. Please try again later.");
                mSignUpRequested = false;
                updateSignUpProgressUI();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<>();
                params.put("tag", "register");
                params.put("name", name);
                params.put("email", email);
                params.put("password", password);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Requests the login data of the Ftp host
     */
    private void preCreateDir() {
        String tag_string_req = "req_ftp_config";

        StringRequest strReq = new StringRequest(Request.Method.POST, Constants.URL_DB_REQUESTS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    JSONObject ftpObj = jObj.getJSONObject("ftp");
                    new UserUploadDir().execute(ftpObj.getString("url"),
                            ftpObj.getString("user"), ftpObj.getString("password"));
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
                params.put("tag", "ftp_config");

                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * A {@link AsyncTask} subclass that creates a new user folder on the Ftp server.
     */
    private class UserUploadDir extends AsyncTask<String, Integer, Void> {
        @Override
        protected void onProgressUpdate(Integer... progress) {}

        @Override
        protected Void doInBackground(String... params) {
            return createDir(params[0], params[1], params[2]);
        }

        /**
         * Creates a new user folder on the Ftp server
         */
        private Void createDir(final String url, final String user, final String password) {
            FTPClient mFtpClient = new FTPClient();
            try {
                mFtpClient.addProtocolCommandListener(new PrintCommandListener(new
                        PrintWriter(System.out)));
                mFtpClient.setConnectTimeout(300);
                mFtpClient.connect(InetAddress.getByName(url));
                mFtpClient.login(user, password);
                if (FTPReply.isPositiveCompletion(mFtpClient.getReplyCode())) {
                    mFtpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    mFtpClient.enterLocalPassiveMode();
                    mFtpClient.changeWorkingDirectory("/html/witestify/uploads");
                    mFtpClient.makeDirectory(mInputName.getText().toString());
                }

                mFtpClient.disconnect();
            } catch (SocketException e) {
                Log.e(TAG, "FTP SocketException" + e.toString());
            } catch (UnknownHostException e) {
                Log.e(TAG, "FTP UnknownHostException" + e.toString());
            } catch (IOException e) {
                Log.e(TAG, "FTP IOException" + e.toString());
            }
            return null;
        }
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
     * Updates the visibility of the buttons and the progress bar.
     * Hides the buttons and displays the progress bar during a sign in attempt.
     * Hides the progress bar and displays the button otherwise.
     */
    private void updateSignUpProgressUI() {
        if (mSignUpRequested) {
            mProgressSignUp.setVisibility(ProgressBar.VISIBLE);
            mSignUpInfo.setVisibility(TextView.VISIBLE);
            mSignUpButton.setVisibility(Button.GONE);
            mLinkToSignInButton.setVisibility(Button.GONE);
        } else {
            mProgressSignUp.setVisibility(ProgressBar.GONE);
            mSignUpInfo.setVisibility(TextView.GONE);
            mSignUpButton.setVisibility(Button.VISIBLE);
            mLinkToSignInButton.setVisibility(Button.VISIBLE);
        }
    }

    /**
     * Checks if the e-mail input is a valid e-mail address according
     * to the defined pattern {@link SignUpActivity#EMAIL_PATTERN}.
     */
    private boolean validateEmail(String email) {
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
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
        Snackbar.make(mSignUpLinearLayout, message, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
    }
}
