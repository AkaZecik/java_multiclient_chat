package sender;


import message.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Sender implements Runnable {
    private final Socket socket;
    private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    public Sender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        ObjectOutputStream objectOutputStream;

        try {
            OutputStream outputStream = socket.getOutputStream();
            objectOutputStream = new ObjectOutputStream(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        running = true;

        while (running || !messages.isEmpty()) {
            try {
                Message message = messages.take();

                if (message != Message.poisonedMessage) {
                    objectOutputStream.writeObject(message);
                }
            } catch (InterruptedException | IOException ignored) {
            }
        }
    }

    public void putMessage(Message message) {
        try {
            messages.put(message);
        } catch (InterruptedException ignored) {
        }
    }

    public void close() {
        running = false;
        takePoison();
    }

    private void takePoison() {
        try {
            messages.put(Message.poisonedMessage);
        } catch (InterruptedException ignored) {
        }
    }
}