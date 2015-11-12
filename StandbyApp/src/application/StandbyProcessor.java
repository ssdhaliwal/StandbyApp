/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import interfaces.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyProcessor extends Thread implements IStandbyProcessor {

    private StandbyProperties _properties = null;
    private StandbyProcessingStatusType _processingStatus = null;

    public StandbyProcessor(StandbyProperties properties) {
        setProperties(properties);
        setProcessingStatusType(StandbyProcessingStatusType.INITIALIZED);

        initialize();
    }

    private void initialize() {
        getProperties().setProcessor(this);
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
            // loop through all apps defined (ip/port combination)
            // connect to each app
            // retrieve status
            // on error - update app state to error
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", run, " + ex.getMessage());
        } finally {
            setProcessingStatusType(StandbyProcessingStatusType.IDLE);
        }
    }

    @Override
    public boolean processorStart() throws Exception {
        if (getProcessingStatusType() != StandbyProcessingStatusType.RUNNING) {
            setProcessingStatusType(StandbyProcessingStatusType.STARTED);

            getProperties().getProcessor().start();
        }

        return true;
    }

    @Override
    public boolean processorStop() throws Exception {
        try {
            this.interrupt();
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", listenerStop, " + ex.getMessage());
        }

        getProperties().setProcessor(null);
        setProcessingStatusType(StandbyProcessingStatusType.STOPPED);
        return true;
    }
}
