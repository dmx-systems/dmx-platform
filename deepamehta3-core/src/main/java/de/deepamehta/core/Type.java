package de.deepamehta.core;



public interface Type extends Topic {

    ViewConfiguration getViewConfig();

    void setViewConfig(ViewConfiguration viewConfig);

    // ---

    // FIXME: to be dropped
    Object getViewConfig(String typeUri, String settingUri);
}
