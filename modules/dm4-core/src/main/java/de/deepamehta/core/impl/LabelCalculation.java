package de.deepamehta.core.impl;

import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



class LabelCalculation {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_CHILD_SEPARATOR = " ";
    private static final String LABEL_TOPIC_SEPARATOR = ", ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private DeepaMehtaObjectModelImpl comp;
    private List<String> labelAssocDefUris;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * @param   comp    A composite.
     */
    LabelCalculation(DeepaMehtaObjectModelImpl comp) {
        this.comp = comp;
        this.labelAssocDefUris = comp.getType().getLabelAssocDefUris();
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void calculate() {
        try {
            StringBuilder builder = new StringBuilder();
            for (String assocDefUri : labelAssocDefUris) {
                comp.loadChildTopics(assocDefUri);
                appendLabel(calculateChildLabel(assocDefUri), builder, LABEL_CHILD_SEPARATOR);
            }
            //
            comp._updateSimpleValue(new SimpleValue(builder.toString()));
        } catch (Exception e) {
            throw new RuntimeException("Calculating and updating label of " + comp.objectInfo() +
                " failed (assoc defs involved: " + labelAssocDefUris + ")", e);
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
