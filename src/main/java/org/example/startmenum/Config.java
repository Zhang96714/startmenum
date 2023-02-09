package org.example.startmenum;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {

    public static final String configPath= "cp";//default config file name: cp

    private static final Set<String> ignoreDir=new DirSet();

    public static void init(){
       String cp= System.getProperty("cp");
       if(null == cp || cp.equals("")){
           cp=configPath;
       }
        Path configPath= Paths.get(cp).toAbsolutePath();
       if(Files.exists(configPath)){
           try {
               List<String> lines=Files.readAllLines(configPath);
               ignoreDir.addAll(lines);
           } catch (IOException ignore) {

           }
       }
    }

    public static boolean isIgnore(Path dir){
        if(ignoreDir.isEmpty()){
            return false;
        }

        String dirPath=dir.toString();
        String dirName=dirPath.substring(dirPath.lastIndexOf(File.separatorChar)+1);
        return ignoreDir.contains(dirName)
                || ignoreDir.contains(dir.toAbsolutePath().toString());
    }

    static class DirSet extends HashSet<String>{

        /**
         * to lowercase
         *
         * @param s element whose presence in this collection is to be ensured
         * @return true if added
         */
        @Override
        public boolean add(String s) {
            return null != s && !s.equals("") && !skipLine(s) && super.add(s.toLowerCase());
        }

        private boolean skipLine(String line){
            return line.startsWith("#") || line.startsWith("//");
        }

        /**
         * to lowercase
         *
         * @param o element whose presence in this set is to be tested
         * @return true if contain
         */
        @Override
        public boolean contains(Object o) {
            return super.contains(o.toString().toLowerCase());
        }
    }
}
