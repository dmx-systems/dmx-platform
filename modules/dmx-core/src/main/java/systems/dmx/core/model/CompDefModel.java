package systems.dmx.core.model;



/**
 * The data that underly a {@link CompDef}.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
public interface CompDefModel extends AssocModel {

    String getCompDefUri();

    /**
     * @return  the URI of the Custom Assoc Type set for this comp def, or <code>null</code> if no
     *          Custom Assoc Type is set.
     */
    String getCustomAssocTypeUri();

    /**
     * @return  the type to be used to create an association instance based on this comp def.
     */
    String getInstanceLevelAssocTypeUri();

    String getParentTypeUri();

    String getChildTypeUri();

    String getChildCardinalityUri();

    ViewConfigModel getViewConfig();

    // ---

    // TODO: currently not supported. Drop from public API?
    void setChildCardinalityUri(String childCardinalityUri);

    void setViewConfig(ViewConfigModel viewConfig);
}
