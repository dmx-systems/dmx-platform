package de.deepamehta.core.impl.service;

import de.deepamehta.core.Type;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.util.JavaUtils;



abstract class Storage {

    protected EmbeddedService dms;

    void storeAndIndexValue(long objectId, String typeUri, SimpleValue value) {
        SimpleValue oldValue = storeValue(objectId, value);                 // abstract
        indexValue(objectId, typeUri, value, oldValue);
    }

    // ---

    private void indexValue(long objectId, String typeUri, SimpleValue value, SimpleValue oldValue) {
        Type type = getType(typeUri);                                       // abstract
        String indexKey = type.getUri();
        // strip HTML tags before indexing
        if (type.getDataTypeUri().equals("dm4.core.html")) {
            value = new SimpleValue(JavaUtils.stripHTML(value.toString()));
            if (oldValue != null) {
                oldValue = new SimpleValue(JavaUtils.stripHTML(oldValue.toString()));
            }
        }
        //
        for (IndexMode indexMode : type.getIndexModes()) {
            indexValue(objectId, indexMode, indexKey, value, oldValue);     // abstract
        }
    }

    // ---

    protected abstract SimpleValue storeValue(long objectId, SimpleValue value);

    protected abstract void indexValue(long objectId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                            SimpleValue oldValue);
    protected abstract Type getType(String typeUri);
}



class TopicStorage extends Storage {

    TopicStorage(EmbeddedService dms) {
        this.dms = dms;
    }

    @Override
    protected SimpleValue storeValue(long topicId, SimpleValue value) {
        return dms.storage.setTopicValue(topicId, value);
    }

    @Override
    protected void indexValue(long topicId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                  SimpleValue oldValue) {
        dms.storage.indexTopicValue(topicId, indexMode, indexKey, value, oldValue);
    }

    @Override
    protected Type getType(String typeUri) {
        return dms.getTopicType(typeUri, null);         // FIXME: clientState=null
    }
}



class AssociationStorage extends Storage {

    AssociationStorage(EmbeddedService dms) {
        this.dms = dms;
    }

    @Override
    protected SimpleValue storeValue(long assocId, SimpleValue value) {
        return dms.storage.setAssociationValue(assocId, value);
    }

    @Override
    protected void indexValue(long assocId, IndexMode indexMode, String indexKey, SimpleValue value,
                                                                                  SimpleValue oldValue) {
        dms.storage.indexAssociationValue(assocId, indexMode, indexKey, value, oldValue);
    }

    @Override
    protected Type getType(String typeUri) {
        return dms.getAssociationType(typeUri, null);   // FIXME: clientState=null
    }
}
