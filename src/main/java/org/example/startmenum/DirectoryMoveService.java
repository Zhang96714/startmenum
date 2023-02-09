package org.example.startmenum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectoryMoveService {
    final String START_MENU = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs";//public
    final String USER_START_MENU="C:\\Users\\%s\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs";
    static boolean autoRemoveEmpty=true;
    static boolean useCpFile=true;

    private Path startMenu;
    private final Map<Path, Op> opMap;
    private final Map<String, Op> ops;

    Scanner scanner = new Scanner(System.in);

    DirectoryMoveService() {
        opMap = new TreeMap<>();//sorted
        ops = new HashMap<>();

        ops.put(Op.DEL_OP, new DeleteOp());
        ops.put(Op.MOVE_OP, new MoveOp());
        ops.put(Op.SKIP_OP, new SkipOp());

        init();
    }

    void init(){
        Config.init();
    }

    public void moveStartMenu(String location) throws IOException {
        startMenu = Paths.get(location);
        List<Path> subDirs = new ArrayList<>();
        //dirs
        addDirs(subDirs);
        if (subDirs.isEmpty()) {
            return;
        }

        System.out.println("开始操作操作说明:1 删除 2 移动 3[s] 跳过,s会保存到cp文件中");
        //add op
        addOps(subDirs);
        //tasks
        printTasks();
        //run tasks
        runTasks();
    }

    void addDirs(List<Path> subDirs) throws IOException{
        Files.list(startMenu).forEach(path -> {
            //only directory
            if (path.toFile().isDirectory()) {
                boolean add=true;
                if(useCpFile){
                    add= Config.isNotIgnore(path);
                }
                if(add){
                    subDirs.add(path);
                }

            }
        });
    }

    void addOps(List<Path> subDirs) throws IOException{
        Config.Bool bool=new Config.Bool();

        for (Path path : subDirs) {
            long count = Files.list(path).count();
            //skip ignore, remove empty dir
            if (0 == count && autoRemoveEmpty && Config.isNotIgnore(path)) {
                opMap.put(path, getOp(Op.DEL_OP));
                continue;
            }

            //files
            Files.list(path).forEach(file -> {
                //not exit
                if(Config.isNotIgnore(file)){
                    if(Files.isDirectory(file)){
                        //sub dir
                        try {
                            Files.list(file).forEach(subFile -> {
                                if(Config.isNotIgnore(subFile) && !Files.isDirectory(subFile)){
                                    prepareOps(subFile,bool);
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }else {
                        //file
                        prepareOps(file,bool);
                    }
                }
            });
            if(bool.isExit()){
                //exit
                System.exit(0);
            }

        }

        if(bool.isSave()){
            Config.updateCpFile();
        }
    }

    void prepareOps(Path file, Config.Bool bool){
        //not exit
        if (!bool.isExit()) {
            String filename=file.getFileName().toString();
            System.out.print("\""+filename + "\",选择 操作:");
            String op = scanner.nextLine();
            while (!isValidOp(op) && isContinue(op)) {
                op = scanner.nextLine();
            }
            if(isContinue(op)){
                //cp file update
                String o=op;
                if(validSaveOp(op)){
                    Config.appendToCpFile(file);
                    bool.setSave();
                    //first
                    o=op.substring(0,1);
                }
                opMap.put(file, getOp(o));
            }else {
                bool.setExit();
            }
        }
    }

    Op getOp(String op) {
        return ops.get(op);
    }

    void printTasks() {
        if (opMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Path, Op> entry : opMap.entrySet()
        ) {
            if (!entry.getValue().skip()) {
                System.out.println("file:" + entry.getKey() + "行为:" + entry.getValue());
            }
        }
    }

    void runTasks() {
        if (!opMap.isEmpty()) {
            for (Map.Entry<Path, Op> e : opMap.entrySet()
            ) {
                Path k = e.getKey();
                Op v = e.getValue();
                try {
                    v.work(k);
                } catch (MoveException ex) {
                    ex.printStackTrace();
                }
            }
            //clean
            opMap.clear();
        }
    }

    List<String> supportOps = Arrays.asList(Op.DEL_OP, Op.MOVE_OP, Op.SKIP_OP);

    boolean isValidOp(String inputOp) {
        return supportOps.contains(inputOp) || validSaveOp(inputOp);
    }

    boolean validSaveOp(String inputOp){
        return "3s".equals(inputOp);
    }

    boolean isContinue(String end){
        return !"exit".equals(end);
    }

    interface Op {
        String DEL_OP = "1";
        String MOVE_OP = "2";
        String SKIP_OP = "3";

        default void work(Path path) throws MoveException{}

        default boolean skip() {
            return false;
        }
    }

    static class SkipOp implements Op {

        @Override
        public String toString() {
            return "跳过";
        }

        @Override
        public boolean skip() {
            return true;
        }
    }

    static class MoveOp implements Op {

        /**
         * move to parent
         *
         * @param path current file path
         */
        @Override
        public void work(Path path) throws MoveException {
            //get parent path
            Path p = path.getParent().getParent();
            Path newFile=p.resolve(Config.getName(path));
            try {
                if(Files.exists(newFile)){
                    //delete
                    Files.delete(newFile);
                }
                Files.move(path, newFile);
            } catch (IOException e) {
                throw new MoveException(e);
            }
        }

        @Override
        public String toString() {
            return "移动";
        }
    }

    static class DeleteOp implements Op {

        @Override
        public void work(Path path) throws MoveException{
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new MoveException(e);
            }
        }

        @Override
        public String toString() {
            return "删除";
        }
    }

    public static void main(String[] args) throws IOException {
        DirectoryMoveService service=new DirectoryMoveService();
        service.moveStartMenu(service.START_MENU);

        String username=System.getProperty("user.name");
        if(null !=username  && !"".equals(username)){
            service.moveStartMenu(service.USER_START_MENU.replace("%s",username));
        }
    }
}
