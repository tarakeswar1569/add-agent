package com.cgi.addagent.generator;

import com.cgi.addagent.model.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeCodeGenerator {
    private final TemplateEngine templateEngine = new TemplateEngine();

    public Map<String, String> generate(ModuleDesign module, ProjectConfig config) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        String packageBase = module.packageBase();
        String classPrefix = module.classPrefix();
        String base = "src/main/java/" + packageBase.replace('.', '/') + "/";

        Map<String, String> variables = new HashMap<>();
        variables.put("JAVA_VERSION", config.javaVersion());
        variables.put("SPRING_BOOT_VERSION", config.springBootVersion());
        variables.put("PACKAGE_BASE", packageBase);
        variables.put("ARTIFACT_ID", module.folderName());
        variables.put("MODULE_NAME", module.moduleName());
        variables.put("DB_DRIVER_DEPENDENCY", config.dbDriverDependency());
        variables.put("EXTRA_DEPENDENCIES", config.extraBeDependencies());
        variables.put("PACKAGING", config.packaging());
        variables.put("CLASS_PREFIX", classPrefix);
        variables.put("BASE_PATH", module.basePath());
        variables.put("SERVICE_VAR", module.serviceVar());
        variables.put("TABLE_NAME", module.dbTables().isEmpty() ? classPrefix.toUpperCase() : module.dbTables().getFirst().name());
        variables.put("ENDPOINT_METHODS", endpointMethods(module));
        variables.put("SERVICE_METHODS", serviceMethods(module));
        variables.put("SERVICE_IMPL_METHODS", serviceMethods(module));
        variables.put("REQUEST_FIELDS", fields(module, true));
        variables.put("RESPONSE_FIELDS", fields(module, false));
        variables.put("EXCEPTION_HANDLERS", "");

        files.put("pom.xml", templateEngine.render("be/pom.xml.tmpl", variables));

        Map<String, String> appVars = new HashMap<>();
        appVars.put("ARTIFACT_ID", module.folderName());
        appVars.put("DB_NAME", module.folderName());
        appVars.put("DB_TYPE", config.dbType());
        appVars.put("PACKAGE_BASE", packageBase);
        files.put("src/main/resources/application.yml", templateEngine.render("be/application.yml.tmpl", appVars));

        add(files, base + classPrefix + "Application.java", "be/Application.java.tmpl", variables);
        add(files, base + "controller/" + classPrefix + "Controller.java", "be/Controller.java.tmpl", variables);
        add(files, base + "service/" + classPrefix + "Service.java", "be/Service.java.tmpl", variables);
        add(files, base + "service/impl/" + classPrefix + "ServiceImpl.java", "be/ServiceImpl.java.tmpl", variables);
        add(files, base + "dao/" + classPrefix + "DAO.java", "be/DAO.java.tmpl", variables);
        add(files, base + "dao/impl/" + classPrefix + "DAOImpl.java", "be/DAOImpl.java.tmpl", variables);
        add(files, base + "entity/" + classPrefix + "Entity.java", "be/Entity.java.tmpl", variables);
        add(files, base + "repository/" + classPrefix + "Repository.java", "be/Repository.java.tmpl", variables);
        add(files, base + "model/" + classPrefix + "Model.java", "be/Model.java.tmpl", variables);

        if (!fields(module, true).isBlank()) {
            add(files, base + "model/" + classPrefix + "Request.java", "be/RequestDTO.java.tmpl", variables);
        }
        if (!fields(module, false).isBlank()) {
            add(files, base + "model/" + classPrefix + "Response.java", "be/ResponseDTO.java.tmpl", variables);
        }
        if (module.layerNames().stream().anyMatch(x -> x.equalsIgnoreCase("filter"))) {
            add(files, base + "filter/" + classPrefix + "AuthFilter.java", "be/Filter.java.tmpl", variables);
        }

        StringBuilder handlers = new StringBuilder();
        for (ExceptionInfo exception : module.exceptions()) {
            Map<String, String> exceptionVars = new HashMap<>(variables);
            exceptionVars.put("EXCEPTION_CLASS", exception.className());
            files.put(base + "exception/" + exception.className() + ".java",
                    templateEngine.render("be/Exception.java.tmpl", exceptionVars));
            handlers.append("@ExceptionHandler(")
                    .append(exception.className())
                    .append(".class) public void handle")
                    .append(exception.className())
                    .append("(")
                    .append(exception.className())
                    .append(" ex) {}\n");
        }
        if (!module.exceptions().isEmpty()) {
            variables.put("EXCEPTION_HANDLERS", handlers.toString());
            add(files, base + "exception/GlobalExceptionHandler.java", "be/GlobalExceptionHandler.java.tmpl", variables);
        }

        String testBase = "src/test/java/" + packageBase.replace('.', '/') + "/";
        add(files, testBase + classPrefix + "ApplicationTest.java", "be/ApplicationTest.java.tmpl", variables);
        add(files, testBase + "controller/" + classPrefix + "ControllerTest.java", "be/ControllerTest.java.tmpl", variables);
        add(files, testBase + "service/impl/" + classPrefix + "ServiceImplTest.java", "be/ServiceImplTest.java.tmpl", variables);
        return files;
    }

    private void add(Map<String, String> files, String path, String template, Map<String, String> variables) throws Exception {
        files.put(path, templateEngine.render(template, variables));
    }

    private String endpointMethods(ModuleDesign module) {
        StringBuilder result = new StringBuilder();
        for (ApiEndpoint endpoint : module.endpoints()) {
            String annotation = switch (endpoint.method().toUpperCase()) {
                case "GET" -> "Get";
                case "POST" -> "Post";
                case "PUT" -> "Put";
                case "DELETE" -> "Delete";
                default -> "Patch";
            };
            result.append("@")
                    .append(annotation)
                    .append("Mapping(\"")
                    .append(endpoint.path())
                    .append("\") public Object ")
                    .append(endpoint.methodName())
                    .append("(){ return null; }\n");
        }
        return result.toString();
    }

    private String serviceMethods(ModuleDesign module) {
        StringBuilder result = new StringBuilder();
        for (ApiEndpoint endpoint : module.endpoints()) {
            result.append("  Object ").append(endpoint.methodName()).append("();\n");
        }
        return result.toString();
    }

    private String fields(ModuleDesign module, boolean request) {
        Set<String> fields = new java.util.LinkedHashSet<>();
        for (ApiEndpoint endpoint : module.endpoints()) {
            List<String> current = request ? endpoint.requestFields() : endpoint.responseFields();
            if (current != null) fields.addAll(current);
        }
        return String.join(", ", fields);
    }
}
