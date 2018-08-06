package systems.dmx.core.model;

import systems.dmx.core.model.builder.TopicModelBuilder;
import systems.dmx.core.model.builder.TopicTypeModelBuilder;



public interface ModelBuilder {

    TopicModelBuilder topicModel();

    TopicTypeModelBuilder topicTypeModel();
}
