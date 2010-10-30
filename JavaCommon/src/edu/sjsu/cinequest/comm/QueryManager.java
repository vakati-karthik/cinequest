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

package edu.sjsu.cinequest.comm;

import java.util.Hashtable;
import java.util.Vector;

import android.util.Log;

import edu.sjsu.cinequest.comm.cinequestitem.Film;
import edu.sjsu.cinequest.comm.cinequestitem.Schedule;
import edu.sjsu.cinequest.comm.cinequestitem.UserSchedule;
import edu.sjsu.cinequest.comm.xmlparser.DatesParser;
import edu.sjsu.cinequest.comm.xmlparser.EventsParser;
import edu.sjsu.cinequest.comm.xmlparser.FilmParser;
import edu.sjsu.cinequest.comm.xmlparser.FilmsParser;
import edu.sjsu.cinequest.comm.xmlparser.GenresParser;
import edu.sjsu.cinequest.comm.xmlparser.LinkParser;
import edu.sjsu.cinequest.comm.xmlparser.ProgramItemParser;
import edu.sjsu.cinequest.comm.xmlparser.SchedulesParser;
import edu.sjsu.cinequest.comm.xmlparser.SeasonParser;
import edu.sjsu.cinequest.comm.xmlparser.SectionsParser;
import edu.sjsu.cinequest.comm.xmlparser.UserScheduleParser;
import edu.sjsu.cinequest.comm.xmlparser.VenuesParser;

/**
 * @author Kevin Ross (cs160_109)
 */
public class QueryManager
{
    private static final String queryBase = "mobile.cinequest.org/mobileCQ.php";
    private static final String[] queries =
        { "?type=program_item&id=", // 0
    			"?type=mode", // 1
                "?type=film&id=", // 2
                "?type=programs", // 3
                "?type=schedule&id=", // 4 -- unused
                //"?type=schedules", // -- unused
                //"?type=venue&id=", // -- unused
                "?type=blink&b=prg", // 5
                "?type=blink&b=dvd", // 6
                "?type=venues", // 7
                "?type=genres", // 8
                "?type=schedules&day=", // 9
                "?type=fdates", // 10
                "?type=dvds", // 11
                "?type=dvd&id=", // 12
                "?type=films&genre=", // 13
                "?type=xml&name=", // 14
                "?type=xml&name=items&id=", // 15
                "?type=schedules" //16 films by date
        };
    private static final String imageBase = "http://mobile.cinequest.org/";
    private static final String mainImageURL = "imgs/mobile/creative.gif";
    public static final String registrationURL = "http://mobile.cinequest.org/isch_reg.php";
    
    private String makeQuery(int type, String arg)
    {
        String query = "http://" + queryBase + queries[type] + arg;
        Platform.getInstance().log(query);
        return query;
    }

    private String makeQuery(int type, int arg)
    {
        return makeQuery(type, "" + arg);
    }

    public interface Callable
    {
        Object run() throws Throwable;
    }

