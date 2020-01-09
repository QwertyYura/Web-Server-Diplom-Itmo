package ru.ifmo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

class Parser {

    private static final Logger LOG = LoggerFactory.getLogger(Parser.class);
    private static String filePath = System.getProperty("user.dir") + "\\logs\\access.log";

    private static String hostIP; //- хост/IP-адрес, с которого произведён запрос к серверу;
    private static String type; //— тип запроса, его содержимое и версия;
    private static String statusCode = ""; //— код состояния HTTP;
    private static String size = ""; // — количество отданных сервером байт;
    private static String urlSource = ""; //%{Referer} — URL-источник запроса;
    private static String userAgent; //%{User-Agent} — HTTP-заголовок, содержащий информацию о запросе (клиентское приложение, язык и т. д.);
    private static String destinationPath; //%{Host} — имя Virtual Host, к которому идет обращение.

    private static String getTime() {
        Date currentTime = new Date();
        SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return newDateFormat.format(currentTime);
    }

    private static String modificator(String str) {
        try {
            return str.replace("[", "").replace("]", "").replace(",", "")
                    .replace("{", "").replace("}", "");
        } catch (NullPointerException e) {
            LOG.warn(e.getMessage());
        }
        return null;
    }

    private static ArrayList<String> tokenizer(String string) throws NoSuchElementException {
        String[] splitter = string.split(",");
        ArrayList list = new ArrayList();
        Collections.addAll(list, splitter);
        return list;
    }

    static synchronized void requestParser(Request request) {
        String requestString = request.toString();
        String time = getTime();
        ArrayList<String> list = tokenizer(requestString);
        for (String tokens : list) {
            if (tokens.contains("Host")) {
                String[] getHost = tokens.substring(tokens.indexOf("Host=") + 5).split(":");
                hostIP = getHost[0];
            }
            if (tokens.contains("method")) {
                String[] getMethod = tokens.split("=");
                type = getMethod[1] + "=";
            }
            if (tokens.contains("path")) {
                String[] getMethod = tokens.split("=");
                destinationPath = getMethod[1];
            }
            if (tokens.contains("User-Agent")) {
                userAgent = "User-Agent" + tokens.substring(tokens.indexOf("="));
            }
        }

        fileWriter(" " + hostIP + " - - [" + time + "] " + type + destinationPath + " " + statusCode + " " + size + " " + urlSource
                + " " + userAgent + "\n");
    }

    static synchronized void responseParser(String response) {
        String time = getTime();
        ArrayList<String> list = tokenizer(response);
        for (String tokens : list) {
            if (tokens.contains("Host")) {
                String[] getHost = tokens.substring(tokens.indexOf("Host=") + 5).split(":");
                hostIP = getHost[0];
            }
        }
        fileWriter(" " + hostIP + " - - | " + time + " | " + response.lastIndexOf("\n") + "\n");
    }

    private static void fileWriter(String parsingString) {
        try (FileOutputStream outFile = new FileOutputStream(filePath, true);
             BufferedOutputStream outBuff = new BufferedOutputStream(outFile)) {
            byte[] buffer = parsingString.getBytes();
            outBuff.write(buffer, 0, buffer.length);
            outBuff.flush();
        } catch (IOException ex) {
            LOG.warn(ex.fillInStackTrace().toString());
        } finally {
            System.gc();
        }
    }
}
