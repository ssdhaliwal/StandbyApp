/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyListenerConnection extends Thread {

    private StandbyProperties _properties = null;
    private StandbyProcessingStatusType _processingStatus = null;
    private volatile Socket _client = null;

    public StandbyListenerConnection(StandbyProperties properties, Socket client) {
        setProcessingStatusType(StandbyProcessingStatusType.INITIALIZED);

        setProperties(properties);
        setClient(client);

        getProperties().addConnection(this);
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

    public synchronized Socket getClient() {
        return this._client;
    }

    protected synchronized void setClient(Socket socket) {
        this._client = socket;

        try {
            this._client.setSoLinger(false, 0);
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", setClient, " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            setProcessingStatusType(StandbyProcessingStatusType.RUNNING);

            InputStream in = getClient().getInputStream();
            OutputStream out = getClient().getOutputStream();

            serve(in, out);
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", run, " + ex.getMessage());
        } finally {
            setProcessingStatusType(StandbyProcessingStatusType.STOPPED);

            getProperties().removeConnection(this);
        }
    }

    public void serve(InputStream iStream, OutputStream oStream) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(iStream));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(oStream)));

        try {
            if ((getProperties().getListener().getProcessingStatusType() == StandbyProcessingStatusType.RUNNING)
                    && (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING)) {
                String line = in.readLine();

                if ((line == null) || line.equals(".") || line.equals("\n")) {
                    Thread.yield();
                    return;
                }

                try {
                    String[] lineData = line.split(Pattern.quote(","));

                    // update the status of the node received to standby
                    getProperties().getNode(lineData[0]).setState(StandbyNodeStateType.valueOf(lineData[1]));
                    Thread.yield();

                    // send status of self
                    out.print(getProperties().getNodeStatus() + "\n");
                    out.flush();
                } catch (Exception ex) {
                    getProperties().logError(getClass().toString() + ", serve, internal, " + ex.getMessage());
                }

                // yield processing to other threads
                Thread.yield();
            }
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", serve, " + ex.getMessage());
        } finally {
            // flush the outbound stream and ignore any exception
            try {
                out.flush();
            } catch (Exception exi) {
            }

            // close all socket streams and ignore any exceptions
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
