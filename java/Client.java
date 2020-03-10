import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/* Simple command line client to send requests to a server.
 * Compile:    javac Client
 * Show usage: java  Client
 **/
public class Client {

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        Socket s;
        try {
            s = new Socket(ip, port);
        } catch (IOException e) {
            System.out.println("failed to connect to " + ip + ":" + port + ": " + e.getMessage());
            return;
        }

        OutputStream out = s.getOutputStream();
        InputStream in = s.getInputStream();

        // write request
        for (int i = 2; i < args.length; i++) {
            writeString(out, args[i] + "\n");
        }
        writeString(out, "\n");

        // read response
        StringBuilder sb = new StringBuilder();
        while (readLine(sb, in) > 1) {
            System.out.println(sb.toString());
            sb = new StringBuilder();
        }

        s.close();
    }

    private static void printUsage() {
        System.out.println("ABOUT");
        System.out.println("    CLI client for sending requests to bank-server (see github.com/mikenoethiger/bank-server)");
        System.out.println("USAGE");
        System.out.println("    java Client <ip> <port> <action> [arguments]");
        System.out.println("ACTIONS");
        System.out.println(
                "    Get Account Numbers: 1\n" +
                "    Get Account:         2 account_number\n" +
                "    Create Account:      3 owner\n" +
                "    Close Account:       4 account_number\n" +
                "    Transfer:            5 from_account_number to_accoutn_number amount\n" +
                "    Deposit:             6 account_number amount\n" +
                "    Withdraw:            7 account_number amount"
                );
    }

    private static void writeString(OutputStream out, String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write(s.charAt(i));
        }
    }

    private static int readLine(StringBuilder sb, InputStream in) throws IOException {
        int bytes = 0;
        int buf;
        while ((buf = in.read()) != -1) {
            bytes++;
            if ((char) buf == '\n') break;
            sb.append((char) buf);
        }
        return bytes;
    }
}
