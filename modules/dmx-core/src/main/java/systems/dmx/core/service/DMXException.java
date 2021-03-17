package systems.dmx.core.service;



public class DMXException extends RuntimeException {

    private CriticalityLevel level;

    public DMXException(String message, CriticalityLevel level) {
        super(message);
        this.level = level;
    }

    public CriticalityLevel getLevel() {
        return level;
    }
}
