/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.midiandmore.spamscan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts a new Thread
 *
 * @author Andreas Pschorn
 */
public class SocketThread implements Runnable, Software {

    /**
     * @return the userChannels
     */
    public HashMap<String, ArrayList<String>> getUserChannels() {
        return userChannels;
    }

    /**
     * @param userChannels the userChannels to set
     */
    public void setUserChannels(HashMap<String, ArrayList<String>> userChannels) {
        this.userChannels = userChannels;
    }

    /**
     * @return the flood
     */
    public HashMap<String, Integer> getFlood() {
        return flood;
    }

    /**
     * @param flood the flood to set
     */
    public void setFlood(HashMap<String, Integer> flood) {
        this.flood = flood;
    }

    /**
     * @return the channels
     */
    public ArrayList<String> getChannels() {
        return channels;
    }

    /**
     * @param channels the channels to set
     */
    public void setChannels(ArrayList<String> channels) {
        this.channels = channels;
    }

    /**
     * @return the nick
     */
    public String getNick() {
        return nick;
    }

    /**
     * @param nick the nick to set
     */
    public void setNick(String nick) {
        this.nick = nick;
    }

    /**
     * @return the identd
     */
    public String getIdentd() {
        return identd;
    }

    /**
     * @param identd the identd to set
     */
    public void setIdentd(String identd) {
        this.identd = identd;
    }

    /**
     * @return the servername
     */
    public String getServername() {
        return servername;
    }

