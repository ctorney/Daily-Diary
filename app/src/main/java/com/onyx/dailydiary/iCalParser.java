package com.onyx.dailydiary;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
//import net.fortuna.ical4j.filter.ComponentFilter;
import net.fortuna.ical4j.filter.Filter;
//import net.fortuna.ical4j.filter.FilterExpression;
import net.fortuna.ical4j.filter.PeriodRule;
//import net.fortuna.ical4j.filter.expression.DateExpression;
//import net.fortuna.ical4j.filter.predicate.PeriodRule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.util.MapTimeZoneCache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class iCalParser {
    private static final String icsurl = "https://outlook.office365.com/owa/calendar/b31a3be9550740d2bf5879823966be05@glasgow.ac.uk/2341bb6761e647058d8cb7a1a2fd4db417272045294428868729/calendar.ics";
    private static final String icsfilename = "calendar.ics";
    private String filepath = "DailyNotes";

    private static final String TAG = iCalParser.class.getSimpleName();

    private Context context;

    Calendar calendar = null;
    public iCalParser(Context context) {

        this.context = context;
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
        Log.d(TAG, "iCalParser");

    }



    public void parse_file()
    {
//

        try {

            File file = new File(context.getExternalFilesDir(filepath), icsfilename);

            FileInputStream fin = new FileInputStream(file.getPath());

            CalendarBuilder builder = new CalendarBuilder();
//                    URL url = new URL(icsurl);

            calendar = builder.build(fin);

//            java.util.Calendar today = java.util.Calendar.getInstance();
//            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
//            today.clear(java.util.Calendar.MINUTE);
//            today.set(java.util.Calendar.SECOND, 1);
            //today.clear(java.util.Calendar.SECOND);

// create a period starting now with a duration of one (1) day..
//            Period period = new Period(new DateTime(today.getTime()), new Dur(1, 0, 0, 0));
//            Predicate<VEvent> filter = new FilterExpression().equalTo('organizer', 'Mailto:B@example.com').toComponentPredicate();
//            List<VEvent> events = calendar.getComponents("vevent").stream().filter(filter).collect(Collectors.toList());
//            FilterExpression aa = DateExpression() FilterExpression.greaterThan("DtStart", new DateTime(today.getTime()));

            ;
//            LocalDate inputDate = LocalDate.now();
//            String startDate = selectedDate.format(DateTimeForma
//            tter.ofPattern("yyyyMMdd")) + "T000000";
//            String endDate = selectedDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T235959";
//            Log.d(TAG, startDate);
//
//            LocalTime midnight = LocalTime.MIDNIGHT;
//            LocalDate today2 = LocalDate.now();
//            LocalDateTime todayMidnight = LocalDateTime.of(today2, midnight);
//            LocalDateTime tomorrowMidnight = todayMidnight.plusDays(1);
//            Log.d(TAG, String.valueOf(todayMidnight));
//            Log.d(TAG, String.valueOf(tomorrowMidnight));

//            Predicate startPredicate = new ComponentFilter().predicate(FilterExpression.not(FilterExpression.lessThanEqual("DtStart", startDate)));

//            Predicate predicate = new ComponentFilter().predicate(FilterExpression.greaterThan("DtStart", todayMidnight));//.and(startPredicate);


//            Stream components = calendar.getComponents(Component.VEVENT).stream().filter(predicate);
//            Iterator<Component> ci = components.iterator();

//            DtStart
//                    DtEnd
//            VEvent event = (VEvent) c;
//            event.gets

//
//            Predicate<VEvent> filter = FilterExpression.parse("dtstart > now() and dtstart <= endOfDay(+P1W)").toComponentPredicate();
//            List<VEvent> events = calendar.getComponents("vevent").stream().filter(filter).collect(Collectors.toList());
//
//
//            Filter filter = new Filter(new PeriodRule(period));
//
//            List eventsToday = filter.filter(calendar.getComponents(Component.VEVENT));
//// filter events in the next week..
//            Predicate<VEvent> filter = FilterExpression.parse("dtstart > now() and dtstart <= endOfDay(+P1W)").toComponentPredicate();
//            List<VEvent> events = calendar.getComponents("vevent").stream().filter(filter).collect(Collectors.toList());
//
//            ComponentList components = calendar.getComponents();

        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(),Toast.LENGTH_LONG).show();
            Log.d(TAG, e.getMessage());
        }

    }


//
//        try {
//            Path icsfile = download(FILE_URL, filepath);
//            FileInputStream fin = new FileInputStream("mycalendar.ics");
//            CalendarBuilder builder = new CalendarBuilder();
//            Calendar calendar = builder.build(fin);
//        }
//        catch (IOException | ParserException e) {
//
//
//        }



//    private static Path download(String sourceURL, String targetDirectory)
//    {
//        Path targetPath = null;
//        try {
//            URL url = new URL(sourceURL);
//            String fileName = sourceURL.substring(sourceURL.lastIndexOf('/') + 1, sourceURL.length());
//            targetPath = new File(targetDirectory + File.separator + fileName).toPath();
//            Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//        }
//        catch (IOException e)
//        {
//
//        }
//
//        return targetPath;
//    }

    public List<String> get_day_events(LocalDate inputDate){

        List<String> eventList = new ArrayList<>();
        if (calendar==null)
            return eventList;
        try{
            DateTime selectedDate = new DateTime(Date.from(inputDate.atStartOfDay().plusSeconds(1).atZone(ZoneId.systemDefault()).toInstant()));

            Period period = new Period(selectedDate, new Dur(0, 23, 59, 0));
            Predicate<CalendarComponent> periodRule = new PeriodRule<>(period);
            Filter<CalendarComponent> filter = new Filter<>(new Predicate[] {periodRule}, Filter.MATCH_ANY);
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
                }
                catch (Exception e) {
                    Log.d(TAG, e.getMessage());

                }
            }

            // remove duplicates
            eventList = eventList.stream().distinct().collect(Collectors.toList());


        }
        catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return eventList;
    }
    public void sync_calendars(){
        try {
//
            Toast.makeText(context, "downloading..",
                    Toast.LENGTH_LONG).show();
//
            DownloadManager downloadmanager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(icsurl);

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("My File");
            request.setDescription("Downloading");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//                    request.setVisibleInDownloadsUi(false);
            //request.setDestinationUri(Uri.parse("file://" + icsfilename));
            request.setDestinationInExternalFilesDir(context, filepath, icsfilename);
            downloadmanager.enqueue(request);


                    /* URL website = new URL(icsurl);
                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());


                    File toFile = new File(getExternalFilesDir(filepath), icsfilename);
                    Toast.makeText(MainActivity.this, "making file..",
                            Toast.LENGTH_LONG).show();
                    FileOutputStream fos = new FileOutputStream(toFile);
                    Toast.makeText(MainActivity.this, "copying..",
                            Toast.LENGTH_LONG).show();
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                    Toast.makeText(MainActivity.this, "closing..",
                            Toast.LENGTH_LONG).show();
                    fos.close();
                    rbc.close(); */
            Toast.makeText(context, "done.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
