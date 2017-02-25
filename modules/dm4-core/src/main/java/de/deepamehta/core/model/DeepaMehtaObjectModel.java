package de.deepamehta.core.model;

import de.deepamehta.core.Identifiable;
import de.deepamehta.core.JSONEnabled;



public interface DeepaMehtaObjectModel extends Identifiable, JSONEnabled, Cloneable {

    // --- ID ---

    long getId();

    void setId(long id);

    // --- URI ---

    String getUri();

    void setUri(String uri);

    // --- Type URI ---

    String getTypeUri();

    void setTypeUri(String typeUri);

    // --- Simple Value ---

    SimpleValue getSimpleValue();

    // ---

    void setSimpleValue(String value);

    void setSimpleValue(int value);

    void setSimpleValue(long value);

    void setSimpleValue(boolean value);

    void setSimpleValue(SimpleValue value);

    // --- Child Topics ---

    ChildTopicsModel getChildTopicsModel();

    void setChildTopicsModel(ChildTopicsModel childTopics);

    // --- misc ---

    void set(DeepaMehtaObjectModel object);

    // ---

    RoleModel createRoleModel(String roleTypeUri);



    // === Java API ===

    DeepaMehtaObjectModel clone();
}
