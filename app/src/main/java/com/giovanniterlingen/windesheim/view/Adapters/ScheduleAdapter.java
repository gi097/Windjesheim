/**
 * Copyright (c) 2019 Giovanni Terlingen
 * <p/>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.giovanniterlingen.windesheim.view.Adapters;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.controllers.DatabaseController;
import com.giovanniterlingen.windesheim.models.Lesson;
import com.giovanniterlingen.windesheim.utils.ColorUtils;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.giovanniterlingen.windesheim.utils.TimeUtils;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private final ScheduleActivity activity;
    private final Date date;
    private Lesson[] lessons;

    public ScheduleAdapter(ScheduleActivity activity, Lesson[] lessons, Date date) {
        this.activity = activity;
        this.lessons = lessons;
        this.date = date;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final TextView lessonName = holder.lessonName;
        final TextView lessonTime = holder.lessonTime;
        final TextView lessonRoom = holder.lessonRoom;
        final TextView lessonComponent = holder.lessonComponent;
        final RelativeLayout menuButton = holder.menuButton;
        final ImageView menuButtonImage = holder.menuButtonImage;
        final View scheduleIdentifier = holder.scheduleIdentifier;

        Lesson lesson = this.lessons[position];
        long startTime = lesson.getStartTime().getTime();
        long endTime = lesson.getEndTime().getTime();
        long currentTime = System.currentTimeMillis();

        lessonName.setText(lesson.getSubject());
        lessonRoom.setText(lesson.getRoom());
        lessonComponent.setText(lesson.getScheduleType() == Constants.SCHEDULE_TYPE.TEACHER ?
                lesson.getClassName() : lesson.getTeacher());
        lessonComponent.setSelected(true);

        if (startTime <= currentTime && endTime >= currentTime) {
            lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimaryText));
            lessonTime.setText(ApplicationLoader.applicationContext
                    .getResources().getString(R.string.lesson_started));
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Lesson lesson = ScheduleAdapter.this.lessons[holder.getAdapterPosition()];
                    if (!lessonTime.getText().toString().equals(ApplicationLoader.applicationContext
                            .getResources().getString(R.string.lesson_started))) {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimaryText));
                        lessonTime.setText(ApplicationLoader.applicationContext
                                .getResources().getString(R.string.lesson_started));
                    } else {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        String lessonTimes =
                                TimeUtils.getHourMinuteFormat().format(lesson
                                        .getStartTime()) + " - " +
                                        TimeUtils.getHourMinuteFormat()
                                                .format(lesson.getEndTime());
                        lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorSecondaryText));
                        lessonTime.setText(lessonTimes);
                    }
                }
            });
        } else if (endTime < currentTime) {
            lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimaryText));
            lessonTime.setText(ApplicationLoader.applicationContext
                    .getResources().getString(R.string.finished));
            holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Lesson lesson = ScheduleAdapter.this.lessons[holder.getAdapterPosition()];
                    if (!lessonTime.getText().toString().equals(ApplicationLoader.applicationContext
                            .getResources().getString(R.string.finished))) {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorPrimaryText));
                        lessonTime.setText(ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.finished));
                    } else {
                        TranslateAnimation animation = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                                Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
                        );
                        animation.setDuration(100);
                        lessonTime.setAnimation(animation);
                        String lessonTimes =
                                TimeUtils.getHourMinuteFormat().format(lesson
                                        .getStartTime()) + " - " +
                                        TimeUtils.getHourMinuteFormat()
                                                .format(lesson.getEndTime());
                        lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorSecondaryText));
                        lessonTime.setText(lessonTimes);
                    }
                }
            });
        } else {
            String lessonTimes = TimeUtils.getHourMinuteFormat().format(lesson.getStartTime()) +
                    " - " + TimeUtils.getHourMinuteFormat().format(lesson.getEndTime());
            lessonTime.setTextColor(ContextCompat.getColor(activity, R.color.colorSecondaryText));
            lessonTime.setText(lessonTimes);
            holder.cardView.setOnClickListener(null);
        }
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuButtonImage.setImageDrawable(ResourcesCompat.getDrawable(
                        activity.getResources(), R.drawable.overflow_open, null));
                PopupMenu popupMenu = new PopupMenu(activity, menuButton);
                popupMenu.inflate(R.menu.menu_schedule);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        Lesson lesson = ScheduleAdapter.this.lessons[holder.getAdapterPosition()];
                        if (item.getItemId() == R.id.hide_lesson) {
                            showPromptDialog(lesson);
                            return true;
                        }
                        if (item.getItemId() == R.id.save_lesson) {
                            addLessonToCalendar(lesson);
                        }
                        return true;
                    }
                });
                popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        menuButtonImage.setImageDrawable(ResourcesCompat.getDrawable(
                                activity.getResources(), R.drawable.overflow_normal, null));
                    }
                });
                popupMenu.show();
            }
        });
        scheduleIdentifier.setBackgroundColor(ColorUtils.getColorById(lesson.getScheduleId()));
    }

    private void addLessonToCalendar(Lesson lesson) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event");
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, lesson.getStartTime().getTime());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, lesson.getEndTime().getTime());
        intent.putExtra(CalendarContract.Events.ALL_DAY, false);
        intent.putExtra(CalendarContract.Events.TITLE, lesson.getSubject());
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, lesson.getRoom());

        try {
            activity.startActivity(intent);

            Bundle bundle = new Bundle();
            bundle.putString(Constants.TELEMETRY_PROPERTY_NAME,
                    lesson.getSubject());
            TelemetryUtils.getInstance()
                    .logEvent(Constants.TELEMETRY_KEY_LESSON_ADDED_CALENDAR, bundle);
        } catch (ActivityNotFoundException e) {
            activity.showSnackbar(activity.getResources().getString(R.string.no_calendar_found));
        }
    }

    private void showPromptDialog(final Lesson lesson) {
        new AlertDialog.Builder(activity)
                .setTitle(activity.getResources().getString(R.string.confirmation))
                .setMessage(activity.getResources().getString(R.string.deletion_description))
                .setPositiveButton(activity.getResources().getString(R.string.hide),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                DatabaseController.getInstance().hideLesson(lesson);

                                Bundle bundle = new Bundle();
                                bundle.putString(Constants.TELEMETRY_PROPERTY_NAME,
                                        lesson.getSubject());
                                TelemetryUtils.getInstance()
                                        .logEvent(Constants.TELEMETRY_KEY_LESSON_HIDDEN, bundle);

                                updateLessons(DatabaseController.getInstance().getLessons(date));
                                final boolean isEmpty = getItemCount() == 0;
                                if (isEmpty) {
                                    activity.updateFragmentView();
                                }
                                Snackbar snackbar = Snackbar.make(activity
                                                .findViewById(R.id.coordinator_layout),
                                        activity.getResources().getString(R.string.lesson_hidden),
                                        Snackbar.LENGTH_SHORT);
                                snackbar.setAction(activity.getResources()
                                        .getString(R.string.undo), new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        DatabaseController.getInstance().restoreLesson(lesson);

                                        Bundle bundle = new Bundle();
                                        bundle.putString(Constants.TELEMETRY_PROPERTY_NAME,
                                                lesson.getSubject());
                                        TelemetryUtils.getInstance()
                                                .logEvent(Constants.TELEMETRY_KEY_LESSON_RESTORED, bundle);

                                        updateLessons(DatabaseController.getInstance().getLessons(date));
                                        if (isEmpty) {
                                            activity.updateFragmentView();
                                        }
                                        Snackbar snackbar1 = Snackbar.make(activity
                                                        .findViewById(R.id.coordinator_layout),
                                                activity.getResources().getString(R.string.lesson_restored),
                                                Snackbar.LENGTH_SHORT);
                                        snackbar1.show();
                                        ApplicationLoader.restartNotificationThread();
                                    }
                                });
                                snackbar.show();
                                ApplicationLoader.restartNotificationThread();
                                dialog.cancel();
                            }
                        })
                .setNegativeButton(activity.getResources()
                        .getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(activity).
                inflate(R.layout.adapter_item_schedule, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return lessons.length;
    }

    public void updateLessons(Lesson[] lessons) {
        this.lessons = lessons;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final TextView lessonName;
        final TextView lessonTime;
        final TextView lessonRoom;
        final TextView lessonComponent;
        final RelativeLayout menuButton;
        final ImageView menuButtonImage;
        final CardView cardView;
        final View scheduleIdentifier;

        ViewHolder(View view) {
            super(view);
            lessonName = view.findViewById(R.id.schedule_list_row_name);
            lessonTime = view.findViewById(R.id.schedule_list_row_time);
            lessonRoom = view.findViewById(R.id.schedule_list_row_room);
            lessonComponent = view.findViewById(R.id.schedule_list_row_component);
            menuButton = view.findViewById(R.id.menu_button);
            menuButtonImage = view.findViewById(R.id.menu_button_image);
            cardView = view.findViewById(R.id.card);
            scheduleIdentifier = view.findViewById(R.id.schedule_identifier);
        }
    }
}
