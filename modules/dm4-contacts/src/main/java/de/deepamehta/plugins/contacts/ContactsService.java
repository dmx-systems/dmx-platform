package de.deepamehta.plugins.contacts;

import de.deepamehta.core.RelatedTopic;

import java.util.List;



public interface ContactsService {

    List<RelatedTopic> getInstitutions(long personId);

    List<RelatedTopic> getPersons(long instId);
}
