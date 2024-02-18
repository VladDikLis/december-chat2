package ru.flamexander.december.chat.server;

import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class InMemoryUserService implements UserService {
    class User {
        private String login;
        private String password;
        private String username;
        private Boolean kick;
        private String role;
        private Boolean ban;
        private String kickdate;


        public User(String login, String password, String username, Boolean kick, String role, Boolean ban, String kickdate) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.kick = kick;
            this.role = role;
            this.ban = ban;
            this.kickdate = kickdate;

        }
    }

    private List<InMemoryUserService.User> users;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/UsrerRole";
    private static final String DB_LOGIN = "postgres";
    private static final String DB_PASSWORD = "228950";
    private static Connection connect;
    private static Statement statement;
    private static PreparedStatement pStatement;

    private void connect() throws SQLException {
        connect = DriverManager.getConnection(DB_URL, DB_LOGIN, DB_PASSWORD);
        statement = connect.createStatement();
    }

    public InMemoryUserService() {
        try {
            connect();
            List<InMemoryUserService.User> users = new ArrayList<>();
            ResultSet rs = statement.executeQuery("Select login, password, username, kick, role, ban, kickdate from users");
            while (rs.next()) {
                users.add(new User(rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("username"),
                        rs.getBoolean("kick"),
                        rs.getString("role"),
                        rs.getBoolean("ban"),
                        rs.getString("kickdate")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        List<InMemoryUserService.User> users = new ArrayList<>();
        try {
            pStatement = connect.prepareStatement("Select username from users where login = ? and password = ?");
            pStatement.setString(1, login);
            pStatement.setString(2, password);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void createNewUser(String login, String password, String username) {
        try {
            pStatement = connect.prepareStatement(
                    "INSERT INTO users(login, password, username, kick, role, ban, kickDate) VALUES(?, ?, ?, ?, ?, ?, ?) ");
            connect.setAutoCommit(true);
            pStatement.setString(1, login);
            pStatement.setString(2, password);
            pStatement.setString(3, username);
            pStatement.setBoolean(4, false);
            pStatement.setString(5, "user");
            pStatement.setBoolean(6, false);
            pStatement.setString(7, "null");
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        try {
            pStatement = connect.prepareStatement("Select 1 from users where login = ?");
            pStatement.setString(1, login);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) {
        try {
            pStatement = connect.prepareStatement("Select 1 from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean checkAdmin(String username) {
        try {
            pStatement = connect.prepareStatement("Select role from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return (Objects.equals(rs.getString(1), "admin"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void userKick(String username, String kickdate) {
        try {
            pStatement = connect.prepareStatement("update users set kick = true, kickdate = ? where username = ?");
            connect.setAutoCommit(true);
            pStatement.setString(1, kickdate);
            pStatement.setString(2, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void newActivity(String username) {
        try {
            pStatement = connect.prepareStatement("update users set activedate = ? where username = ?");
            connect.setAutoCommit(true);
            Date date = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(20));
            String stringDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(date);
            pStatement.setString(1, stringDate);
            pStatement.setString(2, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void userUnkick(String username) {
        try {
            pStatement = connect.prepareStatement("update users set kick = false where username = ?");
            connect.setAutoCommit(true);
            pStatement.setString(1, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void userBan(String username) {
        try {
            pStatement = connect.prepareStatement("update users set ban = true where username = ?");
            connect.setAutoCommit(true);
            pStatement.setString(1, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean checkKick(String username) {
        try {
            pStatement = connect.prepareStatement("Select kick from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return (Objects.equals(rs.getString(1), "true"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long checkKickDate(String username) {
        try {
            pStatement = connect.prepareStatement("Select kickdate from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    DateFormat newDateF = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
                    Date nowDate = new Date();
                    Date kickDate = newDateF.parse(rs.getString(1));
                    long ms = kickDate.getTime() - nowDate.getTime();
                    long minutes = TimeUnit.MINUTES.convert(ms, TimeUnit.MILLISECONDS);
                    return minutes;
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public long getDowntime(String username) {
        try {
            pStatement = connect.prepareStatement("Select activedate from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    DateFormat newDateF = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
                    Date nowDate = new Date();
                    Date kickDate = newDateF.parse(rs.getString(1));
                    long ms = kickDate.getTime() - nowDate.getTime();
                    long minutes = TimeUnit.MINUTES.convert(ms, TimeUnit.MILLISECONDS);
                    System.out.println(minutes);
                    return minutes;
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public boolean checkBan(String username) {
        try {
            pStatement = connect.prepareStatement("Select ban from users where username = ?");
            pStatement.setString(1, username);
            try (ResultSet rs = pStatement.executeQuery()) {
                while (rs.next()) {
                    return (Objects.equals(rs.getString(1), "true"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void userChangeNick(String username, String NewUsername) {
        try {
            pStatement = connect.prepareStatement("update users set username = ? where username = ?");
            connect.setAutoCommit(true);
            pStatement.setString(1, NewUsername);
            pStatement.setString(2, username);
            pStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}