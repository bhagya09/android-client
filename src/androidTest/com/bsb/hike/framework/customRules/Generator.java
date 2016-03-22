package androidTest.com.bsb.hike.framework.customRules;
import org.junit.rules.TestRule;

/**
 * Created by surbhisharma on 15/03/16.
 */

public interface Generator<T> extends TestRule {
        public T value();
    }

