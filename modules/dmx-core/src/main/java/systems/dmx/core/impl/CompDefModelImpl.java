package systems.dmx.core.impl;

import static systems.dmx.core.Constants.*;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.CompDefModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.model.TopicPlayerModel;
import systems.dmx.core.model.ViewConfigModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.logging.Logger;



class CompDefModelImpl extends AssocModelImpl implements CompDefModel {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    ViewConfigModelImpl viewConfig;     // is never null

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    CompDefModelImpl(AssocModelImpl assoc) {
        this(assoc, null);
    }

    /**
     * @param   assoc   the underlying association.
     */
    CompDefModelImpl(AssocModelImpl assoc, ViewConfigModelImpl viewConfig) {
        super(assoc);
        this.viewConfig = viewConfig != null ? viewConfig : mf.newViewConfigModel();
        // ### TODO: why null check? Compare to TypeModelImpl constructor -> see previous constructor
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public String getCompDefUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return getChildTypeUri() + (customAssocTypeUri !=null ? "#" + customAssocTypeUri : "");
    }

    @Override
    public String getCustomAssocTypeUri() {
        TopicModel customAssocType = getCustomAssocType();
        return customAssocType != null ? customAssocType.getUri() : null;
    }

    @Override
    public String getInstanceLevelAssocTypeUri() {
        String customAssocTypeUri = getCustomAssocTypeUri();
        return customAssocTypeUri !=null ? customAssocTypeUri : defaultInstanceLevelAssocTypeUri();
    }

    @Override
    public String getParentTypeUri() {
        return ((TopicPlayerModel) getPlayerByRole(PARENT_TYPE)).getTopicUri();
    }

    @Override
    public String getChildTypeUri() {
        return ((TopicPlayerModel) getPlayerByRole(CHILD_TYPE)).getTopicUri();
    }

    @Override
    public String getChildCardinalityUri() {
        return _getCardinalityUri();
    }

    @Override
    public ViewConfigModelImpl getViewConfig() {
        return viewConfig;
    }

    // ---

    @Override
    public void setChildCardinalityUri(String childCardinalityUri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setViewConfig(ViewConfigModel viewConfig) {
        this.viewConfig = (ViewConfigModelImpl) viewConfig;
    }

    // ---

    @Override
    public JSONObject toJSON() {
        try {
            return super.toJSON()
                .put("viewConfigTopics", viewConfig.toJSONArray());
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    @Override
    String className() {
        return "comp def";
    }

    @Override
    CompDefImpl instantiate() {
        return new CompDefImpl(this, al);
    }

    @Override
    CompDefModelImpl createModelWithChildTopics(ChildTopicsModel childTopics) {
        return mf.newCompDefModel(childTopics);
    }



    // === Core Internal Hooks ===

    @Override
    void postCreate() {
        // Note: defining an empty postCreate() hook suppresses Type Editor Support as invoked by the superclass's
        // postCreate() hook.
        // - When an comp def is created *programmatically* (through a migration) a full CompDefModel is instantiated,
        //   and no further Type Editor Support must be executed.
        // - When an comp def is created *interactively* (by creating an association in conjunction with auto-typing)
        //   a sole AssocModel is instantiated, and Type editor support is required.
    }



    // === Read from ChildTopicsModel ===

    final String _getCardinalityUri() {
        TopicModelImpl cardinality = getChildTopics().getTopicOrNull(CARDINALITY);
        if (cardinality == null) {
            // ### TODO: should a cardinality topic always exist?
            throw new RuntimeException("Comp def \"" + getCompDefUri() + "\" has no \"Cardinality\" topic");
            // return false;
        }
        return cardinality.uri;
    }

    final boolean isIdentityAttr() {
        TopicModel isIdentityAttr = getChildTopics().getTopicOrNull(IDENTITY_ATTR);
        if (isIdentityAttr == null) {
            // ### TODO: should a isIdentityAttr topic always exist?
            // throw new RuntimeException("Comp def \"" + getCompDefUri() + "\" has no \"Identity Attribute\" topic");
            return false;
        }
        return isIdentityAttr.getSimpleValue().booleanValue();
    }

    final boolean includeInLabel() {
        TopicModel includeInLabel = getChildTopics().getTopicOrNull(INCLUDE_IN_LABEL);
        if (includeInLabel == null) {
            // ### TODO: should a includeInLabel topic always exist?
            // throw new RuntimeException("Comp def \"" + getCompDefUri() + "\" has no \"Include in Label\" topic");
            return false;
        }
        return includeInLabel.getSimpleValue().booleanValue();
    }



    // === Access Control ===

    /**
     * Overrides DMXObjectModelImpl
     */
    boolean isReadable() {
        try {
            // 1) check comp def
            if (!super.isReadable()) {
                logger.fine("### Comp def \"" + getCompDefUri() + "\" not READable");
                return false;
            }
            // Note: there is no need to check READability for the comp def's child type.
            // If the child type is not READable the entire comp def is not READable as well.
            //
            // 2) check custom assoc type, if set
            TopicModelImpl assocType = getCustomAssocType();
            if (assocType != null && !assocType.isReadable()) {
                logger.fine("### Comp def \"" + getCompDefUri() + "\" not READable (custom assoc type not READable)");
                return false;
            }
            //
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Checking comp def READability failed (" + this + ")", e);
        }
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private TopicModelImpl getCustomAssocType() {
        return getChildTopics().getTopicOrNull(ASSOC_TYPE + "#" + CUSTOM_ASSOC_TYPE);
    }

    private String defaultInstanceLevelAssocTypeUri() {
        if (typeUri.equals(COMPOSITION_DEF)) {
            return COMPOSITION;
        } else {
            throw new RuntimeException("Unexpected association type URI: \"" + typeUri + "\"");
        }
    }
}
