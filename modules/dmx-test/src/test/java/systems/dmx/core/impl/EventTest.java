package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import static systems.dmx.datetime.Constants.*;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.storage.spi.DMXTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;



public class EventTest extends CoreServiceTestEnvironment {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------------- Tests

    @Test
    public void datetime() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineEventModel();
            createEvent();
            List<Topic> events = dmx.getTopicsByType("dmx.events.event");
            assertEquals(1, events.size());
            //
            ChildTopics event = events.get(0).getChildTopics();
            ChildTopics from = event.getChildTopics("dmx.datetime#dmx.datetime.from");
            ChildTopics date = from.getChildTopics(DATE);
            ChildTopics time = from.getChildTopics(TIME);
            assertEquals(7,    date.getInt(MONTH));
            assertEquals(1,    date.getInt(DAY));
            assertEquals(2020, date.getInt(YEAR));
            assertEquals(22,   time.getInt(HOUR));
            assertEquals(0,    time.getInt(MINUTE));
            //
            tx.success();
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void defineEventModel() {
        defineDateTimeModel();
        // Event
        dmx.createTopicType(mf.newTopicTypeModel("dmx.events.event_name", "Event Name", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.events.event",      "Event",      ENTITY)
          .addCompDef(mf.newCompDefModel(null, true, false, "dmx.events.event", "dmx.events.event_name", ONE))
          .addCompDef(mf.newCompDefModel(FROM, false, false, "dmx.events.event", DATETIME, ONE))
          .addCompDef(mf.newCompDefModel(TO,   false, false, "dmx.events.event", DATETIME, ONE))
        );
    }

    private void defineDateTimeModel() {
        // Date
        dmx.createTopicType(mf.newTopicTypeModel(MONTH, "Month", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(DAY,   "Day",   NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(YEAR,  "Year",  NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(DATE,  "Date",  VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, MONTH, ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, DAY,   ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, DATE, YEAR,  ONE))
        );
        // Time
        dmx.createTopicType(mf.newTopicTypeModel(HOUR,   "Hour",   NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(MINUTE, "Minute", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel(TIME,   "Time",   VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, TIME, HOUR,   ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, TIME, MINUTE, ONE))
        );
        // Date/Time
        dmx.createTopicType(mf.newTopicTypeModel(DATETIME, "Date/Time", VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, DATETIME, DATE, ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, DATETIME, TIME, ONE))
        );
        // From/To
        dmx.createAssocType(mf.newAssocTypeModel(FROM, "From", TEXT));
        dmx.createAssocType(mf.newAssocTypeModel(TO,   "To",   TEXT));
    }

    private Topic createEvent() {
        return dmx.createTopic(mf.newTopicModel("dmx.events.event", mf.newChildTopicsModel()
            .set("dmx.events.event_name", "Release Party")
            .set("dmx.datetime#dmx.datetime.from", mf.newChildTopicsModel()
                .set(DATE, mf.newChildTopicsModel()
                    .set(MONTH, 7)   // July
                    .set(DAY,   1)   // 1st
                    .set(YEAR,  2020)
                )
                .set(TIME, mf.newChildTopicsModel()
                    .set(HOUR,  22)
                    .set(MINUTE, 0)
                )
            )
        ));
    }
}
