package client;

import message.Message;
import user.User;

import java.io.Serializable;

class UserMessage implements Serializable {
    private final User fromUser;
    private final User toUser;
    private final String stringMessage;

    UserMessage(User fromUser, User toUser, String stringMessage) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.stringMessage = stringMessage;
    }

    User getFromUser() {
        return fromUser;
    }

    User getToUser() {
        return toUser;
    }

    private String getStringMessage() {
        return stringMessage;
    }

    Message castToMessage() {
        Message message = new Message(7);
        message.put("from", fromUser);
        message.put("to", toUser);
        message.put("message", stringMessage);
        return message;
    }

    @Override
    public String toString() {
        return "From " + getFromUser().getUsername() + ": " + getStringMessage();
    }
}