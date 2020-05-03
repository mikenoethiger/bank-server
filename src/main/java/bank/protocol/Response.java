package bank.protocol;

import java.util.Arrays;

/**
 * Response according to https://github.com/mikenoethiger/bank-server-socket#response
 */
public class Response {

    public static final int STATUS_OK = 0;

    public static final Response ERROR_ACCOUNT_DOES_NOT_EXIST = new Response(1, new String[]{"Account does not exist."});
    public static final Response ERROR_ACCOUNT_COULD_NOT_BE_CREATED = new Response(2, new String[]{"Account could not be created."});
    public static final Response ERROR_ACCOUNT_COULD_NOT_BE_CLOSED = new Response(3, new String[]{"Account could not be closed."});
    public static final Response ERROR_INACTIVE_ACCOUNT = new Response(4, new String[]{"Inactive account."});
    public static final Response ERROR_ACCOUNT_OVERDRAW = new Response(5, new String[]{"Account overdraw."});
    public static final Response ERROR_ILLEGAL_ARGUMENT = new Response(6, new String[]{"Illegal argument."});
    public static final Response ERROR_BAD_REQUEST = new Response(7, new String[]{"Bad request."});
    public static final Response ERROR_INTERNAL_ERROR = new Response(8, new String[]{"Internal error."});

    /* the actions document possible response status codes https://github.com/mikenoethiger/bank-server-socket#actions */
    public final int statusCode; /* https://github.com/mikenoethiger/bank-server-socket#status-codes */
    public final String[] data;

    public Response(int statusCode, String[] data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String[] getData() {
        return data;
    }

    public static Response fromString(String str) {
        String[] response = str.split("\n");
        String[] data = Arrays.copyOfRange(response, 1, response.length);
        return new Response(Integer.valueOf(response[0]), data);
    }

    public boolean ok() {
        return getStatusCode() == STATUS_OK;
    }

    @Override
    public String toString() {
        /* as specified in https://github.com/mikenoethiger/bank-server-socket#response */
        return statusCode + "\n" + String.join("\n", data) + "\n\n";
    }
}
