/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.ui.sidepanel.parentalcontrols;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.android.tv.MainActivity;
import com.android.tv.R;
import com.android.tv.dialog.WebDialogFragment;
import com.android.tv.license.LicenseUtils;
import com.android.tv.parental.ContentRatingSystem;
import com.android.tv.parental.ContentRatingSystem.Rating;
import com.android.tv.parental.ParentalControlSettings;
import com.android.tv.ui.sidepanel.CheckBoxItem;
import com.android.tv.ui.sidepanel.DividerItem;
import com.android.tv.ui.sidepanel.Item;
import com.android.tv.ui.sidepanel.RadioButtonItem;
import com.android.tv.ui.sidepanel.SideFragment;
import com.android.tv.util.TvSettings;
import com.android.tv.util.TvSettings.ContentRatingLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RatingsFragment extends SideFragment {
    private static final SparseIntArray sLevelResourceIdMap;
    private static final SparseIntArray sDescriptionResourceIdMap;
    private static final String TRACKER_LABEL = "Ratings";
    private int mItemsSize;

    static {
        sLevelResourceIdMap = new SparseIntArray(5);
        sLevelResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_NONE,
                R.string.option_rating_none);
        sLevelResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_HIGH,
                R.string.option_rating_high);
        sLevelResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_MEDIUM,
                R.string.option_rating_medium);
        sLevelResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_LOW,
                R.string.option_rating_low);
        sLevelResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_CUSTOM,
                R.string.option_rating_custom);

        sDescriptionResourceIdMap = new SparseIntArray(sLevelResourceIdMap.size());
        sDescriptionResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_HIGH,
                R.string.option_rating_high_description);
        sDescriptionResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_MEDIUM,
                R.string.option_rating_medium_description);
        sDescriptionResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_LOW,
                R.string.option_rating_low_description);
        sDescriptionResourceIdMap.put(TvSettings.CONTENT_RATING_LEVEL_CUSTOM,
                R.string.option_rating_custom_description);
    }

    private final List<RatingLevelItem> mRatingLevelItems = new ArrayList<>();

    public static String getDescription(MainActivity tvActivity) {
        @ContentRatingLevel int currentLevel =
                tvActivity.getParentalControlSettings().getContentRatingLevel();
        if (sLevelResourceIdMap.indexOfKey(currentLevel) >= 0) {
            return tvActivity.getString(sLevelResourceIdMap.get(currentLevel));
        }
        return null;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.option_ratings);
    }

    @Override
    public String getTrackerLabel() {
        return TRACKER_LABEL;
    }

    @Override
    protected List<Item> getItemList() {
        List<Item> items = new ArrayList<>();

        mRatingLevelItems.clear();
        for (int i = 0; i < sLevelResourceIdMap.size(); ++i) {
            mRatingLevelItems.add(new RatingLevelItem(sLevelResourceIdMap.keyAt(i)));
        }
        updateRatingLevels();
        items.addAll(mRatingLevelItems);

        List<ContentRatingSystem> contentRatingSystems =
                getMainActivity().getContentRatingsManager().getContentRatingSystems();
        Collections.sort(contentRatingSystems, ContentRatingSystem.DISPLAY_NAME_COMPARATOR);

        for (ContentRatingSystem s : contentRatingSystems) {
            if (getMainActivity().getParentalControlSettings().isContentRatingSystemEnabled(s)) {
                items.add(new DividerItem(s.getDisplayName()));
                for (Rating rating : s.getRatings()) {
                    RatingItem item = rating.getSubRatings().size() == 0 ?
                            new RatingItem(s, rating) :
                            new RatingWithSubItem(s, rating);
                    items.add(item);
                }
            }
        }
        if (LicenseUtils.hasRatingAttribution(getMainActivity().getAssets())) {
            // Display the attribution if our content rating system is selected.
            items.add(new DividerItem());
            items.add(new AttributionItem(getMainActivity()));
        }
        mItemsSize = items.size();
        return items;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMainActivity().getParentalControlSettings().loadRatings();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Although we set the attribution item at the end of the item list non-focusable, we do get
        // its position when the fragment is resumed. This ensures that we do not select the
        // non-focusable item at the end of the list. See b/17387103.
        if (getSelectedPosition() >= mItemsSize) {
            setSelectedPosition(mItemsSize - 1);
        }
    }

    private void updateRatingLevels() {
        @ContentRatingLevel int ratingLevel =
                getMainActivity().getParentalControlSettings().getContentRatingLevel();
        for (RatingLevelItem ratingLevelItem : mRatingLevelItems) {
            ratingLevelItem.setChecked(ratingLevel == ratingLevelItem.mRatingLevel);
        }
    }

    private class RatingLevelItem extends RadioButtonItem {
        private final int mRatingLevel;

        private RatingLevelItem(int ratingLevel) {
            super(getString(sLevelResourceIdMap.get(ratingLevel)),
                    (sDescriptionResourceIdMap.indexOfKey(ratingLevel) >= 0) ?
                            getString(sDescriptionResourceIdMap.get(ratingLevel)) : null);
            mRatingLevel = ratingLevel;
        }

        @Override
        protected void onSelected() {
            super.onSelected();
            getMainActivity().getParentalControlSettings().setContentRatingLevel(
                    getMainActivity().getContentRatingsManager(), mRatingLevel);
            notifyItemsChanged(mRatingLevelItems.size());
        }
    }

    private class RatingItem extends CheckBoxItem {
        protected final ContentRatingSystem mContentRatingSystem;
        protected final Rating mRating;
        private final Drawable mIcon;
        private CompoundButton mCompoundButton;

        private RatingItem(ContentRatingSystem contentRatingSystem, Rating rating) {
            super(rating.getTitle(), rating.getDescription());
            mContentRatingSystem = contentRatingSystem;
            mRating = rating;
            mIcon = rating.getIcon();
        }

        @Override
        protected void onBind(View view) {
            super.onBind(view);

            mCompoundButton = (CompoundButton) view.findViewById(getCompoundButtonId());
            mCompoundButton.setVisibility(View.VISIBLE);

            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if (mIcon != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageDrawable(mIcon);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onUnbind() {
            super.onUnbind();
            mCompoundButton = null;
        }

        @Override
        protected void onUpdate() {
            super.onUpdate();
            mCompoundButton.setButtonDrawable(getButtonDrawable());
            setChecked(getMainActivity().getParentalControlSettings().isRatingBlocked(
                    mContentRatingSystem, mRating));
        }

        @Override
        protected void onSelected() {
            super.onSelected();
            if (getMainActivity().getParentalControlSettings()
                    .setRatingBlocked(mContentRatingSystem, mRating, isChecked())) {
                updateRatingLevels();
            }
        }

        @Override
        protected int getResourceId() {
            return R.layout.option_item_rating;
        }

        protected int getButtonDrawable() {
            return R.drawable.btn_lock_material_anim;
        }
    }

    private class RatingWithSubItem extends RatingItem {
        private RatingWithSubItem(ContentRatingSystem contentRatingSystem, Rating rating) {
            super(contentRatingSystem, rating);
        }

        @Override
        protected void onSelected() {
            getMainActivity().getOverlayManager().getSideFragmentManager()
                    .show(new SubRatingsFragment(mContentRatingSystem, mRating));
        }

        @Override
        protected int getButtonDrawable() {
            int blockedStatus = getMainActivity().getParentalControlSettings().getBlockedStatus(
                    mContentRatingSystem, mRating);
            if (blockedStatus == ParentalControlSettings.RATING_BLOCKED) {
                return R.drawable.btn_lock_material;
            } else if (blockedStatus == ParentalControlSettings.RATING_BLOCKED_PARTIAL) {
                return R.drawable.btn_partial_lock_material;
            }
            return R.drawable.btn_unlock_material;
        }
    }

    /**
     * Opens a dialog showing the sources of the rating descriptions.
     */
    public static class AttributionItem extends Item {
        public final static String DIALOG_TAG = AttributionItem.class.getSimpleName();
        public static final String TRACKER_LABEL = "Sources for content rating systems";
        private final MainActivity mMainActivity;

        public AttributionItem(MainActivity mainActivity) {
            mMainActivity = mainActivity;
        }

        @Override
        protected int getResourceId() {
            return R.layout.option_item_attribution;
        }

        @Override
        protected void onSelected() {
            WebDialogFragment dialog = WebDialogFragment.newInstance(
                    LicenseUtils.RATING_SOURCE_FILE,
                    mMainActivity.getString(R.string.option_attribution), TRACKER_LABEL);
            mMainActivity.getOverlayManager().showDialogFragment(DIALOG_TAG, dialog, false);
        }
    }
}
