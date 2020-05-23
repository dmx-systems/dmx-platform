package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
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
            ChildTopics date = from.getChildTopics("dmx.datetime.date");
            ChildTopics time = from.getChildTopics("dmx.datetime.time");
            assertEquals(7,    date.getInt("dmx.datetime.month"));
            assertEquals(1,    date.getInt("dmx.datetime.day"));
            assertEquals(2020, date.getInt("dmx.datetime.year"));
            assertEquals(22,   time.getInt("dmx.datetime.hour"));
            assertEquals(0,    time.getInt("dmx.datetime.minute"));
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
          .addCompDef(mf.newCompDefModel("dmx.datetime.from", false, false, "dmx.events.event", "dmx.datetime", ONE))
          .addCompDef(mf.newCompDefModel("dmx.datetime.to",   false, false, "dmx.events.event", "dmx.datetime", ONE))
        );
    }

    private void defineDateTimeModel() {
        // Date
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.month", "Month", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.day",   "Day",   NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.year",  "Year",  NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.date",  "Date",  VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime.date", "dmx.datetime.month", ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime.date", "dmx.datetime.day",   ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime.date", "dmx.datetime.year",  ONE))
        );
        // Time
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.hour",   "Hour",   NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.minute", "Minute", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime.time",   "Time",   VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime.time", "dmx.datetime.hour",   ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime.time", "dmx.datetime.minute", ONE))
        );
        // Date/Time
        dmx.createTopicType(mf.newTopicTypeModel("dmx.datetime", "Date/Time", VALUE)
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime", "dmx.datetime.date", ONE))
            .addCompDef(mf.newCompDefModel(null, false, true, "dmx.datetime", "dmx.datetime.time", ONE))
        );
        // From/To
        dmx.createAssocType(mf.newAssocTypeModel("dmx.datetime.from", "From", TEXT));
        dmx.createAssocType(mf.newAssocTypeModel("dmx.datetime.to",   "To",   TEXT));
    }

    private Topic createEvent() {
        return dmx.createTopic(mf.newTopicModel("dmx.events.event", mf.newChildTopicsModel()
            .set("dmx.events.event_name", "Release Party")
            .set("dmx.datetime#dmx.datetime.from", mf.newChildTopicsModel()
                .set("dmx.datetime.date", mf.newChildTopicsModel()
                    .set("dmx.datetime.month", 7)   // July
                    .set("dmx.datetime.day",   1)   // 1st
                    .set("dmx.datetime.year",  2020)
                )
                .set("dmx.datetime.time", mf.newChildTopicsModel()
                    .set("dmx.datetime.hour",  22)
                    .set("dmx.datetime.minute", 0)
                )
            )
        ));
    }
}
