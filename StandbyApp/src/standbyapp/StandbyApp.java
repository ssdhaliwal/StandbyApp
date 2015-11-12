/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package standbyapp;

import application.*;
import interfaces.*;

/**
 *
 * @author dhaliwal-admin
 */
public class StandbyApp {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            IStandbyController sc = new StandbyController();
            sc.run();
        } catch (Exception ex) {
            System.out.println(ex.getMessage() + "\n" + ex.getStackTrace());
        }
    }
}
