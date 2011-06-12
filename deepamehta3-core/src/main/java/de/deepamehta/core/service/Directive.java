package de.deepamehta.core.service;



public enum Directive {

    UPDATE_ASSOCIATION, UPDATE_TOPIC_TYPE;

    String s() {
        return name().toLowerCase();
    }
}
