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

package ims.witestify.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * A class to manage the session so that a user does not
 * need to sign in every time he closes the application
 */
public class SessionManager {

    private SharedPreferences pref;
    private Editor mEditor;
    private Context mContext;

    /** Shared pref mode */
    int PRIVATE_MODE = 0;

    /** Shared preferences file name */
    private static final String PREF_NAME = "witestify_login";

    /** Shared preferences key for the login status */
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    /** Shared preferences key for the username */
    private static final String KEY_USERNAME = "username";

    public SessionManager(Context context) {
        this.mContext = context;
        pref = mContext.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        mEditor = pref.edit();
        mEditor.apply();
    }

    /**
     * Sets the login status shared preferences key
     */
    public void setLogin(boolean isLoggedIn) {
        mEditor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        mEditor.commit();
    }

    /**
     * Returns the value of the login status shared preferences key
     */
    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Sets the username shared preferences key
     */
    public void setUser(String username) {
        mEditor.putString(KEY_USERNAME, username);
        mEditor.commit();
    }

    /**
     * Returns the value for the shared preferences key of the username
     */
    public String getCurrentUser() {
        return pref.getString(KEY_USERNAME, null);
    }
}
