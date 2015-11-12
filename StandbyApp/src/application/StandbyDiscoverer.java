/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import java.util.*;
import java.util.regex.*;
import java.net.*;
import java.io.*;
import interfaces.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyDiscoverer extends Thread implements IStandbyDiscoverer {

    private StandbyProperties _properties = null;
    private StandbyProcessingStatusType _processingStatus = null;

    public StandbyDiscoverer(StandbyProperties properties) {
        setProperties(properties);
        setProcessingStatusType(StandbyProcessingStatusType.INITIALIZED);

        initialize();
    }

    private void initialize() {
        getProperties().setDiscoverer(this);
        getProperties().setInitialDiscovery(false);
    }

    public StandbyProperties getProperties() {
        return this._properties;
    }

    private void setProperties(StandbyProperties properties) {
        this._properties = properties;
    }

    public StandbyProcessingStatusType getProcessingStatusType() {
        return this._processingStatus;
    }

    public void setProcessingStatusType(StandbyProcessingStatusType processingStatus) {
        this._processingStatus = processingStatus;
    }

    @Override
    public void run() {
        try {
            setProcessingStatusType(StandbyProcessingStatusType.RUNNING);
            ArrayList<String> clusters = new ArrayList<String>(getProperties().getClusterProperties().keySet());

            while (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING) {
                // loop through all apps defined (ip/port combination)
                String status = getProperties().getNodeStatus();

                for (String node : clusters) {
                    StandbyNodeProperties nodeProperties = null;

                    if (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING) {
                        BufferedReader in = null;
                        PrintWriter out = null;

                        try {
                            // connect to each app
                            nodeProperties = getProperties().getClusterProperties().get(node);

                            Socket client = new Socket(nodeProperties.getIpAddress(), nodeProperties.getPort());
                            client.setSoLinger(false, 0);

                            // retrieve status
                            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())));

                            // loop as long as the service is running and the connection is
                            // active
                            if ((getProcessingStatusType() == StandbyProcessingStatusType.RUNNING)
                                    && !client.isClosed()) {
                                out.write(status + "\n");
                                out.flush();
                            }

                            if ((getProcessingStatusType() == StandbyProcessingStatusType.RUNNING)
                                    && !client.isClosed()) {
                                String line = in.readLine();

                                String[] lineData = line.split(Pattern.quote(","));
                                getProperties().getNode(lineData[0]).setState(StandbyNodeStateType.valueOf(lineData[1]));
                                Thread.yield();

                                System.out.println("[" + getProperties().getRoleChangeCount() + "], "
                                        + getProperties().getNode().getId() + " -> " + (line.contains("PRIMARY") ? " >> " : " ++ ")
                                        + line + (line.contains("PRIMARY") ? " << " : " ++ "));
                            }
                        } catch (Exception ex) {
                            nodeProperties.setState(StandbyNodeStateType.NONRESPONSIVE);
                            System.out.println("[" + getProperties().getRoleChangeCount() + "], "
                                    + getProperties().getNode().getId() + " -> " + " -- " + 
                                    nodeProperties.getId() + "," + nodeProperties.getState().toString() + " -- ");
                            //getProperties().logError(getClass().toString() + ", run, internal, " + ex.getMessage());
                        } finally {
                            try {
                                out.close();
                            } catch (Exception exi) {
                            }
                            try {
                                in.close();
                            } catch (Exception exi) {
                            }
                        }
                    }
                }

                System.out.println();
                getProperties().setInitialDiscovery(true);
                Thread.sleep(getProperties().getIdleWait());
            }
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", run, " + ex.getMessage());
        } finally {
            setProcessingStatusType(StandbyProcessingStatusType.IDLE);
        }
    }

    @Override
    public boolean discovererStart() throws Exception {
        // ensure discoverer is not already running
        if (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING) {
        } else {
            setProcessingStatusType(StandbyProcessingStatusType.STARTED);
            getProperties().getDiscoverer().start();
        }

        return true;
    }

    @Override
    public boolean discovererStop() throws Exception {
        try {
            this.interrupt();
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", discovererStop, " + ex.getMessage());
        }

        getProperties().setDiscoverer(null);
        setProcessingStatusType(StandbyProcessingStatusType.STOPPED);

        return true;
    }
}