    public void getWebData(final Callback callback, final Callable task)
    {
        if (callback == null || task == null)
            throw new NullPointerException();
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Object result = task.run();
                    Platform.getInstance().invoke(callback, result);
                }
                catch (Throwable e)
                {
                    Platform.getInstance().failure(callback, e);
                }
            }
        });
        t.start();
    }

    public void getDVDs(final Callback callback)
    {
    	getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return FilmsParser.parse(makeQuery(11, ""), callback);
            }
        });
    }
    
    public void getDVD(final int id, final Callback callback)
    {
    	getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
            	Film f = FilmParser.parseFilm(makeQuery(12, id), callback);
            	f.setDownload(true);
                return f;
            }
        });
    }

    public void getGenres(final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return GenresParser.parse(makeQuery(8, ""), callback);
            }
        });
    }

    public void getFilm(final int id, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return FilmParser.parseFilm(makeQuery(2, id), callback);
            }
        });
    }

    /**
     * Gets all special events of a given type
     * @param type either "forums" or "special_events"
     * @param callback returns a vector of Schedule items
     */
    public void getEventSchedules(final String type, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return EventsParser.parseEvents(makeQuery(14, "events"), type, callback);
            }
        });
    }    
    
    public void getSchedulesDay(final String date, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                Vector result = SchedulesParser.parseSchedule(makeQuery(9, date), callback);
                Vector events = EventsParser.parseEvents(makeQuery(14, "events"), null, callback);
                add(result, events, date);
                return result;
            }
        });
    }
    
    private static void add(Vector schedules, Vector events, String date)
    {
        for (int i = events.size() - 1; i >= 0; i--)
        {
            Schedule evt = (Schedule) events.elementAt(i);
            evt.setSpecialItem(true);
            String evtTime = evt.getStartTime(); 
            if (evtTime.startsWith(date))
            {
                boolean done = false;
                for (int j = schedules.size() - 1; !done && j >= 0; j--)
                {
                    Schedule sched = (Schedule) schedules.elementAt(j);
                    if (evt.getItemId() == sched.getItemId())
                    {   
                        schedules.setElementAt(evt, j); // Favor the special item
                        done = true;
                    }
                    else
                    {
                        String schedTime = sched.getStartTime(); 
                        int offset = date.length() + 1;
                        if (evtTime.substring(offset).compareTo(schedTime.substring(offset)) > 0)
                        {
                            schedules.insertElementAt(evt, j + 1);
                            done = true;
                        }
                    }
                }
                if (!done) schedules.insertElementAt(evt, 0);
            }
        }
    }

    public void getVenues(final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return VenuesParser.parse(makeQuery(7, ""), callback);
            }
        });
    }
    
    public void getAllFilms(final Callback callback)
    {
    	Log.i("block","in queryManager's get all films");
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return FilmsParser.parse(makeQuery(3, ""), callback);
            }
        });
    }
    
    public void getScheduls(final Callback callback)
    {
    	Log.i("block","in queryManager's get schedules");
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return SchedulesParser.parseSchedule(makeQuery(16, ""), callback);
            }
        });
    }
    
    public void getFilmsByGenre(final String genre, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return FilmsParser.parse(makeQuery(13, genre), callback);
            }
        });
    }
        
    /**
     * Gets a special screen as a vector of Section objects  
     * @param type one of "home", "info", "offseason", "offseasoninfo", "release", "pick"
     * @param callback returns the result
     */
    public void getSpecialScreen(final String type, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return SectionsParser.parse(makeQuery(14, type), callback);
            }
        });
    }
    
    /**
     * A method to get the operating mode (on or off season) from Cinequest's servers.
     * @param callback returns the result
     */
    public void getSeasonMode(final Callback callback)
    {
    	getWebData(callback, new Callable()
    	{
    		public Object run() throws Throwable
    		{
    			return SeasonParser.parse(makeQuery(1, ""), callback);
    		}
    	});
    }
    
    /**
     * A method to get the festival dates from Cinequest.
     * @param callback returns the result
     */
    public void getFestivalDates(final Callback callback)
    {
    	getWebData(callback, new Callable()
    	{
    		public Object run() throws Throwable
    		{
    			return DatesParser.parse(makeQuery(10, ""), callback);
    		}
    	});
    }
    
    /**
     * Gets a program item 
     * @param id the item ID
     * @param callback returns the result
     */
    public void getProgramItem(final int id, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return ProgramItemParser.parseProgramItem(makeQuery(0, "" + id), callback);                
            }
        });             
    }

    /**
     * Gets a "mobile" item (i.e. a kind of event that is not in the database but described by an external XML file)
     * @param id the item ID
     * @param callback returns the result
     */
    public void getMobileItem(final int id, final Callback callback)
    {
        getWebData(callback, new Callable()
        {
            public Object run() throws Throwable
            {
                return ProgramItemParser.parseProgramItem(makeQuery(15, id), callback);                
            }
        });             
    }
    
    public void getDvdLink(final Callback callback)
    {
    	getWebData(callback, new Callable()
    	{
    		public Object run() throws Throwable
    		{
    			return LinkParser.parse(makeQuery(6, ""), callback);
    		}
    	});
    }
    
    public void getTicketLink(final Callback callback)
    {
    	getWebData(callback, new Callable()
    	{
    		public Object run() throws Throwable
    		{
    			return LinkParser.parse(makeQuery(5, ""), callback);
    		}
    	});
    }
    
    public void getSchedule(final Callback callback, final String email, final String password)
    {
       getWebData(callback,  
             new Callable()
             {         
                public Object run() throws Throwable
                {
                   Hashtable postData = new Hashtable();
                   postData.put("type", "SLGET");
                   postData.put("username", email);
                   postData.put("password", password);
                   return UserScheduleParser.parseSchedule("https://" + queryBase, postData, callback);
                }               
             });
    }
    
    public void saveSchedule(final Callback callback, final String email, final String password, final UserSchedule schedule)
    {
       getWebData(callback,  
             new Callable()
             {         
                public Object run() throws Throwable
                {
                   Hashtable postData = new Hashtable();
                   postData.put("type", "SLPUT");
                   postData.put("username", email);
                   postData.put("password", password);
                   postData.put("lastChanged", schedule.getLastChanged());
                   postData.put("items", schedule.getIds());
                   return UserScheduleParser.parseSchedule("https://" + queryBase, postData, callback);
                }                      
             });
    }
    
    /**
     * Resolves a relative image URL
     * @param url the relative URL to resolve
     * @return the absolute URL for fetching the image
     */
    public String resolveRelativeImageURL(String url)
    {
        if (url.startsWith("http://")) return url;
        else return imageBase + url;  
    }
    
    /**
     * Gets the URL for the main image (in the entry screen)
     */
    public String getMainImageURL()
    {
        return resolveRelativeImageURL(mainImageURL);
    }
}
