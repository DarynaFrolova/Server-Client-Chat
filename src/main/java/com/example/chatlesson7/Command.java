package com.example.chatlesson7;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Command {

    AUTH("/auth") { // /auth login1 pass1

        @Override
        public String[] parse(String commandText) {
            final String[] split = commandText.split(COMMAND_DELIMITER);
            return new String[]{split[1], split[2]};
        }
    },

    AUTHOK("/authok") {
        @Override
        public String[] parse(String commandText) { //  /authok nick1
            return new String[]{commandText.split(COMMAND_DELIMITER)[1]};
        }
    },

    PRIVATE_MESSAGE("/w") { // /w nick1 Длинное сообщение для пользователя

        @Override
        public String[] parse(String commandText) {
            final String[] split = commandText.split(COMMAND_DELIMITER, 3);
            return new String[]{split[1], split[2]};
        }
    },

    END("/end") { // /end

        @Override
        public String[] parse(String commandText) {
            return new String[0];
        }
    },

    ERROR("/error") { // /error Сообщение об ошибке

        @Override
        public String[] parse(String commandText) {
            final String errorMsg = commandText.split(COMMAND_DELIMITER, 2)[1];
            return new String[]{errorMsg};
        }
    },

    CLIENTS("/clients") {
        @Override
        public String[] parse(String commandText) { // /clients nick1 nick2 nick3
            final String[] split = commandText.split(COMMAND_DELIMITER);

            final String[] nicks = new String[split.length - 1];
            for (int i = 1; i < split.length; i++) {
                nicks[i - 1] = split[i];
            }
            return nicks;
        }
    };

    static final String COMMAND_DELIMITER = "\\s+";
    private static final Map<String, Command> map = Stream.of(Command.values())
            .collect(Collectors.toMap(Command::getCommand, Function.identity()));
    private String command;
    private String[] params = new String[0];

    Command(String command) {
        this.command = command;
    }

    public static boolean isCommand(String message) {
        return message.startsWith("/");
    }

    public static Command getCommand(String message) {
        message = message.trim();
        if (!isCommand(message)) {
            throw new RuntimeException("'" + message + "' is not a command");
        }
        final int index = message.indexOf(" ");

       String cmd = index > 0 ? message.substring(0, index) : message;

        final Command command = map.get(cmd);
        if (command == null) {
            throw new RuntimeException("'" + cmd + "' unknown command");
        }
        return command;
    }

    public String[] getParams() {
        return params;
    }

    public String getCommand() {
        return command;
    }

    public abstract String[] parse(String commandText);

    public String collectMessage(String... params) {
        final String command = this.getCommand();
        return command +
                (params == null
                        ? ""
                        : " " + String.join(" ", params)); // /authok nick1
    }
}
