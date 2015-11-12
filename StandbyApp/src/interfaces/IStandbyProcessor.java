/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import application.*;

/**
 *
 * @author dhaliwal-admin
 */
public interface IStandbyProcessor {
    boolean processorStart() throws Exception;
    boolean processorStop() throws Exception;
}
