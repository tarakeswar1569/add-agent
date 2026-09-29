package com.cgi.addagent.generator;
import com.cgi.addagent.model.*; import java.util.*;
public class FeCodeGenerator {
 private final TemplateEngine t=new TemplateEngine();
 public Map<String,String> generate(ModuleDesign m,ProjectConfig c)throws Exception{
  Map<String,String> o=new LinkedHashMap<>();String pre=m.classPrefix(), prefix=Character.toLowerCase(pre.charAt(0))+pre.substring(1);Map<String,String> v=new HashMap<>();
  v.put("CLASS_PREFIX",pre);v.put("CONTAINER_NAME",pre+"Container");v.put("SERVICE_VAR",prefix);v.put("FETCH_METHOD",m.primaryEndpoint()==null?"fetchData":m.primaryEndpoint().methodName());v.put("API_BASE_PATH",m.basePath());v.put("REQUEST_TYPE",pre+"Request");v.put("RESPONSE_TYPE",pre+"Response");v.put("ARTIFACT_ID",m.folderName());
  List<FrontendComponent> cs=m.components()==null?List.of():m.components();StringBuilder imports=new StringBuilder(),els=new StringBuilder();for(FrontendComponent x:cs){if(x.name().equals(pre+"Container"))continue;imports.append("import { ").append(x.name()).append(" } from '../components/").append(x.name()).append("/").append(x.name()).append("';\n");els.append("<").append(x.name()).append(" />");}
  v.put("COMPONENT_IMPORTS",imports.toString());v.put("CHILD_COMPONENT_ELEMENTS",els.toString());o.put("containers/"+pre+"Container/"+pre+"Container.tsx",t.render("fe/Container.tsx.tmpl",v));o.put("containers/"+pre+"Container/"+pre+"Container.test.tsx",t.render("fe/ContainerTest.tsx.tmpl",v));
  for(FrontendComponent x:cs){if(x.name().equals(pre+"Container"))continue;Map<String,String>xv=new HashMap<>(v);xv.put("CLASS_PREFIX",x.name());xv.put("COMPONENT_DESCRIPTION",x.description());o.put("components/"+x.name()+"/"+x.name()+".tsx",t.render("fe/Component.tsx.tmpl",xv));o.put("components/"+x.name()+"/"+x.name()+".test.tsx",t.render("fe/ComponentTest.tsx.tmpl",xv));}
  o.put("services/"+prefix+"Service.ts",t.render("fe/Service.ts.tmpl",v));v.put("INTERFACE_NAME",pre+"Request");v.put("INTERFACE_FIELDS",fields(m,true));v.put("TYPE_DESCRIPTION","Request type derived from ADD");o.put("ts/"+pre+"Request.ts",t.render("fe/Interface.ts.tmpl",v));v.put("INTERFACE_NAME",pre+"Response");v.put("INTERFACE_FIELDS",fields(m,false));v.put("TYPE_DESCRIPTION","Response type derived from ADD");o.put("ts/"+pre+"Response.ts",t.render("fe/Interface.ts.tmpl",v));
  v.put("HTTP_CLIENT_PKG",c.httpClientPackage());v.put("STYLING_PKGS",c.stylingPackages());v.put("TEST_PKGS",c.testPackages());o.put("package.json",t.render("fe/package.json.tmpl",v));o.put("tsconfig.json",t.render("fe/tsconfig.json.tmpl",v));return o;
 }
 private String fields(ModuleDesign m,boolean req){Set<String>s=new LinkedHashSet<>();for(ApiEndpoint e:m.endpoints())s.addAll(req?e.requestFields():e.responseFields());return String.join("\n  ",s);}
}
