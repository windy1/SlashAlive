package tv.twitch.moonmoon.slashalive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

public final class ReflectionUtils {

    public static Optional<Method> getRpNameMethod(Logger log) {
        try {
            return Optional.of(Class.forName("com.Alvaeron.api.RPEngineAPI")
                .getDeclaredMethod("getRpName", String.class));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            String message = "failed to retrieve getRpName on RPEngine, " +
                "falling back to username";
            log.warning(String.format(message, e.getMessage()));
            return Optional.empty();
        }
    }

    public static Optional<Object> invokeSafe(Logger log, Method method, Object... params) {
        try {
            return Optional.ofNullable(method.invoke(null, params));
        } catch (IllegalAccessException | InvocationTargetException e) {
            String message = "failed to invoke method `%s`: `%s`";
            log.warning(String.format(message, method.getName(), e.getMessage()));
            return Optional.empty();
        }
    }
}
