package com.cgi.addagent.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record ProjectConfig(
        String projectType,
        String javaVersion,
        String springBootVersion,
        String buildTool,
        String dbType,
        String packaging,
        List<String> beLibraries,
        String feFramework,
        String feLanguage,
        String packageManager,
        String styling,
        String httpClient,
        String testFramework,
        String folderStructure) {

    public ProjectConfig {
        projectType = valueOr(projectType, "single");
        javaVersion = valueOr(javaVersion, "21");
        springBootVersion = valueOr(springBootVersion, "3.3.2");
        buildTool = valueOr(buildTool, "maven");
        dbType = valueOr(dbType, "db2");
        packaging = valueOr(packaging, "jar");
        beLibraries = beLibraries == null ? List.of() : List.copyOf(new ArrayList<>(beLibraries));
        feFramework = valueOr(feFramework, "react");
        feLanguage = valueOr(feLanguage, "typescript");
        packageManager = valueOr(packageManager, "npm");
        styling = valueOr(styling, "css-modules");
        httpClient = valueOr(httpClient, "axios");
        testFramework = valueOr(testFramework, "jest-rtl");
        folderStructure = valueOr(folderStructure, "feature");
    }

    public static ProjectConfig defaults() {
        return new ProjectConfig(
                "single", "21", "3.3.2", "maven", "db2", "jar", List.of(),
                "react", "typescript", "npm", "css-modules", "axios", "jest-rtl", "feature");
    }

    public String dbDriverDependency() {
        return switch (dbType.toLowerCase(Locale.ROOT)) {
            case "oracle" -> "<dependency><groupId>com.oracle.database.jdbc</groupId><artifactId>ojdbc11</artifactId><version>23.5.0.24.07</version></dependency>";
            case "postgresql", "postgres" -> "<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>";
            case "h2" -> "<dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>";
            default -> "<dependency><groupId>com.ibm.db2</groupId><artifactId>jcc</artifactId><version>11.5.9.0</version><scope>runtime</scope></dependency>";
        };
    }

    public String extraBeDependencies() {
        StringBuilder s = new StringBuilder();
        for (String lib : beLibraries) {
            switch (lib.toLowerCase(Locale.ROOT)) {
                case "mapstruct" -> s.append("<dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId><version>1.6.3</version></dependency>");
                case "flyway" -> s.append("<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>");
                case "liquibase" -> s.append("<dependency><groupId>org.liquibase</groupId><artifactId>liquibase-core</artifactId></dependency>");
                case "spring-cloud" -> s.append("<dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter</artifactId></dependency>");
                default -> { }
            }
        }
        return s.toString();
    }

    public String httpClientPackage() {
        return "axios".equalsIgnoreCase(httpClient) ? "\"axios\": \"^1.8.0\"" : "";
    }

    public String testPackages() {
        return "\"@testing-library/react\": \"^16.2.0\", \"@testing-library/jest-dom\": \"^6.6.3\", \"@testing-library/user-event\": \"^14.6.1\"";
    }

    public String stylingPackages() {
        return "css-modules".equalsIgnoreCase(styling) ? "" : "\"" + styling + "\": \"latest\"";
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
