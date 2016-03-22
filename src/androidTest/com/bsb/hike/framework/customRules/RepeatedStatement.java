package androidTest.com.bsb.hike.framework.customRules;

import org.junit.runners.model.Statement;

/**
 * Created by surbhisharma on 15/03/16.
 */
class RepeatedStatement<T> extends Statement {

    private final Statement test;
    private final Iterable<T> values;
    private final AccessibleErrorCollector errorCollector;

    public RepeatedStatement(Statement test, Iterable<T> values,
                             AccessibleErrorCollector errorCollector) {
        this.test = test;
        this.values = values;
        this.errorCollector = errorCollector;
    }

    @Override
    public void evaluate() throws Throwable {
        for (T v : values) {
            try {
                test.evaluate();
            } catch (Throwable t) {
                errorCollector.addError(new AssertionError("For value: "
                        + v, t));
            }
        }
        errorCollector.verify();
    }
}
