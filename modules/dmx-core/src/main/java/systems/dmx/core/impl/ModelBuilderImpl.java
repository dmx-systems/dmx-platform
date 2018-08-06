package systems.dmx.core.impl;

import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.DMXObjectModel;
import systems.dmx.core.model.IndexMode;
import systems.dmx.core.model.ModelBuilder;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.builder.DMXObjectModelBuilder;
import systems.dmx.core.model.builder.TopicModelBuilder;
import systems.dmx.core.model.builder.TypeModelBuilder;
import systems.dmx.core.model.builder.TopicTypeModelBuilder;
import systems.dmx.core.util.SequencedHashMap;
import java.util.ArrayList;
import java.util.List;



// https://stackoverflow.com/questions/21086417/builder-pattern-and-inheritance
class ModelBuilderImpl implements ModelBuilder {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    PersistenceLayer pl;

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public TopicModelBuilder topicModel() {
        return new TopicModelBuilderImpl();
    }

    @Override
    public TopicTypeModelBuilder topicTypeModel() {
        return new TopicTypeModelBuilderImpl();
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    abstract class DMXObjectModelBuilderImpl<B extends DMXObjectModelBuilder<B>> implements DMXObjectModelBuilder<B> {

        long id;
        String uri;
        String typeUri;
        SimpleValue value;
        ChildTopicsModelImpl childTopics;

        @Override
        public B id(long id) {
            this.id = id;
            return self();
        }

        @Override
        public B uri(String uri) {
            this.uri = uri;
            return self();
        }

        // abstract DMXObjectModel build();

        abstract B self();
    }

    class TopicModelBuilderImpl extends DMXObjectModelBuilderImpl<TopicModelBuilder> implements TopicModelBuilder {

        @Override
        public TopicModelImpl build() {
            return new TopicModelImpl(id, uri, typeUri, value, childTopics, pl);
        }

        @Override
        TopicModelBuilder self() {
            return this;
        }
    }

    abstract class TypeModelBuilderImpl<B extends TypeModelBuilder<B>> extends DMXObjectModelBuilderImpl<B>
                                                                                        implements TypeModelBuilder<B> {

        String dataTypeUri;
        List<IndexMode> indexModes;
        List<AssociationDefinitionModelImpl> assocDefs;
        ViewConfigurationModelImpl viewConfig;

        @Override
        public B dataType(String dataTypeUri) {
            this.dataTypeUri = dataTypeUri;
            return self();
        }
    }

    class TopicTypeModelBuilderImpl extends TypeModelBuilderImpl<TopicTypeModelBuilder>
                                                                                      implements TopicTypeModelBuilder {

        @Override
        public TopicTypeModelImpl build() {
            return new TopicTypeModelImpl(id, uri, typeUri, value, childTopics,
                dataTypeUri, indexModes, new ArrayList() /* assocDefs */, viewConfig, pl);
        }

        @Override
        TopicTypeModelBuilder self() {
            return this;
        }
    }
}
