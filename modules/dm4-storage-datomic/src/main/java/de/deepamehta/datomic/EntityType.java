package de.deepamehta.datomic;



enum EntityType {

    TOPIC(":dm4.entity-type/topic"),
    ASSOC(":dm4.entity-type/assoc");

    String ident;

    private EntityType(String ident) {
        this.ident = ident;
    }
}
