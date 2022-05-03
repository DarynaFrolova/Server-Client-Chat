package com.example.chatlesson7.server;

import com.example.chatlesson7.Controller;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ChatHistory {

    // Запись локальной истории (не будут видны личные сообщения, которыми обменивались другие клиенты) в текстовый файл на клиенте
    public void saveClientHistory(Controller controller, String login) {
        String fileName  = "history_" + login + ".txt";
        File userHistory = new File(fileName);

        try (PrintWriter fileWriter1 = new PrintWriter(new FileWriter(userHistory, false));
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter1)) {
            bufferedWriter.write(controller.getTextArea().getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Запись истории чата (для демонстрации при загрузке клиента)
    public void saveChatHistory(Controller controller) {
        File chatHistory = new File("history.txt");

        try (PrintWriter fileWriter2 = new PrintWriter(new FileWriter(chatHistory, false));
             BufferedWriter bufferedWriter2 = new BufferedWriter(fileWriter2)) {
            bufferedWriter2.write(controller.getTextArea().getText());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Демонстрация последних 100 строк истории чата
    public void loadHistory(Controller controller) throws IOException {
        int maxLines = 100;
        List<String> historyList = new ArrayList<>();
        FileInputStream in = new FileInputStream("history.txt");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        String temp;
        while ((temp = bufferedReader.readLine()) != null) {
            if (!temp.startsWith("to ") && !temp.startsWith("from ")) {
                if (historyList.size() == maxLines) {
                    historyList.remove(0);
                }
                historyList.add(temp);
            }
        }

        for (String s : historyList) {
            controller.getTextArea().appendText(s + "\n");
        }
    }

}
