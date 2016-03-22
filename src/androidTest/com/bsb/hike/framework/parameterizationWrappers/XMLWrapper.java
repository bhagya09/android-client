package androidTest.com.bsb.hike.framework.parameterizationWrappers;


import java.io.File;
import org.w3c.dom.*;
import org.xml.sax.SAXException;


import javax.xml.XMLConstants;
/*
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
*/
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by surbhisharma on 16/03/16.
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
 /*


    @Override
    public ArrayList<Object[]> getParameters(File file) {
        Method method = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(Method.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
             method = (Method) unmarshaller.unmarshal(file);
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<Object[]> returnParameterList = new ArrayList<Object[]>();
        Test[] tests = method.getTest();
        for(int i=0;i<tests.length;i++)
        {
            Test currentTest = tests[i];
            Object[] testParams = new Object[currentTest.getParameter().size() +1];
            testParams[0] = currentTest.getName();
            for(int j = 0;j<currentTest.getParameter().size();j++)
            {
                testParams[j+1] = ((Parameter)(currentTest.getParameter().toArray())[j]).getValue();
            }
            returnParameterList.add(testParams);
        }
        return returnParameterList;
    }
    */
}
