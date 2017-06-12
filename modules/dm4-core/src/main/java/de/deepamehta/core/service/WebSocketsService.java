package de.deepamehta.core.service;



public interface WebSocketsService {

    void messageToAll(String pluginUri, String message);

    void messageToAllButOne(String pluginUri, String message);

    void messageToOne(String pluginUri, String message);
}
