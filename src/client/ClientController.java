package client;

import message.Message;
import receiver.Receiver;
import sender.Sender;
import user.User;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientController implements Runnable {
    private final Socket socket;
    private final Receiver receiver;
    private final Sender sender;
    private final Client client;
    private volatile User user;
    private volatile boolean running = false;

    ClientController(Socket socket, Client client) {
        this.client = client;
        this.socket = socket;
        this.receiver = new Receiver(socket);
        this.sender = new Sender(socket);
        this.user = new User();
    }

    User getUser() {
        return user;
    }

    @Override
    public void run() {
        Thread receiverThread = new Thread(receiver);
        Thread senderThread = new Thread(sender);
        receiverThread.start();
        senderThread.start();

        running = true;

        establishId();
        receiveUsernames();
        waitForUsernameResponse();
        receiveMessages();

        try {
            senderThread.join();
            socket.close();
        } catch (InterruptedException | IOException ignored) {
        }
    }

    private void establishId() {
        while (running) {
            Message message = receiver.takeMessage();

            if (message.getType() == 1) {
                user.setId((int) message.get("id"));
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void receiveUsernames() {
        List<User> users = null;

        while (running && users == null) {
            Message message = receiver.takeMessage();

            if (message.getType() == 2) {
                users = (List<User>) message.get("users");
                client.showUsersList(users);
                client.showChoosingUsernameBox();
            }
        }
    }

    void proposeUsername(String username) {
        Message message = new Message(3);
        message.put("username", username);
        sender.putMessage(message);
    }

    private void waitForUsernameResponse() {
        while (running && user.getUsername() == null) {
            Message message = receiver.takeMessage();

            switch (message.getType()) {
                case 4: {
                    client.rejectUsername();
                    break;
                }
                case 5: {
                    user.setUsername(((User) message.get("user")).getUsername());
                    client.acceptUsername();
                    break;
                }
                case 6: {
                    User newUser = (User) message.get("newUser");
                    client.addUser(newUser);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void receiveMessages() {
        while (running) {
            Message message = receiver.takeMessage();

            switch (message.getType()) {
                case 6: {
                    User newUser = (User) message.get("newUser");
                    client.addUser(newUser);
                    break;
                }
                case 7: {
                    User fromUser = (User) message.get("from");
                    User toUser = (User) message.get("to");
                    String msg = (String) message.get("message");
                    UserMessage userMessage = new UserMessage(fromUser, toUser, msg);
                    client.addMessage(userMessage);
                    break;
                }
                case 9: {
                    User user = (User) message.get("user");
                    client.removeUser(user);
                    break;
                }
                default:
                    break;
            }
        }
    }

    void sendMessage(UserMessage userMessage) {
        sender.putMessage(userMessage.castToMessage());
    }

    void close() {
        Message message = new Message(8);
        message.put("user", user);
        sender.putMessage(message);
        running = false;
        sender.close();
    }
}
