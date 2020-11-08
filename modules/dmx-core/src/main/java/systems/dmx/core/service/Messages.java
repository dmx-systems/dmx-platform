package systems.dmx.core.service;

import systems.dmx.core.service.websocket.WebSocketConnection;
import systems.dmx.core.service.websocket.WebSocketService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;



public class Messages implements Iterable<Messages.Message> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Message> messages = new ArrayList();

    // ------------------------------------------------------------------------------------------------- Class Variables

    public enum Dest {
        ORIGIN() {
            @Override
            public void send(Message message, WebSocketService wss) {
                wss.sendToOrigin(message.message);
            }
        },
        ALL() {
            @Override
            public void send(Message message, WebSocketService wss) {
                wss.sendToAll(message.message);
            }
        },
        ALL_BUT_ORIGIN() {
            @Override
            public void send(Message message, WebSocketService wss) {
                wss.sendToAllButOrigin(message.message);
            }
        },
        READ_ALLOWED() {
            @Override
            public void send(Message message, WebSocketService wss) {
                wss.sendToReadAllowed(message.message, (Long) message.params[0]);
            }
        },
        SOME() {
            @Override
            public void send(Message message, WebSocketService wss) {
                wss.sendToSome(message.message, (Predicate<WebSocketConnection>) message.params[0]);
            }
        };

        public abstract void send(Message message, WebSocketService wss);
    }

    private static Logger logger = Logger.getLogger("systems.dmx.core.service.Messages");

    private static final ThreadLocal<Messages> threadLocalDirectives = new ThreadLocal() {
        @Override
        protected Messages initialValue() {
            logger.fine("### Creating tread-local messages");
            return new Messages();
        }
    };

    // -------------------------------------------------------------------------------------------------- Public Methods

    public static Messages get() {
        return threadLocalDirectives.get();
    }

    public void add(Dest dest, String message, Object... params) {
        messages.add(new Message(dest, message, params));
    }

    public static void remove() {
        logger.fine("### Removing tread-local messages");
        threadLocalDirectives.remove();
    }

    // *** Iterable ***

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    public class Message {

        public Dest dest;
        public String message;
        public Object[] params;

        private Message(Dest dest, String message, Object[] params) {
            this.dest = dest;
            this.message = message;
            this.params = params;
        }
    }
}
