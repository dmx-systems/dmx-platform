package systems.dmx.core.impl;

import systems.dmx.core.service.websocket.WebSocketConnection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;



class Messages implements Iterable<Messages.Message> {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private List<Message> messages = new ArrayList();

    // ------------------------------------------------------------------------------------------------- Class Variables

    static enum Dest {
        ORIGIN {
            @Override
            void send(Message message, WebSocketServiceImpl wss) {
                wss._sendToOrigin(message.message);
            }
        },
        ALL {
            @Override
            void send(Message message, WebSocketServiceImpl wss) {
                wss._sendToAll(message.message);
            }
        },
        ALL_BUT_ORIGIN {
            @Override
            void send(Message message, WebSocketServiceImpl wss) {
                wss._sendToAllButOrigin(message.message);
            }
        },
        READ_ALLOWED {
            @Override
            void send(Message message, WebSocketServiceImpl wss) {
                wss._sendToReadAllowed(message.message, (Long) message.params[0]);
            }
        },
        SOME {
            @Override
            void send(Message message, WebSocketServiceImpl wss) {
                wss._sendToSome(message.message, (Predicate<WebSocketConnection>) message.params[0]);
            }
        };

        abstract void send(Message message, WebSocketServiceImpl wss);
    }

    private static Logger logger = Logger.getLogger("systems.dmx.core.impl.Messages");

    private static final ThreadLocal<Messages> threadLocalDirectives = new ThreadLocal() {
        @Override
        protected Messages initialValue() {
            logger.fine("### Creating tread-local messages");
            return new Messages();
        }
    };

    // -------------------------------------------------------------------------------------------------- Public Methods

    static Messages get() {
        return threadLocalDirectives.get();
    }

    void add(Dest dest, String message, Object... params) {
        messages.add(new Message(dest, message, params));
    }

    static void remove() {
        logger.fine("### Removing tread-local messages");
        threadLocalDirectives.remove();
    }

    // *** Iterable ***

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    // -------------------------------------------------------------------------------------------------- Nested Classes

    class Message {

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
