/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import interfaces.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyListener extends Thread implements IStandbyListener {

    private StandbyProperties _properties = null;
    private StandbyProcessingStatusType _processingStatus = null;
    private volatile ServerSocket _listenerSocket = null;
    private int _listenerPort = 0;

    public StandbyListener(StandbyProperties properties) {
        setProperties(properties);
        setProcessingStatusType(StandbyProcessingStatusType.INITIALIZED);

        initialize();
    }

    private void initialize() {
        getProperties().setListener(this);

        // extract local values for quick reference
        ArrayList<String> clusters = new ArrayList<String>(getProperties().getClusterProperties().keySet());
        for (String node : clusters) {
            StandbyNodeProperties nodeProperties = getProperties().getClusterProperties().get(node);

            if (nodeProperties.getListener()) {
                setListenerPort(nodeProperties.getPort());
            }
        }
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

    public ServerSocket getListenerSocket() {
        return this._listenerSocket;
    }

    public void setListenerSocket(ServerSocket socket) {
        this._listenerSocket = socket;
    }

    public int getListenerPort() {
        return this._listenerPort;
    }

    private void setListenerPort(int port) {
        this._listenerPort = port;
    }

    @Override
    public void run() {
        try {
            setProcessingStatusType(StandbyProcessingStatusType.RUNNING);
            getProperties().getNode().setState(StandbyNodeStateType.STANDBY);

            while (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING) {
                Socket client = getListenerSocket().accept();

                if (client != null) {
                    StandbyListenerConnection connection = new StandbyListenerConnection(getProperties(), client);
                    connection.start();
                }
            }
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", run, " + ex.getMessage());
        } finally {
            setProcessingStatusType(StandbyProcessingStatusType.IDLE);
            listenerStop();
        }
    }

    @Override
    public boolean listenerStart() {
        try {
            // create listener for the app address/port
            setListenerSocket(new ServerSocket(getListenerPort()));

            // give it a non-zero timeout so accept() can be interrupted
            getListenerSocket().setSoTimeout(getProperties().getConnectionTimeout());
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", listenerStart, " + ex.getMessage());
        }

        // set listener state to running
        setProcessingStatusType(StandbyProcessingStatusType.STARTED);

        getProperties().getListener().start();
        return true;
    }

    @Override
    public boolean listenerStop() {
        try {
            this.interrupt();

            // stop the listener (no new connections)
            getListenerSocket().close();
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", listenerStop, " + ex.getMessage());
        }

        // loop through and remove all active connections (force close)
        getProperties().clearConnections();

        getProperties().setListener(null);
        setProcessingStatusType(StandbyProcessingStatusType.STOPPED);
        return true;
    }
}
