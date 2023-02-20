package com.onyx.dailydiary.ical;

import static android.app.DownloadManager.Request.VISIBILITY_HIDDEN;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.fortuna.ical4j.data.CalendarBuilder;
//import net.fortuna.ical4j.filter.ComponentFilter;
import net.fortuna.ical4j.filter.Filter;
//import net.fortuna.ical4j.filter.FilterExpression;
import net.fortuna.ical4j.filter.PeriodRule;
//import net.fortuna.ical4j.filter.expression.DateExpression;
//import net.fortuna.ical4j.filter.predicate.PeriodRule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class iCalParser {
    private final String filepath = "Calendars";
    private final String filename = "calendar_list.txt";
    private static final String TAG = iCalParser.class.getSimpleName();
    ArrayList<ArrayList<String>> calendarList= new ArrayList<>();
    ArrayList<Calendar> iCalCalendars= new ArrayList<>();
    private Context context;

//    Calendar calendar = null;
    public iCalParser(Context context) {

        this.context = context;
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
        Log.d(TAG, "iCalParser");

    }

    public void loadCalendarList()
    {
        calendarList.clear();
        File calendarFile = new File(context.getExternalFilesDir(filepath), filename);

        if (calendarFile.exists())
        {
            FileInputStream is = null;
            try {
                is = new FileInputStream(calendarFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = null;
            try {
                line = reader.readLine();

                while(line != null){
                    String[] splitLine = line.split(",");
                    Log.d(TAG, String.valueOf(splitLine));

                    if (splitLine.length==3){
                        ArrayList<String> calendarLine = new ArrayList<>(3);
                        calendarLine.add(splitLine[0]);
                        calendarLine.add(splitLine[1]);
                        calendarLine.add(splitLine[2]);
                        calendarList.add(calendarLine);
                    }
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }
    }

    public void loadCalendars()
    {
        iCalCalendars.clear();
        loadCalendarList();

        for (int i = 0; i < calendarList.size(); i++) {
            String icsname = calendarList.get(i).get(0);

            String icsfilename = calendarList.get(i).get(2);
            Log.d(TAG, "Loading " + icsname);

            try {

                File file = new File(context.getExternalFilesDir(filepath), icsfilename);
                FileInputStream fin = new FileInputStream(file.getPath());
                CalendarBuilder builder = new CalendarBuilder();
                Calendar calendar = builder.build(fin);

                iCalCalendars.add(calendar);

            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }

    }


    public List<String> get_day_events(LocalDate inputDate){

        List<String> eventList = new ArrayList<>();
        int i = 1;
        for (Calendar calendar : iCalCalendars) {
            Log.d(TAG, "Loading events " + i);
            i++;

            try {
                DateTime selectedDate = new DateTime(Date.from(inputDate.atStartOfDay().plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()));

                Period period = new Period(selectedDate, new Dur(0, 23, 59, 0));
                Predicate<CalendarComponent> periodRule = new PeriodRule<>(period);
                Filter<CalendarComponent> filter = new Filter<>(new Predicate[]{periodRule}, Filter.MATCH_ANY);
                Collection<CalendarComponent> events = filter.filter(calendar.getComponents(CalendarComponent.VEVENT));


                DateTimeFormatter fullformatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                LocalDateTime eventTime;

                Iterator<CalendarComponent> ci = events.iterator();
                while (ci.hasNext()) {
                    Component c = ci.next();
                    try {
                        VEvent event = (VEvent) c;
                        String summary = event.getProperty("SUMMARY").getValue();
                        String eventTimeStr = event.getProperty("DtStart").getValue();

                        if (eventTimeStr.length() > 8) {
                            eventTime = LocalDateTime.parse(eventTimeStr, fullformatter);
                            if ((eventTime.getHour() + eventTime.getMinute()) != 0) {
                                String formattedTime = eventTime.format(timeFormatter);
                                summary = formattedTime + " " + summary;
                            }

                        }
                        eventList.add(summary);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());

                    }
                }

                // remove duplicates
                eventList = eventList.stream().distinct().collect(Collectors.toList());


            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
        return eventList;
    }
    public void sync_calendars(){

        loadCalendarList();
        Toast.makeText(context, "downloading calendars..", Toast.LENGTH_LONG).show();
        for (int i = 0; i < calendarList.size(); i++) {

            String icsfilename = calendarList.get(i).get(2);
            String icsurl = calendarList.get(i).get(1);
            String icsname = calendarList.get(i).get(0);
//            Log.d(TAG, "calendar sync " + icsname);


            try {
//                icsurl = "https://www.maths.gla.ac.uk/iCal/s/1/calendar.ics";
//                icsurl = "https://moodle.gla.ac.uk/calendar/export_execute.php?userid=80983&authtoken=59416297c2bacdcc6fddf0912f6832c7e01cb264&preset_what=all&preset_time=custom";
                DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri uri = Uri.parse(icsurl);
//                Log.d(TAG, "calendar sync " + icsurl);
//                Log.d(TAG, "calendar sync " + icsfilename);


                DownloadManager.Request request = new DownloadManager.Request(uri);
//                request.setNotificationVisibility(VISIBILITY_HIDDEN);
                request.setTitle(icsname);
                request.setDescription("Downloading");
//                request.setNotificationVisibility(VISIBILITY_VISIBLE);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(context, filepath, icsfilename);
                File calendarFile = new File(context.getExternalFilesDir(filepath), icsfilename);


                if (calendarFile.exists()) {
                    calendarFile.delete();
                }

                downloadmanager.enqueue(request);

            } catch (Exception e) {
                Toast.makeText(context, "Unable to download calendar " + icsname + ". Check url or internet connection.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, e.getMessage());

            }
        }
    }
}
