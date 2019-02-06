package systems.dmx.events;

import systems.dmx.core.RelatedTopic;

import java.util.List;



public interface EventsService {

    List<RelatedTopic> getEvents(long personId);

    List<RelatedTopic> getPersons(long eventId);
}
