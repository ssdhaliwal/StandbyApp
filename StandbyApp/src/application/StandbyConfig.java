/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import elsu.common.GlobalStack;
import java.io.*;
import java.util.*;
import elsu.support.*;
import org.apache.commons.lang3.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyConfig {

    public static String APPCONFIG = "standbyapp/app.config";
    public static String DTGFORMAT = "YYYMMDD HH24:mm:ss";
    protected XMLReader _xmlr = null;
    // store all application wide properties from app.config
    private Map<String, String> _applicationProperties = new HashMap<>();
    private Map<String, StandbyNodeProperties> _clusterList = new HashMap<>();

    public StandbyConfig() {
        try {
            // check is app.config and log4j.properties file is stored in the
            // application 
            extractConfigFile(APPCONFIG);

            // try to create the XML reader instance for XML document parsing
            // using the app.config file location
            _xmlr = new XMLReader(StandbyConfig.APPCONFIG);

            // display the config to the user
            showConfig();

            // load the config into application or service properties hashMaps
            initialize();
        } catch (Exception ex) {
            System.out.println(getClass().toString() + ", constructor, " + ex.getMessage());
        }
    }

    public Map<String, String> getApplicationProperties() {
        return this._applicationProperties;
    }

    public Map<String, StandbyNodeProperties> getClusterProperties() {
        return this._clusterList;
    }

    private void initialize() throws Exception {
        try {
            // clear the storage hashMaps
            getApplicationProperties().clear();

            // load the app.config into memory
            loadNodes(_xmlr.getDocument());
        } catch (Exception ex) {
            System.out.println(getClass().toString() + ", initialization, " + ex.getMessage());
        }
    }

    private void extractConfigFile(String filename) throws Exception {
        // create a reference to the location of the configuration file
        File cf = new File(filename);

        // if the file does not exist, try to extract it from the jar resource
        if (!cf.exists()) {
            // notify the user we are extracting the store app.config
            System.out.println(getClass().toString() + ", extractConfigFile, " + "extracting config file: " + filename);

            // create directories
            cf.getParentFile().mkdirs();

            // open the input stream from the jar resource
            BufferedReader configIFile = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filename)));

            // declare storage for the output file
            BufferedWriter configOFile = null;

            // if input file if valid, then extract the data
            if (configIFile != null) {
                try {
                    // open the output file
                    configOFile = new BufferedWriter(new FileWriter(cf));

                    // declare storage for the data from the input stream
                    String line = "";

                    // loop the config file, read each line until no more data
                    while ((line = configIFile.readLine()) != null) {
                        // write the data to the output file and insert the new
                        // line terminator after each line
                        configOFile.write(line + GlobalStack.LINESEPARATOR);

                        // yield processing to other threads
                        Thread.yield();
                    }

                    // notify user the status of the config file
                    System.out.println(getClass().toString() + ", extractConfigFile, " + "extraction complete.");
                } catch (Exception ex) {
                    // if exception during processing, return it to the user
                    System.out.println(getClass().toString() + ", extractConfigFile, " + ex.getMessage());
                    throw new Exception(ex);
                } finally {
                    // close the input file to prevent resource leaks
                    if (configIFile != null) {
                        try {
                            configIFile.close();
                        } catch (Exception exi) {
                        }
                    }

                    // close the output file to prevent resource leaks
                    if (configOFile != null) {
                        try {
                            configOFile.flush();
                        } catch (Exception exi) {
                        }
                        try {
                            configOFile.close();
                        } catch (Exception exi) {
                        }
                    }
                }
            }
        } else {
            // config file already existed, notify user we are using it
            System.out.println(getClass().toString() + ", extractConfigFile, " + "using config file: " + filename);
        }
    }

    protected void loadNodes(org.w3c.dom.Node node) throws Exception {
        // store the application properties.  these are custom properties and
        // can be changed without impacting the core property parsing
        // collects the list of attribute key names from the application block
        org.w3c.dom.NodeList attributes = _xmlr.getNodeListByXPath("/root/application/attributes/key/@name");
        ArrayList<String> attributesList = new ArrayList<String>();
        for (int i = 0; i < attributes.getLength(); i++) {
            attributesList.add(attributes.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // retrive the value of the attribute key name
        for (String key : attributesList) {
            String value = _xmlr.getNodeValueByXPath("/root/application/attributes/key[@name='" + key + "']");
            getApplicationProperties().put(key, value);

            // yield processing to other threads
            Thread.yield();
        }

        // open log file
        if (getApplicationProperties().get("log.config") != null) {
            Log4JManager.LOG4JCONFIG = (String) getApplicationProperties().get("log.config");

            extractConfigFile(Log4JManager.LOG4JCONFIG);
            Log4JManager l4j = new Log4JManager("NCSMessageProcessorLogger");
        }

        // load cluster list
        org.w3c.dom.NodeList svcNodes = _xmlr.getNodeListByXPath("/root/cluster/node/@id");
        ArrayList<String> clusterNodes = new ArrayList<String>();
        for (int i = 0; i < svcNodes.getLength(); i++) {
            clusterNodes.add(svcNodes.item(i).getNodeValue());
            Log4JManager.LOG.info(svcNodes.item(i).getNodeValue());

            // yield processing to other threads
            Thread.yield();
        }

        // load each cluster info via object translation
        for (String cluster : clusterNodes) {
            Log4JManager.LOG.info(".. loading config for cluster (" + cluster + ")");
            StandbyNodeProperties clusterNode = new StandbyNodeProperties();

            clusterNode.setId(cluster);
            clusterNode.setIpAddress(_xmlr.getNodeValueByXPath("/root/cluster/node[@id='" + cluster + "']/address"));

            try {
                clusterNode.setPort(Integer.parseInt(_xmlr.getNodeValueByXPath("/root/cluster/node[@id='" + cluster + "']/port")));
            } catch (Exception ex) {
                clusterNode.setPort(0);
            }

            try {
                clusterNode.setListener(Boolean.parseBoolean(_xmlr.getNodeValueByXPath("/root/cluster/node[@id='" + cluster + "']/listener")));
            } catch (Exception ex) {
                clusterNode.setListener(false);
            }

            try {
                clusterNode.setLevel(Integer.parseInt(_xmlr.getNodeValueByXPath("/root/cluster/node[@id='" + cluster + "']/level")));
            } catch (Exception ex) {
                clusterNode.setLevel(99999);
            }

            Log4JManager.LOG.info(".. " + cluster.toString());
            getClusterProperties().put(cluster, clusterNode);
        }
    }

    private void showConfig() {
        showConfigNodes(_xmlr.getDocument(), 1);

        //System.out.println("------------");
        //org.w3c.dom.NodeList nl = _xmlr.getNodesByElement("connections");
        //for (int i = 0; i < nl.getLength(); i++) {
        //    showConfigNodes(nl.item(i), 1);
        //}
        //System.out.println("------------");
        //nl = _xmlr.getNodesByElement("service");
        //for (int i = 0; i < nl.getLength(); i++) {
        //    System.out.println("---" + nl.item(i).getNodeName());
        //    showConfigNodes(nl.item(i), 1);
        //}
    }

    protected void showConfigNodes(org.w3c.dom.Node parent, int level) {
        // create a local class to display node value/text and associated
        // node attributes
        class SubShowNode {

            // loop through the node attributes for the node passed
            String displayNodeAttributes(org.w3c.dom.Node node) {
                // create string build object to support string concatanation
                StringBuilder sb = new StringBuilder();

                // retrieve node attributes (if any)
                ArrayList nAttributes = _xmlr.getNodeAttributes(node);

                // loop through the attributes array and append them to the
                // string builder object
                for (Object na : nAttributes) {
                    // append the attribute details (key/text) to the string
                    // builder object
                    sb.append(" [ATTR=" + ((org.w3c.dom.Node) na).getNodeName() + "//" + ((org.w3c.dom.Node) na).getNodeValue() + "]");

                    // yield processing to other threads
                    Thread.yield();
                }

                // return the string builder representation as a string
                return sb.toString();
            }
        }

        // declare the showNode class to allow methods to reference the display
        // method to prevent duplicaion in code
        SubShowNode showNode = new SubShowNode();

        // retrieve the child nodes for processing
        ArrayList nodes = _xmlr.getNodeChildren(parent);

        // if node level is 1, then this is root node, display it with no
        // indentation
        if (level == 1) {
            // display the parent node name
            String data = StringUtils.repeat('~', level) + parent.getNodeName();

            // use the sub function to extract node attributes
            data += showNode.displayNodeAttributes(parent);

            // display all collected data to the user output
            System.out.println(data);
        }

        // parse the list of child nodes for the node being processed
        for (Object node : nodes) {
            // display the parent node name
            String data = StringUtils.repeat('\t', level) + ((org.w3c.dom.Node) node).getNodeName();

            // use the sub function to extract node attributes
            data += showNode.displayNodeAttributes((org.w3c.dom.Node) node);

            // if node has a text value, display the text
            if (_xmlr.getNodeText((org.w3c.dom.Node) node) != null) {
                data += " (TEXT=" + _xmlr.getNodeText((org.w3c.dom.Node) node) + ")";
            }

            // display all collected data to the user output
            System.out.println(data);

            // recall the function (recursion) to see if the node has child 
            // nodes and preocess them in hierarchial level
            showConfigNodes((org.w3c.dom.Node) node, (level + 1));

            // yield processing to other threads
            Thread.yield();
        }
    }
}
