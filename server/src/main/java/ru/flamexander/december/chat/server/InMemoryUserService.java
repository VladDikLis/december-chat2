package ru.flamexander.december.chat.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryUserService implements UserService {
    class User {
        private String login;
        private String password;
        private String username;
        private String role;
        private Boolean kick;


        public User(String login, String password, String username, String role) {
            this.login = login;
            this.password = password;
            this.username = username;
            this.role = role;
            this.kick = false;
        }
    }

    private List<User> users;

    public InMemoryUserService() {
        this.users = new ArrayList<>(Arrays.asList(
                new User("login1", "pass1", "user1", "user"),
                new User("login2", "pass2", "user2", "user"),
                new User("login3", "pass3", "user3", "user"),
                new User("login4", "pass4", "admin", "admin")
        ));
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User u : users) {
            if (u.login.equals(login) && u.password.equals(password)) {
                return u.username;
            }
        }
        return null;
    }

    @Override
    public void createNewUser(String login, String password, String username, String role) {
        users.add(new User(login, password, username, role));
    }

    @Override
    public boolean isLoginAlreadyExist(String login) {
        for (User u : users) {
            if (u.login.equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean checkAdmin(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                if (user.role.equals("admin")) {
                    return true;
                }
            }
        }
        return false;
    }
    public void userKick(String username) {
        for (User u : users) {
            if (u.username.equals(username)) {
                u.kick = true;
            }
        }
    }
    public boolean checkKick(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                return user.kick;
            }
        }
        return false;
    }

}