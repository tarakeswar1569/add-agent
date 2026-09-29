package com.cgi.addagent.generator;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
public class TemplateEngine {
 public String render(String templateName,Map<String,String> variables)throws Exception{
   String content=null; Path loc=Path.of(TemplateEngine.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
   if(loc!=null){Path p=loc.resolve("templates").resolve(templateName);if(Files.exists(p))content=Files.readString(p,StandardCharsets.UTF_8);
     else {Path p2=loc.resolve(templateName);if(Files.exists(p2))content=Files.readString(p2,StandardCharsets.UTF_8);}}
   if(content==null){try(InputStream in=getClass().getResourceAsStream("/templates/"+templateName)){if(in!=null)content=new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
   if(content==null)throw new FileNotFoundException("Template not found: "+templateName);
   for(var e:variables.entrySet())content=content.replace("{{"+e.getKey()+"}}",e.getValue()==null?"":e.getValue()); return content;
 }
}
