/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import java.util.*;
import elsu.support.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyProperties extends StandbyConfig {

    private volatile StandbyListener _listener = null;
    private volatile StandbyDiscoverer _discoverer = null;
    private volatile StandbyProcessor _processor = null;
    private volatile Map<String, Object> _nodeList = null;
    private volatile int _activeConnections = 0;
    private volatile ArrayList<StandbyListenerConnection> _connections = null;
    private int _connectionTimeout = 60000;
    private volatile int _idleWait = 15000;
    private volatile boolean _initialDiscoveryRunning = false;
    private volatile int _roleChangeCount = 0;

    public StandbyProperties() throws Exception {
        initialize();
    }

    private void initialize() throws Exception {
        // initialize the connection array
        try {
            this._connections = new ArrayList<StandbyListenerConnection>();
            setConnectionTimeout(Integer.valueOf(getApplicationProperties().get("connection.timeout").toString()) * 1000);
            setIdleWait(Integer.parseInt(getApplicationProperties().get("idle.wait")) * 1000);
        } catch (Exception ex) {
            System.out.println(getClass().toString() + ", initialization, " + ex.getMessage());
        }
    }

    public StandbyListener getListener() {
        return this._listener;
    }

    public void setListener(StandbyListener listener) {
        this._listener = listener;
    }

    public StandbyDiscoverer getDiscoverer() {
        return this._discoverer;
    }

    public void setDiscoverer(StandbyDiscoverer discoverer) {
        this._discoverer = discoverer;
    }

    public StandbyProcessor getProcessor() {
        return this._processor;
    }

    public void setProcessor(StandbyProcessor processor) {
        this._processor = processor;
    }

    public Map<String, Object> getNodeList() {
        return this._nodeList;
    }

    public int getActiveConnectionsCount() {
        return this._activeConnections;
    }

    public void addConnection(StandbyListenerConnection connection) {
        this._connections.add(connection);
        this._activeConnections++;
    }

    public void removeConnection(StandbyListenerConnection connection) {
        this._connections.remove(connection);
        this._activeConnections--;
    }

    public void clearConnections() {
        for (StandbyListenerConnection connection : this._connections) {
            connection.setProcessingStatusType(StandbyProcessingStatusType.STOPPED);
            connection.interrupt();
        }

        this._activeConnections = 0;
        this._connections.clear();
    }

    public int getConnectionTimeout() {
        return this._connectionTimeout;
    }

    private void setConnectionTimeout(int connectionTimeout) {
        this._connectionTimeout = connectionTimeout;
    }

    public int getIdleWait() {
        return this._idleWait;
    }

    public void setIdleWait(int wait) {
        this._idleWait = wait;
    }

    public boolean getInitialDiscovery() {
        return this._initialDiscoveryRunning;
    }

    public void setInitialDiscovery(boolean running) {
        this._initialDiscoveryRunning = running;
    }

    public int getRoleChangeCount() {
        return this._roleChangeCount;
    }

    public void setRoleChangeCount() {
        // set the role change value (between 25 and 50)
        Random rnd = new Random();
        this._roleChangeCount = rnd.nextInt(50 - 25 + 1) + 25;
    }

    public int decrementRoleChangeCount() {
        return this._roleChangeCount--;
    }

    public synchronized void logDebug(Object info) {
        if (Log4JManager.LOG != null) {
            Log4JManager.LOG.debug(info.toString());
        }
    }

    public synchronized void logError(Object info) {
        if (Log4JManager.LOG != null) {
            Log4JManager.LOG.error(info.toString());
        }
    }

    public synchronized void logInfo(Object info) {
        if (Log4JManager.LOG != null) {
            Log4JManager.LOG.info(info.toString());
        }
    }

    public StandbyNodeProperties getNode() {
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());
        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getListener()) {
                return nodeProperties;
            }
        }

        return null;
    }

    public StandbyNodeProperties getNode(String id) {
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());
        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getId().equals(id)) {
                return nodeProperties;
            }
        }

        return null;
    }

    public String getNodeStatus() {
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());
        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getListener()) {
                return nodeProperties.getId() + "," + nodeProperties.getState().toString();
            }
        }

        return null;
    }

    public int getActiveNodeCount() {
        int result = 0;
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());

        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if ((nodeProperties.getState() == StandbyNodeStateType.PRIMARY)
                    || (nodeProperties.getState() == StandbyNodeStateType.STANDBY)) {
                result++;
            }
        }

        return result;
    }

    public int getStandbyNodeCount() {
        int result = 0;
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());

        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getState() == StandbyNodeStateType.STANDBY) {
                result++;
            }
        }

        return result;
    }

    public StandbyNodeProperties getPrimaryNode() {
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());
        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getState() == StandbyNodeStateType.PRIMARY) {
                return nodeProperties;
            }
        }

        return null;
    }

    public StandbyNodeProperties getLowestAvailableNode() {
        ArrayList<String> clusters = new ArrayList<String>(getClusterProperties().keySet());

        int level = 99999;
        StandbyNodeProperties lowestAvailableNode = null;

        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getClusterProperties().get(node);

            if (nodeProperties.getState() == StandbyNodeStateType.PRIMARY) {
                lowestAvailableNode = null;
                break;
            } else if (nodeProperties.getState() == StandbyNodeStateType.STANDBY) {
                if (nodeProperties.getLevel() < level) {
                    level = nodeProperties.getLevel();
                    lowestAvailableNode = nodeProperties;
                }
            }
        }

        return lowestAvailableNode;
    }
}
