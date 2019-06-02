package systems.dmx.core.impl;

import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.SimpleValue;
import systems.dmx.core.model.TopicModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



class LabelCalculation {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_CHILD_SEPARATOR = " ";
    private static final String LABEL_TOPIC_SEPARATOR = ", ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DMXObjectModelImpl comp;
    private List<String> labelCompDefUris;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Preconditions:
     *   - comp is not null
     *   - comp is composite
     *
     * @param   comp    A composite.
     */
    LabelCalculation(DMXObjectModelImpl comp) {
        this.comp = comp;
        this.labelCompDefUris = comp.getType().getLabelCompDefUris();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void calculate() {
        try {
            StringBuilder builder = new StringBuilder();
            for (String assocDefUri : labelCompDefUris) {
                comp.loadChildTopics(assocDefUri, false);   // deep=false, FIXME?
                appendLabel(calculateChildLabel(assocDefUri), builder, LABEL_CHILD_SEPARATOR);
            }
            //
            comp._updateSimpleValue(new SimpleValue(builder.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Calculating and updating label of " + comp.objectInfo() +
                " failed (assoc defs involved: " + labelCompDefUris + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private String calculateChildLabel(String assocDefUri) {
        Object value = comp.getChildTopicsModel().get(assocDefUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return "";
        }
        //
        if (value instanceof TopicModel) {
            // single value
            return ((TopicModel) value).getSimpleValue().toString();
        } else if (value instanceof List) {
            // multiple value
            StringBuilder builder = new StringBuilder();
            for (TopicModel childTopic : (List<TopicModel>) value) {
                appendLabel(childTopic.getSimpleValue().toString(), builder, LABEL_TOPIC_SEPARATOR);
            }
            return builder.toString();
        } else {
            throw new RuntimeException("Unexpected value in a ChildTopicsModel: " + value);
        }
    }

    private void appendLabel(String label, StringBuilder builder, String separator) {
        // add separator
        if (builder.length() > 0 && label.length() > 0) {
            builder.append(separator);
        }
        //
        builder.append(label);
    }
}
