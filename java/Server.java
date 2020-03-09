
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.StringBuilder;
import java.lang.NumberFormatException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.Runnable;

public class Server {

	/* error responses according to protocol specification (see readme.md) */
	private static final String[] ERROR_INTERNAL_ERROR = {"nok", "0", "Internal error."};
	private static final String[] ERROR_ACCOUNT_DOES_NOT_EXIST = {"nok", "1", "Account does not exist."};
	private static final String[] ERROR_ACCOUNT_COULD_NOT_BE_CREATED = {"nok", "2", "Account could not be created."};
	private static final String[] ERROR_ACCOUNT_COULD_NOT_BE_CLOSED = {"nok", "3", "Account could not be closed."};
	private static final String[] ERROR_INACTIVE_ACCOUNT = {"nok", "4", "Inactive account."};
	private static final String[] ERROR_ACCOUNT_OVERDRAW = {"nok", "5", "Account overdraw."};
	private static final String[] ERROR_ILLEGAL_ARGUMENT = {"nok", "6", "Illegal argument."};
	private static final String[] ERROR_BAD_REQUEST = {"nok", "7", "Bad request."};

	/* request/response delimiter according to protocol specification (see readme.md) */
	private static final char DELIMITER = '\n';

	/* pool size for thread pool which handles requests */
	private static final int POOL_SIZE = 50;

	/* states for request reading algorithm (see readRequest())*/
	private static final int STATE_READ = 0;
	private static final int STATE_LINE_BREAK = 1;

	/* Bank instance for manipulating bank data.
	   The current implementation stores all data
	   inside the bank instance. I.e. alla data
	   will be lost upon program termination.
	   This could be changed with another bank
	   implementaion.
	*/
	private static final Bank BANK = new Bank();

