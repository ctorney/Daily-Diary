package com.onyx.dailydiary;

import android.util.Log;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

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

public class iCalParser {
    private static final String FILE_URL = "https://outlook.office365.com/owa/calendar/b31a3be9550740d2bf5879823966be05@glasgow.ac.uk/2341bb6761e647058d8cb7a1a2fd4db417272045294428868729/calendar.ics";
    private static final String FILE_NAME = "calendar.ics";
    private String filepath = "DailyNotes";

    private static final String TAG = iCalParser.class.getSimpleName();


    public iCalParser() {

        Log.d(TAG, "iCalParser");


        try {
            Path icsfile = download(FILE_URL, filepath);
            FileInputStream fin = new FileInputStream("mycalendar.ics");
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(fin);
        }
        catch (IOException | ParserException e) {


        }

    }

    private static Path download(String sourceURL, String targetDirectory)
    {
        Path targetPath = null;
        try {
            URL url = new URL(sourceURL);
            String fileName = sourceURL.substring(sourceURL.lastIndexOf('/') + 1, sourceURL.length());
            targetPath = new File(targetDirectory + File.separator + fileName).toPath();
            Files.copy(url.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {

        }

        return targetPath;
    }
}
