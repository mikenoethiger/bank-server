
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
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

public class Server {

	private static final String[] ERROR_INTERNAL_ERROR = {"nok", "0", "Internal error."};
	private static final String[] ERROR_ACCOUNT_DOES_NOT_EXIST = {"nok", "1", "Account does not exist."};
	private static final String[] ERROR_ACCOUNT_COULD_NOT_BE_CREATED = {"nok", "2", "Account could not be created."};
	private static final String[] ERROR_ACCOUNT_COULD_NOT_BE_CLOSED = {"nok", "3", "Account could not be closed."};
	private static final String[] ERROR_INACTIVE_ACCOUNT = {"nok", "4", "Inactive account."};
	private static final String[] ERROR_ACCOUNT_OVERDRAW = {"nok", "5", "Account overdraw."};
	private static final String[] ERROR_ILLEGAL_ARGUMENT = {"nok", "6", "Illegal argument."};
	private static final String[] ERROR_BAD_REQUEST = {"nok", "7", "Bad request."};

	private static final char DELIMITER = '\n';

	private static final int STATE_READ = 0;
	private static final int STATE_LINE_BREAK = 1;

    private static final int PORT = 50001;

	private static final Bank BANK = new Bank();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        while (true) {
			System.out.println("waiting for requests...");
			try (Socket s = server.accept()) {
				System.out.println("connected...");
				InputStream in = s.getInputStream();
				String[] request = readRequest(in);
				System.out.println(Arrays.toString(request));
				String[] response = processRequest(request);
				System.out.println(Arrays.toString(response));
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				for (String line : response) {
					out.writeChars(line);
					out.writeChar('\n');
				}
				out.writeChar('\n');
				out.close();
			} catch (IOException e) {
				System.out.println("could not accept connection");
			}
			System.out.println("served request");
        }
    }

	private static String[] processRequest(String[] request) {
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

	private static String[] getAccountNumbers(String[] request) {
		Set<String> accounts = BANK.getAccountNumbers();
		String[] response = new String[accounts.size()+1];
		response[0] = "ok";
		int i = 1;
		for (String a : accounts) {
			response[i++] = a;
		}
		return response;
	}

	private static String[] getAccount(String[] request) {
		if (request.length < 2) return ERROR_BAD_REQUEST;
		Account a = BANK.getAccount(request[1]);
		if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;
		return new String[]{"ok", a.getNumber(), a.getOwner(), String.valueOf(a.getBalance()), a.isActive() ? "1" : "0"};
	}

	private static String[] createAccount(String[] request) {
		if (request.length < 2) return ERROR_BAD_REQUEST;
		String account = BANK.createAccount(request[1]);
		if (account == null) return ERROR_ACCOUNT_COULD_NOT_BE_CREATED;
		return new String[]{"ok", account};
	}

	private static String[] closeAccount(String[] request) {
		if (request.length < 2) return ERROR_BAD_REQUEST;
		boolean result = BANK.closeAccount(request[1]);
		if (result) return new String[]{"ok"};
		else return ERROR_ACCOUNT_COULD_NOT_BE_CLOSED;
	}

	private static String[] transfer(String[] request) {
		if (request.length < 4) return ERROR_BAD_REQUEST;

		// parse accounts
		Account from = BANK.getAccount(request[1]);
		Account to = BANK.getAccount(request[1]);
		if (from == null || to == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

		// parse amount
		double amount;
		try { amount = Double.parseDouble(request[3]); }
		catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

		// transfer money
		try { BANK.transfer(from, to, amount); }
		catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
		catch (OverdrawException e) { return ERROR_ACCOUNT_OVERDRAW; }
		catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

		return new String[]{"ok"};
	}

	private static String[] deposit(String[] request) {
		if (request.length < 3) return ERROR_BAD_REQUEST;

		// parse account
		Account a = BANK.getAccount(request[1]);
		if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

		// parse amount
		double amount;
		try { amount = Double.parseDouble(request[2]); }
		catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

		try { a.deposit(amount); }
		catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
		catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

		return new String[]{"ok"};
	}

	private static String[] withdraw(String[] request) {
		if (request.length < 3) return ERROR_BAD_REQUEST;

		// parse account
		Account a = BANK.getAccount(request[1]);
		if (a == null) return ERROR_ACCOUNT_DOES_NOT_EXIST;

		// parse amount
		double amount;
		try { amount = Double.parseDouble(request[2]); }
		catch (NumberFormatException e) { return ERROR_BAD_REQUEST; }

		try { a.withdraw(amount); }
		catch (InactiveException e) { return ERROR_INACTIVE_ACCOUNT; }
		catch (OverdrawException e) { return ERROR_ACCOUNT_OVERDRAW; }
		catch (IllegalArgumentException e) { return ERROR_ILLEGAL_ARGUMENT; }

		return new String[]{"ok"};
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
		public String createAccount(String owner) {
			// limit memory usage
			if (accounts_num > MAX_ACCOUNTS) return null;
			Account a = new Account(owner);
			accounts.put(a.number, a);
			synchronized (accounts_num_lock) {
				accounts_num++;
			}
			return a.number;
		}

		public boolean closeAccount(String number) {
			if (!accounts.containsKey(number)) return false;
			Account a = accounts.get(number);
			if (!a.isActive()) return false;
			if (a.balance > 0) return false;
			a.makeInactive();
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

		void makeInactive() {
			active = false;
		}
	}

	private static class InactiveException extends Exception {}
	private static class OverdrawException extends Exception {}
}
