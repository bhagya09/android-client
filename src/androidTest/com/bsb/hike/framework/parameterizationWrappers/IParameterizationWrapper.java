package androidTest.com.bsb.hike.framework.parameterizationWrappers;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by surbhisharma on 16/03/16.
 */
public interface IParameterizationWrapper {
    public File openFile(String path);    public boolean checkFileSanity(File file) throws IOException, SAXException;
    public ArrayList<Object[]> getParameters(File file);
}
