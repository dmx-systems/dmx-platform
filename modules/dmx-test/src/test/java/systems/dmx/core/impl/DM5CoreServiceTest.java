package systems.dmx.core.impl;

import systems.dmx.core.Association;
import systems.dmx.core.AssociationDefinition;
import systems.dmx.core.ChildTopics;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedAssociation;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.SimpleValue;
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
    public void compositeModel() {
        DMXTransaction tx = dmx.beginTx();
        try {
            defineLottoModel();
            Topic num1 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(23)));
            Topic num2 = dmx.createTopic(mf.newTopicModel("lotto.number", new SimpleValue(42)));
            Topic draw = dmx.createTopic(mf.newTopicModel("lotto.draw"));
            draw.getChildTopics()
                .addRef("lotto.number", num1.getId())
                .addRef("lotto.number", num2.getId());
        } finally {
            tx.finish();
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private void defineLottoModel() {
        dmx.createTopicType(mf.newTopicTypeModel("lotto.number", "Lotto Number", "dmx.core.number"));
        dmx.createTopicType(mf.newTopicTypeModel("lotto.draw", "Lotto Draw", "dmx.core.identity")
            .addAssocDef(mf.newAssociationDefinitionModel(
                "lotto.draw", "lotto.number", "dmx.core.many"
            ))
        );
    }
}