    /**
     * @param servername the servername to set
     */
    public void setServername(String servername) {
        this.servername = servername;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the ip
     */
    public byte[] getIp() {
        return ip;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(byte[] ip) {
        this.ip = ip;
    }

    private Thread thread;
    private Spamscan mi;
    private Socket socket;
    private PrintWriter pw;
    private BufferedReader br;
    private boolean runs;
    private String serverNumeric;
    private String numeric;
    private String nick;
    private String identd;
    private String servername;
    private String description;
    private byte[] ip;
    private ArrayList<String> channels;
    private HashMap<String, Integer> flood;
    private HashMap<String, ArrayList<String>> userChannels;
    private HashMap<String, String> userAccount;
    private boolean reg;

    public SocketThread(Spamscan mi) {
        setMi(mi);
        setChannels(new ArrayList<>());
        setFlood(new HashMap<>());
        setUserChannels(new HashMap<>());
        setUserAccount(new HashMap<>());
        setReg(false);
        (thread = new Thread(this)).start();
    }

    protected void handshake(String nick, String password, String servername, String description, String numeric, String identd) {
        sendText("PASS :%s", password);
        sendText("SERVER %s %d %d %d J10 %s]]] :%s", servername, 1, time(), time(), numeric, description);
        var ia = getSocket().getInetAddress().getHostAddress();
        var li = String.valueOf(ipToInt(ia)).getBytes();
        setServername(servername);
        setNick(nick);
        setIdentd(identd);
        setDescription(description);
        setIp(li);
        setNumeric(numeric);
        sendText("%s N %s 1 %d %s %s +oikr %s %sAAA :%s", getNumeric(), getNick(), time(), getIdentd(), getServername(), getNick(), getNumeric(), getDescription());
        sendText("%s EB", numeric);
    }

    /**
     * Turns an IP address into an integer and returns this
     *
     * @param addr
     * @return
     */
    private int ipToInt(String addr) {
        String[] addrArray = addr.split("\\.");
        int[] num = new int[]{
            Integer.parseInt(addrArray[0]),
            Integer.parseInt(addrArray[1]),
            Integer.parseInt(addrArray[2]),
            Integer.parseInt(addrArray[3])
        };

        int result = ((num[0] & 255) << 24);
        result = result | ((num[1] & 255) << 16);
        result = result | ((num[2] & 255) << 8);
        result = result | (num[3] & 255);
        return result;
    }

    protected void sendText(String text, Object... args) {
        getPw().println(text.formatted(args));
        getPw().flush();
        System.out.println(text.formatted(args));
    }

    protected void parseLine(String text) {
        var p = getMi().getConfig().getConfigFile();
        text = text.trim();
        if (text.startsWith("SERVER")) {
            setServerNumeric(text.split(" ")[6].substring(0, 1));
        } else if (getServerNumeric() != null) {
            var elem = text.split(" ");
            if (elem[1].equals("N")) {
                var priv = elem[7].contains("r");
                var hidden = elem[7].contains("h");
                String acc = null;
                String nick = null;
                if (elem[8].contains(":")) {
                    acc = elem[8].split(":", 2)[0];
                    if (hidden) {
                        nick = elem[11];
                    } else {
                        nick = elem[10];
                    }
                } else {
                    acc = "";
                    if (hidden) {
                        nick = elem[10];
                    } else {
                        nick = elem[9];
                    }
                }
                getUserAccount().put(nick, acc);
                sendText("Added account %s for %s", acc, nick);
            } else if (elem[1].equals("AC")) {
                var acc = elem[3];
                var nick = elem[2];
                if (getUserAccount().containsKey(nick)) {
                    getUserAccount().replace(nick, acc);
                } else {
                    getUserAccount().put(nick, acc);
                }
                sendText("Added account %s for %s", acc, nick);
            } else if (elem[1].equals("EB")) {
                sendText("%s EA", getNumeric());
                var list = getMi().getDb().getChannel();
                for (var channel : list) {
                    joinChannel(channel);
                }
            } else if (elem[1].equals("G")) {
                sendText("%s Z %s", getNumeric(), text.substring(5));
            } else if (elem[1].equals("P") && elem[2].equals(getNumeric() + "AAA")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < elem.length; i++) {
                    sb.append(elem[i]);
                    sb.append(" ");
                }
                var command = sb.toString().trim();
                if (command.startsWith(":")) {
                    command = command.substring(1);
                }
                var nick = getUserAccount().get(elem[0]);
                var auth = command.split(" ");
                if (isPrivileged(nick) && auth[0].equalsIgnoreCase("AUTH")) {
                    if (auth.length >= 3 && auth[1].equals(p.getProperty("authuser")) && auth[2].equals(p.getProperty("authpassword"))) {
                        setReg(true);
                        sendText("%sAAA O %s :Successfully authed.", getNumeric(), elem[0]);
                    } else {
                        sendText("%sAAA O %s :Unknown command, or access denied.", getNumeric(), elem[0]);
                    }
                } else if ((isPrivileged(nick) || isReg()) && auth.length >= 2 && auth[0].equalsIgnoreCase("ADDCHAN")) {
                    var channel = auth[1];
                    getMi().getDb().addChan(channel);
                    joinChannel(channel);
                    setReg(false);
                    sendText("%sAAA O %s :Added channel %s", getNumeric(), elem[0], channel);
                } else if ((isPrivileged(nick) || isReg()) && auth.length >= 2 && auth[0].equalsIgnoreCase("DELCHAN")) {
                    var channel = auth[1];
                    getMi().getDb().removeChan(channel);
                    partChannel(channel);
                    setReg(false);
                    sendText("%sAAA O %s :Removed channel %s", getNumeric(), elem[0], channel);
                } else if (isPrivileged(nick) && auth.length >= 2 && auth[0].equalsIgnoreCase("BADWORD")) {
                    var flag = auth[1];
                    var b = getMi().getConfig().getBadwordFile();
                    if (flag.equalsIgnoreCase("ADD") || flag.equalsIgnoreCase("DELETE")) {
                        StringBuilder sb1 = new StringBuilder();
                        for (int i = 2; i < auth.length; i++) {
                            sb1.append(auth[i]);
                            sb1.append(" ");
                        }
                        var parsed = sb1.toString().trim();
                        if (b.containsKey(parsed.toLowerCase())) {
                            if (flag.equalsIgnoreCase("ADD")) {
                                sendText("%sAAA O %s :Badword (%s) allready exists.", getNumeric(), elem[0], parsed);
                            } else if (flag.equalsIgnoreCase("DELETE")) {
                                b.remove(parsed.toLowerCase());
                                getMi().getConfig().saveDataToJSON("badwords-spamscan.json", b, "name", "value");
                                sendText("%sAAA O %s :Badword (%s) successfully removed.", getNumeric(), elem[0], parsed);
                            }
                        } else {
                            if (flag.equalsIgnoreCase("ADD")) {
                                b.put(parsed.toLowerCase(), "");
                                getMi().getConfig().saveDataToJSON("badwords-spamscan.json", b, "name", "value");
                                sendText("%sAAA O %s :Badword (%s) successfully added.", getNumeric(), elem[0], parsed);
                            } else if (flag.equalsIgnoreCase("DELETE")) {
                                sendText("%sAAA O %s :Badword (%s) doesn't exists.", getNumeric(), elem[0], parsed);
                            }
                        }
                    } else if (flag.equalsIgnoreCase("LIST")) {
                        sendText("%sAAA O %s :--- Badwords ---", getNumeric(), elem[0]);
                        for (var key : b.keySet()) {
                            sendText("%sAAA O %s :%s", getNumeric(), elem[0], key);
                        }
                        sendText("%sAAA O %s :--- End of list ---", getNumeric(), elem[0]);
                    } else {
                        sendText("%sAAA O %s :Unknown flag.", getNumeric(), elem[0]);
                    }
                } else if (auth[0].equalsIgnoreCase("SHOWCOMMANDS")) {
                    sendText("%sAAA O %s :SpamScan Version %s", getNumeric(), elem[0], VERSION);
                    sendText("%sAAA O %s :The following commands are available to you:", getNumeric(), elem[0]);
                    sendText("%sAAA O %s :--- Commands available for users ---", getNumeric(), elem[0]);
                    if (isPrivileged(nick)) {
                        sendText("%sAAA O %s :ADDCHAN", getNumeric(), elem[0]);
                        sendText("%sAAA O %s :AUTH", getNumeric(), elem[0]);
                        sendText("%sAAA O %s :BADWORD", getNumeric(), elem[0]);
                        sendText("%sAAA O %s :DELCHAN", getNumeric(), elem[0]);
                    }
                    sendText("%sAAA O %s :HELP", getNumeric(), elem[0]);
                    sendText("%sAAA O %s :SHOWCOMMANDS", getNumeric(), elem[0]);
                    sendText("%sAAA O %s :VERSION", getNumeric(), elem[0]);
                    sendText("%sAAA O %s :End of list.", getNumeric(), elem[0]);
                } else if (auth[0].equalsIgnoreCase("VERSION")) {
                    sendText("%sAAA O %s :SpamScan v%s by %s", getNumeric(), elem[0], VERSION, VENDOR);
                    sendText("%sAAA O %s :By %s", getNumeric(), elem[0], AUTHOR);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("ADDCHAN")) {
                    sendText("%sAAA O %s :ADDCHAN <#channel>", getNumeric(), elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("AUTH")) {
                    sendText("%sAAA O %s :AUTH <requestname> <requestpassword>", getNumeric(), elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("BADWORD")) {
                    sendText("%sAAA O %s :BADWORD <ADD|LIST|DELETE> [badword]", getNumeric(), elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("DELCHAN")) {
                    sendText("%sAAA O %s :DELCHAN <#channel>", getNumeric(), elem[0]);
                } else {
                    sendText("%sAAA O %s :Unknown command, or access denied.", getNumeric(), elem[0]);
                }
            } else if ((elem[1].equals("P") || elem[1].equals("O")) && getChannels().contains(elem[2].toLowerCase())) {
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < elem.length; i++) {
                    if (elem[3].startsWith(":")) {
                        elem[3] = elem[3].substring(1);
                    }
                    sb.append(elem[i]);
                    sb.append(" ");
                }
                var command = sb.toString().trim();
                var nick = elem[0];
                if (!getUserChannels().containsKey(nick)) {
                    var list = new ArrayList<String>();
                    list.add(elem[2].toLowerCase());
                    getUserChannels().put(nick, list);
                } else {
                    var list = getUserChannels().get(nick);
                    if (!list.contains(elem[2].toLowerCase())) {
                        list.add(elem[2].toLowerCase());
                        getUserChannels().replace(nick, list);
                    }
                }
                if (!getFlood().containsKey(nick)) {
                    getFlood().put(nick, 0);
                } else {
                    var info = getFlood().get(nick);
                    info = info + 1;
                    getFlood().replace(nick, info);
                    if (info > 5) {
                        sendText("%sAAA D %s %d : You are violating network rules!", getNumeric(), nick, time());
                    }
                }
                var b = getMi().getConfig().getBadwordFile();
                for (var key : b.keySet()) {
                    var key1 = (String) key;
                    if(command.toLowerCase().contains(key1.toLowerCase())) {
                        sendText("%sAAA D %s %d : You are violating network rules!", getNumeric(), nick, time());
                        break;                        
                    }
                }
            } else if (elem[1].equals("L")) {
                var nick = elem[0];
                if (getUserChannels().containsKey(nick)) {
                    var list = getUserChannels().get(nick);
                    if (list.contains(elem[2].toLowerCase())) {
                        list.remove(elem[2].toLowerCase());
                        if (list.isEmpty()) {
                            getUserChannels().remove(nick);
                            getFlood().remove(nick);
                        } else {
                            getUserChannels().replace(nick, list);
                        }
                    }
                }
            } else if (elem[1].equals("K")) {
                var nick = elem[3];
                if (getUserChannels().containsKey(nick)) {
                    var list = getUserChannels().get(nick);
                    if (list.contains(elem[2].toLowerCase())) {
                        list.remove(elem[2].toLowerCase());
                        if (list.isEmpty()) {
                            getUserChannels().remove(nick);
                            getFlood().remove(nick);
                        } else {
                            getUserChannels().replace(nick, list);
                        }
                    }
                }
            } else if (elem[1].equals("Q")) {
                var nick = elem[0];
                getUserAccount().remove(nick);
                if (getFlood().containsKey(nick)) {
                    getFlood().remove(nick);
                }
                if (getUserChannels().containsKey(nick)) {
                    getUserChannels().remove(nick);
                }
            }
        }
        System.out.println(text);
    }

    private boolean isPrivileged(String nick) {
        if (!nick.isBlank()) {
            var flags = getMi().getDb().getFlags(nick);
            var oper = isOper(flags);
            if (oper == 0) {
                oper = isAdmin(flags);
            }
            if (oper == 0) {
                oper = isDev(flags);
            }
            return oper != 0;
        }
        return false;
    }

    private int isNoInfo(int flags) {
        return 0;
    }

    private int isInactive(int flags) {
        return (flags & QUFLAG_INACTIVE);
    }

    private int isGline(int flags) {
        return (flags & QUFLAG_GLINE);
    }

    private int isNotice(int flags) {
        return (flags & QUFLAG_NOTICE);
    }

    private int isSuspended(int flags) {
        return (flags & QUFLAG_SUSPENDED);
    }

    private int isOper(int flags) {
        return (flags & QUFLAG_OPER);
    }

    private int isDev(int flags) {
        return (flags & QUFLAG_DEV);
    }

    private int isProtect(int flags) {
        return (flags & QUFLAG_PROTECT);
    }

    private int isHelper(int flags) {
        return (flags & QUFLAG_HELPER);
    }

    private int isAdmin(int flags) {
        return (flags & QUFLAG_ADMIN);
    }

    private int isInfo(int flags) {
        return (flags & QUFLAG_INFO);
    }

    private int isDelayedGline(int flags) {
        return (flags & QUFLAG_DELAYEDGLINE);
    }

    private int isNoAuthLimit(int flags) {
        return (flags & QUFLAG_NOAUTHLIMIT);
    }

    private int isCleanupExempt(int flags) {
        return (flags & QUFLAG_CLEANUPEXEMPT);
    }

    private int isStaff(int flags) {
        return (flags & QUFLAG_STAFF);
    }

    private void joinChannel(String channel) {
        sendText("%sAAA J %s", getNumeric(), channel);
        sendText("%s M %s +o %sAAA", getNumeric(), channel, getNumeric());
        getChannels().add(channel.toLowerCase());
    }
    
    private void partChannel(String channel) {
        sendText("%sAAA L %s", getNumeric(), channel);
        getChannels().remove(channel.toLowerCase());
    }    

    private long time() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public void run() {
        setRuns(true);
        var host = getMi().getConfig().getConfigFile().getProperty("host");
        var port = getMi().getConfig().getConfigFile().getProperty("port");
        var password = getMi().getConfig().getConfigFile().getProperty("password");
        var nick = getMi().getConfig().getConfigFile().getProperty("nick");
        var servername = getMi().getConfig().getConfigFile().getProperty("servername");
        var description = getMi().getConfig().getConfigFile().getProperty("description");
        var numeric = getMi().getConfig().getConfigFile().getProperty("numeric");
        var identd = getMi().getConfig().getConfigFile().getProperty("identd");
        try {
            setSocket(new Socket(host, Integer.parseInt(port)));
            setPw(new PrintWriter(getSocket().getOutputStream()));
            setBr(new BufferedReader(new InputStreamReader(getSocket().getInputStream())));
            var content = "";
            handshake(nick, password, servername, description, numeric, identd);
            while (!getSocket().isClosed() && (content = getBr().readLine()) != null && isRuns()) {
                parseLine(content);
            }
        } catch (IOException | NumberFormatException ex) {
        }
        if (getPw() != null) {
            try {
                getPw().close();
            } catch (Exception ex) {
            }
        }
        if (getBr() != null) {
            try {
                getBr().close();
            } catch (IOException ex) {
            }
        }
        if (getSocket() != null && !getSocket().isClosed()) {
            try {
                getSocket().close();
            } catch (IOException ex) {
            }
        }
        setPw(null);
        setBr(null);
        setSocket(null);
        setRuns(false);
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

    /**
     * @return the socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @param socket the socket to set
     */
    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    /**
     * @return the pw
     */
    public PrintWriter getPw() {
        return pw;
    }

    /**
     * @param pw the pw to set
     */
    public void setPw(PrintWriter pw) {
        this.pw = pw;
    }

    /**
     * @return the br
     */
    public BufferedReader getBr() {
        return br;
    }

    /**
     * @param br the br to set
     */
    public void setBr(BufferedReader br) {
        this.br = br;
    }

    /**
     * @return the runs
     */
    public boolean isRuns() {
        return runs;
    }

    /**
     * @param runs the runs to set
     */
    public void setRuns(boolean runs) {
        this.runs = runs;
    }

    /**
     * @return the serverNumeric
     */
    public String getServerNumeric() {
        return serverNumeric;
    }

    /**
     * @param serverNumeric the serverNumeric to set
     */
    public void setServerNumeric(String serverNumeric) {
        this.serverNumeric = serverNumeric;
    }

    /**
     * @return the numeric
     */
    public String getNumeric() {
        return numeric;
    }

    /**
     * @param numeric the numeric to set
     */
    public void setNumeric(String numeric) {
        this.numeric = numeric;
    }

    /**
     * @return the reg
     */
    public boolean isReg() {
        return reg;
    }

    /**
     * @param reg the reg to set
     */
    public void setReg(boolean reg) {
        this.reg = reg;
    }

    /**
     * @return the userAccount
     */
    public HashMap<String, String> getUserAccount() {
        return userAccount;
    }

    /**
     * @param userAccount the userAccount to set
     */
    public void setUserAccount(HashMap<String, String> userAccount) {
        this.userAccount = userAccount;
    }
}
