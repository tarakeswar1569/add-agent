package com.cgi.addagent.extractor;
import com.cgi.addagent.model.*;
import java.util.*;
import java.util.regex.*;
public class ModuleExtractor {
 private static final Pattern EP=Pattern.compile("\\b(GET|POST|PUT|DELETE|PATCH)\\s+(/[^\\s]+)",Pattern.CASE_INSENSITIVE);
 private static final Pattern EX=Pattern.compile("\\b([A-Z][A-Za-z0-9]*(?:Exception|Error))\\b");
 private static final Pattern FIELD=Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*:\\s*([A-Za-z0-9_\\[\\]<>?]+)");
 private static final Set<String> SUFFIX=Set.of("Container","Panel","Table","List","Header","Footer","Form","Modal","Card","Button","Section","Widget","Page","View","Grid");
 public ModuleDesign extract(String moduleName,String layer,List<Paragraph> paragraphs){
   String folder=moduleName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");
   String pkg="com.cgi."+folder.replace("-",""); String prefix=pascal(moduleName);
   List<ApiEndpoint> eps=new ArrayList<>(); Set<String> seen=new HashSet<>(); List<ExceptionInfo> exs=new ArrayList<>(); List<DbTable> tables=new ArrayList<>(); List<FrontendComponent> comps=new ArrayList<>(); List<String> raw=new ArrayList<>(); Set<String> fields=new LinkedHashSet<>();
   String base="";
   for(Paragraph p:paragraphs){ raw.add(p.text()); Matcher em=EP.matcher(p.text()); while(em.find()){String method=em.group(1).toUpperCase();String path=em.group(2);String key=method+":"+path;if(seen.add(key)){if(base.isEmpty())base=basePath(path);eps.add(new ApiEndpoint(method,path,path.substring(base.length()),methodName(method,path),new ArrayList<>(),new ArrayList<>()));}}
     Matcher xm=EX.matcher(p.text()); while(xm.find()) exs.add(new ExceptionInfo(xm.group(1),p.text(),400,"BAD_REQUEST"));
     if(p.text().toUpperCase(Locale.ROOT).contains("TABLE")) tables.add(new DbTable(cleanTable(p.text()),p.text()));
     if("FE".equalsIgnoreCase(layer) && p.style()!=null && p.style().equalsIgnoreCase("ListParagraph")){String n=p.text().split("[: -]",2)[0].trim();if(SUFFIX.stream().anyMatch(n::endsWith)) comps.add(new FrontendComponent(n,p.text(),List.of()));}
     Matcher fm=FIELD.matcher(p.text()); while(fm.find()) fields.add(fm.group(1)+": "+fm.group(2));
   }
   List<String> lf=new ArrayList<>(fields); for(int i=0;i<eps.size();i++){ApiEndpoint e=eps.get(i);eps.set(i,new ApiEndpoint(e.method(),e.path(),e.subPath(),e.methodName(),i%2==0?lf:List.of(),i%2==1?lf:List.of()));}
   if(comps.isEmpty() && "FE".equalsIgnoreCase(layer)){comps.add(new FrontendComponent(prefix+"List","Placeholder list",List.of()));comps.add(new FrontendComponent(prefix+"Header","Placeholder header",List.of()));}
   return new ModuleDesign(moduleName,layer,pkg,prefix,base,eps,List.of("controller","service","dao","repository","entity","model"),exs,tables,comps,raw);
 }
 private String basePath(String p){String[] x=p.split("/");return x.length>=3?"/"+x[1]+"/"+x[2]:p;}
 private String methodName(String m,String p){String s=p.replaceAll("[^A-Za-z0-9]+"," ").trim();StringBuilder b=new StringBuilder(m.toLowerCase());for(String x:s.split(" ")){if(!x.isEmpty())b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));}return b.toString();}
 private String cleanTable(String s){return s.replaceAll("(?i).*?TABLE\\s*[:=]?\\s*","").split("\\s+")[0];}
 private String pascal(String s){StringBuilder b=new StringBuilder();for(String x:s.split("[^A-Za-z0-9]+"))if(!x.isBlank())b.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));return b.toString();}
}
