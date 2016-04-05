package androidTest.com.bsb.hike.framework.parameterizationWrappers;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by surbhisharma on 16/03/16.
 * Stub - reads inputs from JSON and passes it to the current test
 */
public class JSONWrapper implements IParameterizationWrapper {

    @Override
    public File openFile(String path) {
        return null;
    }

    @Override
    public boolean checkFileSanity(File file) {
        return false;
    }

    @Override
    public ArrayList<Object[]> getParameters(File file) {
        return null;
    }
}
