import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

public class Client {

	private static final String IP = "127.0.0.1";
	private static final int PORT = 50001;
	private static final String request = "3\nmike nöthiger\n\n";
	public static void main(String[] args) throws IOException {
		Socket s = new Socket(IP, PORT);
		OutputStream out = s.getOutputStream();
		InputStream in = s.getInputStream();

		System.out.println("writing request...");
		for (int i = 0; i < request.length(); i++) {
			out.write(request.charAt(i));
		}
		System.out.println("reading response...");

		int buf;
		while ((buf = in.read()) != -1) {
			System.out.print((char) buf);
		}
		System.out.println();

		s.close();
	}
}
