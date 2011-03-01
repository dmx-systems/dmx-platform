package de.deepamehta.core.storage.neo4j;

import de.deepamehta.core.model.DataField;

import org.neo4j.graphdb.Node;

import java.util.Map;
import java.util.logging.Logger;



/**
 * Backs {@link DataField} by Neo4j database.
 *
 * @author <a href="mailto:jri@deepamehta.de">Jörg Richter</a>
 */
class Neo4jDataField extends DataField {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    Node node;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    /**
     * Used when a data field is read from the database.
     */
    Neo4jDataField(Map properties, Node node) {
        // super(properties);
        //
        // Note: we can't invoke super() because the super constructor invokes methods which are overridden here
        // (setProperty() indirectly via setDefaults()). This would cause NullPointerException because this object's
        // instance variables ("logger" and "node") are only initialized *after* super() has returned.
        //
        // From www.artima.com/designtechniques/initialization.html: Although <init> methods are called in an order
        // starting from the object's class and proceeding up the inheritance path to class Object, instance variables
        // are initialized in the reverse order.
        //
        // We rely on DataField's setDefaults() facility for the migration of database content when DataField got an
        // additional property.
        //
        this.node = node;
        setProperties(new DataField(properties).getProperties());   // this workaround looks crude
    }

    /**
     * Constructs a data field and writes it to the database.
     */
    Neo4jDataField(DataField dataField, Neo4jStorage storage) {
        this.node = storage.createMetaProperty(dataField.getUri());
        logger.info("Creating data field " + dataField + " => ID=" + node.getId());
        setProperties(dataField.getProperties());
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public void setProperty(String key, Object value) {
        logger.info("#################################### Neo4jDataField: set " + key + "=" + value);
        // update memory
        super.setProperty(key, value);
        // update DB
        node.setProperty(key, value);
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        // update memory
        super.setProperties(properties);
        // update DB
        StringBuilder log = new StringBuilder();
        for (String key : properties.keySet()) {
            Object newValue = properties.get(key);
            Object oldValue = node.getProperty(key, null);
            if (oldValue != null && !oldValue.equals(newValue)) {
                log.append("\n  " + key + ": \"" + oldValue + "\" => \"" + newValue + "\"");
            }
            //
            node.setProperty(key, newValue);
        }
        if (log.length() > 0) {
            logger.warning("### Overriding properties of data field " + this + ":" + log);
        }
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    Node getNode() {
        return node;
    }
}
