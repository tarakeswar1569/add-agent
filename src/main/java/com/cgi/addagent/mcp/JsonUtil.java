package com.cgi.addagent.mcp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal JSON helpers for the small JSON-RPC/MCP payloads used by this application. */
public final class JsonUtil {
    private JsonUtil() {}

    public static String getString(String json, String field) {
        if (json == null || field == null) return null;
        String regex = "\\\"" + Pattern.quote(field)
                + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        return matcher.find() ? unescapeJson(matcher.group(1)) : null;
    }

    public static int getInt(String json, String field) {
        if (json == null || field == null) return 0;
        String regex = "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(-?\\d+)";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public static boolean getBoolean(String json, String field, boolean defaultValue) {
        if (json == null || field == null) return defaultValue;
        String regex = "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*(true|false)";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : defaultValue;
    }

    /** Returns the JSON object beginning at the named field. */
    public static String getObject(String json, String field) {
        if (json == null || field == null) return null;
        String token = "\"" + field + "\"";
        int fieldIndex = json.indexOf(token);
        if (fieldIndex < 0) return null;

        int start = json.indexOf('{', fieldIndex + token.length());
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (ch == '\\') escaped = true;
                else if (ch == '"') inString = false;
                continue;
            }
            if (ch == '"') inString = true;
            else if (ch == '{') depth++;
            else if (ch == '}') {
                depth--;
                if (depth == 0) return json.substring(start, i + 1);
            }
        }
        return null;
    }

    public static String getArgument(String json, String argumentName) {
        String arguments = getObject(json, "arguments");
        if (arguments == null) arguments = getObject(json, "params");
        return getString(arguments == null ? json : arguments, argumentName);
    }

    public static String successResponse(int id, String resultJson) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id
                + ",\"result\":" + resultJson + "}";
    }

    public static String errorResponse(int id, int code, String message) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id
                + ",\"error\":{\"code\":" + code
                + ",\"message\":\"" + escapeJson(message) + "\"}}";
    }

    public static String toolResult(int id, String text) {
        String result = "{\"content\":[{\"type\":\"text\",\"text\":\""
                + escapeJson(text) + "\"}]}";
        return successResponse(id, result);
    }

    public static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");
    }

    private static String unescapeJson(String value) {
        if (value == null) return null;
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') escaped = true;
                else result.append(ch);
                continue;
            }
            switch (ch) {
                case '"' -> result.append('"');
                case '\\' -> result.append('\\');
                case 'n' -> result.append('\n');
                case 't' -> result.append('\t');
                case 'r' -> result.append('\r');
                default -> result.append(ch);
            }
            escaped = false;
        }
        if (escaped) result.append('\\');
        return result.toString();
    }
}
