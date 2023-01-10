package org.jenkins.tools.test.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ExecutedTestNamesSolver {

    private static final String WARNING_MSG = "[WARNING] Unable to retrieve info from: %s";

    private static final String TEST_PLACEHOLDER = "TEST-%s.xml";

    /*
        Element names for failure and error as declared at
        https://maven.apache.org/surefire/maven-failsafe-plugin/xsd/failsafe-test-report-3.0.xsd and
        https://gitbox.apache.org/repos/asf?p=maven-surefire.git;a=blob;f=maven-surefire-plugin/src/site/resources/xsd/surefire-test-report-3.0.xsd
     */
    private static final String FAILURE_ELEMENT = "failure";
    private static final String ERROR_ELEMENT = "error";

    public ExecutedTestNamesDetails solve(Set<String> types, Set<String> executedTests, File baseDirectory) throws ExecutedTestNamesSolverException {

        System.out.println("[INFO] -------------------------------------------------------");
        System.out.println("[INFO] Solving test names");
        System.out.println("[INFO] -------------------------------------------------------");

        ExecutedTestNamesDetails testNames = new ExecutedTestNamesDetails();

        List<String> reportsDirectoryPaths = getReportsDirectoryPaths(types, baseDirectory);
        if (reportsDirectoryPaths.isEmpty()) {
            System.out.println("[WARNING] No test reports found!");
            return testNames;
        }

        for (String reportsDirectoryPath : reportsDirectoryPaths) {
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
                            if (containsFailure(testcase.getChildNodes())) {
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
        printDetails(testNames.getExecuted(), "Executed");
        printDetails(testNames.getFailed(), "Failed");
        return testNames;
    }

    private void printDetails(Set<String> tests, String type) {
        System.out.println("[INFO] ");
        int size = tests != null ? tests.size() : 0;
        System.out.println(String.format("[INFO] %s: %s", type, size));
        if (size != 0) {
            for (String testName : tests) {
                System.out.println(String.format("[INFO] - %s", testName));
            }
        }
    }

    private List<String> getReportsDirectoryPaths(Set<String> types, File baseDirectory) throws ExecutedTestNamesSolverException {
        List<String> paths = new LinkedList<>();
        if (types == null) {
            return paths;
        }
        for (String type : types) {
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

    private boolean containsFailure(NodeList nodeList) {
        for (int j = 0; j < nodeList.getLength(); j++) {
            String elementName = nodeList.item(j).getNodeName();
            if (elementName.equals(FAILURE_ELEMENT)
                    || elementName.equals(ERROR_ELEMENT)) {
                return true;
            }
        }

        return false;
    }

}
