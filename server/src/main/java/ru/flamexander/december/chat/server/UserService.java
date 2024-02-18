package ru.flamexander.december.chat.server;

public interface UserService {
    String getUsernameByLoginAndPassword(String login, String password);
    void createNewUser(String login, String password, String username);
    boolean isLoginAlreadyExist(String login);
    boolean isUsernameAlreadyExist(String username);
    boolean checkAdmin(String username);
    void userKick(String username, String kickdate);
    void userUnkick(String username);
    void userBan(String username);
    boolean checkKick(String username);
    long checkKickDate(String username);
    boolean checkBan(String username);
    void newActivity(String username);
    long getDowntime(String username);
    void userChangeNick(String username, String NewUsername);
}