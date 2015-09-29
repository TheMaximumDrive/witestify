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
import android.support.annotation.NonNull;

import java.util.Comparator;

/**
 * A class for a detected face during video capture
 */
public class DetectedFace implements Parcelable, Comparable<DetectedFace> {

    /** Time when the face or faces appeared during video capture */
    private long muSecond;

    /** Number of faces that appeared */
    private int faces;

    public DetectedFace(long muSecond, int faces) {
        this.muSecond = muSecond;
        this.faces = faces;
    }

    public DetectedFace(Parcel source) {
        muSecond = source.readLong();
        faces = source.readInt();
    }

    public long getMuSeconds() {
        return muSecond;
    }

    public int getFaces() {
        return faces;
    }

    @Override
    public int compareTo(@NonNull DetectedFace another) {
        return another.getFaces() - this.faces;
    }

    public static Comparator<DetectedFace> FaceCounterComparator
            = new Comparator<DetectedFace>() {

        @Override
        public int compare(DetectedFace lhs, DetectedFace rhs) {
            long diff = rhs.getMuSeconds() - lhs.getMuSeconds();
            if(diff > 0) {
                return 1;
            } else if(diff == 0) {
                return 0;
            } else {
                return -1;
            }
        }
    };

    @Override
     public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(muSecond);
        dest.writeInt(faces);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public DetectedFace createFromParcel(Parcel source) {
            return new DetectedFace(source);
        }

        public DetectedFace[] newArray(int size) {
            return new DetectedFace[size];
        }
    };
}
