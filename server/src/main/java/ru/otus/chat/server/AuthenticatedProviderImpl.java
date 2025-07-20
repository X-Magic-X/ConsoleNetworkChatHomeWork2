package ru.otus.chat.server;

import ru.otus.chat.server.service.SQL;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticatedProviderImpl implements AuthenticatedProvider {
    private final Server server;

    public AuthenticatedProviderImpl(Server server) {
        this.server = server;
        initialize();
    }

    @Override
    public void initialize() {
        System.out.println("Сервис авторизации запущен");
    }

    private boolean checkLoginExist(String login) {
        PreparedStatement ps = SQL.getCheck_login_ps();
        try {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkUsernameExist(String username) {
        PreparedStatement ps = SQL.getCheck_username_ps();
        try {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        PreparedStatement ps = SQL.get_user_by_login_pass_ps();
        int id = -1;
        String authUsername = null;
        int roleId = -1;
        UserRole role;
        try {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("user_id");
                    authUsername = rs.getString("username");
                    roleId = rs.getInt("role");
                    System.out.println(id + authUsername + roleId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (authUsername == null || roleId == -1 || id == -1) {
            clientHandler.sendMsg("Некорректный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }
        role = switch (roleId) {
            case 1 -> UserRole.USER;
            case 2 -> UserRole.ADMIN;
            default -> throw new RuntimeException("Некорректная роль пользователя");
        };

        clientHandler.setUsername(authUsername);
        clientHandler.setRole(role);
        clientHandler.setUserId(id);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authok " + authUsername);
        return true;
    }

    @Override
    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
        PreparedStatement psUser = SQL.getUser_add_ps();
        PreparedStatement psRole = SQL.getUser_role_add_ps();
        int userId = -1;
        if (login.length() < 3) {
            clientHandler.sendMsg("Логин должен быть 3+ символа");
            return false;
        }
        if (username.length() < 3) {
            clientHandler.sendMsg("Имя пользователя должна быть 3+ символа");
            return false;
        }
        if (password.length() < 3) {
            clientHandler.sendMsg("Пароль должен быть 3+ символа");
            return false;
        }
        if (checkLoginExist(login)) {
            clientHandler.sendMsg("Такой логин уже занят");
            return false;
        }
        if (checkUsernameExist(username)) {
            clientHandler.sendMsg("Такое имя пользователя уже занято");
            return false;
        }
        try {
            psUser.setString(1, username);
            psUser.setString(2, login);
            psUser.setString(3, password);
            try (ResultSet rs = psUser.executeQuery()) {
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                    psRole.setInt(1, userId);
                    psRole.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        clientHandler.setUsername(username);
        clientHandler.setUserId(userId);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);

        return true;
    }
}
