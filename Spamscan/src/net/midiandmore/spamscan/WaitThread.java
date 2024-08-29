/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.midiandmore.spamscan;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author windo
 */
public class WaitThread implements Runnable {

    public WaitThread(Spamscan mi) {
        setMi(mi);
        (thread = new Thread(this)).start();
    }

    /**
     * @return the mi
     */
    public Spamscan getMi() {
        return mi;
    }

    /**
     * @param mi the mi to set
     */
    public void setMi(Spamscan mi) {
        this.mi = mi;
    }

    private Thread thread;
    private Spamscan mi;

    @Override
    public void run() {
        while (true) {
            try {
                getMi().getDb().createSchema();
                getMi().getDb().createTable();
                if (getMi().getSocketThread() == null) {
                    getMi().setSocketThread(new SocketThread(getMi()));
                } else if (getMi().getSocketThread().getSocket() == null) {
                    getMi().getSocketThread().setRuns(false);
                    getMi().setSocketThread(null);
                } else if (getMi().getSocketThread().isRuns()
                        && getMi().getSocketThread().getSocket().isClosed()) {
                    getMi().getSocketThread().setRuns(false);
                    getMi().setSocketThread(null);
                } else {
                    var set = getMi().getSocketThread().getFlood().keySet();
                    for (var nick : set) {
                        var flood = getMi().getSocketThread().getFlood().get(nick);
                        if (flood != 0) {
                            getMi().getSocketThread().getFlood().replace(nick, flood - 1);
                        }
                    }
                }
                thread.sleep(3000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(WaitThread.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }
        }
    }

}
