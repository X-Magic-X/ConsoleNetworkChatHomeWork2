package ru.otus.chat.server.service;

import java.sql.*;

public class SQL {
    private static Connection connection;
    private static PreparedStatement user_add_ps;
    private static PreparedStatement user_role_add_ps;
    private static PreparedStatement user_by_login_pass_ps;
    private static PreparedStatement check_login_ps;
    private static PreparedStatement check_username_ps;

    private static final String USER_ADD_QUERY = """
            insert into users (username, login, password)
            values (?, ?, ?)
            RETURNING user_id
            """;
    private static final String USER_ROLE_ADD_QUERY = """
            insert into user_roles(user_id, role_id)
            values (?, 1)
            """;
    private static final String GET_USER_BY_LOGIN_PASS_QUERY = """
            SELECT
                u.user_id,
                u.login,
                u.password,
                u.username,
                r.role_id AS role
            FROM
                public.users u
            JOIN
                public.user_roles ur ON u.user_id = ur.user_id
            JOIN
                public.roles r ON ur.role_id = r.role_id
            WHERE
                u.login = ? AND u.password = ?
            """;
    private static final String CHECK_LOGIN_QUERY = """
            SELECT login
            FROM users u
            WHERE u.login = ?
            """;
    private static final String CHECK_USERNAME_QUERY = """
            SELECT username
            FROM users u
            WHERE u.username = ?
            """;

    public static void init(String DATABASE_URL, String DATABASE_USER, String DATABASE_PASSWORD) {
        try {
            connection = DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            System.out.println("Соединение с БД установлено");
        } catch (SQLException e) {
            System.out.println("Ошибка соединения с БД");
            throw new RuntimeException(e);
        }
        statement_init();
    }

    private static void statement_init() {
        try {
            PreparedStatement add_user = connection.prepareStatement(USER_ADD_QUERY);
            PreparedStatement user_role_add = connection.prepareStatement(USER_ROLE_ADD_QUERY);
            PreparedStatement get_us_by_log_pass = connection.prepareStatement(GET_USER_BY_LOGIN_PASS_QUERY);
            PreparedStatement check_login = connection.prepareStatement(CHECK_LOGIN_QUERY);
            PreparedStatement check_username = connection.prepareStatement(CHECK_USERNAME_QUERY);

            user_add_ps = add_user;
            user_role_add_ps = user_role_add;
            user_by_login_pass_ps = get_us_by_log_pass;
            check_login_ps = check_login;
            check_username_ps = check_username;

        } catch (SQLException e) {
            System.out.println("Ошибка инициализации БД");
            throw new RuntimeException(e);
        }
    }

    public static PreparedStatement getCheck_username_ps() {
        return check_username_ps;
    }

    public static PreparedStatement get_user_by_login_pass_ps() {
        return user_by_login_pass_ps;
    }

    public static PreparedStatement getCheck_login_ps() {
        return check_login_ps;
    }

    public static PreparedStatement getUser_role_add_ps() {
        return user_role_add_ps;
    }

    public static PreparedStatement getUser_add_ps() {
        return user_add_ps;
    }

    public static void close() {
        try {
            if (!user_add_ps.isClosed()) {
                user_add_ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!user_role_add_ps.isClosed()) {
                user_role_add_ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!check_login_ps.isClosed()) {
                check_login_ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!user_by_login_pass_ps.isClosed()) {
                user_by_login_pass_ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!check_username_ps.isClosed()) {
                check_username_ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
