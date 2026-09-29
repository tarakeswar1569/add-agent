package com.cgi.addagent.model;
import java.util.*;
public record ModuleDesign(String moduleName, String layer, String packageBase, String classPrefix,
                           String basePath, List<ApiEndpoint> endpoints, List<String> layerNames,
                           List<ExceptionInfo> exceptions, List<DbTable> dbTables,
                           List<FrontendComponent> components, List<String> rawContent) {
  public boolean isBe(){ return "BE".equalsIgnoreCase(layer); }
  public boolean isFe(){ return "FE".equalsIgnoreCase(layer); }
  public String serviceVar(){ return decap(classPrefix)+"Service"; }
  public String daoVar(){ return decap(classPrefix)+"DAO"; }
  public String folderName(){ return moduleName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)",""); }
  public ApiEndpoint primaryEndpoint(){ return endpoints==null||endpoints.isEmpty()?null:endpoints.getFirst(); }
  private static String decap(String s){ return s==null||s.isEmpty()?s:Character.toLowerCase(s.charAt(0))+s.substring(1); }
}
