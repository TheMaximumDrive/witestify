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

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import ims.witestify.adapter.KeyframeAdapter;
import ims.witestify.pojo.Video;

/**
 * An activity that displays the keyframes of a video using {@link ViewPager}
 * and {@link KeyframeAdapter}.
 */
public class KeyframesActivity extends AppCompatActivity {

    /** Represents a parameter to require a video object passed to the activity as an extra */
    public static final String VIDEO_EXTRA_PARAM = "video_object";

    /** Provides a ViewPager to place content */
    private ViewPager mViewPager;
    /** Represents a Button that switches from the current to the next video keyframe */
    private Button mPrevKeyframeButton;
    /** Represents a Button that switches from the current to the previous video keyframe */
    private Button mNextKeyframeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyframes);

        initToolbar();

        mViewPager = (ViewPager) findViewById(R.id.viewpager_keyframes);
        mPrevKeyframeButton = (Button) findViewById(R.id.btn_prevKeyframe);
        mNextKeyframeButton = (Button) findViewById(R.id.btn_nextKeyframe);

        Video mVideo = getIntent().getParcelableExtra(VIDEO_EXTRA_PARAM);
        ArrayList<String> filePaths = new ArrayList<>();
        filePaths.add(mVideo.getKeyframe() + "01.jpg");
        filePaths.add(mVideo.getKeyframe() + "02.jpg");
        filePaths.add(mVideo.getKeyframe() + "03.jpg");

        KeyframeAdapter mKeyframeAdapter = new KeyframeAdapter(KeyframesActivity.this, filePaths);

        mViewPager.setAdapter(mKeyframeAdapter);

        // displaying first keyframe
        mViewPager.setCurrentItem(0);
        disablePreviousButton();

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {  }

            @Override
            public void onPageSelected(int position) {
                switch(position) {
                    case 0:
                        disablePreviousButton();
                        break;
                    case 1:
                        enablePreviousButton();
                        enableNextButton();
                        break;
                    case 2:
                        disableNextButton();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {  }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Initializes the Action Bar, so that a back button and the title are shown
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
     * Is invoked, when the "Previous" button is clicked, and switches
     * from the current to the previous video keyframe
     */
    public void prevKeyframeButtonHandler(View view) {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
    }

    /**
     * Is invoked, when the "Next" button is clicked, and switches
     * from the current to the next video keyframe
     */
    public void nextKeyframeButtonHandler(View view) {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
    }

    private void enableNextButton() {
        int colorId = getResources().getColor(R.color.app_colorPrimary);
        mNextKeyframeButton.setBackgroundColor(colorId);
        mNextKeyframeButton.setTextColor(Color.WHITE);
        mNextKeyframeButton.setEnabled(true);
    }

    private void disableNextButton() {
        mNextKeyframeButton.setBackgroundColor(Color.TRANSPARENT);
        mNextKeyframeButton.setTextColor(Color.parseColor("#4B4B4B"));
        mNextKeyframeButton.setEnabled(false);
    }

    private void enablePreviousButton() {
        int colorId = getResources().getColor(R.color.app_colorPrimary);
        mPrevKeyframeButton.setBackgroundColor( colorId);
        mPrevKeyframeButton.setTextColor(Color.WHITE);
        mPrevKeyframeButton.setEnabled(true);
    }

    private void disablePreviousButton() {
        mPrevKeyframeButton.setBackgroundColor(Color.TRANSPARENT);
        mPrevKeyframeButton.setTextColor(Color.parseColor("#4B4B4B"));
        mPrevKeyframeButton.setEnabled(false);
    }
}
