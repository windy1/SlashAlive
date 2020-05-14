package tv.twitch.moonmoon.slashalive;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

public final class ReflectionUtils {

    private static final String RP_ENGINE_API = "com.Alvaeron.api.RPEngineAPI";

    public static Optional<Method> getRpNameMethod(Logger log) {
        return getMethodSafe(log, "getRpName", String.class);
    }

    public static Optional<Method> getRpRaceMethod(Logger log) {
        return getMethodSafe(log, "getRpRace", String.class);
    }

    private static Optional<Method> getMethodSafe(
        Logger log,
        String name,
        Class<?>... params
    ) {
        try {
            return Optional.of(Class.forName(ReflectionUtils.RP_ENGINE_API)
                .getDeclaredMethod(name, params));
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            String message = "failed to retrieve method `%s` in `%s`, using fallbacks";
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
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
