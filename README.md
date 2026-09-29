# add-code-agent

`add-code-agent` is a Java 21 standalone Maven application that reads an Application Design Document (ADD) in `.docx` or `.docm` format and generates backend and/or frontend code skeletons.

The application can be used in batch/interactive CLI mode or as an MCP STDIO server for clients such as VS Code GitHub Copilot.

## Requirements

- Java 21
- Maven 3.9+
- An ADD document in `.docx` or `.docm` format
- For MCP use: an MCP-capable client such as VS Code GitHub Copilot

The agent itself uses Java standard-library APIs at runtime. It does not require Spring Boot, Jackson, or another JSON library.

## Project layout

```text
add-code-agent/
├── pom.xml
├── src/main/java/com/cgi/addagent/
│   ├── Main.java
│   ├── parser/DocParser.java
│   ├── router/SectionRouter.java
│   ├── extractor/ModuleExtractor.java
│   ├── generator/
│   ├── writer/CodeFileWriter.java
│   ├── model/
│   └── mcp/
└── src/main/resources/templates/
    ├── be/
    └── fe/
```

## Build

From the project root:

```bash
mvn clean install
```

The assembly plugin creates:

```text
target/add-code-agent.jar
```

Verify the JAR:

```bash
java -jar target/add-code-agent.jar
```

With no arguments, the application prints its usage information.

## CLI usage

Generate all layers for all discovered modules:

```bash
java -jar target/add-code-agent.jar \
  --add C:/ADD/Banking.docm \
  --output C:/Generated/Banking \
  --mode ALL \
  --module ALL
```

Generate only one backend module:

```bash
java -jar target/add-code-agent.jar \
  --add C:/ADD/Banking.docm \
  --output C:/Generated/Banking \
  --mode BE \
  --module Account
```

Generate only one frontend module:

```bash
java -jar target/add-code-agent.jar \
  --add C:/ADD/Banking.docm \
  --output C:/Generated/Banking \
  --mode FE \
  --module Account
```

Interactive mode:

```bash
java -jar target/add-code-agent.jar --interactive
```

## MCP mode

Start the MCP STDIO server directly:

```bash
java -jar target/add-code-agent.jar --mcp
```

Do not type normal text into the process while it is running in MCP mode. STDIN and STDOUT are reserved for JSON-RPC traffic.

Diagnostics should go to STDERR, not STDOUT.

### MCP tools

The MCP server intentionally exposes a small, explicit workflow:

1. `configure_project`
2. `load_add`
3. `get_modules`
4. `generation_plan`
5. `generate_module`
6. `generate_all`

The important design rule is that `generate_module` does not guess the module or layer. The client must provide them explicitly.

### Recommended workflow

#### 1. Configure the project

Use `configure_project` when non-default technology choices are required. All properties are optional; omitted properties use the defaults.

Example arguments:

```json
{
  "dbType": "postgresql",
  "beLibraries": "mapstruct,flyway",
  "feFramework": "react",
  "feLanguage": "typescript",
  "httpClient": "axios"
}
```

Default configuration:

```text
projectType       = single
javaVersion       = 21
springBootVersion = 3.3.2
buildTool         = maven
dbType            = db2
packaging         = jar
beLibraries       = empty
feFramework       = react
feLanguage        = typescript
packageManager    = npm
styling           = css-modules
httpClient        = axios
testFramework     = jest-rtl
folderStructure   = feature
```

#### 2. Load the ADD

Call `load_add` with the complete path:

```json
{
  "add_path": "C:/ADD/Banking.docm"
}
```

The server parses the document and stores the discovered backend/frontend module sections in memory.

#### 3. Get the modules

Call `get_modules` to see the available modules.

Example result:

```text
Backend modules:
  1. Account
  2. Customer
  3. Payment
  4. Transaction

Frontend modules:
  1. Account
  2. Customer
  3. Payment
  4. Transaction
```

#### 4. Review a generation plan

Before writing files, call `generation_plan`:

```json
{
  "module": "Account",
  "layer": "BE",
  "output_dir": "C:/Generated/Banking"
}
```

This generates the same in-memory file map used by the actual generator but does not write files.

#### 5. Generate one module

Call `generate_module` explicitly:

```json
{
  "module": "Account",
  "layer": "BE",
  "output_dir": "C:/Generated/Banking"
}
```

Valid layer values are:

```text
BE
FE
ALL
```

