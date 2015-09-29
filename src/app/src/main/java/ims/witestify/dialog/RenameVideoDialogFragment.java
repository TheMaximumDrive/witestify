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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import ims.witestify.R;

/**
 * A {@link DialogFragment} subclass that provides an user interface to rename a video
 */
public class RenameVideoDialogFragment extends DialogFragment {

    /**
     * Interface providing a listener that responds to the input of a new video title
     */
    public interface RenameVideoListener {
        /**
         * Responds to the input of a new video title
         * @param dialog A DialogFragment
         * @param oldTitle Old video title
         * @param newTitle New video title
         */
        void onRenameVideoDialogSave(DialogFragment dialog,
                                     final String oldTitle,
                                     final String newTitle);
    }

    /** Provides a listener that responds to the input */
    RenameVideoListener mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mCallback = (RenameVideoListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement RenameVideoListener");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogLayout =
                View.inflate(getActivity().getBaseContext(), R.layout.dialog_rename_video, null);
        final Context context = dialogLayout.getContext();

        Bundle argsBundle = getArguments();
        final String oldTitle = argsBundle.getString("title");
        final EditText mInputTitle =
                (EditText) dialogLayout.findViewById(R.id.rename_video_video_title);
        mInputTitle.setText(oldTitle);
        mInputTitle.selectAll();
        mInputTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mInputTitle.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager mInputMethodManager =
                                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        mInputMethodManager.showSoftInput(mInputTitle, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        mInputTitle.requestFocus();

        builder.setView(dialogLayout)
                .setTitle("Rename video")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newTitle = mInputTitle.getText().toString();

                        if(oldTitle != null && oldTitle.equals(newTitle)) {
                            RenameVideoDialogFragment.this.getDialog().cancel();
                        } else {
                            mCallback.onRenameVideoDialogSave(RenameVideoDialogFragment.this,
                                    oldTitle, newTitle);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RenameVideoDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
