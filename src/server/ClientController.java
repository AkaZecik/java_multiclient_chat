package server;

import message.Message;
import receiver.Receiver;
import sender.Sender;
import user.User;

import java.io.IOException;
import java.net.Socket;

class ClientController implements Runnable {
    private final Receiver receiver;
    private final Sender sender;
    private final Server server;
    private final Socket socket;
    private volatile User user;
    private volatile boolean running = false;

    ClientController(int id, Socket socket, Server server) {
        this.user = new User(id, null);
        this.server = server;
        this.socket = socket;
        this.receiver = new Receiver(socket);
        this.sender = new Sender(socket);
    }

    User getUser() {
        return user;
    }

    public void run() {
        Thread receiverThread = new Thread(receiver);
        Thread senderThread = new Thread(sender);
        receiverThread.start();
        senderThread.start();

        running = true;

        assignId();
        sendCurrentUsers();
        establishUsername();
        receiveMessages();

        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void assignId() {
        Message message = new Message(1);
        message.put("id", user.getId());
        sender.putMessage(message);
    }

    private void sendCurrentUsers() {
        Message message = new Message(2);
        message.put("users", server.currentUsers());
        sender.putMessage(message);
    }

    private void establishUsername() {
        Message message;

        while (user.getUsername() == null) {
            message = receiver.takeMessage();

            if (message.getType() != 3) {
                continue;
            }

            String proposedUsername = (String) message.get("username");

            synchronized (ClientController.class) {
                if (server.verifyUsername(proposedUsername)) {
                    user.setUsername(proposedUsername);
                } else {
                    message = new Message(4);
                    message.put("message", "This username is taken");
                    sender.putMessage(message);
                }
            }
        }

        message = new Message(5);
        message.put("user", user);
        sender.putMessage(message);
        message = new Message(6);
        message.put("newUser", user);
        broadcastMessage(message);
    }

    private void receiveMessages() {
        while (running) {
            Message message = receiver.takeMessage();

            switch (message.getType()) {
                case 7:
                    User to = (User) message.get("to");

                    if (to.getId() == 0) {
                        broadcastMessage(message);
                    } else {
                        server.sendMessage(message, to);
                    }

                    break;
                case 8:
                    User fromUser = (User) message.get("user");
                    message = new Message(9);
                    message.put("user", fromUser);
                    broadcastMessage(message);
                    close();
                    server.removeClientWorker(user.getId());
                    break;
                default:
                    break;
            }
        }
    }

    void sendMessage(Message message) {
        sender.putMessage(message);
    }

    private void broadcastMessage(Message message) {
        server.broadcastMessage(message, this);
    }

    private void close() {
        running = false;
        sender.close();
    }
}
