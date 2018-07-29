package systems.dmx.contacts;

import systems.dmx.core.RelatedTopic;

import java.util.List;



public interface ContactsService {

    List<RelatedTopic> getInstitutions(long personId);

    List<RelatedTopic> getPersons(long instId);
}
