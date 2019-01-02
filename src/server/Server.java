package server;

import message.Message;
import user.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Server implements Runnable {
    private final Map<Integer, ClientController> clientControllers = new HashMap<>();
    private final int port;

    private Server() {
        this.port = 5000;
    }

    public static void main(String args[]) {
        Server server = new Server();
        new Thread(server).start();
    }

    public void run() {
        System.out.println("INFO: Server started");

        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            int id = 1;

            while (true) {
                Socket socket = serverSocket.accept();
                startClientWorker(id++, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startClientWorker(int id, Socket socket) {
        ClientController clientController = new ClientController(id, socket, this);
        Thread thread = new Thread(clientController);

        synchronized (clientControllers) {
            clientControllers.put(id, clientController);
        }

        thread.start();
    }

    void sendMessage(Message message, User toUser) {
        clientControllers.get(toUser.getId()).sendMessage(message);
    }

    void broadcastMessage(Message message, ClientController except) {
        synchronized (clientControllers) {
            for (ClientController clientController : clientControllers.values()) {
                if (clientController != except) {
                    clientController.sendMessage(message);
                }
            }
        }
    }

    List<User> currentUsers() {
        synchronized (clientControllers) {
            List<User> cu = new LinkedList<>();
            User allUser = new User(0, "ALL");
            cu.add(allUser);
            cu.addAll(clientControllers.values().stream().filter(cl -> cl.getUser().getUsername() != null)
                    .map(ClientController::getUser).collect(Collectors.toList()));
            return cu;
        }
    }

    boolean verifyUsername(String username) {
        if (username == null || username.equals("ALL")) {
            return false;
        }

        synchronized (clientControllers) {
            for (ClientController clientController : clientControllers.values()) {
                if (username.equals(clientController.getUser().getUsername())) {
                    return false;
                }
            }
        }

        return true;
    }

    void removeClientWorker(int id) {
        clientControllers.remove(id);
    }
}
