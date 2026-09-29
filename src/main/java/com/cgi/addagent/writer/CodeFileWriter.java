package com.cgi.addagent.writer;
import java.io.*; import java.nio.file.*; import java.util.*;
public class CodeFileWriter {
 public int write(String targetDir,String layer,String moduleName,Map<String,String> fileMap)throws IOException{
   Path root=Path.of(targetDir,layer.toUpperCase(Locale.ROOT),moduleName);int n=0;
   for(var e:fileMap.entrySet()){String rel=e.getKey();if("FE".equalsIgnoreCase(layer))rel=Path.of("src",rel).toString();Path p=root.resolve(rel).normalize();if(!p.startsWith(root))throw new IOException("Invalid output path: "+rel);
     Files.createDirectories(p.getParent());if(Files.exists(p)){Path bak=p.resolveSibling(p.getFileName()+".bak");Files.copy(p,bak,StandardCopyOption.REPLACE_EXISTING);}Files.writeString(p,e.getValue());n++;}
   return n;
 }
}
