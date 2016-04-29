package androidTest.com.bsb.hike.framework.parameterizationWrappers;

import java.io.File;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by surbhisharma on 16/03/16.
 * Reads Inputs from an XML and passes it to test
 */
public class XMLWrapper implements IParameterizationWrapper {

    File parameterFile;
    DocumentBuilderFactory factory = null;
    DocumentBuilder db = null;
    Document doc = null;

    public XMLWrapper(String fileName) throws IOException, SAXException, ParserConfigurationException {
        factory = DocumentBuilderFactory.newInstance();
        db = factory.newDocumentBuilder();
        doc = db.parse(new File(fileName));
    }



    @Override
    public File openFile(String path) {

        return new File(path);
    }

    /*
    STUB - To be implemented
     */
    @Override
    public boolean checkFileSanity(File file) throws IOException, SAXException {
        Schema schema = null;
        try {
            String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
            SchemaFactory factory = SchemaFactory.newInstance(language);
            schema = factory.newSchema(file);
        } catch (Exception e) {
            e.getStackTrace();
        }
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(doc));

        return false;
    }

    @Override
    public ArrayList<Object[]> getParameters(File file) {
        ArrayList<Object[]> toReturn = new ArrayList<Object[]>();
        NodeList testCases = doc.getElementsByTagName("test");
        for(int i =0;i<testCases.getLength();i++)
        {
            String currentTestCaseName = testCases.item(i).getAttributes().getNamedItem("name").getNodeValue();
            NodeList testParameters = ((Element)testCases.item(i)).getElementsByTagName("parameter");
            Object[] parameterList = new Object[testParameters.getLength() + 1];
            parameterList[0] = currentTestCaseName;
            for(int j =0 ;j<testParameters.getLength();j++)
            {
                Node parameter = testParameters.item(j);
                parameterList[j+1]=parameter.getAttributes().getNamedItem("value").getNodeValue();
            }
            toReturn.add(parameterList);
        }
        return toReturn;
    }
}
