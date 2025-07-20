package ru.otus.chat.server;

import ru.otus.chat.server.service.SQL;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticatedProviderImpl  implements AuthenticatedProvider{
    private final Server server;
    private final String DATABASE_URL;
    private final String DATABASE_USER;
    private final String DATABASE_PASSWORD;

    public AuthenticatedProviderImpl(Server server, String DATABASE_URL, String DATABASE_USER, String DATABASE_PASSWORD) {
        this.server = server;
        this.DATABASE_URL = DATABASE_URL;
        this.DATABASE_USER = DATABASE_USER;
        this.DATABASE_PASSWORD = DATABASE_PASSWORD;
        initialize();
    }

    @Override
    public void initialize() {
        System.out.println("Сервис авторизации запущен");
    }

    private boolean checkLoginExist(String login){
        try {
            SQL.check_login_ps.setString(1, login);
            try (ResultSet rs = SQL.check_login_ps.executeQuery()) {
                return rs.next();
            }
        }catch (SQLException e) {
           throw new  RuntimeException(e);
        }
    }


    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        int id = -1;
        String authUsername = null;
        int roleId = -1;
        UserRole role;
        try{
            SQL.get_user_by_login_pass_ps.setString(1, login);
            SQL.get_user_by_login_pass_ps.setString(2, password);
            try(ResultSet rs = SQL.get_user_by_login_pass_ps.executeQuery()){
                if(rs.next()) {
                     id = rs.getInt("user_id");
                     authUsername = rs.getString("username");
                     roleId = rs.getInt("role");
                    System.out.println(id + authUsername + roleId);
                }
            }
        } catch (SQLException e){
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
        return false;
    }
}
