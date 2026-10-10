package dev.maxenceng.lazyclaude;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declared on each module's {@code package-info}, so the architecture rules read a module's kind instead of its name. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface ApplicationModule {

    ModuleType type();
}
