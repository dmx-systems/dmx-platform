package systems.dmx.core.model;



public interface RelatedObjectModel extends DMXObjectModel {

    AssocModel getRelatingAssoc();

    <M extends DMXObjectModel> M getOtherDMXObject();
}
