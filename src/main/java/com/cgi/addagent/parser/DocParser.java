package com.cgi.addagent.parser;
import com.cgi.addagent.model.Paragraph;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
public class DocParser {
 public List<Paragraph> parse(String filePath) throws Exception {
  List<Paragraph> result=new ArrayList<>();
  try(ZipFile zip=new ZipFile(filePath)){
   ZipEntry e=zip.getEntry("word/document.xml");
   if(e==null) throw new FileNotFoundException("Missing word/document.xml in "+filePath);
   DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
   f.setNamespaceAware(true);
   try(InputStream in=zip.getInputStream(e)){
    Document d=f.newDocumentBuilder().parse(in);
    NodeList ps=d.getElementsByTagNameNS("*","p");
    for(int i=0;i<ps.getLength();i++){
     Element p=(Element)ps.item(i); String style="";
     NodeList sp=p.getElementsByTagNameNS("*","pStyle");
     if(sp.getLength()>0) style=((Element)sp.item(0)).getAttributeNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main","val");
     NodeList ts=p.getElementsByTagNameNS("*","t"); StringBuilder text=new StringBuilder();
     for(int j=0;j<ts.getLength();j++) text.append(ts.item(j).getTextContent());
     if(!text.toString().isBlank()) result.add(new Paragraph(style,text.toString().trim()));
    }
   }
  }
  return result;
 }
}
