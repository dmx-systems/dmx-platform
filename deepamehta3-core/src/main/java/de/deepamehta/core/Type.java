package de.deepamehta.core;



public interface Type extends Topic {

    // === Data Type ===

    String getDataTypeUri();

    void setDataTypeUri(String dataTypeUri);

    // === View Configuration ===

    ViewConfiguration getViewConfig();

    // FIXME: to be dropped
    Object getViewConfig(String typeUri, String settingUri);
}
