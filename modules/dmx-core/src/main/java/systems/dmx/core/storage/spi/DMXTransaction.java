package systems.dmx.core.storage.spi;



public interface DMXTransaction {

    void success();

    void failure();

    void finish();
}
