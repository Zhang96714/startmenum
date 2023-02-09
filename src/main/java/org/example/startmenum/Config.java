package org.example.startmenum;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Config {

    public static final String configPath = "cp";//default config file name: cp
    private static final Set<String> ignoreDir = new DirSet();
    private static Path CURRENT_CP_PATH;

    public static void init() {
        String cp = System.getProperty("cp");
        if (null == cp || cp.equals("")) {
            cp = configPath;
        }
        Path configPath = Paths.get(cp).toAbsolutePath();
        CURRENT_CP_PATH = configPath;
        if (Files.exists(configPath)) {
            try {
                List<String> lines = Files.readAllLines(configPath);
                ignoreDir.addAll(lines);
            } catch (IOException ignore) {

            }
        }else {
            try {
                Files.createFile(configPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static boolean isNotIgnore(Path dir) {
        if (ignoreDir.isEmpty()) {
            return true;
        }
        return !ignoreDir.contains(getName(dir))
                && !ignoreDir.contains(dir.toAbsolutePath().toString());
    }

    public static String getName(Path dir) {
        String dirPath = dir.toString();
        return dirPath.substring(dirPath.lastIndexOf(File.separatorChar) + 1);
    }

    public static void updateCpFile() {
        if(!Files.isWritable(CURRENT_CP_PATH)){
           boolean b= CURRENT_CP_PATH.toFile().setWritable(true);
           if(!b){
               System.out.println("set cp file writable fail.");
           }
        }
        if(Files.isWritable(CURRENT_CP_PATH)){
            try(
                    BufferedWriter writer=Files.newBufferedWriter(CURRENT_CP_PATH)
                    ){
                for (String line:ignoreDir) {
                    writer.append(line).append(System.lineSeparator());
                }
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    public static void appendToCpFile(Path path){
        String name=getName(path);
        ignoreDir.add(name);
    }

    static class DirSet extends HashSet<String> {

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

        private boolean skipLine(String line) {
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

    public static class Bool{

        AtomicBoolean exit=new AtomicBoolean(false);
        AtomicBoolean save= new AtomicBoolean(false);

        public boolean isExit(){
            return exit.get();
        }

        public boolean isSave(){
            return save.get();
        }

        public void setExit(){
            exit.set(true);
        }

        public void setSave(){
            save.set(true);
        }
    }
}
