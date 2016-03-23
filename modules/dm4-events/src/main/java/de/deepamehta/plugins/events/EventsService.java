package de.deepamehta.plugins.events;

import de.deepamehta.core.RelatedTopic;

import java.util.List;



public interface EventsService {

    List<RelatedTopic> getEvents(long personId);

    List<RelatedTopic> getParticipants(long eventId);
}
