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

package ims.witestify.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

import ims.witestify.AppController;
import ims.witestify.R;

/**
 * An {@link PagerAdapter} subclass that provides the adapter to populate pages of a
 * {@link ViewPager} with video keyframes.
 */
public class KeyframeAdapter extends PagerAdapter {
    private Activity mActivity;
    private ArrayList<String> mImagePaths;

    public KeyframeAdapter(Activity activity,
                           ArrayList<String> imagePaths) {
        this.mActivity = activity;
        this.mImagePaths = imagePaths;
    }

    /**
     * Returns the number of video keyframes available.
     */
    @Override
    public int getCount() {
        return this.mImagePaths.size();
    }

    /**
     * Returns whether the View is associated with the object
     * returned by {@link #instantiateItem(ViewGroup, int)}.
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    /**
     * Creates the page for the given position.
     * The page contains a video keyframe.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        NetworkImageView mKeyframeDisplayImage;

        LayoutInflater mLayoutInflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = mLayoutInflater.inflate(R.layout.layout_fullscreen_keyframe, container,
                false);

        mKeyframeDisplayImage = (NetworkImageView) viewLayout.findViewById(R.id.img_KeyframeDisplay);

        ImageLoader mImageLoader = AppController.getInstance().getImageLoader();
        mKeyframeDisplayImage.setImageUrl(mImagePaths.get(position), mImageLoader);

        container.addView(viewLayout);

        return viewLayout;
    }

    /**
     * Remove a page for the given position.
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((RelativeLayout) object);

    }
}
