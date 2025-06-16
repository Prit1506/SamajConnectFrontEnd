package com.example.samajconnectfrontend.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.samajconnectfrontend.models.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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

    public void setEventReminder(Event event) {
        if (!hasCalendarPermissions()) {
            requestCalendarPermissions();
            return;
        }

        // Check if calendar apps are available
        if (!isCalendarAppAvailable()) {
            // Fallback to direct calendar intent
            setReminderWithIntent(event);
            return;
        }

        // Try to add to device calendar first
        if (addToDeviceCalendar(event)) {
            if (listener != null) {
                listener.onReminderSet(true, "Reminder set successfully");
            }
            Toast.makeText(context, "Event added to calendar", Toast.LENGTH_SHORT).show();
        } else {
            // Fallback to calendar intent
            setReminderWithIntent(event);
        }
    }

    private boolean hasCalendarPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCalendarPermissions() {
        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    CALENDAR_PERMISSION_REQUEST_CODE);
        }

        if (listener != null) {
            listener.onPermissionRequired();
        }
    }

    private boolean isCalendarAppAvailable() {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);

        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        return !resolveInfos.isEmpty();
    }

    private boolean addToDeviceCalendar(Event event) {
        try {
            ContentResolver contentResolver = context.getContentResolver();

            // Parse event date
            Date eventDate = parseEventDate(event.getEventDate());
            if (eventDate == null) {
                Log.e(TAG, "Could not parse event date: " + event.getEventDate());
                return false;
            }

            // Create calendar event
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, eventDate.getTime());
            values.put(CalendarContract.Events.DTEND, eventDate.getTime() + (2 * 60 * 60 * 1000)); // 2 hours duration
            values.put(CalendarContract.Events.TITLE, event.getEventTitle());
            values.put(CalendarContract.Events.DESCRIPTION, event.getDescription());
            values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
            values.put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId());
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            Uri uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values);

            if (uri != null) {
                // Add reminder (15 minutes before)
                long eventId = Long.parseLong(uri.getLastPathSegment());
                addReminderToEvent(eventId, 15); // 15 minutes before

                Log.d(TAG, "Event added to calendar with ID: " + eventId);
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding event to calendar", e);
        }

        return false;
    }

    private void addReminderToEvent(long eventId, int minutesBefore) {
        try {
            ContentResolver contentResolver = context.getContentResolver();

            ContentValues reminderValues = new ContentValues();
            reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventId);
            reminderValues.put(CalendarContract.Reminders.MINUTES, minutesBefore);
            reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);
            Log.d(TAG, "Reminder added for event " + eventId + " - " + minutesBefore + " minutes before");

        } catch (Exception e) {
            Log.e(TAG, "Error adding reminder", e);
        }
    }

    private long getDefaultCalendarId() {
        // For simplicity, return 1 (usually the primary calendar)
        // In a production app, you should query for available calendars
        return 1;
    }

    private void setReminderWithIntent(Event event) {
        try {
            Date eventDate = parseEventDate(event.getEventDate());
            if (eventDate == null) {
                Toast.makeText(context, "Invalid event date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create calendar intent
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventDate.getTime());
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventDate.getTime() + (2 * 60 * 60 * 1000)); // 2 hours
            intent.putExtra(CalendarContract.Events.TITLE, event.getEventTitle());
            intent.putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription());
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
            intent.putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

            // Check if there's an app to handle this intent
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);

                if (listener != null) {
                    listener.onReminderSet(true, "Opening calendar app to set reminder");
                }
                Toast.makeText(context, "Opening calendar to set reminder", Toast.LENGTH_SHORT).show();
            } else {
                // Final fallback - try to open any calendar app
                openCalendarApp(event);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating calendar intent", e);
            Toast.makeText(context, "Error setting reminder", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onReminderSet(false, "Error setting reminder");
            }
        }
    }

    private void openCalendarApp(Event event) {
        try {
            // Try to open Google Calendar specifically
            Intent googleCalendarIntent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.calendar");
            if (googleCalendarIntent != null) {
                context.startActivity(googleCalendarIntent);
                Toast.makeText(context, "Please manually add the event to your calendar", Toast.LENGTH_LONG).show();
                return;
            }

            // Try generic calendar intent
            Intent calendarIntent = new Intent(Intent.ACTION_MAIN);
            calendarIntent.addCategory(Intent.CATEGORY_APP_CALENDAR);

            if (calendarIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(calendarIntent);
                Toast.makeText(context, "Please manually add the event to your calendar", Toast.LENGTH_LONG).show();
            } else {
                // Last resort - web calendar
                openWebCalendar(event);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening calendar app", e);
            Toast.makeText(context, "No calendar app found. Please install Google Calendar or any calendar app.", Toast.LENGTH_LONG).show();

            if (listener != null) {
                listener.onReminderSet(false, "No calendar app found");
            }
        }
    }

    private void openWebCalendar(Event event) {
        try {
            // Create Google Calendar web URL
            Date eventDate = parseEventDate(event.getEventDate());
            if (eventDate == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(eventDate);

            String dateString = String.format(Locale.US, "%04d%02d%02dT%02d%02d%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND));

            String url = "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                    "&text=" + Uri.encode(event.getEventTitle()) +
                    "&dates=" + dateString + "/" + dateString +
                    "&details=" + Uri.encode(event.getDescription()) +
                    "&location=" + Uri.encode(event.getLocation());

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(browserIntent);

            Toast.makeText(context, "Opening web calendar to set reminder", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onReminderSet(true, "Opening web calendar");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening web calendar", e);
            Toast.makeText(context, "Unable to set reminder", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onReminderSet(false, "Unable to set reminder");
            }
        }
    }

    private Date parseEventDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return format.parse(dateString);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);

            // Try alternative formats
            String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy-MM-dd",
                    "dd/MM/yyyy HH:mm:ss",
                    "dd/MM/yyyy HH:mm",
                    "dd/MM/yyyy"
            };

            for (String formatStr : formats) {
                try {
                    SimpleDateFormat altFormat = new SimpleDateFormat(formatStr, Locale.getDefault());
                    return altFormat.parse(dateString);
                } catch (ParseException ignored) {
                    // Continue to next format
                }
            }
        }

        return null;
    }

    // Method to check if Google Calendar is installed
    public boolean isGoogleCalendarInstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.google.android.calendar", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Method to install Google Calendar
    public void promptInstallGoogleCalendar() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.google.android.calendar"));

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            // Fallback to web browser
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.calendar"));
            context.startActivity(intent);
        }
    }
}