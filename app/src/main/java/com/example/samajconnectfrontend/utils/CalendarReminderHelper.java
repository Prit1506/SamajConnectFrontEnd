package com.example.samajconnectfrontend.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.samajconnectfrontend.models.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarReminderHelper {

    private static final String TAG = "CalendarReminderHelper";
    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 1001;

    private Context context;
    private OnReminderSetListener listener;

    public interface OnReminderSetListener {
        void onReminderSet(boolean success, String message);
        void onPermissionRequired();
    }

    public CalendarReminderHelper(Context context) {
        this.context = context;
    }

    public void setOnReminderSetListener(OnReminderSetListener listener) {
        this.listener = listener;
    }

    /**
     * Main method to set reminder for an event
     */
    public void setEventReminder(Event event) {
        if (!hasCalendarPermissions()) {
            requestCalendarPermissions();
            return;
        }

        // Option 1: Open calendar app with pre-filled event details
        openCalendarAppWithEvent(event);

        // Option 2: Directly insert into calendar (uncomment if you prefer this approach)
        // insertEventToCalendar(event);
    }

    /**
     * Opens the default calendar app with pre-filled event details
     */
    private void openCalendarAppWithEvent(Event event) {
        try {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);

            // Set event title
            String title = !TextUtils.isEmpty(event.getEventTitle()) ?
                    event.getEventTitle() : "Event Reminder";
            intent.putExtra(CalendarContract.Events.TITLE, title);

            // Set event description
            String description = buildEventDescription(event);
            intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

            // Set event location
            if (!TextUtils.isEmpty(event.getLocation())) {
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
            }

            // Parse and set event date/time
            long eventTimeMillis = parseEventDateTime(event.getEventDate());
            if (eventTimeMillis > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTimeMillis);
                // Set end time to 2 hours after start time (default duration)
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTimeMillis + (2 * 60 * 60 * 1000));
            }

            // Set reminder - 1 hour before event
            intent.putExtra(CalendarContract.Events.HAS_ALARM, true);

            // Set availability
            intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            // Start calendar activity
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);

                if (listener != null) {
                    listener.onReminderSet(true, "Calendar opened for reminder setup");
                }

                Toast.makeText(context, "Calendar opened. Please save the event to set reminder.",
                        Toast.LENGTH_LONG).show();
            } else {
                // No calendar app found
                if (listener != null) {
                    listener.onReminderSet(false, "No calendar app found");
                }
                Toast.makeText(context, "No calendar app found on device", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening calendar app", e);
            if (listener != null) {
                listener.onReminderSet(false, "Error opening calendar: " + e.getMessage());
            }
            Toast.makeText(context, "Error opening calendar app", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Directly inserts event into device calendar (alternative approach)
     */
    private void insertEventToCalendar(Event event) {
        try {
            ContentResolver cr = context.getContentResolver();
            ContentValues values = new ContentValues();

            // Event details
            String title = !TextUtils.isEmpty(event.getEventTitle()) ?
                    event.getEventTitle() : "Event Reminder";
            values.put(CalendarContract.Events.TITLE, title);

            String description = buildEventDescription(event);
            values.put(CalendarContract.Events.DESCRIPTION, description);

            if (!TextUtils.isEmpty(event.getLocation())) {
                values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
            }

            // Parse event date/time
            long eventTimeMillis = parseEventDateTime(event.getEventDate());
            if (eventTimeMillis <= 0) {
                Toast.makeText(context, "Invalid event date", Toast.LENGTH_SHORT).show();
                return;
            }

            values.put(CalendarContract.Events.DTSTART, eventTimeMillis);
            values.put(CalendarContract.Events.DTEND, eventTimeMillis + (2 * 60 * 60 * 1000)); // 2 hours duration

            // Set timezone
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            // Set calendar (use primary calendar)
            long calendarId = getPrimaryCalendarId();
            if (calendarId == -1) {
                Toast.makeText(context, "No calendar found", Toast.LENGTH_SHORT).show();
                return;
            }
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);

            // Other properties
            values.put(CalendarContract.Events.HAS_ALARM, 1);
            values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            // Insert event
            Uri eventUri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

            if (eventUri != null) {
                // Get the event ID
                long eventId = Long.parseLong(eventUri.getLastPathSegment());

                // Add reminder (1 hour before)
                addEventReminder(eventId, 60); // 60 minutes before

                if (listener != null) {
                    listener.onReminderSet(true, "Reminder set successfully");
                }
                Toast.makeText(context, "Event added to calendar with reminder", Toast.LENGTH_SHORT).show();
            } else {
                if (listener != null) {
                    listener.onReminderSet(false, "Failed to add event to calendar");
                }
                Toast.makeText(context, "Failed to add event to calendar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error inserting event to calendar", e);
            if (listener != null) {
                listener.onReminderSet(false, "Error: " + e.getMessage());
            }
            Toast.makeText(context, "Error adding event to calendar", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a reminder to an existing calendar event
     */
    private void addEventReminder(long eventId, int minutesBefore) {
        try {
            ContentResolver cr = context.getContentResolver();
            ContentValues reminderValues = new ContentValues();

            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.MINUTES, minutesBefore);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

        } catch (Exception e) {
            Log.e(TAG, "Error adding reminder", e);
        }
    }

    /**
     * Gets the primary calendar ID
     */
    private long getPrimaryCalendarId() {
        try {
            ContentResolver cr = context.getContentResolver();
            String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY};

            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String selection = CalendarContract.Calendars.VISIBLE + " = 1";

            android.database.Cursor cursor = cr.query(uri, projection, selection, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    int isPrimary = cursor.getInt(1);

                    if (isPrimary == 1) {
                        cursor.close();
                        return id;
                    }
                }

                // If no primary calendar, return the first one
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(0);
                    cursor.close();
                    return id;
                }

                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting calendar ID", e);
        }

        return -1;
    }

    /**
     * Builds event description from event details
     */
    private String buildEventDescription(Event event) {
        StringBuilder description = new StringBuilder();

        if (!TextUtils.isEmpty(event.getEventDescription())) {
            description.append(event.getEventDescription());
        }

        if (!TextUtils.isEmpty(event.getLocation())) {
            if (description.length() > 0) {
                description.append("\n\n");
            }
            description.append("Location: ").append(event.getLocation());
        }

        description.append("\n\nAdded from SamajConnect App");

        return description.toString();
    }

    /**
     * Parses event date string to milliseconds
     */
    private long parseEventDateTime(String dateString) {
        if (TextUtils.isEmpty(dateString)) {
            return 0;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + dateString, e);

            // Try alternative format
            try {
                SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = altFormat.parse(dateString);

                // Set time to 10:00 AM if only date is provided
                if (date != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.set(Calendar.HOUR_OF_DAY, 10);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    return calendar.getTimeInMillis();
                }
            } catch (ParseException e2) {
                Log.e(TAG, "Failed to parse alternative date format", e2);
            }
        }

        return 0;
    }

    /**
     * Checks if calendar permissions are granted
     */
    private boolean hasCalendarPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests calendar permissions
     */
    private void requestCalendarPermissions() {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR
                    },
                    CALENDAR_PERMISSION_REQUEST_CODE);
        }

        if (listener != null) {
            listener.onPermissionRequired();
        }
    }

    /**
     * Call this method from your Activity's onRequestPermissionsResult
     */
    public void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(context, "Calendar permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Calendar permissions required for reminder feature",
                        Toast.LENGTH_LONG).show();

                if (listener != null) {
                    listener.onReminderSet(false, "Calendar permissions denied");
                }
            }
        }
    }
}