    public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			printUsage();
			return;
		}
		int port = Integer.parseInt(args[0]);

		ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        ServerSocket server = new ServerSocket(port);
		System.out.println("listening...");

		try {
			while (true) {
				pool.execute(new ConnectionHandler(server.accept(), BANK));
			}
		} catch (IOException e) {
			pool.shutdown();
		}
    }

	private static void printUsage() {
		System.out.println("ABOUT");
		System.out.println("    Hypothetical bank-server (see github.com/mikenoethiger/bank-server).");
		System.out.println("    Starts to listen for connections on specified port.");
		System.out.println("USAGE");
		System.out.println("    java Server <port>");
	}

	/* Handles one connection, i.e. one client. Instantiate multiple
	   ConnectionHandler's and run each in a separate thread in order
	   to serve several clients in parallel. The handler starts to read
	   and process requests from the socket as soon as run() is called
	   run() returns as soon as the client closes the socket connection.

	   It would be reasonable to close the connection from the server side
	   if no request was received for a longer time period. This behavior
	   is currently not implemented.
	*/
	private static class ConnectionHandler implements Runnable {

		private final Socket socket;
		private final Bank bank; /* this is where the accounts are stored (i.e. in memory) */

		ConnectionHandler(Socket socket, Bank bank) throws IOException {
			this.socket = socket;
			this.bank = bank;
		}

		public void run() {
			InputStream in;
			DataOutputStream out;
			OutputStream out2;
			InetSocketAddress remote;

			try {
				in = socket.getInputStream();
				out = new DataOutputStream(socket.getOutputStream());
				out2 = socket.getOutputStream();
				remote = (InetSocketAddress) socket.getRemoteSocketAddress();
				System.out.println("connected to " + remote.getHostName() + "...");

				String[] request = readRequest(in);

				while (request.length > 0) {
					System.out.println("request: " + Arrays.toString(request));
					String[] response = processRequest(request);
					System.out.println("response: " + Arrays.toString(response));
					for (String line : response) {
						writeString(out2, line);
						writeString(out2, "\n");
						// for (int i = 0; i < line.length(); i++) {
						// 	out2.write(line.charAt(i));
						// }
						// out2.write('\n');
					}
					writeString(out2, "\n");
					//out2.write('\n');
					request = readRequest(in);
				}
				System.out.println("disconnected from " + remote.getHostName() + "...");
				out.close();
				socket.close();
			} catch (IOException e) {
				System.out.println("failed to handle connection");
			} finally {
				try { socket.close(); }
				catch (IOException e) {}
			}
		}

		/**
	     * Write a string to out.
	     *
	     * Writes each character as 1 byte as opposed to
	     * {@link DataOutputStream#writeChars(String)}
	     * which writes each character as 2 bytes.
	     *
	     * @param out stream to write to
	     * @param s string to write
	     * @throws IOException
	     */
	    private static void writeString(OutputStream out, String s) throws IOException {
	        for (int i = 0; i < s.length(); i++) {
	            out.write(s.charAt(i));
	        }
	    }

		private static String[] readRequest(InputStream in) throws IOException {
			int buf;
			StringBuilder sb = new StringBuilder();
			List<String> request = new ArrayList<>();
			int state = STATE_READ;
			while ((buf = in.read()) != -1) {
				if (state == STATE_LINE_BREAK) {
					// double line break denotes end of request
					if (buf == DELIMITER) break;
					else {
						sb.append((char) buf);
						state = STATE_READ;
					}
				} else { // STATE_READ
					if (buf == DELIMITER) {
						request.add(sb.toString());
						sb = new StringBuilder();
						state = STATE_LINE_BREAK;
					} else {
						sb.append((char) buf);
					}
				}
			}

			String[] request_arr = new String[request.size()];
			request_arr = request.toArray(request_arr);
			return request_arr;
		}

		private String[] processRequest(String[] request) {
			if (request.length < 1) return ERROR_BAD_REQUEST;

			int action;
			try { action = Integer.parseInt(request[0]); }
			catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

			switch (action) {
				case 1: return getAccountNumbers(request);
				case 2: return getAccount(request);
				case 3: return createAccount(request);
				case 4: return closeAccount(request);
				case 5: return transfer(request);
				case 6: return deposit(request);
				case 7: return withdraw(request);
				default: return ERROR_BAD_REQUEST;
			}
		}

		/* ------------------
		    ACTION HANDLES
		   ------------------ */

		private String[] getAccountNumbers(String[] request) {
			Set<String> accounts = bank.getAccountNumbers();
			String[] response = new String[accounts.size()+1];
			response[0] = "ok";
			int i = 1;
			for (String a : accounts) {
				response[i++] = a;
			}
			return response;
		}

		private String[] getAccount(String[] request) {
			if (request.length < 2) return ERROR_BAD_REQUEST;
			Account a = bank.getAccount(request[1]);
			if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;
			return new String[]{"ok", a.getNumber(), a.getOwner(), String.valueOf(a.getBalance()), a.isActive() ? "1" : "0"};
		}

		private String[] createAccount(String[] request) {
			if (request.length < 2) return ERROR_BAD_REQUEST;
			Account account = bank.createAccount(request[1]);
			if (account == null) return ERROR_ACCOUNT_COULD_NOT_BE_CREATED;
			return new String[]{"ok", account.getNumber(), account.getOwner(), String.valueOf(account.getBalance()), account.isActive() ? "1" : "0" };
		}

		private String[] closeAccount(String[] request) {
			if (request.length < 2) return ERROR_BAD_REQUEST;
			boolean result = bank.closeAccount(request[1]);
			if (result) return new String[]{"ok"};
			else return ERROR_ACCOUNT_COULD_NOT_BE_CLOSED;
		}

		private String[] transfer(String[] request) {
			if (request.length < 4) return ERROR_BAD_REQUEST;

			// parse accounts
			Account from = bank.getAccount(request[1]);
			Account to = bank.getAccount(request[2]);
			if (from == null || to == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

			// parse amount
			double amount;
			try { amount = Double.parseDouble(request[3]); }
			catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

			// transfer money
			try { bank.transfer(from, to, amount); }
			catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
			catch (OverdrawException e) { return ERROR_ACCOUNT_OVERDRAW; }
			catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

			return new String[]{"ok", String.valueOf(from.getBalance()), String.valueOf(to.getBalance())};
		}

		private String[] deposit(String[] request) {
			if (request.length < 3) return ERROR_BAD_REQUEST;

			// parse account
			Account a = bank.getAccount(request[1]);
			if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

			// parse amount
			double amount;
			try { amount = Double.parseDouble(request[2]); }
			catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

			try { a.deposit(amount); }
			catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
			catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

			return new String[]{"ok", String.valueOf(a.getBalance())};
		}

		private String[] withdraw(String[] request) {
			if (request.length < 3) return ERROR_BAD_REQUEST;

			// parse account
			Account a = bank.getAccount(request[1]);
			if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

			// parse amount
			double amount;
			try { amount = Double.parseDouble(request[2]); }
			catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

			try { a.withdraw(amount); }
			catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
			catch (OverdrawException e) { return ERROR_ACCOUNT_OVERDRAW; }
			catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

			return new String[]{"ok", String.valueOf(a.getBalance())};
		}
	}

	public static class Bank {

		private static final int MAX_ACCOUNTS = 500;

		private static int accounts_num = 0;
		private final Object accounts_num_lock = new Object();

		private final Object transfer_lock = new Object();

		private final Map<String, Account> accounts = new HashMap<>();

		public Set<String> getAccountNumbers() {
			return accounts.values().stream().filter(Account::isActive).map(Account::getNumber).collect(Collectors.toSet());
		}

		/* thread safe */
		public Account createAccount(String owner) {
			// limit memory usage
			if (accounts_num > MAX_ACCOUNTS) return null;
			Account a = new Account(owner);
			accounts.put(a.number, a);
			synchronized (accounts_num_lock) {
				accounts_num++;
			}
			return a;
		}

		/* thread safe */
		public boolean closeAccount(String number) {
			if (!accounts.containsKey(number)) return false;
			Account a = accounts.get(number);
			/* we use the transfer_lock because changing activeness
			   might interfere with changing balances; a transfer is
			   not allowed for inactive accounts and making an account
			   inactive is not allowed with a positive balance.
			   Imagine the following scenario:
			   Account: active=true balance=0
			   Time  Thread1           Thread2
			    1     closeAccount      deposit(10)
			    2       balance=0? yes    active? yes
			    3       make inactive     set balance 10
			   Account: active=false balance=10
			   Now with synchronization:
			   Account: active=true balance=0
			   Time   Thread1           Thread2
			    1      closeAccount      deposit(10)
			    2       lock (granted)    lock (blocks)
			    3       balance=0? yes     |
			    4       make inactive      |
			    5       unlock            lock (granted)
			    6                         active? false
			    7                         early return
			    8                         unlock
			   Account: active=false balance=0
			*/
			synchronized (transfer_lock) {
				if (!a.isActive()) return false;
				if (a.balance > 0) return false;
				a.makeInactive();
			}
			return true;
		}

		public Account getAccount(String number) {
			return accounts.get(number);
		}

		/* thread safe */
		public void transfer(Account from, Account to, double amount)
				throws InactiveException, OverdrawException {
			if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
			synchronized (transfer_lock) {
				if (!from.isActive() || !to.isActive()) throw new InactiveException();
				if (from.getBalance() < amount) throw new OverdrawException();
				from.withdraw(amount);
				to.deposit(amount);
			}
		}

	}

	private static class Account {
		private static final String IBAN_PREFIX = "CH56";
		private static long next_account_number = 1000_0000_0000_0000_0L;
		private static final Object LOCK = new Object();

		private String number;
		private String owner;
		private double balance;
		private boolean active = true;

		private Account(String owner) {
			this.owner = owner;
			synchronized (LOCK) {
				this.number = IBAN_PREFIX + next_account_number++;
			}
			this.balance = 0;
		}

		public double getBalance() {
			return balance;
		}

		public String getOwner() {
			return owner;
		}

		public String getNumber() {
			return number;
		}

		public boolean isActive() {
			return active;
		}

		public synchronized void deposit(double amount) throws InactiveException {
			if (!isActive()) throw new InactiveException();
			if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
			balance += amount;
		}

		public synchronized void withdraw(double amount) throws InactiveException, OverdrawException {
			if (amount < 0) throw new IllegalArgumentException("negative amount not allowed");
			if (amount > balance) throw new OverdrawException();
			if (!isActive()) throw new InactiveException();
			balance -= amount;
		}

		synchronized void makeInactive() {
			active = false;
		}
	}

	private static class InactiveException extends Exception {}
	private static class OverdrawException extends Exception {}
}
