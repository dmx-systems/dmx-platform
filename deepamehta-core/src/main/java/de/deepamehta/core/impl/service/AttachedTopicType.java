package de.deepamehta.core.impl.service;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicTypeModel;

import java.util.logging.Logger;



/**
 * A topic type that is attached to the {@link DeepaMehtaService}.
 */
class AttachedTopicType extends AttachedType implements TopicType {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    AttachedTopicType(EmbeddedService dms) {
        super(dms);     // The model and the attached object cache remain uninitialized.
                        // They are initialized later on through fetch().
    }

    AttachedTopicType(TopicTypeModel model, EmbeddedService dms) {
        super(model, dms);
    }

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ********************************
    // *** TopicType Implementation ***
    // ********************************



    // *******************************
    // *** AttachedTopic Overrides ***
    // *******************************



    @Override
    protected String className() {
        return "topic type";
    }

    @Override
    public TopicTypeModel getModel() {
        return (TopicTypeModel) super.getModel();
    }



    // ----------------------------------------------------------------------------------------- Package Private Methods

    void update(TopicTypeModel model) {
        logger.info("Updating topic type \"" + getUri() + "\" (new " + model + ")");
        String uri = model.getUri();
        SimpleValue value = model.getSimpleValue();
        String dataTypeUri = model.getDataTypeUri();
        //
        boolean uriChanged = !getUri().equals(uri);
        boolean valueChanged = !getSimpleValue().equals(value);
        boolean dataTypeChanged = !getDataTypeUri().equals(dataTypeUri);
        //
        if (uriChanged || valueChanged) {
            if (uriChanged) {
                logger.info("Changing URI from \"" + getUri() + "\" -> \"" + uri + "\"");
            }
            if (valueChanged) {
                logger.info("Changing name from \"" + getSimpleValue() + "\" -> \"" + value + "\"");
            }
            if (uriChanged) {
                dms.typeCache.invalidate(getUri());
                super.update(model);
                dms.typeCache.put(this);
            } else {
                super.update(model);
            }
        }
        if (dataTypeChanged) {
            logger.info("Changing data type from \"" + getDataTypeUri() + "\" -> \"" + dataTypeUri + "\"");
            setDataTypeUri(dataTypeUri);
        }
        //
        if (!uriChanged && !valueChanged && !dataTypeChanged) {
            logger.info("Updating topic type \"" + getUri() + "\" ABORTED -- no changes made by user");
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods
}
