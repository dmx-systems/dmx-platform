package systems.dmx.core.model;



/**
 * Data that underlies a {@link RoleType}.
 *
 * @author <a href="mailto:jri@dmx.berlin">Jörg Richter</a>
 */
public interface RoleTypeModel extends TopicModel {

    // View Config

    ViewConfigModel getViewConfig();

    Object getViewConfigValue(String configTypeUri, String childTypeUri);

    void setViewConfig(ViewConfigModel viewConfig);
}
