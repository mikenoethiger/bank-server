package bank.protocol;


import java.util.Arrays;

/**
 * Request according to https://github.com/mikenoethiger/bank-server-socket#request
 */
public class Request {

    public static final int ACTION_GET_ACCOUNT_NUMBERS = 1;
    public static final int ACTION_GET_ACCOUNT         = 2;
    public static final int ACTION_CREATE_ACCOUNT      = 3;
    public static final int ACTION_CLOSE_ACCOUNT       = 4;
    public static final int ACTION_TRANSFER            = 5;
    public static final int ACTION_DEPOSIT             = 6;
    public static final int ACTION_WITHDRAW            = 7;

    /* actions are documented here https://github.com/mikenoethiger/bank-server-socket#actions */
    public final int actionId;
    public final String[] args;

    public Request(int actionId, String[] args) {
        this.actionId = actionId;
        this.args = args;
    }

    public int getActionId() {
        return actionId;
    }

    public String[] getArgs() {
        return args;
    }

    public static Request fromString(String str) {
        String[] req = str.split("\n");
        String[] data = Arrays.copyOfRange(req, 1, req.length);
        return new Request(Integer.valueOf(req[0]), data);
    }

    @Override
    public String toString() {
        /* as specified in https://github.com/mikenoethiger/bank-server-socket#request */
        return actionId + "\n" + String.join("\n", args) + "\n\n";
    }
}
