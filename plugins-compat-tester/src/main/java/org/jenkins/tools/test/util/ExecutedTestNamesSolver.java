package org.jenkins.tools.test.util;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jenkins.tools.test.exception.ExecutedTestNamesSolverException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ExecutedTestNamesSolver {

    private static final String WARNING_MSG = "[WARNING] Unable to retrieve info from: %s";
    
    private static final String TEST_PLACEHOLDER = "TEST-%s.xml";

    public Set<String> solve(Set<String> executedTests, File baseDirectory) throws ExecutedTestNamesSolverException {

        System.out.println("[INFO] -------------------------------------------------------");
        System.out.println("[INFO] Solving test names");
        System.out.println("[INFO] -------------------------------------------------------");
        
        Set<String> testNames = new TreeSet<>();
        try {
            
            String surefireReportsDirectoryPath = baseDirectory.getAbsolutePath() + File.separator + "target" + File.separator + "surefire-reports";
            File surefireReportsDirectory = Paths.get(surefireReportsDirectoryPath).toFile();
            if (!surefireReportsDirectory.exists()) {
                System.out.println(String.format(WARNING_MSG, surefireReportsDirectoryPath));
                return Collections.emptySet();
            }
            
            System.out.println(String.format("[INFO] Reading %s", surefireReportsDirectoryPath));
            
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            for (String testName : executedTests) {
                String reference = String.format(TEST_PLACEHOLDER, testName);
                String testReportPath = surefireReportsDirectoryPath + File.separator + reference;
                File testReport = Paths.get(testReportPath).toFile();
                if (!testReport.exists()) {
                    System.out.println(String.format(WARNING_MSG, testReportPath));
                }
                
                Document document = builder.parse(testReport);
                Node testsuite = document.getChildNodes().item(0);
                String nodeValue = testsuite.getAttributes().getNamedItem("tests").getNodeValue();
                Integer testCount = Integer.valueOf(nodeValue);
                int found = 0;
                for (int i = 0; i < testsuite.getChildNodes().getLength(); i++) {
                    Node testcase = testsuite.getChildNodes().item(i);
                    if (testcase.getAttributes() != null && testcase.getAttributes().getNamedItem("classname") != null) {
                        String clazzName = testcase.getAttributes().getNamedItem("classname").getNodeValue();
                        String test = testcase.getAttributes().getNamedItem("name").getNodeValue();
                        found++;
                        testNames.add(String.format("%s.%s", clazzName, test));
                    }
                }
                
                if (testCount.intValue() != found) {
                    System.out.println(String.format("[WARNING] Extracted: %s, Expected: %s from %s", found, testCount, testReportPath));
                } else {
                    System.out.println(String.format("[INFO] Extracted %s testnames from %s", testCount, testReportPath));
                }
            }

        } catch (Exception e) {
            throw new ExecutedTestNamesSolverException(e);
        }
        
        System.out.println("[INFO] ");
        System.out.println("[INFO] Results:");
        System.out.println("[INFO] ");
        for (String testName : testNames) {
            System.out.println(String.format("[INFO] - %s", testName));
        }
        
        return testNames;
    }
    
}
