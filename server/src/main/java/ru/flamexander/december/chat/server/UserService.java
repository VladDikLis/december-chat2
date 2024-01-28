package ru.flamexander.december.chat.server;

public interface UserService {
    String getUsernameByLoginAndPassword(String login, String password);
    void createNewUser(String login, String password, String username, String role);
    boolean isLoginAlreadyExist(String login);
    boolean isUsernameAlreadyExist(String username);
    boolean checkAdmin(String username);
    void userKick(String username);
    boolean checkKick(String username);
}