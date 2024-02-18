package ru.flamexander.december.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                authentication();
                listenUserChatMessages();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void listenUserChatMessages() throws IOException {
        server.getUserService().newActivity(username);
        new Thread(() -> {
           while (true) {
               try {
                   TimeUnit.SECONDS.sleep(60);
                   if (server.getUserService().getDowntime(username) <= 0) {
                       server.unsubscribe(this);
                   };
               } catch (InterruptedException e) {}
           }
        }).start();
        while (true) {
            if (server.getUserService().checkBan(username)) {
                sendMessage("Вы забанены");
                return;
            }
            if (server.getUserService().checkKick(username)) {
                if (server.getUserService().checkKickDate(username) <= 0) {
                    server.getUserService().userUnkick(username);
                } else {
                    sendMessage("Вы кикнуты. Вход в чат будет доступен менее чем через " + server.getUserService().checkKickDate(username) + " минут");
                    return;
                }
            }
            server.getUserService().newActivity(username);

            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                }
                if (message.startsWith("/w ")) {
                    String[] part = message.split(" ", 3);
                    server.sendPrivateMessage(this, part[1], part[2]);
                }
                if (message.startsWith("/kick ")) {
                    if (server.getUserService().checkAdmin(username)) {
                        String[] element = message.split(" ", 3);
                        if (element.length != 3) {
                            sendMessage("Ошибка выполнения команды");
                        } else {
                            server.userGoKick(element[1], this, element[2]);
                        }
                    } else {
                        sendMessage("Недостаточно прав");
                    }
                }
                if (message.startsWith("/ban ")) {
                    if (server.getUserService().checkAdmin(username)) {
                        String[] element = message.split(" ", 2);
                        if (element.length != 2) {
                            sendMessage("Ошибка выполнения команды");
                        } else {
                            server.userGoBan(element[1], this);
                        }
                    } else {
                        sendMessage("Недостаточно прав");
                    }
                }
                if (message.startsWith("/changenick ")) {
                    String[] element = message.split(" ", 2);
                    if (element.length != 2) {
                        sendMessage("Ошибка выполнения команды");
                    } else {
                        server.userChangeNick(element[1], this, this.username);
                        this.username = element[1];
                    }
                }
                if (message.startsWith("/activelist")) {
                    String activeUser = server.getActiveUser();
                   sendMessage("Список активных пользователей:" + activeUser);
                }
                if (message.startsWith("/shutdown")) {
                    if (server.getUserService().checkAdmin(username)) {
                        server.broadcastMessage("Администратор завершает работу чата");
                        System.exit(0);
                    } else {
                        sendMessage("Недостаточно прав");
                    }
                }
            } else {
                Date date = new Date();
                SimpleDateFormat formatForDateNow = new SimpleDateFormat("hh:mm:ss");
                server.broadcastMessage("<" + formatForDateNow.format(date) + "> " + username + ": " + message);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
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

    private boolean tryToAuthenticate(String message) {
        String[] elements = message.split(" "); // /auth login1 pass1
        if (elements.length != 3) {
            sendMessage("СЕРВЕР: некорректная команда аутентификации");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        String usernameFromUserService = server.getUserService().getUsernameByLoginAndPassword(login, password);
        if (usernameFromUserService == null) {
            sendMessage("СЕРВЕР: пользователя с указанным логин/паролем не существует");
            return false;
        }
        if (server.isUserBusy(usernameFromUserService)) {
            sendMessage("СЕРВЕР: учетная запись уже занята");
            return false;
        }
        username = usernameFromUserService;
        server.subscribe(this);
        sendMessage("/authok " + username);
        sendMessage("СЕРВЕР: " + username + ", добро пожаловать в чат!");
        return true;
    }

    private boolean register(String message) {
        String[] elements = message.split(" "); // /auth login1 pass1 user1
        if (elements.length != 4) {
            sendMessage("СЕРВЕР: некорректная команда аутентификации");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        String registrationUsername = elements[3];
        if (server.getUserService().isLoginAlreadyExist(login)) {
            sendMessage("СЕРВЕР: указанный login уже занят");
            return false;
        }
        if (server.getUserService().isUsernameAlreadyExist(registrationUsername)) {
            sendMessage("СЕРВЕР: указанное имя пользователя уже занято");
            return false;
        }
        server.getUserService().createNewUser(login, password, registrationUsername);
        username = registrationUsername;
        sendMessage("/authok " + username);
        sendMessage("СЕРВЕР: " + username + ", вы успешно прошли регистрацию, добро пожаловать в чат!");
        server.subscribe(this);
        return true;
    }

    private void authentication() throws IOException {
        while (true) {
            String message = in.readUTF();
            boolean isSucceed = false;
            if (message.startsWith("/auth ")) {
                isSucceed = tryToAuthenticate(message);
            } else if (message.startsWith("/register ")) {
                isSucceed = register(message);
            } else {
                sendMessage("СЕРВЕР: требуется войти в учетную запись или зарегистрироваться");
            }
            if (isSucceed) {
                break;
            }
        }
    }
}