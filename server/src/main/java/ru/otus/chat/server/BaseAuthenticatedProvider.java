package ru.otus.chat.server;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BaseAuthenticatedProvider implements AuthenticatedProvider{
    private class User {
        private int id;
        private String login;
        private String password;
        private String username;
        private UserRole role;

        public User(int id, String login, String password, String username) {
            this.id = id;
            this.login = login;
            this.password = password;
            this.username = username;

        }

        public User(int id, String login, String password, String username, UserRole role) {
            this.id = id;
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
        }

        public void setRole(UserRole role) {
            this.role = role;
        }
        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", login='" + login + '\'' +
                    ", password='" + password + '\'' +
                    ", username='" + username + '\'' +
                    ", role=" + role +
                    '}';
        }
    }
    private List<User> users;

    private Server server;

    private static final String DATABASE_URL = "jdbc:postgresql://0.0.0.0:1234/postgres";
    private static final String DATABASE_USER = "postgres";
    private static final String DATABASE_PASSWORD = "pass123";
    private static final String USERS_QUERY = "select * from users;";
    private static final String USER_ROLES_QUERY = """
                    select r.role_id, r."role_name" from roles r
                    join user_roles ur on r.role_id = ur.role_id
                    where ur.user_id = ?;
                    """;
    private static final String USER_ADD_QUERY = """
            insert into users (username, login, password)
            values (?, ?, ?)
            RETURNING user_id
            """;
    private static final String USER_ROLE_ADD_QUERY = """
            insert into user_roles(user_id, role_id)
            values (?, 1)
            """;

    private final Connection connection;

    public BaseAuthenticatedProvider(Server server) {
        this.server = server;
        this.users = new CopyOnWriteArrayList<>();
        try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            initialize();
        } catch (SQLException e) {
            System.out.println("Соединение с БД не установлено");
            throw new RuntimeException(e);
        }
    }

    public void initialize() {
        System.out.println("Соединение с БД установлено");
        users = getAll();
        System.out.println(users);
        System.out.println("Логин qwe занят? " + isLoginAlreadyExists("qwe"));
        System.out.println("юзернем qwe1 занят? " + isUsernameAlreadyExists("qwe1"));
        System.out.println("роль пользователя qwe: " + getRoleByLoginAndPassword("qwe", "qwe"));

        System.out.println(users);
    }

    public List<User> getAll() {
        List<User> result = new ArrayList();
        UserRole currentRoles = null;
        try (Statement statement = connection.createStatement()) {
            try(ResultSet rs = statement.executeQuery(USERS_QUERY)){
                while (rs.next()){
                    int id = rs.getInt("user_id");
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    String login = rs.getString("login");
                    User currentUser = new User(id, login, password, username);
                    result.add(currentUser);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try(PreparedStatement ps = connection.prepareStatement(USER_ROLES_QUERY)){
            for (User user : result) {
                ps.setInt(1, user.getId());
                try(ResultSet resultSet = ps.executeQuery()){
                    while (resultSet.next()){
                        int id = resultSet.getInt("role_id");
                        currentRoles = switch (id) {
                            case 1 -> UserRole.USER;
                            case 2 -> UserRole.ADMIN;
                            default -> throw new RuntimeException("Некорректный id роли");
                        };
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                user.setRole(currentRoles);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private boolean isLoginAlreadyExists(String login) {
        for (User user : users) {
            if (user.login.equals(login.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsernameAlreadyExists(String username) {
        for (User user : users) {
            if (user.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    private UserRole getRoleByLoginAndPassword(String login, String password) {
        for (BaseAuthenticatedProvider.User user : users) {
            if (user.login.equals(login.toLowerCase()) && user.password.equals(password)) {
                return user.role;
            }
        }
        return null;
    }

    private String getUsernameByLoginAndPassword(String login, String password) {
        for (BaseAuthenticatedProvider.User user : users) {
            if (user.login.equals(login.toLowerCase()) && user.password.equals(password)) {
                return user.username;
            }
        }
        return null;
    }


    @Override
    public boolean authenticate(ClientHandler clientHandler, String login, String password) {
        String authUsername = getUsernameByLoginAndPassword(login, password);
        UserRole role = getRoleByLoginAndPassword(login, password);
        if (authUsername == null || role == null) {
            clientHandler.sendMsg("Некорректный логин/пароль");
            return false;
        }
        if (server.isUsernameBusy(authUsername)) {
            clientHandler.sendMsg("Указанная учетная запись уже занята");
            return false;
        }
        clientHandler.setUsername(authUsername);
        clientHandler.setRole(role);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/authok " + authUsername);
        return true;
    }


    public boolean registration(ClientHandler clientHandler, String login, String password, String username) {
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
        if (isLoginAlreadyExists(login)) {
            clientHandler.sendMsg("Такой логин уже занят");
            return false;
        }
        if (isUsernameAlreadyExists(username)) {
            clientHandler.sendMsg("Такое имя пользователя уже занято");
            return false;
        }
        try(PreparedStatement ps = connection.prepareStatement(USER_ADD_QUERY)){
            ps.setString(1, username);
            ps.setString(2, login);
            ps.setString(3, password);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) {
                    userId = rs.getInt("user_id");
                    try (PreparedStatement rps = connection.prepareStatement(USER_ROLE_ADD_QUERY)) {
                        rps.setInt(1, userId);
                        rps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        users.add(new User(userId ,login, password, username, UserRole.USER));

        clientHandler.setUsername(username);
        server.subscribe(clientHandler);
        clientHandler.sendMsg("/regok " + username);
        return true;
    }
}
