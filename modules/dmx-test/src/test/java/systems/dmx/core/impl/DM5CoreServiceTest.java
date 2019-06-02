package systems.dmx.core.impl;

import systems.dmx.core.Association;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.CompDef;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedAssociation;
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

    private Logger logger = Logger.getLogger(getClass().getName());

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
            draw.getChildTopics()
                .addRef("lotto.number", num1.getId())
                .addRef("lotto.number", num2.getId());
        } finally {
            tx.finish();
        }
    }

    @Test
    public void addDeletionRef() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineLottoModel();
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw"));
            long drawId = draw.getId();
            draw.getChildTopics()
                .add("lotto.number", 23)
                .add("lotto.number", 42);
            //
            draw = dmx.getTopic(drawId);
            List<RelatedTopic> numbers = draw.getChildTopics().getTopics("lotto.number");
            assertSame(2, numbers.size());
            assertSame(23, numbers.get(0).getSimpleValue().intValue());
            assertSame(42, numbers.get(1).getSimpleValue().intValue());
            //
            draw.getChildTopics().addDeletionRef("lotto.number", numbers.get(0).getId());
            //
            draw = dmx.getTopic(drawId);
            numbers = draw.getChildTopics().getTopics("lotto.number");
            assertSame(1, numbers.size());
            assertSame(42, numbers.get(0).getSimpleValue().intValue());
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

    // Author: Malte Reißig
    @Test
    public void childParentUpdate() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineSimpleNameIdentityModel();
            String fullAssocDefUriToBeUpdated = "simple.name";
            // create composite topic
            ChildTopicsModel cm1 = mf.newChildTopicsModel().put(fullAssocDefUriToBeUpdated, "Test");
            Topic topic = dmx.createTopic(mf.newTopicModel("simple.entity", cm1));
            assertTrue("Test".equals(topic.getSimpleValue().toString()));
            // updating simple child text topic
            TopicType parentType = dmx.getTopicType(topic.getTypeUri());
            CompDef typeRelation = parentType.getAssocDef(fullAssocDefUriToBeUpdated);
            ChildTopicsModel cm2 = mf.newChildTopicsModel().put(fullAssocDefUriToBeUpdated, "Test Studio");
            topic.updateChildTopics(cm2, typeRelation);
            // assert child topic value and parent value update
            assertTrue("Test Studio".equals(topic.getChildTopics().getString(fullAssocDefUriToBeUpdated)));
            assertTrue("Test Studio".equals(topic.getSimpleValue().toString()));
        } finally {
            tx.finish();
        }
    }

    // Author: Malte Reißig
    @Test
    public void addChildTopics() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineManyNamesIdentityModel();
            String fullAssocDefUriToBeUpdated = "simple.name";
            ChildTopicsModel ctm = mf.newChildTopicsModel().add(fullAssocDefUriToBeUpdated,
                    mf.newTopicModel("simple.name", new SimpleValue("Text 1")));
            Topic topic = dmx.createTopic(mf.newTopicModel("simple.entity", ctm));
            Topic futureChild = dmx.createTopic(mf.newTopicModel("simple.name", new SimpleValue("Text 2")));
            topic.getChildTopics().add(fullAssocDefUriToBeUpdated, futureChild.getModel());
        } finally {
            tx.finish();
        }
    }

    // Author: Malte Reißig
    @Test
    public void addRefChildTopics() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineManyNamesIdentityModel();
            String fullAssocDefUriToBeUpdated = "simple.name";
            ChildTopicsModel ctm = mf.newChildTopicsModel().add(fullAssocDefUriToBeUpdated,
                    mf.newTopicModel("simple.name", new SimpleValue("Text 1")));
            Topic topic = dmx.createTopic(mf.newTopicModel("simple.entity", ctm));
            Topic futureChild = dmx.createTopic(mf.newTopicModel("simple.name", new SimpleValue("Text 2")));
            topic.getChildTopics().addRef(fullAssocDefUriToBeUpdated, futureChild.getId());
            List<RelatedTopic> childs = topic.getChildTopics().getTopics(fullAssocDefUriToBeUpdated);
            assertSame(2, childs.size());
            assertEquals("Text 1", childs.get(0).getSimpleValue().toString());
            assertEquals("Text 2", childs.get(1).getSimpleValue().toString());
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void defineLottoModel() {
        dmx.createTopicType(mf.newTopicTypeModel("lotto.number", "Lotto Number", "dmx.core.number"));
        dmx.createTopicType(mf.newTopicTypeModel("lotto.draw", "Lotto Draw", "dmx.core.identity")
            .addAssocDef(mf.newCompDefModel(
                "lotto.draw", "lotto.number", "dmx.core.many"
            ))
        );
    }

    private void defineValueLottoModel() {
        dmx.createTopicType(mf.newTopicTypeModel("lotto.number", "Lotto Number", "dmx.core.number"));
        dmx.createTopicType(mf.newTopicTypeModel("lotto.draw", "Lotto Draw", "dmx.core.value")
            .addAssocDef(mf.newCompDefModel(
                "lotto.draw", "lotto.number", "dmx.core.many"
            ))
        );
    }

    private void defineSimpleNameIdentityModel() {
        dmx.createTopicType(mf.newTopicTypeModel("simple.name", "Simple Name", "dmx.core.text"));
        dmx.createTopicType(mf.newTopicTypeModel("simple.entity", "Simple Entity", "dmx.core.identity")
            .addAssocDef(mf.newCompDefModel(
                "simple.entity", "simple.name", "dmx.core.one"
            ))
        );
    }

    private void defineManyNamesIdentityModel() {
        dmx.createTopicType(mf.newTopicTypeModel("simple.name", "Simple Name", "dmx.core.text"));
        dmx.createTopicType(mf.newTopicTypeModel("simple.entity", "Simple Entity", "dmx.core.identity")
            .addAssocDef(mf.newCompDefModel(
                "simple.entity", "simple.name", "dmx.core.many"
            ))
        );
    }
}
