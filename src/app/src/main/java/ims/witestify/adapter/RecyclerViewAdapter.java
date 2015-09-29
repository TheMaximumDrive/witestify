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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ims.witestify.AppController;
import ims.witestify.KeyframesActivity;
import ims.witestify.R;
import ims.witestify.StandardVideoPlayerActivity;
import ims.witestify.dialog.RenameVideoDialogFragment;
import ims.witestify.exoplayer.ExoVideoPlayerActivity;
import ims.witestify.pojo.Video;

/**
 * An {@link RecyclerView.Adapter} subclass that provides the adapter to populate a
 * {@link RecyclerView} with {@link CardView}.
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int EMPTY_VIEW = 10;

    /** List of videos to populate */
    private List<Video> videos;

    /** Provides a listener that responds to clicks on an item */
    private OnItemClickListener mItemClickListener;

    private Activity activity;
    private FragmentManager fragmentManager;
    private ConnectivityManager connectivityManager;

    public RecyclerViewAdapter(Activity activity,
                               FragmentManager fragmentManager,
                               List<Video> videos) {
        this.activity = activity;
        this.fragmentManager = fragmentManager;
        this.videos = videos;

        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Return number of videos to populate or 1, if there are no videos.
     */
    @Override
    public int getItemCount() {
        return videos.size() > 0 ? videos.size() : 1;
    }

    /**
     * Return item type depending on the number of videos.
     */
    @Override
    public int getItemViewType(int position) {
        if (videos.size() == 0) {
            return EMPTY_VIEW;
        }
        return super.getItemViewType(position);
    }

    /**
     * Inflate the view with {@link EmptyViewHolder} if there are no videos.
     * Inflate the view with {@link VideoViewHolder} if there is at least one video.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v;
        if(i == EMPTY_VIEW) {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_no_entries, viewGroup, false);
            return new EmptyViewHolder(v);
        } else {
            v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_video, viewGroup, false);
            return new VideoViewHolder(v);
        }
    }

    /**
     * Create a {@link VideoViewHolder}, if viewHolder is an instance of {@link VideoViewHolder}
     * and fill it with the provided video data.
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if(viewHolder instanceof  VideoViewHolder) {
            VideoViewHolder videoViewHolder = (VideoViewHolder) viewHolder;
            ImageLoader mImageLoader = AppController.getInstance().getImageLoader();
            videoViewHolder.keyframe.setImageUrl(videos.get(i).getKeyframe() + "01.jpg", mImageLoader);
            int minutes = (int) (videos.get(i).getDuration() / 60);
            long seconds = videos.get(i).getDuration() % 60;
            String duration = String.format("%d:%02d", minutes, seconds);
            videoViewHolder.title.setText(videos.get(i).getTitle() + " (" + duration + ")");
            videoViewHolder.location.setText(videos.get(i).getLocation());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
            Date tmpDate = new Date();
            try {
                tmpDate = sdf.parse(videos.get(i).getTimestamp());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String recordingDate =
                    new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMAN).format(tmpDate);
            String recordingTime =
                    new SimpleDateFormat("hh:mm a, z", Locale.GERMAN).format(tmpDate);
            videoViewHolder.date.setText(recordingDate);
            videoViewHolder.time.setText(recordingTime);

            initPlayButton(videoViewHolder, i);
            initKeyframeButton(videoViewHolder, i);
            initRenameButton(videoViewHolder, i);
        }
    }

    /**
     * Interface providing a listener that responds to clicks on an item
     */
    public interface OnItemClickListener {
        /**
         * Responds to a click on the view
         * @param view View that was clicked
         * @param video Video associated with the view that was clicked
         */
        void onItemClick(View view, Video video);
    }

    /**
     * Sets the listener that responds to clicks to an item
     */
    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    /**
     * Sets the list of videos and notifies registered observers that the data has changed
     */
    public void setVideos(ArrayList<Video> videoList) {
        this.videos = videoList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder to display an empty item
     */
    public class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * ViewHolder to display a video item
     */
    public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CardView cv;
        TextView title;
        TextView location;
        TextView date;
        TextView time;
        NetworkImageView keyframe;

        LinearLayout mCardViewLayout;
        Button mPlayButton;
        Button mKeyframesButton;
        Button mRenameButton;

        VideoViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            cv = (CardView) itemView.findViewById(R.id.video_card_view);
            title = (TextView) itemView.findViewById(R.id.video_title);
            location = (TextView) itemView.findViewById(R.id.video_location);
            date = (TextView) itemView.findViewById(R.id.video_date);
            time = (TextView) itemView.findViewById(R.id.video_time);
            keyframe = (NetworkImageView) itemView.findViewById(R.id.video_keyframe);

            mCardViewLayout = (LinearLayout) itemView.findViewById(R.id.cardview_layout);
            mPlayButton = (Button) itemView.findViewById(R.id.btn_playVideo);
            mKeyframesButton = (Button) itemView.findViewById(R.id.btn_showKeyframes);
            mRenameButton = (Button) itemView.findViewById(R.id.btn_renameVideo);
        }

        @Override
        public void onClick(View v) {
            if(mItemClickListener != null) {
                mItemClickListener.onItemClick(itemView, videos.get(getLayoutPosition()));
            }
        }
    }

    /**
     * Initializes the button for video playback
     */
    private void initPlayButton(final VideoViewHolder videoViewHolder, final int i) {
        videoViewHolder.mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isInternetAvailable(videoViewHolder)) {
                    Intent intent;
                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        intent = new Intent(activity, ExoVideoPlayerActivity.class)
                                .setData(Uri.parse(videos.get(i).getUrl()));
                    /*} else {
                        intent = new Intent(activity, StandardVideoPlayerActivity.class)
                                .setData(Uri.parse(videos.get(i).getUrl()));
                    }*/
                    activity.startActivity(intent);
                }
            }
        });
    }

    /**
     * Initializes the button to preview video keyframes
     */
    private void initKeyframeButton(final VideoViewHolder videoViewHolder, final int i) {
        videoViewHolder.mKeyframesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInternetAvailable(videoViewHolder)) {
                    Intent intent = new Intent(activity, KeyframesActivity.class);
                    intent.putExtra(KeyframesActivity.VIDEO_EXTRA_PARAM, videos.get(i));
                    activity.startActivity(intent);
                }
            }
        });
    }

    /**
     * Initializes the button to rename the title of a video
     */
    private void initRenameButton(final VideoViewHolder videoViewHolder, final int i) {
        videoViewHolder.mRenameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isInternetAvailable(videoViewHolder)) {
                    Bundle args = new Bundle();
                    args.putString("title", videos.get(i).getTitle());
                    DialogFragment newFragment = new RenameVideoDialogFragment();
                    newFragment.setArguments(args);
                    newFragment.show(fragmentManager, "RenameVideoDialogFragment");
                }
            }
        });
    }

    /**
     * Checks if the device is connected to the internet.
     * Returns true, if internet connection is available, otherwise false
     */
    private boolean isInternetAvailable(final VideoViewHolder videoViewHolder) {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        if(!(isConnected))
            Snackbar.make(videoViewHolder.mCardViewLayout,
                    activity.getString(R.string.no_internet), Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();
        return isConnected;
    }
}
