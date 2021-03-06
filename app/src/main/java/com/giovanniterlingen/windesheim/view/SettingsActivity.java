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
package com.giovanniterlingen.windesheim.view;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.Constants;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.utils.CalendarUtils;
import com.giovanniterlingen.windesheim.utils.CookieUtils;
import com.giovanniterlingen.windesheim.utils.TelemetryUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class SettingsActivity extends BaseActivity {

    private SharedPreferences preferences;
    private TextView intervalTextview;
    private TextView weekCountTextView;
    private TextView calendarNameTextView;
    private SwitchCompat lessonStart;
    private SwitchCompat darkMode;
    private SwitchCompat telemetry;
    private SwitchCompat sync;
    private CharSequence[] items;
    private int notificationId = Constants.NOTIFICATION_TYPE_NOT_SET;

    private final int PERMISSIONS_REQUEST_WRITE_CALENDAR_TURN_ON_SYNC = 1;
    private final int PERMISSIONS_REQUEST_WRITE_CALENDAR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        items = new CharSequence[]{getResources().getString(R.string.interval_one_hour),
                getResources().getString(R.string.interval_thirty_minutes),
                getResources().getString(R.string.interval_fifteen_minutes),
                getResources().getString(R.string.interval_always_on)};
        preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);

        lessonStart = findViewById(R.id.lesson_notification_switch);
        lessonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                if (lessonStart.isChecked()) {
                    editor.putInt(Constants.PREFS_NOTIFICATIONS_TYPE, Constants.NOTIFICATION_TYPE_ALWAYS_ON);
                } else {
                    editor.putInt(Constants.PREFS_NOTIFICATIONS_TYPE, Constants.NOTIFICATION_TYPE_OFF);
                }
                editor.apply();

                ApplicationLoader.restartNotificationThread();
                updateIntervalTextView();
            }
        });
        int pref = preferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE, 0);
        lessonStart.setChecked(pref != 0 && pref != Constants.NOTIFICATION_TYPE_OFF);

        sync = findViewById(R.id.sync_calendar_switch);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();

                if (!hasCalendarPermissions()) {
                    requestCalendarPermissions(true);
                    sync.setChecked(false);
                    editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, false);
                } else if (sync.isChecked()) {
                    long currentCalendar = preferences.getLong(Constants.PREFS_SYNC_CALENDAR_ID, -1);
                    if (currentCalendar > -1) {
                        editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, true);
                    } else {
                        showCalendarDialog(true);
                    }
                } else {
                    editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, false);
                    CalendarUtils.deleteAllLessonsFromCalendar();
                }
                editor.apply();
            }
        });

        LinearLayout calendarRow = findViewById(R.id.settings_calendar);
        calendarRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hasCalendarPermissions()) {
                    requestCalendarPermissions(false);
                    return;
                }
                showCalendarDialog(false);
            }
        });

        calendarNameTextView = findViewById(R.id.settings_calendar_name);

        LinearLayout weekCountRow = findViewById(R.id.settings_weeks_to_show_row);
        weekCountRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWeekCountDialog();
            }
        });
        int currentWeekCount = preferences.getInt(Constants.PREFS_WEEK_COUNT,
                Constants.DEFAULT_WEEK_COUNT);
        weekCountTextView = findViewById(R.id.settings_weeks_to_show_text);
        String weeks = getResources().getQuantityString(R.plurals.week_count, currentWeekCount,
                currentWeekCount);
        weekCountTextView.setText(getResources().getString(R.string.settings_week_count_current,
                weeks));

        darkMode = findViewById(R.id.dark_mode_switch);
        darkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_DARK_MODE, darkMode.isChecked());
                editor.commit(); // Make sure to use commit()

                if (darkMode.isChecked()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                restart();
            }
        });
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean useDarkMode = preferences.getBoolean(Constants.PREFS_DARK_MODE,
                currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        darkMode.setChecked(useDarkMode);

        telemetry = findViewById(R.id.telemetry_switch);
        telemetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_TELEMETRY_ALLOWED, telemetry.isChecked());
                editor.apply();

                FirebaseAnalytics.getInstance(SettingsActivity.this)
                        .setAnalyticsCollectionEnabled(telemetry.isChecked());
            }
        });
        boolean allowTelemetry = preferences.getBoolean(Constants.PREFS_TELEMETRY_ALLOWED, true);
        telemetry.setChecked(allowTelemetry);

        Button deleteAccountButton = findViewById(R.id.logout_button);
        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CookieUtils.deleteCookies();
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(Constants.PREFS_USERNAME);
                editor.remove(Constants.PREFS_PASSWORD);
                editor.apply();

                CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
                Snackbar snackbar = Snackbar.make(coordinatorLayout,
                        getString(R.string.settings_logout_msg), Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        intervalTextview = findViewById(R.id.settings_interval_textview);
        updateIntervalTextView();

        View intervalRow = findViewById(R.id.settings_interval_row);
        intervalRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNotificationPrompt();
            }
        });

        final SwitchCompat scheduleChangeSwitch = findViewById(R.id.schedule_change_notification_switch);
        scheduleChangeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_SCHEDULE_CHANGE_NOTIFICATION, scheduleChangeSwitch.isChecked());
                editor.apply();
            }
        });
        scheduleChangeSwitch.setChecked(preferences.getBoolean(Constants.PREFS_SCHEDULE_CHANGE_NOTIFICATION, true));
    }

    @Override
    protected void onResume() {
        super.onResume();
        TelemetryUtils.getInstance().setCurrentScreen(this, "SettingsActivity");

        boolean syncLessons = preferences.getBoolean(Constants.PREFS_SYNC_CALENDAR, false);
        sync.setChecked(syncLessons);

        if (syncLessons && !hasCalendarPermissions()) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, false);
            editor.apply();

            sync.setChecked(false);
        }
        updateCurrentCalendar();
    }

    @Override
    protected void onPause() {
        TelemetryUtils.getInstance().setCurrentScreen(this, null);
        super.onPause();
    }

    private void updateCurrentCalendar() {
        long currentCalendar = preferences.getLong(Constants.PREFS_SYNC_CALENDAR_ID, -1);
        if (currentCalendar > -1) {
            if (hasCalendarPermissions()) {
                if (CalendarUtils.calendarExists(currentCalendar)) {
                    calendarNameTextView.setText(CalendarUtils.getCalendarNameById(currentCalendar));
                } else {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, false);
                    editor.apply();

                    sync.setChecked(false);
                }
            } else {
                calendarNameTextView.setText(getResources()
                        .getString(R.string.settings_calendar_no_permissions));
            }
        } else {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, false);
            editor.apply();

            sync.setChecked(false);
            calendarNameTextView.setText(R.string.settings_sync_calendar_description);
        }
    }

    private void updateIntervalTextView() {
        int interval = preferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                Constants.NOTIFICATION_TYPE_NOT_SET);
        if (interval == Constants.NOTIFICATION_TYPE_OFF) {
            intervalTextview.setText(getResources().getString(R.string.interval_off));
        } else if (interval != Constants.NOTIFICATION_TYPE_NOT_SET) {
            intervalTextview.setText(items[interval - 2]);
        }
    }

    private void updateLessonSwitch() {
        int interval = preferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                Constants.NOTIFICATION_TYPE_NOT_SET);
        lessonStart.setChecked(interval != Constants.NOTIFICATION_TYPE_NOT_SET &&
                interval != Constants.NOTIFICATION_TYPE_OFF);
    }

    private void createNotificationPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.settings_interval))
                .setSingleChoiceItems(items,
                        preferences.getInt(Constants.PREFS_NOTIFICATIONS_TYPE,
                                Constants.NOTIFICATION_TYPE_OFF) - 2,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                notificationId = item;
                            }
                        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                if (notificationId > -1) {
                    int id = notificationId + 2;
                    editor.putInt(Constants.PREFS_NOTIFICATIONS_TYPE, id);
                    editor.apply();

                    ApplicationLoader.restartNotificationThread();
                    updateLessonSwitch();
                    updateIntervalTextView();
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showWeekCountDialog() {
        int storedCount = preferences.getInt(Constants.PREFS_WEEK_COUNT,
                Constants.DEFAULT_WEEK_COUNT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.settings_week_count));

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_weeks_seekbar,
                (ViewGroup) findViewById(R.id.weeks_dialog));

        final TextView currentWeekCount = layout.findViewById(R.id.week_count_current);
        String weeks = getResources().getQuantityString(R.plurals.week_count, storedCount,
                storedCount);
        currentWeekCount.setText(getResources().getString(R.string.settings_week_count_current,
                weeks));

        final AppCompatSeekBar seekBar = layout.findViewById(R.id.week_count_seekbar);
        seekBar.setMax(Constants.MAX_WEEK_COUNT - 1);
        seekBar.setKeyProgressIncrement(1);
        seekBar.setProgress(storedCount - 1);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String weeks = getResources().getQuantityString(R.plurals.week_count,
                        i + 1, i + 1);
                currentWeekCount.setText(getResources()
                        .getString(R.string.settings_week_count_current, weeks));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int weekCount = seekBar.getProgress() + 1;

                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(Constants.PREFS_WEEK_COUNT, weekCount);
                editor.apply();

                Bundle bundle = new Bundle();
                bundle.putInt(Constants.TELEMETRY_PROPERTY_WEEK_COUNT, weekCount);
                TelemetryUtils.getInstance().logEvent(Constants.TELEMETRY_KEY_WEEK_COUNT_CHANGED,
                        bundle);

                TelemetryUtils.getInstance()
                        .setUserProperty(Constants.TELEMETRY_PROPERTY_WEEK_COUNT,
                                Integer.toString(weekCount));

                String weeks = getResources().getQuantityString(R.plurals.week_count, weekCount,
                        weekCount);
                weekCountTextView.setText(getResources()
                        .getString(R.string.settings_week_count_current, weeks));

                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private boolean hasCalendarPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestCalendarPermissions(boolean turnOnSync) {
        if (hasCalendarPermissions()) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                turnOnSync ? PERMISSIONS_REQUEST_WRITE_CALENDAR_TURN_ON_SYNC :
                        PERMISSIONS_REQUEST_WRITE_CALENDAR);
        showPermissionRequestSnackbar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_WRITE_CALENDAR || requestCode ==
                PERMISSIONS_REQUEST_WRITE_CALENDAR_TURN_ON_SYNC) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                updateCurrentCalendar();
            } else if (requestCode == PERMISSIONS_REQUEST_WRITE_CALENDAR_TURN_ON_SYNC) {
                updateCurrentCalendar();

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, true);
                editor.apply();

                sync.setChecked(true);
            }
        }
    }

    int checkedCalendarItem = 0;

    private void showCalendarDialog(final boolean turnOnSync) {
        final com.giovanniterlingen.windesheim.models.Calendar[] calendars =
                CalendarUtils.getCalendars();
        if (calendars == null) {
            return;
        }

        checkedCalendarItem = 0;
        long currentCalendarId = preferences.getLong(Constants.PREFS_SYNC_CALENDAR_ID, -1);

        CharSequence[] items = new CharSequence[calendars.length];
        for (int i = 0; i < calendars.length; i++) {
            if (calendars[i].getId() == currentCalendarId) {
                checkedCalendarItem = i;
            }
            items[i] = calendars[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.choose_calendar_title));
        builder.setSingleChoiceItems(items, checkedCalendarItem,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        checkedCalendarItem = item;
                    }
                });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(Constants.PREFS_SYNC_CALENDAR_ID,
                        calendars[checkedCalendarItem].getId());
                if (turnOnSync) {
                    editor.putBoolean(Constants.PREFS_SYNC_CALENDAR, true);
                }
                editor.commit();
                updateCurrentCalendar();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showPermissionRequestSnackbar() {
        CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinator_layout);
        Snackbar snackbar = Snackbar.make(coordinatorLayout, getResources()
                .getString(R.string.fix_calendar_permissions), Snackbar.LENGTH_SHORT);
        snackbar.setAction(R.string.fix, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        snackbar.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void restart() {
        Intent intent = new Intent(this, LaunchActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Runtime.getRuntime().exit(0);
    }
}
