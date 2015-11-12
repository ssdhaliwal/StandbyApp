/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import java.util.*;
import interfaces.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyController extends StandbyProperties implements IStandbyController {

    private StandbyProperties _properties = null;
    private StandbyProcessingStatusType _processingStatus = null;

    public StandbyController() throws Exception {
        try {
            setProcessingStatusType(StandbyProcessingStatusType.INITIALIZED);
            initialize();
        } catch (Exception ex) {
            System.out.println(getClass().toString() + ", constructor, " + ex.getMessage());
        }
    }

    private void initialize() throws Exception {
        try {
            StandbyProperties properties = new StandbyProperties();
            setProperties(properties);

            // start listener
            listenerStart();

            // register the app
            discovererStart();
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", initialization, " + ex.getMessage());
        }
    }

    public StandbyProperties getProperties() {
        return _properties;
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

    public boolean listenerStart() {
        // initialize listener
        StandbyListener listener = new StandbyListener(getProperties());
        listener.listenerStart();
        return true;
    }

    public boolean listenerStop() {
        // stop listener
        if (getListener() != null) {
            getListener().listenerStop();
        }

        return true;
    }

    public boolean discovererStart() throws Exception {
        // initialize discovery
        StandbyDiscoverer discoverer = new StandbyDiscoverer(getProperties());
        discoverer.discovererStart();
        return true;
    }

    public boolean discovererStop() throws Exception {
        // stop discoverer
        if (getDiscoverer() != null) {
            getDiscoverer().discovererStop();
        }

        return true;
    }

    public boolean processorStart() throws Exception {
        StandbyProcessor processor = new StandbyProcessor(getProperties());
        processor.processorStart();

        return true;
    }

    public boolean processorStop() throws Exception {
        // stop processor
        if (getProcessor() != null) {
            getProcessor().processorStop();
        }

        return true;
    }

    @Override
    public boolean run() throws Exception {
        try {
            // ** test code for random role change
            getProperties().setRoleChangeCount();
            getProperties().logInfo(getClass().toString() + ", run, " + "pause loop: " + getProperties().getRoleChangeCount());
            // ** test code for random role change (between 25 and 50)

            setProcessingStatusType(StandbyProcessingStatusType.RUNNING);

            while (getProcessingStatusType() == StandbyProcessingStatusType.RUNNING) {
                // ** test code for random role change (between 25 and 50)
                getProperties().decrementRoleChangeCount();

                if (getProperties().getRoleChangeCount() <= 0) {
                    getProperties().setRoleChangeCount();
                    getProperties().logInfo(getClass().toString() + ", run, " + "pause loop: " + getProperties().getRoleChangeCount());

                    if (getProperties().getStandbyNodeCount() > 1) {
                        getProperties().getDiscoverer().discovererStop();
                        getProperties().getListener().listenerStop();

                        System.out.println("..... pausing for 4 mins .....");
                        Thread.sleep(240000);           // 4 mins

                        getProperties().setInitialDiscovery(false);
                        listenerStart();
                        discovererStart();
                    }
                }
                // ** test code for random role change (between 25 and 50)

                // if discoverer is running, continue loop with wait
                if (!getProperties().getInitialDiscovery()) {
                    Thread.sleep(getProperties().getIdleWait());
                } else {
                    // if no primary node and 
                    StandbyNodeProperties primaryNode = getProperties().getPrimaryNode();

                    if (primaryNode == null) {
                        //  node level below current node is standby
                        if (getProperties().getLowestAvailableNode().getLevel() == getProperties().getNode().getLevel()) {
                            // set self to primary
                            getProperties().getNode().setState(StandbyNodeStateType.PRIMARY);
                            // start processor
                        }

                        // continue loop with wait
                        Thread.sleep(getProperties().getIdleWait());
                    } else {
                        Thread.sleep(getProperties().getIdleWait());
                    }
                }
            }
        } catch (Exception ex) {
            getProperties().logError(getClass().toString() + ", run, " + ex.getMessage());
        } finally {
            setProcessingStatusType(StandbyProcessingStatusType.IDLE);

            getProperties().getDiscoverer().discovererStop();
            getProperties().getListener().listenerStop();

            setProcessingStatusType(StandbyProcessingStatusType.STOPPED);
        }

        return true;
    }
}
