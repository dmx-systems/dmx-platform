package de.deepamehta.plugins.contacts;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ResultList;



public interface ContactsService {

    ResultList<RelatedTopic> getInstitutions(long personId);

    ResultList<RelatedTopic> getPersons(long instId);
}
