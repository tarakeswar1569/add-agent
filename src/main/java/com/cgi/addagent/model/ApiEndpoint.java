package com.cgi.addagent.model;
import java.util.List;
public record ApiEndpoint(String method, String path, String subPath, String methodName,
                          List<String> requestFields, List<String> responseFields) {}
