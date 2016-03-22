package androidTest.com.bsb.hike.framework.customRules;
import org.junit.rules.ErrorCollector;

/**
 * Created by surbhisharma on 15/03/16.
 */
public class AccessibleErrorCollector extends ErrorCollector {

    @Override
    public void verify() throws Throwable {
        super.verify();
    }

}
