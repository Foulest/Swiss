package net.foulest.swiss;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A pair of objects.
 *
 * @param <X> The type of the first object.
 * @param <Y> The type of the second object.
 * @author Foulest
 */
@Data
@AllArgsConstructor
public class Pair<X, Y> {

    private final X first;
    private final Y last;

    @Contract("_, _ -> new")
    public static <X, Y> @NotNull Pair<X, Y> of(X x, Y y) {
        return new Pair<>(x, y);
    }
}
