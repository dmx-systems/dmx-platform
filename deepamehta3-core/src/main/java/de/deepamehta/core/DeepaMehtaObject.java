package de.deepamehta.core;

import de.deepamehta.core.model.Composite;
import de.deepamehta.core.model.TopicValue;



public interface DeepaMehtaObject extends JSONEnabled {

    // --- ID ---

    long getId();

    // --- URI ---

    String getUri();

    void setUri(String uri);

    // --- Value ---

    TopicValue getValue();

    void setValue(String value);
    void setValue(int value);
    void setValue(long value);
    void setValue(boolean value);
    void setValue(TopicValue value);

    // --- Type URI ---

    String getTypeUri();

    void setTypeUri(String typeUri);

    // --- Composite ---

    Composite getComposite();

    void setComposite(Composite comp);
}
