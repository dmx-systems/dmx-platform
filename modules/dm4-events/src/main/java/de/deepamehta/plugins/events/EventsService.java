package de.deepamehta.plugins.events;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ResultList;



public interface EventsService {

    ResultList<RelatedTopic> getParticipants(long eventId);

    ResultList<RelatedTopic> getEventsByParticipant(long personId);
}
