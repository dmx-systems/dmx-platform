package de.deepamehta.plugins.events;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ResultList;



public interface EventsService {

    ResultList<RelatedTopic> getEventsOfParticipant(long personId);

    ResultList<RelatedTopic> getParticipantsOfEvent(long eventId);
}
