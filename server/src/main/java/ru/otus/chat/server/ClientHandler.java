package ru.otus.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;
    private UserRole role;
    private boolean authenticated;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                System.out.println("Клиент подключился " + socket.getPort());
                //Цикл аутентификации
                while (true) {
                    sendMsg("Перед работой с чатом необходимо выполнить " +
                            "аутентификацию '/auth login password' \n" +
                            "или регистрацию '/reg login password username'");
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMsg("/exitok");
                            break;
                        }
                        ///auth login password
                        if (message.startsWith("/auth ")) {
                            String token[] = message.split(" ");
                            if (token.length != 3) {
                                sendMsg("Неверный формат команды /auth");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .authenticate(this, token[1], token[2])) {
                                authenticated = true;
                                break;
                            }
                        }
                        ///reg login password username
                        if (message.startsWith("/reg ")) {
                            String token[] = message.split(" ");
                            if (token.length != 4) {
                                sendMsg("Неверный формат команды /reg");
                                continue;
                            }
                            if (server.getAuthenticatedProvider()
                                    .registration(this, token[1], token[2], token[3])) {
                                authenticated = true;
                                break;
                            }
                        }
                    }
                }

                //Цикл работы
                while (authenticated && running) {
                    String message = in.readUTF();
                    if (message.startsWith("/")) {
                        if (message.equals("/exit")) {
                            sendMsg("/exitok");
                            break;
                        }

                        if (message.startsWith("/kick ")) {
                            String[] parts = message.substring(6).split(" ", 2);
                            if (parts.length == 2) {
                                String usernameToKick = parts[0];
                                server.kickUser(this, usernameToKick, parts[1]);

                            } else {
                                sendMsg("Использование: /kick username reason");
                            }
                        }

                    } else {
                        server.broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
                running = false;
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMsg(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void disconnect() {
        if (running) {
            running = false;
            server.unsubscribe(this);
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
