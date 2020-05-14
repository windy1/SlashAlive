package tv.twitch.moonmoon.slashalive;

import java.util.Optional;

public class Result<T> {

    private final T result;
    private final String error;

    private Result(T result, String error) {
        this.result = result;
        this.error = error;
    }

    public static <T> Result<T> ok(T result) {
        return new Result<>(result, null);
    }

    public static <T> Result<T> err(String error) {
        return new Result<>(null, error);
    }

    public Optional<T> getResult() {
        return Optional.ofNullable(result);
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
}
