package org.jenkins.tools.test.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jenkins.tools.test.exception.ExecutedTestNamesSolverException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ExecutedTestNamesSolver {

    private static final String WARNING_MSG = "[WARNING] Unable to retrieve info from: %s";
    
    private static final String TEST_PLACEHOLDER = "TEST-%s.xml";
    
    private Set<String> types;
    
    public ExecutedTestNamesSolver(Set<String> types) {
        initTypes();
        this.types.addAll(types); 
    }

    private void initTypes() {
        this.types = new HashSet<>();
        this.types.add("surefire"); // by default
    }
    
    public ExecutedTestNamesDetails solve(Set<String> executedTests, File baseDirectory) throws ExecutedTestNamesSolverException {

        System.out.println("[INFO] -------------------------------------------------------");
        System.out.println("[INFO] Solving test names");
        System.out.println("[INFO] -------------------------------------------------------");
        
        ExecutedTestNamesDetails testNames = new ExecutedTestNamesDetails();
        
        List<String> reportsDirectoryPaths = getReportsDirectoryPaths(baseDirectory);
        if(reportsDirectoryPaths.isEmpty()) {
            System.out.println("[WARNING] No test reports found!");
            return testNames;
        }
        
        for (String reportsDirectoryPath: reportsDirectoryPaths) {
            try {
                File reportsDirectory = Paths.get(reportsDirectoryPath).toFile();
                if (!reportsDirectory.exists()) {
                    System.out.println(String.format(WARNING_MSG, reportsDirectoryPath));
                    return testNames;
                }
                
                System.out.println(String.format("[INFO] Reading %s", reportsDirectoryPath));
                
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                for (String testName : executedTests) {
                    String reference = String.format(TEST_PLACEHOLDER, testName);
                    String testReportPath = reportsDirectoryPath + File.separator + reference;
                    File testReport = Paths.get(testReportPath).toFile();
                    if (!testReport.exists()) {
                        System.out.println(String.format(WARNING_MSG, testReportPath));
                        continue;
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
                            String testCaseName = String.format("%s.%s", clazzName, test);
                            if (testcase.getChildNodes().getLength() != 0) {
                                testNames.addFailedTest(testCaseName);
                            } else {
                                testNames.addExecutedTest(testCaseName);
                            }
                        }
                    }
                    
                    if (testCount.intValue() != found) {
                        System.out.println(String.format("[WARNING] Extracted: %s, Expected: %s from %s", found, testCount, testReportPath));
                    } else {
                        System.out.println(String.format("[INFO] Extracted %s testnames from %s", testCount, testReportPath));
                    }
                }
    
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new ExecutedTestNamesSolverException(e);
            }
        }
        
        System.out.println("[INFO] ");
        System.out.println("[INFO] Results:");
        System.out.println("[INFO] ");
        System.out.println(String.format("[INFO] Executed: %s", testNames.getExecuted().size()));
        for (String testName : testNames.getExecuted()) {
            System.out.println(String.format("[INFO] - %s", testName));
        }
        System.out.println("[INFO] ");
        System.out.println(String.format("[INFO] Failed: %s", testNames.getFailed().size()));
        for (String testName : testNames.getFailed()) {
            System.out.println(String.format("[INFO] - %s", testName));
        }
        
        return testNames;
    }

    private List<String> getReportsDirectoryPaths(File baseDirectory) throws ExecutedTestNamesSolverException {
        List<String> paths = new LinkedList<>();
        for(String type: types) {
            try (Stream<Path> walk = Files.walk(Paths.get(baseDirectory.getAbsolutePath()))) {
                List<Path> result = walk.filter(Files::isDirectory)
                        .filter(file -> file.getFileName().toString().endsWith(String.format("%s-reports", type)))
                        .collect(Collectors.toList());
                for (Path path : result) {
                    paths.add(path.toString());
                }
            } catch (IOException e) {
                throw new ExecutedTestNamesSolverException(e);
            } 
        }
        return paths;
    }
    
}
