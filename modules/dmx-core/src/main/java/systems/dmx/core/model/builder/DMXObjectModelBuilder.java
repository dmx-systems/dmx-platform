package systems.dmx.core.model.builder;

// import systems.dmx.core.model.DMXObjectModel;



public interface DMXObjectModelBuilder<B extends DMXObjectModelBuilder<B>> {

    B id(long id);

    B uri(String uri);

    // DMXObjectModel build();
}
