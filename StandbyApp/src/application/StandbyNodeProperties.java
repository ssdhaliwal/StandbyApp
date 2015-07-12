/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package application;

import java.util.*;
import elsu.common.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyNodeProperties {
    private String _id = null;
    private String _ipAddress = "127.0.0.1";
    private int _port = 0;
    private boolean _listener = false;
    private int _level = 0;
    private StandbyNodeStateType _state = StandbyNodeStateType.UNKNOWN;
    private Date _lastStatus = DateStack.getDate();
    
    public StandbyNodeProperties() {
        initialize();
    }
    
    private void initialize() {
        setState(StandbyNodeStateType.UNKNOWN);
    }
    
    public String getId() {
        return this._id;
    }
    
    public void setId(String id) {
        this._id = id;
    }
    
    public String getIpAddress() {
        return this._ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this._ipAddress = ipAddress;
    }
    
    public int getPort() {
        return this._port;
    }
    
    public void setPort(int port) {
        this._port = port;
    }

    public boolean getListener() {
        return this._listener;
    }
    
    public void setListener(boolean listener) {
        this._listener = listener;
    }
    
    public int getLevel() {
        return this._level;
    }
    
    public void setLevel(int level) {
        this._level = level;
    }
    
    public StandbyNodeStateType getState() {
        return this._state;
    }
    
    public void setState(StandbyNodeStateType state) {
        this._state = state;
        setStatusDate();
    }
    
    public Date getStatusDate() {
        return this._lastStatus;
    }
    
    private void setStatusDate() {
        this._lastStatus = DateStack.getDate();
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("Object (" + getClass().getName() + ") ");
        result.append("{id: " + getId());
        result.append(", ipAddress: " + getIpAddress());
        result.append(", port: " + getPort());
        result.append(", listener: " + getListener());
        result.append(", state: " + getState());
        result.append(", statusDate: " + getStatusDate());
        result.append("}");
        
        return result.toString();
        
    }
}
