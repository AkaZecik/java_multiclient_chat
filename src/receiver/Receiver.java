package receiver;


import message.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

public class Receiver implements Runnable {
    private final Socket socket;
    private final LinkedBlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    public Receiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        ObjectInputStream objectInputStream;

        try {
            InputStream inputStream = socket.getInputStream();
            objectInputStream = new ObjectInputStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        running = true;

        while (running) {
            try {
                Message message = (Message) objectInputStream.readObject();
                messages.put(message);
            } catch (EOFException | SocketException e) {
                close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Message takeMessage() {
        Message message = null;

        while (message == null) {
            try {
                message = messages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return message;
    }

    private void close() {
        running = false;
        takePoison();
    }

    private void takePoison() {
        try {
            messages.put(Message.poisonedMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}