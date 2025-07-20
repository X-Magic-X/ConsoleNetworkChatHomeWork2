package ru.otus.chat.server;

import ru.otus.chat.server.service.SQL;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private AuthenticatedProvider authenticatedProvider;
    private static final String DATABASE_URL = "jdbc:postgresql://0.0.0.0:1234/postgres";
    private static final String DATABASE_USER = "postgres";
    private static final String DATABASE_PASSWORD = "pass123";

    public Server(int port) {
        this.port = port;
        clients = new CopyOnWriteArrayList<>();
        SQL.init(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
        authenticatedProvider = new AuthenticatedProviderImpl(this);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        broadcastMessage("Клиент " + clientHandler.getUsername() + " вышел из чата");
        System.out.println("Клиент " + clientHandler.getUsername() + " вышел из чата");
        clients.remove(clientHandler);
    }

    public void broadcastMessage(String message) {
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }

    public boolean isUsernameBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void kickUser(ClientHandler admin, String usernameToKick, String reason) {
        if (admin.getRole() != UserRole.ADMIN) {
            admin.sendMsg("Ошибка: недостаточно прав");
            return;
        }

        for (ClientHandler client : clients) {
            if (client.getUsername().equals(usernameToKick)) {
                client.sendMsg("/kickok " + reason);
                client.disconnect();
                broadcastMessage("Пользователь " + usernameToKick + " был отключен администратором " + admin.getUsername());
                return;
            }
        }
        admin.sendMsg("Пользователь " + usernameToKick + " не найден");
    }

    public AuthenticatedProvider getAuthenticatedProvider() {
        return authenticatedProvider;
    }
}
