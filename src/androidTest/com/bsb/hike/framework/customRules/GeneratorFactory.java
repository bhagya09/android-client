package androidTest.com.bsb.hike.framework.customRules;

/**
 * Created by surbhisharma on 15/03/16.
 */
import static java.util.Arrays.asList;

public final class GeneratorFactory {
    private GeneratorFactory() {
    }

    public static <T> Generator<T> list(T... values) {
        return new ListGenerator<T>(asList(values));
    }
}
