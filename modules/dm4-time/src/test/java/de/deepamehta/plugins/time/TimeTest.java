package de.deepamehta.plugins.time;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;



public class TimeTest {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Test
    public void format() {
        logger.info("### default locale: " + Locale.getDefault());
        //
        // available date format styles are SHORT, MEDIUM, LONG, FULL, and DEFAULT (= MEDIUM)
        Date d = new Date();
        logger.info("### " + DateFormat.getInstance().format(d));           // uses SHORT style for date and time
        logger.info("### " + DateFormat.getDateInstance().format(d));       // uses DEFAULT style for date
        logger.info("### " + DateFormat.getTimeInstance().format(d));       // uses DEFAULT style for time
        logger.info("### " + DateFormat.getDateTimeInstance().format(d));   // uses DEFAULT style for date and time
    }

    @Test
    public void englishLocale() {
        Date d = new Date();
        Locale l = Locale.ENGLISH;
        logger.info("### english locale: " + l);
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, l).format(d));
    }

    @Test
    public void germanLocale() {
        Date d = new Date();
        Locale l = Locale.GERMAN;
        logger.info("### german locale: " + l);
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, l).format(d));
        logger.info("### " + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, l).format(d));
    }

    @Test
    public void timezone() {
        Date d = new Date();
        Locale l = Locale.ENGLISH;
        TimeZone tz = TimeZone.getDefault();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, l);
        df.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        logger.info("### default time zone: " + tz);
        logger.info("### " + df.format(d));
    }

    @Test
    public void customFormat() {
        // this generates the format used in HTTP date/time headers, see:
        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        ((SimpleDateFormat) df).applyPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        logger.info("### " + df.format(new Date()));
    }
}
