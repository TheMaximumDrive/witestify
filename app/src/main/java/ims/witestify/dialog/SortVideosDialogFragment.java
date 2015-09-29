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

package ims.witestify.dialog;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import ims.witestify.R;
import ims.witestify.util.Constants;

/**
 * A {@link DialogFragment} subclass that gives the option to sort the video list
 */
public class SortVideosDialogFragment extends DialogFragment implements View.OnClickListener {

    /**
     * Interface providing a listener that responds to the selection of a sort type
     */
    public interface SortVideosListener {
        /**
         * Responds to the selection of a sort type
         * @param dialog A DialogFragment
         * @param sortType Sort type that was selected
         */
        void onSortTypeSelected(DialogFragment dialog,
                                       final String sortType);
    }

    /** Provides a listener that responds to a selection */
    SortVideosListener mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (SortVideosListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SortVideosListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_sort_videos, container);
        RadioButton mMostRecent = (RadioButton) view.findViewById(R.id.radio_most_recent);
        RadioButton mTitle = (RadioButton) view.findViewById(R.id.radio_title);
        RadioButton mLongest = (RadioButton) view.findViewById(R.id.radio_longest);
        RadioButton mShortest = (RadioButton) view.findViewById(R.id.radio_shortest);

        mMostRecent.setOnClickListener(this);
        mTitle.setOnClickListener(this);
        mLongest.setOnClickListener(this);
        mShortest.setOnClickListener(this);

        this.getDialog().setTitle(getString(R.string.sort_by));

        Bundle args = getArguments();
        String sortType = args.getString("sortType");

        switch (sortType) {
            case Constants.SORT_VIDEOS_BY_MOST_RECENT:
                mMostRecent.setChecked(true);
                break;
            case Constants.SORT_VIDEOS_BY_TITLE:
                mTitle.setChecked(true);
                break;
            case Constants.SORT_VIDEOS_BY_LONGEST:
                mLongest.setChecked(true);
                break;
            case Constants.SORT_VIDEOS_BY_SHORTEST:
                mShortest.setChecked(true);
                break;
            default:
                break;
        }

        return view;
    }

    @Override
    public void onClick(View view){
        RadioButton radioButton = (RadioButton) view;
        boolean checked = radioButton.isChecked();

        if (checked) {
            radioButton.setChecked(true);
            switch (view.getId()) {
                case R.id.radio_most_recent:
                    mCallback.onSortTypeSelected(SortVideosDialogFragment.this, Constants.SORT_VIDEOS_BY_MOST_RECENT);
                    SortVideosDialogFragment.this.getDialog().dismiss();
                    break;
                case R.id.radio_title:
                    mCallback.onSortTypeSelected(SortVideosDialogFragment.this, Constants.SORT_VIDEOS_BY_TITLE);
                    SortVideosDialogFragment.this.getDialog().dismiss();
                    break;
                case R.id.radio_longest:
                    mCallback.onSortTypeSelected(SortVideosDialogFragment.this, Constants.SORT_VIDEOS_BY_LONGEST);
                    SortVideosDialogFragment.this.getDialog().dismiss();
                    break;
                case R.id.radio_shortest:
                    mCallback.onSortTypeSelected(SortVideosDialogFragment.this, Constants.SORT_VIDEOS_BY_SHORTEST);
                    SortVideosDialogFragment.this.getDialog().dismiss();
                    break;
                default:
                    break;
            }
        }
    }
}
