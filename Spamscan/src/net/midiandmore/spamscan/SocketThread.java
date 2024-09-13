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
     * @return the hosts
     */
    public HashMap<String, String> getHosts() {
        return hosts;
    }

    /**
     * @param hosts the hosts to set
     */
    public void setHosts(HashMap<String, String> hosts) {
        this.hosts = hosts;
    }

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
    private HashMap<String, Integer> repeat;
    private HashMap<String, ArrayList<String>> userChannels;
    private HashMap<String, String> userAccount;
    private HashMap<String, String> nicks;
    private HashMap<String, String> hosts;
    private HashMap<String, String> repeatLine;
    private boolean reg;

    public SocketThread(Spamscan mi) {
        setMi(mi);
        setChannels(new ArrayList<>());
        setFlood(new HashMap<>());
        setRepeat(new HashMap<>());
        setRepeatLine(new HashMap<>());
        setUserChannels(new HashMap<>());
        setNicks(new HashMap<>());
        setUserAccount(new HashMap<>());
        setHosts(new HashMap<>());
        setReg(false);
        (thread = new Thread(this)).start();
    }

    protected void handshake(String nick, String password, String servername, String description, String numeric, String identd) {
        System.out.println("Starting handshake...");
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
        System.out.println("Registering nick: " + getNick());
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
        if (getMi().getConfig().getConfigFile().getProperty("debug", "false").equalsIgnoreCase("true")) {
            System.out.printf("DEBUG sendText: %s\n", text.formatted(args));
        }
    }

    protected void parseLine(String text) {
        var p = getMi().getConfig().getConfigFile();
        text = text.trim();
        if (text.startsWith("SERVER")) {
            setServerNumeric(text.split(" ")[6].substring(0, 1));
            System.out.println("Getting SERVER response...");
        } else if (getServerNumeric() != null) {
            var elem = text.split(" ");
            if (elem[1].equals("N") && elem.length > 4) {
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
                getNicks().put(nick, elem[2]);
                getHosts().put(nick, elem[5] + "@" + elem[6]);
                getUserAccount().put(nick, acc);
            } else if (elem[1].equals("N") && elem.length == 4) {
                getNicks().replace(elem[0], elem[2]);
            } else if (elem[1].equals("AC")) {
                var acc = elem[3];
                var nick = elem[2];
                if (getUserAccount().containsKey(nick)) {
                    getUserAccount().replace(nick, acc);
                } else {
                    getUserAccount().put(nick, acc);
                }
            } else if (elem[1].equals("EB")) {
                sendText("%s EA", getNumeric());
                System.out.println("Handshake complete...");
                var list = getMi().getDb().getChannel();
                System.out.println("Joining " + list.size() + " channels...");
                for (var channel : list) {
                    joinChannel(channel);
                }
                System.out.println("Channels joined...");
                System.out.println("Successfully connected...");
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
                var notice = "O";
                if (!isNotice(nick)) {
                    notice = "P";
                }
                var auth = command.split(" ");
                if (isPrivileged(nick) && auth[0].equalsIgnoreCase("AUTH")) {
                    if (auth.length >= 3 && auth[1].equals(p.getProperty("authuser")) && auth[2].equals(p.getProperty("authpassword"))) {
                        setReg(true);
                        sendText("%sAAA %s %s :Successfully authed.", getNumeric(), notice, elem[0]);
                    } else {
                        sendText("%sAAA %s %s :Unknown command, or access denied.", getNumeric(), notice, elem[0]);
                    }
                } else if ((isPrivileged(nick) || isReg()) && auth.length >= 2 && auth[0].equalsIgnoreCase("ADDCHAN")) {
                    var channel = auth[1];
                    getMi().getDb().addChan(channel);
                    joinChannel(channel);
                    setReg(false);
                    sendText("%sAAA %s %s :Added channel %s", getNumeric(), notice, elem[0], channel);
                } else if ((isPrivileged(nick) || isReg()) && auth.length >= 2 && auth[0].equalsIgnoreCase("DELCHAN")) {
                    var channel = auth[1];
                    getMi().getDb().removeChan(channel);
                    partChannel(channel);
                    setReg(false);
                    sendText("%sAAA %s %s :Removed channel %s", getNumeric(), notice, elem[0], channel);
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
                                sendText("%sAAA %s %s :Badword (%s) allready exists.", getNumeric(), notice, elem[0], parsed);
                            } else if (flag.equalsIgnoreCase("DELETE")) {
                                b.remove(parsed.toLowerCase());
                                getMi().getConfig().saveDataToJSON("badwords-spamscan.json", b, "name", "value");
                                sendText("%sAAA %s %s :Badword (%s) successfully removed.", getNumeric(), notice, elem[0], parsed);
                            }
                        } else {
                            if (flag.equalsIgnoreCase("ADD")) {
                                b.put(parsed.toLowerCase(), "");
                                getMi().getConfig().saveDataToJSON("badwords-spamscan.json", b, "name", "value");
                                sendText("%sAAA %s %s :Badword (%s) successfully added.", getNumeric(), notice, elem[0], parsed);
                            } else if (flag.equalsIgnoreCase("DELETE")) {
                                sendText("%sAAA %s %s :Badword (%s) doesn't exists.", getNumeric(), notice, elem[0], parsed);
                            }
                        }
                    } else if (flag.equalsIgnoreCase("LIST")) {
                        sendText("%sAAA %s %s :--- Badwords ---", getNumeric(), notice, elem[0]);
                        for (var key : b.keySet()) {
                            sendText("%sAAA %s %s :%s", getNumeric(), notice, elem[0], key);
                        }
                        sendText("%sAAA %s %s :--- End of list ---", getNumeric(), notice, elem[0]);
                    } else {
                        sendText("%sAAA %s %s :Unknown flag.", getNumeric(), notice, elem[0]);
                    }
                } else if (auth[0].equalsIgnoreCase("SHOWCOMMANDS")) {
                    sendText("%sAAA %s %s :SpamScan Version %s", getNumeric(), notice, elem[0], VERSION);
                    sendText("%sAAA %s %s :The following commands are available to you:", getNumeric(), notice, elem[0]);
                    sendText("%sAAA %s %s :--- Commands available for users ---", getNumeric(), notice, elem[0]);
                    if (isPrivileged(nick)) {
                        sendText("%sAAA %s %s :ADDCHAN", getNumeric(), notice, elem[0]);
                        sendText("%sAAA %s %s :AUTH", getNumeric(), notice, elem[0]);
                        sendText("%sAAA %s %s :BADWORD", getNumeric(), notice, elem[0]);
                        sendText("%sAAA %s %s :DELCHAN", getNumeric(), notice, elem[0]);
                    }
                    sendText("%sAAA %s %s :HELP", getNumeric(), notice, elem[0]);
                    sendText("%sAAA %s %s :SHOWCOMMANDS", getNumeric(), notice, elem[0]);
                    sendText("%sAAA %s %s :VERSION", getNumeric(), notice, elem[0]);
                    sendText("%sAAA %s %s :End of list.", getNumeric(), notice, elem[0]);
                } else if (auth[0].equalsIgnoreCase("VERSION")) {
                    sendText("%sAAA %s %s :SpamScan v%s by %s", getNumeric(), notice, elem[0], VERSION, VENDOR);
                    sendText("%sAAA %s %s :By %s", getNumeric(), notice, elem[0], AUTHOR);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("ADDCHAN")) {
                    sendText("%sAAA %s %s :ADDCHAN <#channel>", getNumeric(), notice, elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("AUTH")) {
                    sendText("%sAAA %s %s :AUTH <requestname> <requestpassword>", getNumeric(), notice, elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("BADWORD")) {
                    sendText("%sAAA %s %s :BADWORD <ADD|LIST|DELETE> [badword]", getNumeric(), notice, elem[0]);
                } else if (isPrivileged(nick) && auth.length == 2 && auth[0].equalsIgnoreCase("HELP") && auth[1].equalsIgnoreCase("DELCHAN")) {
                    sendText("%sAAA %s %s :DELCHAN <#channel>", getNumeric(), notice, elem[0]);
                } else {
                    sendText("%sAAA %s %s :Unknown command, or access denied.", getNumeric(), notice, elem[0]);
                }
            } else if ((elem[1].equals("P") || elem[1].equals("O")) && getChannels().contains(elem[2].toLowerCase())) {
                if (!isPrivileged(getUserAccount().get(elem[0]))) {
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
                    if (!getRepeat().containsKey(nick)) {
                        getRepeat().put(nick, 0);
                        getRepeatLine().put(nick, command);
                    } else if (getRepeatLine().get(nick).equalsIgnoreCase(command)) {
                        var info = getRepeat().get(nick);
                        info = info + 1;
                        getRepeat().replace(nick, info);
                        if (info > 3) {
                            int count = getMi().getDb().getId();
                            count++;
                            getMi().getDb().addId("Repeating lines!");
                            sendText("%sAAA D %s %d : (You are violating network rules, ID: %d)", getNumeric(), nick, time(), count);
                            return;
                        }
                    } else {
                        getRepeat().replace(nick, 0);
                        getRepeatLine().replace(nick, command);
                    }
                    if (!getFlood().containsKey(nick)) {
                        getFlood().put(nick, 0);
                    } else {
                        var info = getFlood().get(nick);
                        info = info + 1;
                        getFlood().replace(nick, info);
                        if (info > 5) {
                            int count = getMi().getDb().getId();
                            count++;
                            getMi().getDb().addId("Flooding!");
                            sendText("%sAAA D %s %d : (You are violating network rules, ID: %d)", getNumeric(), nick, time(), count);
                            return;
                        }
                    }
                    var b = getMi().getConfig().getBadwordFile();
                    for (var key : b.keySet()) {
                        var key1 = (String) key;
                        if (command.toLowerCase().contains(key1.toLowerCase())) {
                            int count = getMi().getDb().getId();
                            count++;
                            getMi().getDb().addId("Using of badwords!");
                            sendText("%sAAA D %s %d : (You are violating network rules, ID: %d)", getNumeric(), nick, time(), count);
                            return;
                        }
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
                getNicks().remove(nick);
                getHosts().remove(nick);
                getUserAccount().remove(nick);
                if (getFlood().containsKey(nick)) {
                    getFlood().remove(nick);
                }
                if (getRepeat().containsKey(nick)) {
                    getRepeat().remove(nick);
                }
                if (getRepeatLine().containsKey(nick)) {
                    getRepeatLine().remove(nick);
                }
                if (getUserChannels().containsKey(nick)) {
                    getUserChannels().remove(nick);
                }
            } else if (elem[1].equals("D")) {
                var nick = elem[2];
                getNicks().remove(nick);
                getHosts().remove(nick);
                getUserAccount().remove(nick);
                if (getFlood().containsKey(nick)) {
                    getFlood().remove(nick);
                }
                if (getRepeat().containsKey(nick)) {
                    getRepeat().remove(nick);
                }
                if (getRepeatLine().containsKey(nick)) {
                    getRepeatLine().remove(nick);
                }
                if (getUserChannels().containsKey(nick)) {
                    getUserChannels().remove(nick);
                }
            }
        }
    }

    private boolean isNotice(String nick) {
        if (!nick.isBlank()) {
            var flags = getMi().getDb().getFlags(nick);
            return isNotice(flags);
        }
        return true;
    }

    private boolean isPrivileged(int flags) {
        if (!nick.isBlank()) {
            var oper = isOper(flags);
            if (oper == false) {
                oper = isAdmin(flags);
            }
            if (oper == false) {
                oper = isDev(flags);
            }
            return oper;
        }
        return false;
    }

    private boolean isPrivileged(String nick) {
        if (!nick.isBlank()) {
            var flags = getMi().getDb().getFlags(nick);
            var oper = isOper(flags);
            if (oper == false) {
                oper = isAdmin(flags);
            }
            if (oper == false) {
                oper = isDev(flags);
            }
            return oper;
        }
        return false;
    }

    private boolean isNoInfo(int flags) {
        return flags == 0;
    }

    private boolean isInactive(int flags) {
        return (flags & QUFLAG_INACTIVE) != 0;
    }

    private boolean isGline(int flags) {
        return (flags & QUFLAG_GLINE) != 0;
    }

    private boolean isNotice(int flags) {
        return (flags & QUFLAG_NOTICE) != 0;
    }

    private boolean isSuspended(int flags) {
        return (flags & QUFLAG_SUSPENDED) != 0;
    }

    private boolean isOper(int flags) {
        return (flags & QUFLAG_OPER) != 0;
    }

    private boolean isDev(int flags) {
        return (flags & QUFLAG_DEV) != 0;
    }

    private boolean isProtect(int flags) {
        return (flags & QUFLAG_PROTECT) != 0;
    }

    private boolean isHelper(int flags) {
        return (flags & QUFLAG_HELPER) != 0;
    }

    private boolean isAdmin(int flags) {
        return (flags & QUFLAG_ADMIN) != 0;
    }

    private boolean isInfo(int flags) {
        return (flags & QUFLAG_INFO) != 0;
    }

    private boolean isDelayedGline(int flags) {
        return (flags & QUFLAG_DELAYEDGLINE) != 0;
    }

    private boolean isNoAuthLimit(int flags) {
        return (flags & QUFLAG_NOAUTHLIMIT) != 0;
    }

    private boolean isCleanupExempt(int flags) {
        return (flags & QUFLAG_CLEANUPEXEMPT) != 0;
    }

    private boolean isStaff(int flags) {
        return (flags & QUFLAG_STAFF) != 0;
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
        System.out.println("Connecting to server...");
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
                if (getMi().getConfig().getConfigFile().getProperty("debug", "false").equalsIgnoreCase("true")) {
                    System.out.printf("DEBUG get text: %s\n", content);
                }
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
        System.out.println("Disconnected...");
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

    /**
     * @return the nicks
     */
    public HashMap<String, String> getNicks() {
        return nicks;
    }

    /**
     * @param nicks the nicks to set
     */
    public void setNicks(HashMap<String, String> nicks) {
        this.nicks = nicks;
    }

    /**
     * @return the repeat
     */
    public HashMap<String, Integer> getRepeat() {
        return repeat;
    }

    /**
     * @param repeat the repeat to set
     */
    public void setRepeat(HashMap<String, Integer> repeat) {
        this.repeat = repeat;
    }

    /**
     * @return the repeatLine
     */
    public HashMap<String, String> getRepeatLine() {
        return repeatLine;
    }

    /**
     * @param repeatLine the repeatLine to set
     */
    public void setRepeatLine(HashMap<String, String> repeatLine) {
        this.repeatLine = repeatLine;
    }
}
