package ru.flamexander.december.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Сервер запущен на порту %d. Ожидание подключения клиентов\n", port);
            userService = new InMemoryUserService();
            System.out.println("Запущен сервис для работы с пользователями");
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    new ClientHandler(this, socket);
                } catch (IOException e) {
                    System.out.println("Не удалось подключить клиента");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }

    public synchronized String getActiveUser() {
        String usernameStr = " ";
        for (ClientHandler clientHandler : clients) {
            usernameStr = usernameStr + clientHandler.getUsername() + "  ";
        }
        return usernameStr;
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Подключился новый клиент " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Отключился клиент " + clientHandler.getUsername());
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiverUsername, String message) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(receiverUsername)) {
                Date date = new Date();
                SimpleDateFormat formatForDateNow = new SimpleDateFormat("hh:mm:ss");
                clientHandler.sendMessage("(Приватно) " + "<" + formatForDateNow.format(date) + "> " + sender.getUsername() + " -> " + receiverUsername + ": " + message);
                sender.sendMessage("(Приватно) " + "<" + formatForDateNow.format(date) + "> " + sender.getUsername() + " -> " + receiverUsername + ": " + message);
            }
        }
    }

    public synchronized boolean isUserBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void userGoKick(String username, ClientHandler sender, String minutes) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                Date date = new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(Long.parseLong(minutes)));
                String stringDate = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(date);
                userService.userKick(username, stringDate);
                clientHandler.sendMessage("Вас заблокировали");
                sender.sendMessage("Пользователь заблокирован");
                clients.remove(clientHandler);
                unsubscribe(clientHandler);
                return;
            }
        }
    }
    public synchronized void userGoBan(String username, ClientHandler sender) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                userService.userBan(username);
                clientHandler.sendMessage("Вас заблокировали");
                sender.sendMessage("Пользователь заблокирован");
                clients.remove(clientHandler);
                unsubscribe(clientHandler);
                return;
            }
        }
    }
    public synchronized void userChangeNick(String NewUsername, ClientHandler sender, String username) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                userService.userChangeNick(username, NewUsername);
                sender.sendMessage("Ник изменён");
                return;
            }
        }
    }
}