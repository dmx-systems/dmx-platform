package de.deepamehta.core.impl;

import de.deepamehta.core.model.DataField;

import java.util.ArrayList;
import java.util.List;

public class DataFieldLiteral {

    List<DataField> dataFields = new ArrayList<DataField>();

    public DataFieldLiteral add(String label, String dataType, String uri, String renderer, String indexMode) {
        DataField field = new DataField(label, dataType);
        field.setUri(uri);
        field.setRendererClass(renderer);
        field.setIndexingMode(indexMode);

        dataFields.add(field);
        return this;
    }

    public List<DataField> getList() {
        return dataFields;
    }
}