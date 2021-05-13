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



public class DM5CoreServiceTest extends CoreServiceTestEnvironment {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ----------------------------------------------------------------------------------------------------------- Tests

    @Test
    public void addRefOnModel() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineLottoModel();
            Topic num1 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(23)));
            Topic num2 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(42)));
            //
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .addRef("lotto.number", num1.getId())
                .addRef("lotto.number", num2.getId())
            ));
        } finally {
            tx.finish();
        }
    }

    @Test
    public void addRefOnObject() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineLottoModel();
            Topic num1 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(23)));
            Topic num2 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(42)));
            //
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw"));
            draw.update(mf.newChildTopicsModel()
                .addRef("lotto.number", num1.getId())
                .addRef("lotto.number", num2.getId())
            );
        } finally {
            tx.finish();
        }
    }

    @Test
    public void addDeletionRef_int() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineLottoModel();
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw"));
            long drawId = draw.getId();
            draw.update(mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 42)
            );
            //
            draw = dmx.getTopic(drawId);
            List<RelatedTopic> numbers = draw.getChildTopics().getTopics("lotto.number");
            assertSame(2, numbers.size());
            assertSame(23, numbers.get(0).getSimpleValue().intValue());
            assertSame(42, numbers.get(1).getSimpleValue().intValue());
            //
            draw.update(mf.newChildTopicsModel().addDeletionRef("lotto.number", numbers.get(0).getId()));
            //
            draw = dmx.getTopic(drawId);
            numbers = draw.getChildTopics().getTopics("lotto.number");
            assertSame(1, numbers.size());
            assertSame(42, numbers.get(0).getSimpleValue().intValue());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void addDeletionRef_string() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineManyNamesEntityModel();
            String SIMPLE_NAME   = "simple.name";
            String SIMPLE_ENTITY = "simple.entity";
            //
            Topic entity = dmx.createTopic(mf.newTopicModel(SIMPLE_ENTITY));
            long entityId = entity.getId();
            entity.update(mf.newChildTopicsModel()
                .add(SIMPLE_NAME, "Alice")
                .add(SIMPLE_NAME, "Bob")
            );
            //
            entity = dmx.getTopic(entityId);
            List<RelatedTopic> names = entity.getChildTopics().getTopics(SIMPLE_NAME);
            assertEquals(2, names.size());
            assertEquals("Alice", names.get(0).getSimpleValue().toString());
            assertEquals("Bob", names.get(1).getSimpleValue().toString());
            //
            entity.update(mf.newChildTopicsModel().addDeletionRef(SIMPLE_NAME, names.get(0).getId()));
            //
            entity = dmx.getTopic(entityId);
            names = entity.getChildTopics().getTopics(SIMPLE_NAME);
            assertEquals(1, names.size());
            assertEquals("Bob", names.get(0).getSimpleValue().toString());
        } finally {
            tx.finish();
        }
    }

    // ---

    @Test
    public void compositeValue() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineValueLottoModel();
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 42)
            ));
            //
            List<RelatedTopic> numbers = draw.getChildTopics().getTopics("lotto.number");
            assertSame(2, numbers.size());
            assertSame(23, numbers.get(0).getSimpleValue().intValue());
            assertSame(42, numbers.get(1).getSimpleValue().intValue());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void compositeValueUnification1() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineValueLottoModel();
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 42)
            ));
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 12)
            ));
            //
            assertSame(2, dmx.getTopicsByType("lotto.draw").size());
            assertSame(3, dmx.getTopicsByType("lotto.number").size());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void compositeValueUnification2() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineValueLottoModel();
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 42)
            ));
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 42)
                .add("lotto.number", 23)
            ));
            //
            assertSame(1, dmx.getTopicsByType("lotto.draw").size());
            assertSame(2, dmx.getTopicsByType("lotto.number").size());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void compositeValueUnification3() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineValueLottoModel();
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 23)
                .add("lotto.number", 42)
                .add("lotto.number", 12)
            ));
            dmx.createTopic(mf.newTopicModel("lotto.draw", mf.newChildTopicsModel()
                .add("lotto.number", 42)
                .add("lotto.number", 23)
            ));
            //
            assertSame(2, dmx.getTopicsByType("lotto.draw").size());
            assertSame(3, dmx.getTopicsByType("lotto.number").size());
        } finally {
            tx.finish();
        }
    }

    // Author: Malte Reißig, revised by jri at 2020/04/26
    @Test
    public void childParentValueUpdate() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineSimpleNameEntityModel();
            String SIMPLE_NAME   = "simple.name";
            String SIMPLE_ENTITY = "simple.entity";
            // create composite topic
            Topic topic = dmx.createTopic(mf.newTopicModel(SIMPLE_ENTITY,
                mf.newChildTopicsModel().set(SIMPLE_NAME, "Test")
            ));
            assertEquals("Test", topic.getSimpleValue().toString());
            // update child
            topic.update(mf.newChildTopicsModel().set(SIMPLE_NAME, "Test Studio"));
            // both have changed, child value and parent value
            assertEquals("Test Studio", topic.getChildTopics().getString(SIMPLE_NAME));
            assertEquals("Test Studio", topic.getSimpleValue().toString());
        } finally {
            tx.finish();
        }
    }

    // Author: Malte Reißig, revised by jri at 2020/04/17
    @Test
    public void addChildTopicByValue() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineManyNamesEntityModel();
            String SIMPLE_NAME   = "simple.name";
            String SIMPLE_ENTITY = "simple.entity";
            // create composite topic
            Topic topic = dmx.createTopic(mf.newTopicModel(SIMPLE_ENTITY,
                mf.newChildTopicsModel().add(SIMPLE_NAME, "Text 1")
            ));
            // create simple topic
            Topic topic2 = dmx.createTopic(mf.newTopicModel(SIMPLE_NAME, new SimpleValue("Text 2")));
            // there are now 2 simple.name topics
            assertSame(2, dmx.getTopicsByType(SIMPLE_NAME).size());
            //
            // add child topic by-value
            topic.update(mf.newChildTopicsModel().add(SIMPLE_NAME, "Text 2"));
            // now the composite has 2 children -- the existing "Text 2" topic was identified (by-value) and reused
            List<RelatedTopic> children = topic.getChildTopics().getTopics(SIMPLE_NAME);
            assertEquals(2,              children.size());
            assertEquals(topic2.getId(), children.get(1).getId());
            // no further simple.name topic is created
            assertEquals(2, dmx.getTopicsByType(SIMPLE_NAME).size());
        } finally {
            tx.finish();
        }
    }

    // Author: Malte Reißig, revised by jri at 2020/04/17
    @Test
    public void addChildTopicByRef() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineManyNamesEntityModel();
            String SIMPLE_NAME   = "simple.name";
            String SIMPLE_ENTITY = "simple.entity";
            // create composite topic
            Topic topic = dmx.createTopic(mf.newTopicModel(SIMPLE_ENTITY,
                mf.newChildTopicsModel().add(SIMPLE_NAME, "Text 1")
            ));
            // create simple topic
            Topic topic2 = dmx.createTopic(mf.newTopicModel(SIMPLE_NAME, new SimpleValue("Text 2")));
            // there are now 2 simple.name topics
            assertEquals(2, dmx.getTopicsByType(SIMPLE_NAME).size());
            //
            // add child topic by-ref
            topic.update(mf.newChildTopicsModel().addRef(SIMPLE_NAME, topic2.getId()));
            // now the composite has 2 children
            List<RelatedTopic> children = topic.getChildTopics().getTopics(SIMPLE_NAME);
            assertEquals(2, children.size());
            assertEquals("Text 1", children.get(0).getSimpleValue().toString());
            assertEquals("Text 2", children.get(1).getSimpleValue().toString());
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void defineLottoModel() {
        dmx.createTopicType(mf.newTopicTypeModel("lotto.number", "Lotto Number", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("lotto.draw", "Lotto Draw", ENTITY)
            .addCompDef(mf.newCompDefModel(
                "lotto.draw", "lotto.number", MANY
            ))
        );
    }

    private void defineValueLottoModel() {
        dmx.createTopicType(mf.newTopicTypeModel("lotto.number", "Lotto Number", NUMBER));
        dmx.createTopicType(mf.newTopicTypeModel("lotto.draw", "Lotto Draw", VALUE)
            .addCompDef(mf.newCompDefModel(
                "lotto.draw", "lotto.number", MANY
            ))
        );
    }

    // ---

    private void defineSimpleNameEntityModel() {
        dmx.createTopicType(mf.newTopicTypeModel("simple.name", "Simple Name", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("simple.entity", "Simple Entity", ENTITY)
            .addCompDef(mf.newCompDefModel(
                "simple.entity", "simple.name", ONE
            ))
        );
    }

    private void defineManyNamesEntityModel() {
        dmx.createTopicType(mf.newTopicTypeModel("simple.name", "Simple Name", TEXT));
        dmx.createTopicType(mf.newTopicTypeModel("simple.entity", "Simple Entity", ENTITY)
            .addCompDef(mf.newCompDefModel(
                "simple.entity", "simple.name", MANY
            ))
        );
    }
}
