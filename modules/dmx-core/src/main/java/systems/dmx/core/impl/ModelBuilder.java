package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.IndexMode;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.util.SequencedHashMap;
import java.util.ArrayList;
import java.util.List;



// https://stackoverflow.com/questions/21086417/builder-pattern-and-inheritance
class ModelBuilder {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    PersistenceLayer pl;

    // -------------------------------------------------------------------------------------------------- Public Methods

    TopicModelBuilder topicModel() {
        return new TopicModelBuilder();
    }

    TopicTypeModelBuilder topicTypeModel() {
        return new TopicTypeModelBuilder();
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    abstract class DMXObjectModelBuilder<B extends DMXObjectModelBuilder<B>> {

        long id;
        String uri;
        String typeUri;
        SimpleValue value;
        ChildTopicsModelImpl childTopics;

        B id(long id) {
            this.id = id;
            return self();
        }

        B uri(String uri) {
            this.uri = uri;
            return self();
        }

        // abstract DMXObjectModel build();

        abstract B self();
    }

    class TopicModelBuilder extends DMXObjectModelBuilder<TopicModelBuilder> {

        // @Override
        TopicModelImpl build() {
            return new TopicModelImpl(id, uri, typeUri, value, childTopics, pl);
        }

        @Override
        TopicModelBuilder self() {
            return this;
        }
    }

    abstract class TypeModelBuilder<B extends TypeModelBuilder<B>> extends DMXObjectModelBuilder<B> {

        String dataTypeUri;
        List<IndexMode> indexModes;
        List<AssociationDefinitionModelImpl> assocDefs;
        ViewConfigurationModelImpl viewConfig;

        B dataType(String dataTypeUri) {
            this.dataTypeUri = dataTypeUri;
            return self();
        }
    }

    class TopicTypeModelBuilder extends TypeModelBuilder<TopicTypeModelBuilder> {

        // @Override
        TopicTypeModelImpl build() {
            return new TopicTypeModelImpl(id, uri, typeUri, value, childTopics,
                dataTypeUri, indexModes, new ArrayList() /* assocDefs */, viewConfig, pl);
        }

        @Override
        TopicTypeModelBuilder self() {
            return this;
        }
    }
}
