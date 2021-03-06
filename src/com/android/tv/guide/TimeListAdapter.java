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

package com.android.tv.guide;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.tv.R;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Adapts the time range from {@link ProgramManager} to the timeline header row of the program
 * guide table.
 */
public class TimeListAdapter extends RecyclerView.Adapter<TimeListAdapter.TimeViewHolder> {
    private static final long TIME_UNIT_MS = TimeUnit.MINUTES.toMillis(30);
    private static int sRowHeaderOverlapping;

    // Nearest half hour at or before the start time.
    private long mStartUtcMs;

    public TimeListAdapter(Resources res) {
        if (sRowHeaderOverlapping == 0) {
            sRowHeaderOverlapping = Math.abs(res.getDimensionPixelOffset(
                    R.dimen.program_guide_table_header_row_overlap));
        }
    }

    public void update(long startTimeMs) {
        mStartUtcMs = startTimeMs;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.program_guide_table_header_row_item;
    }

    @Override
    public void onBindViewHolder(TimeViewHolder holder, int position) {
        long startTime = mStartUtcMs + position * TIME_UNIT_MS;
        long endTime = startTime + TIME_UNIT_MS;

        View itemView = holder.itemView;

        TextView textView = (TextView) itemView.findViewById(R.id.time);
        String time = DateFormat.getTimeFormat(itemView.getContext()).format(new Date(startTime));
        textView.setText(time);

        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) itemView.getLayoutParams();
        lp.width = GuideUtils.convertMillisToPixel(startTime, endTime);
        if (position == 0) {
            // Adjust width for the first entry to make the item starts from the fading edge.
            lp.setMarginStart(sRowHeaderOverlapping - lp.width / 2);
        } else {
            lp.setMarginStart(0);
        }
        itemView.setLayoutParams(lp);
    }

    @Override
    public TimeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new TimeViewHolder(itemView);
    }

    public static class TimeViewHolder extends RecyclerView.ViewHolder {
        public TimeViewHolder(View itemView) {
            super(itemView);
        }
    }
}
