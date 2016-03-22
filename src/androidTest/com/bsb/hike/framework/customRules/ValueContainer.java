package androidTest.com.bsb.hike.framework.customRules;

/**
 * Created by surbhisharma on 15/03/16.
 */
public class ValueContainer<T> {
    private T value = null;

    public void set(T t) {
        value = t;
    }

    public T get() {
        return value;
    }
}