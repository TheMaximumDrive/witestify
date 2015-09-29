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

package ims.witestify.pojo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing a video entity
 */
public class Video implements Parcelable {

    /** User associated with the video */
    private String user;

    /** Title of the video */
    private String title;

    /** Location where the video was captured */
    private String location;

    /** Time when the video was captured */
    private String timestamp;

    /** Duration of the video */
    private long duration;

    /** Url of the directory where the video keyframes are stored */
    private String keyframe;

    /** Url of the video */
    private String url;

    public Video() {}

    public Video(Parcel source) {
        this.user = source.readString();
        this.title = source.readString();
        this.location = source.readString();
        this.timestamp = source.readString();
        this.duration = source.readLong();
        this.keyframe = source.readString();
        this.url = source.readString();
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(user);
        dest.writeString(title);
        dest.writeString(location);
        dest.writeString(timestamp);
        dest.writeLong(duration);
        dest.writeString(keyframe);
        dest.writeString(url);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Video createFromParcel(Parcel source) {
            return new Video(source);
        }

        public Video[] newArray(int size) {
            return new Video[size];
        }
    };

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getKeyframe() {
        return keyframe;
    }

    public void setKeyframe(String keyframe) {
        this.keyframe = keyframe;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
