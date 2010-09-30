/*
    Copyright 2008 San Jose State University
    
    This file is part of the Blackberry Cinequest client.

    The Blackberry Cinequest client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Blackberry Cinequest client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Blackberry Cinequest client.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.sjsu.cinequest.client;

import java.util.Calendar;


import net.rim.device.api.i18n.DateFormat;
import net.rim.device.api.i18n.SimpleDateFormat;
import net.rim.device.api.util.DateTimeUtilities;

/**
 * RIM-specific utilities for date handling
 * @author Cay Horstmann
 */
public class DateUtils
{
    private int[] fields = new int[7];
    private Calendar cal = Calendar.getInstance();
    public static final int NORMAL_MODE = 0;
    public static final int FESTIVAL_TEST_MODE = 1;
    public static final int OFFSEASON_TEST_MODE = 2;
    // TODO: Set to NORMAL_MODE before release
    private static int mode = FESTIVAL_TEST_MODE;
    
	private static String dvdLink = "http://www.cinequestonline.org/theater/detail_view.php";
	private static String ticketLink = "http://mobile.cinequest.org/event_view.php";

    private static String[] festivalDates =
    { 
    "2010-02-23", "2010-02-24", "2010-02-25", "2010-02-26", 
    "2010-02-27", "2010-02-28", "2010-03-01", "2010-03-02",
    "2010-03-03", "2010-03-04", "2010-03-05", "2010-03-06",
    "2010-03-07"
    };

    
    /**
     * Get all dates for this festival in the format yyyy-MM-dd
     * @return an array of all dates
     */
    public static String[] getFestivalDates()
    {
        return festivalDates;
    }
    
    /**
     * Set the festival dates
     * @param fdates a String[] containing each festival date (like "2009-02-26") in order.
     */
    public static void setFestivalDates(String[] fdates)
    {
    	festivalDates = fdates;
    }
 
    public static void setDvdLink(String link)
    {
    	DateUtils.dvdLink = link;
    }
    public static void setTicketLink(String link)
    {
    	DateUtils.ticketLink = link;
    }
    public static String getDvdLink()
    {
    	return dvdLink;
    }
    public static String getTicketLink()
    {
    	return ticketLink;
    }
    
    /**
     * Checks whether we are in off-season
     * @return if today's date indicates that we should display the app in off-season mode
     */
    public static boolean isOffSeason()
    {
    	//return season.equalsIgnoreCase("off-season");
        return today().compareTo(festivalDates[festivalDates.length - 1]) > 0;
    }
    
    /**
     * Sets the mode for date/time reporting
     * @param mode one of NORMAL_MODE, FESTIVAL_TEST_MODE, OFFSEASON_TEST_MODE
     */
    public static void setMode(int mode)
    {
        DateUtils.mode = mode;
    }
    
    /**
     * Gets the mode for date/time reporting
     * @return one of NORMAL_MODE, FESTIVAL_TEST_MODE, OFFSEASON_TEST_MODE
     */
    public static int getMode()
    {
        return mode;
    }
    
    /**
     * Parses a number inside a string, without error checking and without extracting a substring
     * @param str a string that contains a decimal number in str.substring(from, to)
     * @param from the starting index
     * @param to the past-the-end index
     * @return the integer that would be obtained by Integer.parseInt(str.substring(from, to))
     */
    private static int parseInt(String str, int from, int to)
    {
        int r = 0;
        for (int i = from; i < to; i++)
        {
            r = 10 * r + str.charAt(i) - '0';
        }
        return r;
    }

    /**
     * Formats a date string into a locale-specific version. (Note: This is not a static method for thread safety)
     * @param date a string in the format yyyy-MM-dd HH:mm or yyyy-MM-dd
     * one of the net.rim.device.api.i18n.DateFormat constants DATE_FULL, DATE_LONG, DATE_MEDIUM,
     * DATE_SHORT, DATE_DEFAULT, TIME_FULL, TIME_LONG, TIME_MEDIUM, TIME_SHORT,
     * TIME_DEFAULT, DATETIME_DEFAULT
     */
    public String format(String date, int style)
    {
        if (date == null) return "";
        fields[0] = parseInt(date, 0, 4);
        fields[1] = parseInt(date, 5, 7) - 1;
        fields[2] = parseInt(date, 8, 10);
        if (date.length() > 10)
        {
            fields[3] = parseInt(date, 11, 13);
            fields[4] = parseInt(date, 14, 16);
        }
        else
        {
            fields[3] = 0;
            fields[4] = 0;
        }
        DateTimeUtilities.setCalendarFields(cal, fields);
        return DateFormat.getInstance(style).format(cal);
    }
    
    /**
     * Returns today's date
     * @return today's date in yyyy-MM-dd format
     */
    public static String today()
    {
        if (mode == FESTIVAL_TEST_MODE) return festivalDates[5];
        if (mode == OFFSEASON_TEST_MODE) return "2099-12-31"; 
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.formatLocal(System.currentTimeMillis());
    }
}
