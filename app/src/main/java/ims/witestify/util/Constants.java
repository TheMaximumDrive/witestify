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

/**
 * Constant values used by this application
 */
public final class Constants {
    private static final String PREFERENCES_FILE = "witestify_settings";

    /** Server request url */
    public static final String URL_DB_REQUESTS = "http://witestify.wenchaochen.at/db/";

    /** Server upload url */
    public static final String URL_UPLOADS = "http://witestify.wenchaochen.at/uploads/";

    /** Address found success */
    public static final int SUCCESS_RESULT = 0;

    /** Address found failure */
    public static final int FAILURE_RESULT = 1;

    /** Application package name */
    public static final String PACKAGE_NAME = "ims.witestify";

    /** Application receiver */
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";

    /** Application result data key */
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";

    /** Application location data extra key */
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    /** Video capture request code */
    public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 1;

    /** Location service enabling request code */
    public static final int ENABLE_LOCATION_REQUEST_CODE = 2;

    /** Sort by most recently added video request code */
    public static final String SORT_VIDEOS_BY_MOST_RECENT = "SORT_BY_MOST_RECENT";

    /** Sort by video title request code */
    public static final String SORT_VIDEOS_BY_TITLE = "SORT_BY_TITLE";

    /** Sort by longest video request code */
    public static final String SORT_VIDEOS_BY_LONGEST = "SORT_BY_LONGEST";

    /** Sort by shortest video request code */
    public static final String SORT_VIDEOS_BY_SHORTEST = "SORT_BY_SHORTEST";
}
