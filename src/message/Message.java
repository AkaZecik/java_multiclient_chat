package message;

import java.util.HashMap;

public class Message extends HashMap<String, Object> implements java.io.Serializable {
    public static Message poisonedMessage = new Message(Integer.MIN_VALUE);
    private final int type;

    public Message(int type) {
        super();
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message type: " + Integer.toString(type) + "\n" + super.toString();
    }

    public int getType() {
        return type;
    }
}
