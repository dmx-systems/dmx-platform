package systems.dmx.core.model.builder;



public interface TypeModelBuilder<B extends TypeModelBuilder<B>> extends DMXObjectModelBuilder<B> {

    B dataType(String dataTypeUri);
}
