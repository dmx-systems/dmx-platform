package de.deepamehta.core.model;



public interface RelatedAssociationModel extends AssociationModel {

    AssociationModel getRelatingAssociation();
}
