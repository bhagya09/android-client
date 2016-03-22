package androidTest.com.bsb.hike.framework.customRules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

/**
 * Created by surbhisharma on 15/03/16.
 */
public class ListGenerator<T> implements Generator<T> {

        private final ValueContainer<T> currentValue = new ValueContainer<T>();

        private final AccessibleErrorCollector errorCollector = new AccessibleErrorCollector();

        private final List<T> values;

        public ListGenerator(List<T> values) {
            this.values = values;
        }

        @Override
        public T value() {
            return currentValue.get();
        }

        @Override
        public Statement apply(Statement test, Description description) {
            return new RepeatedStatement<T>(test, new SyncingIterable<T>(values,
                    currentValue), errorCollector);
        }
}


