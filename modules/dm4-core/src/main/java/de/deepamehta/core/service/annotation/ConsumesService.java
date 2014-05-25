package de.deepamehta.core.service.annotation;

import de.deepamehta.core.service.PluginService;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConsumesService {
    Class<? extends PluginService>[] value();
}