The output is written under:

```text
C:/Generated/Banking/BE/Account
```

or:

```text
C:/Generated/Banking/FE/Account
```

Existing files are backed up with a `.bak` suffix before being overwritten.

#### 6. Generate everything

`generate_all` requires explicit confirmation:

```json
{
  "output_dir": "C:/Generated/Banking",
  "confirm": true
}
```

If `confirm` is false or omitted, nothing is generated.

## MCP manual smoke test

Start the server:

```bash
java -jar target/add-code-agent.jar --mcp
```

Send an initialize request:

```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
```

Expected response contains:

```text
"serverInfo":{"name":"add-code-agent","version":"1.0.0"}
```

Then request the tools:

```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

The response should list exactly:

```text
configure_project
load_add
get_modules
generation_plan
generate_module
generate_all
```

## VS Code MCP configuration

Create or update `.vscode/mcp.json`:

```json
{
  "servers": {
    "add-code-agent": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "C:/projects/add-code-agent/target/add-code-agent.jar",
        "--mcp"
      ]
    }
  }
}
```

Replace the JAR path with the actual absolute path on the machine running VS Code.

After changing the JAR, rebuild it and restart/reload the MCP server in VS Code so the client does not continue using an older JAR.

## Recommended Copilot workflow

Use the custom MCP server as the owner of the ADD-to-code workflow:

```text
Load ADD
   ↓
Get modules
   ↓
Select module
   ↓
Select BE / FE / ALL
   ↓
Review generation plan
   ↓
Confirm
   ↓
Generate module
```

The MCP tools deliberately require explicit arguments. This reduces the chance that the client will infer a different module, invent a workflow, or start generating code without the user's explicit selection.

Do not put interactive `Scanner` prompts inside `McpServer`. In MCP mode, STDIN/STDOUT are the protocol channel.

## IntelliJ IDEA development/debugging

1. Open the `add-code-agent` project in IntelliJ IDEA.
2. Configure Project SDK as Java 21.
3. Allow IntelliJ to import the Maven project.
4. Run `mvn clean install` from the Maven tool window or terminal.
5. For normal CLI debugging, create an Application run configuration with:
   - Main class: `com.cgi.addagent.Main`
   - Program arguments: for example `--add C:/ADD/Banking.docm --output C:/Generated/Banking --mode BE --module Account`
6. For MCP debugging, use `--mcp` as the program argument.
7. Useful breakpoints are in `McpServer`, `JsonUtil`, `DocParser`, `SectionRouter`, `ModuleExtractor`, `BeCodeGenerator`, `FeCodeGenerator`, and `CodeFileWriter`.

## Troubleshooting

### VS Code reports `Process exited with code 1`

Run the exact command from a VS Code terminal:

```bash
java -jar C:/projects/add-code-agent/target/add-code-agent.jar --mcp
```

If this fails, fix the JAR/Java/build problem before troubleshooting MCP configuration.

Check Java:

```bash
java -version
```

It should report Java 21.

Rebuild:

```bash
mvn clean install
```

Then restart the MCP server in VS Code.

### VS Code starts the server but tools are missing

Run the manual `initialize` and `tools/list` smoke tests. The current server should expose:

```text
configure_project
load_add
get_modules
generation_plan
generate_module
generate_all
```

If old names such as `parse_add` or `generate_skeleton` appear, VS Code is using an older JAR.

### MCP JSON errors

Do not print banners, debug messages, stack traces, or prompts to STDOUT while `--mcp` is running. STDOUT must contain only JSON-RPC responses.

Use STDERR for diagnostics.

### `Module was not found`

Call `load_add` first, then `get_modules`. Use the exact discovered module name or a name that normalizes to it.

### `generate_all` refuses to run

The tool intentionally requires:

```json
"confirm": true
```

This is a safety check against accidentally generating or overwriting every discovered module.

### Generated files are overwritten

The writer creates a `.bak` copy before replacing an existing file.

## Design notes

- The MCP server is stateful for the lifetime of the process.
- The loaded ADD and discovered modules remain in memory until the MCP process exits or another ADD is loaded.
- Module selection is validated against discovered module names.
- Generation uses the same `ModuleExtractor` and generator classes used by the CLI.
- `ProjectConfig` is passed into both backend and frontend generators.
- The agent does not depend on a JSON library; `JsonUtil` handles only the limited MCP JSON structures required by this application.
