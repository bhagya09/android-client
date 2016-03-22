package androidTest.com.bsb.hike.framework.parameterizationWrappers;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by surbhisharma on 16/03/16.
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
