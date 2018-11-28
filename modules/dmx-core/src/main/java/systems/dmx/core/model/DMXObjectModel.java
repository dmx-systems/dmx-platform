package systems.dmx.core.model;

import systems.dmx.core.Identifiable;
import systems.dmx.core.JSONEnabled;



public interface DMXObjectModel extends Identifiable, JSONEnabled, Cloneable {

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

    // ### TODO: drop it?
    void set(DMXObjectModel object);

    // ---

    RoleModel createRoleModel(String roleTypeUri);



    // === Java API ===

    DMXObjectModel clone();
}
