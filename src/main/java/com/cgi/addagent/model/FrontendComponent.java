package com.cgi.addagent.model;
import java.util.List;
public record FrontendComponent(String name, String description, List<String> childNames) {}
