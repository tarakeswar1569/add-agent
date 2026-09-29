package com.cgi.addagent.mcp;

import com.cgi.addagent.extractor.ModuleExtractor;
import com.cgi.addagent.generator.BeCodeGenerator;
import com.cgi.addagent.generator.FeCodeGenerator;
import com.cgi.addagent.model.ModuleDesign;
import com.cgi.addagent.model.Paragraph;
import com.cgi.addagent.model.ProjectConfig;
import com.cgi.addagent.parser.DocParser;
import com.cgi.addagent.router.SectionRouter;
import com.cgi.addagent.writer.CodeFileWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * STDIO MCP server for add-code-agent.
 *
 * The MCP layer intentionally exposes a small, explicit workflow so the client
 * must identify the ADD, module, layer, and output directory rather than asking
 * the model to infer them.
 */
public class McpServer {
    private List<Paragraph> allParagraphs = List.of();
    private Map<String, List<Paragraph>> feModules = new LinkedHashMap<>();
    private Map<String, List<Paragraph>> beModules = new LinkedHashMap<>();
    private String lastAddPath;
    private ProjectConfig projectConfig = ProjectConfig.defaults();

    public void start() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)), true);

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank()) continue;
            String response = handle(line);
            if (response != null) {
                // MCP stdio requires one JSON object per line. Never write diagnostic text to stdout.
                out.println(response.replace("\r", "").replace("\n", ""));
            }
        }
    }

    private String handle(String json) {
        int id = JsonUtil.getInt(json, "id");
        String method = JsonUtil.getString(json, "method");

        try {
            if ("initialized".equals(method) || "notifications/initialized".equals(method) || "$/cancelRequest".equals(method)) return null;
            if ("ping".equals(method)) {
                return JsonUtil.successResponse(id, "{\"pong\":true}");
            }
            if ("initialize".equals(method)) {
                return JsonUtil.successResponse(id,
                        "{\"protocolVersion\":\"2024-11-05\","
                                + "\"serverInfo\":{\"name\":\"add-code-agent\",\"version\":\"1.0.0\"},"
                                + "\"capabilities\":{\"tools\":{}}}");
            }
            if ("tools/list".equals(method)) {
                return JsonUtil.successResponse(id, toolsList());
            }
            if ("tools/call".equals(method)) {
                return callTool(id, json);
            }
            return JsonUtil.errorResponse(id, -32601, "Method not found: " + method);
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return JsonUtil.errorResponse(id, -32000, message);
        }
    }

    private String toolsList() {
        String configure = "{\"name\":\"configure_project\","
                + "\"description\":\"Configure the backend/frontend technology choices used by code generation.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{"
                + "\"projectType\":{\"type\":\"string\"},"
                + "\"javaVersion\":{\"type\":\"string\"},"
                + "\"springBootVersion\":{\"type\":\"string\"},"
                + "\"buildTool\":{\"type\":\"string\"},"
                + "\"dbType\":{\"type\":\"string\"},"
                + "\"packaging\":{\"type\":\"string\"},"
                + "\"beLibraries\":{\"type\":\"string\",\"description\":\"Comma-separated list, e.g. mapstruct,flyway\"},"
                + "\"feFramework\":{\"type\":\"string\"},"
                + "\"feLanguage\":{\"type\":\"string\"},"
                + "\"packageManager\":{\"type\":\"string\"},"
                + "\"styling\":{\"type\":\"string\"},"
                + "\"httpClient\":{\"type\":\"string\"},"
                + "\"testFramework\":{\"type\":\"string\"},"
                + "\"folderStructure\":{\"type\":\"string\"}}}}";

        String load = "{\"name\":\"load_add\","
                + "\"description\":\"Load and parse one DOCX or DOCM Application Design Document.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{\"add_path\":{\"type\":\"string\"}},"
                + "\"required\":[\"add_path\"]}}";

        String modules = "{\"name\":\"get_modules\","
                + "\"description\":\"Return the backend and frontend modules discovered by the last load_add call.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{}}}";

        String plan = "{\"name\":\"generation_plan\","
                + "\"description\":\"Show the exact files that would be generated for one selected module and layer. Does not write files.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{"
                + "\"module\":{\"type\":\"string\"},"
                + "\"layer\":{\"type\":\"string\",\"enum\":[\"BE\",\"FE\",\"ALL\"]},"
                + "\"output_dir\":{\"type\":\"string\"}},"
                + "\"required\":[\"module\",\"layer\"]}}";

        String generate = "{\"name\":\"generate_module\","
                + "\"description\":\"Generate exactly one explicitly selected module and layer.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{"
                + "\"module\":{\"type\":\"string\"},"
                + "\"layer\":{\"type\":\"string\",\"enum\":[\"BE\",\"FE\",\"ALL\"]},"
                + "\"output_dir\":{\"type\":\"string\"}},"
                + "\"required\":[\"module\",\"layer\",\"output_dir\"]}}";

        String all = "{\"name\":\"generate_all\","
                + "\"description\":\"Generate every discovered backend and frontend module. Requires explicit confirm=true.\","
                + "\"inputSchema\":{\"type\":\"object\",\"properties\":{"
                + "\"output_dir\":{\"type\":\"string\"},"
                + "\"confirm\":{\"type\":\"boolean\"}},"
                + "\"required\":[\"output_dir\",\"confirm\"]}}";

        return "{\"tools\":[" + String.join(",", List.of(configure, load, modules, plan, generate, all)) + "]}";
    }

    private String callTool(int id, String json) throws Exception {
        String params = JsonUtil.getObject(json, "params");
        if (params == null) return JsonUtil.errorResponse(id, -32602, "Missing params object.");

        String name = JsonUtil.getString(params, "name");
        String arguments = JsonUtil.getObject(params, "arguments");
        if (name == null) return JsonUtil.errorResponse(id, -32602, "Missing tool name.");
        if (arguments == null) arguments = "{}";

        return switch (name) {
            case "configure_project" -> configureProject(id, arguments);
            case "load_add" -> loadAdd(id, arguments);
            case "get_modules" -> getModules(id);
            case "generation_plan" -> generationPlan(id, arguments);
            case "generate_module" -> generateModule(id, arguments);
            case "generate_all" -> generateAll(id, arguments);
            default -> JsonUtil.errorResponse(id, -32602, "Unknown tool: " + name);
        };
    }

    private String configureProject(int id, String args) {
        ProjectConfig d = ProjectConfig.defaults();
        List<String> libraries = csv(JsonUtil.getArgument(args, "beLibraries"));
        projectConfig = new ProjectConfig(
                first(args, "projectType", d.projectType()),
                first(args, "javaVersion", d.javaVersion()),
                first(args, "springBootVersion", d.springBootVersion()),
                first(args, "buildTool", d.buildTool()),
                first(args, "dbType", d.dbType()),
                first(args, "packaging", d.packaging()),
                libraries,
                first(args, "feFramework", d.feFramework()),
                first(args, "feLanguage", d.feLanguage()),
                first(args, "packageManager", d.packageManager()),
                first(args, "styling", d.styling()),
                first(args, "httpClient", d.httpClient()),
                first(args, "testFramework", d.testFramework()),
                first(args, "folderStructure", d.folderStructure()));

        return JsonUtil.toolResult(id, "Project configuration saved.\n" + projectConfig);
    }

    private String loadAdd(int id, String args) throws Exception {
        String path = JsonUtil.getArgument(args, "add_path");
        if (path == null || path.isBlank()) {
            return JsonUtil.errorResponse(id, -32602, "add_path is required.");
        }

        allParagraphs = new DocParser().parse(path);
        SectionRouter router = new SectionRouter();
        beModules = router.routeBeSections(allParagraphs);
        feModules = router.routeFeSections(allParagraphs);
        lastAddPath = path;

        StringBuilder result = new StringBuilder();
        result.append("ADD loaded successfully.\n");
        result.append("Path: ").append(path).append("\n");
        result.append("Paragraphs: ").append(allParagraphs.size()).append("\n\n");
        appendModules(result, "Backend", beModules.keySet());
        appendModules(result, "Frontend", feModules.keySet());
        result.append("\nNext step: call get_modules and select a module explicitly.");
        return JsonUtil.toolResult(id, result.toString());
    }

    private String getModules(int id) {
        StringBuilder result = new StringBuilder();
        result.append("Loaded ADD: ").append(lastAddPath == null ? "none" : lastAddPath).append("\n\n");
        appendModules(result, "Backend", beModules.keySet());
        appendModules(result, "Frontend", feModules.keySet());
        if (lastAddPath == null) result.append("\nCall load_add first.");
        return JsonUtil.toolResult(id, result.toString());
    }

    private String generationPlan(int id, String args) throws Exception {
        String module = required(args, "module");
        String layer = required(args, "layer").toUpperCase(Locale.ROOT);
        String outputDir = JsonUtil.getArgument(args, "output_dir");
        List<String> files = plannedFiles(module, layer);

        StringBuilder result = new StringBuilder();
        result.append("Generation plan\n\n");
        result.append("Module: ").append(module).append("\n");
        result.append("Layer: ").append(layer).append("\n");
        if (outputDir != null && !outputDir.isBlank()) result.append("Output: ").append(outputDir).append("\n");
        result.append("\nFiles:\n");
        for (String file : files) result.append("  ").append(file).append("\n");
        result.append("\nNo files were written.");
        result.append("\nNext step: call generate_module with the same module, layer, and output_dir.");
        return JsonUtil.toolResult(id, result.toString());
    }

    private String generateModule(int id, String args) throws Exception {
        String module = required(args, "module");
        String layer = required(args, "layer").toUpperCase(Locale.ROOT);
        String output = required(args, "output_dir");

        List<String> validLayers = List.of("BE", "FE", "ALL");
        if (!validLayers.contains(layer)) {
            return JsonUtil.errorResponse(id, -32602, "layer must be BE, FE, or ALL.");
        }

        ModuleExtractor extractor = new ModuleExtractor();
        CodeFileWriter writer = new CodeFileWriter();
        int count = 0;
        List<String> generated = new ArrayList<>();

        if ("BE".equals(layer) || "ALL".equals(layer)) {
            String key = find(beModules, module);
            if (key == null && "BE".equals(layer)) return missingModule(id, "BE", module, beModules.keySet());
            if (key != null) {
                ModuleDesign design = extractor.extract(key, "BE", beModules.get(key));
                count += writer.write(output, "BE", key, new BeCodeGenerator().generate(design, projectConfig));
                generated.add("BE/" + key);
            }
        }

        if ("FE".equals(layer) || "ALL".equals(layer)) {
            String key = find(feModules, module);
            if (key == null && "FE".equals(layer)) return missingModule(id, "FE", module, feModules.keySet());
            if (key != null) {
                ModuleDesign design = extractor.extract(key, "FE", feModules.get(key));
                count += writer.write(output, "FE", key, new FeCodeGenerator().generate(design, projectConfig));
                generated.add("FE/" + key);
            }
        }

        if (generated.isEmpty()) {
            return JsonUtil.errorResponse(id, -32602, "Module '" + module + "' was not found for layer " + layer + ".");
        }

        return JsonUtil.toolResult(id,
                "Generation completed.\nModules: " + generated + "\nFiles written: " + count
                        + "\nOutput directory: " + output);
    }

    private String generateAll(int id, String args) throws Exception {
        String output = required(args, "output_dir");
        boolean confirm = JsonUtil.getBoolean(args, "confirm", false);
        if (!confirm) {
            return JsonUtil.errorResponse(id, -32602,
                    "generate_all requires confirm=true. Call generation_plan or review the discovered modules before confirming.");
        }
        if (beModules.isEmpty() && feModules.isEmpty()) {
            return JsonUtil.errorResponse(id, -32602, "No ADD is loaded. Call load_add first.");
        }

        ModuleExtractor extractor = new ModuleExtractor();
        CodeFileWriter writer = new CodeFileWriter();
        int count = 0;
        int modules = 0;

        for (String name : beModules.keySet()) {
            ModuleDesign design = extractor.extract(name, "BE", beModules.get(name));
            count += writer.write(output, "BE", name, new BeCodeGenerator().generate(design, projectConfig));
            modules++;
        }
        for (String name : feModules.keySet()) {
            ModuleDesign design = extractor.extract(name, "FE", feModules.get(name));
            count += writer.write(output, "FE", name, new FeCodeGenerator().generate(design, projectConfig));
            modules++;
        }

        return JsonUtil.toolResult(id,
                "Generation completed for all discovered modules.\nModules generated: " + modules
                        + "\nFiles written: " + count + "\nOutput directory: " + output);
    }

    private List<String> plannedFiles(String module, String layer) throws Exception {
        ModuleExtractor extractor = new ModuleExtractor();
        List<String> files = new ArrayList<>();

        if ("BE".equals(layer) || "ALL".equals(layer)) {
            String key = find(beModules, module);
            if (key == null && "BE".equals(layer)) throw new IllegalArgumentException(missingText("BE", module, beModules.keySet()));
            if (key != null) files.addAll(new BeCodeGenerator().generate(
                    extractor.extract(key, "BE", beModules.get(key)), projectConfig).keySet());
        }
        if ("FE".equals(layer) || "ALL".equals(layer)) {
            String key = find(feModules, module);
            if (key == null && "FE".equals(layer)) throw new IllegalArgumentException(missingText("FE", module, feModules.keySet()));
            if (key != null) files.addAll(new FeCodeGenerator().generate(
                    extractor.extract(key, "FE", feModules.get(key)), projectConfig).keySet());
        }
        if (files.isEmpty()) throw new IllegalArgumentException("Module '" + module + "' was not found for layer " + layer + ".");
        return files;
    }

    private String required(String json, String field) {
        String value = JsonUtil.getArgument(json, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value;
    }

    private String first(String json, String field, String fallback) {
        String value = JsonUtil.getArgument(json, field);
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> csv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String find(Map<String, List<Paragraph>> modules, String name) {
        String normalized = SectionRouter.normalise(name);
        for (String key : modules.keySet()) {
            if (SectionRouter.normalise(key).equals(normalized)) return key;
        }
        return null;
    }

    private String missingModule(int id, String layer, String module, java.util.Set<String> available) {
        return JsonUtil.errorResponse(id, -32602, missingText(layer, module, available));
    }

    private String missingText(String layer, String module, java.util.Set<String> available) {
        return "Module '" + module + "' was not found in " + layer + ". Available modules: "
                + (available.isEmpty() ? "none" : String.join(", ", available));
    }

    private void appendModules(StringBuilder result, String label, java.util.Set<String> modules) {
        result.append(label).append(" modules:\n");
        if (modules.isEmpty()) {
            result.append("  (none)\n");
            return;
        }
        int index = 1;
        for (String module : modules) {
            result.append("  ").append(index++).append(". ").append(module).append("\n");
        }
    }
}
