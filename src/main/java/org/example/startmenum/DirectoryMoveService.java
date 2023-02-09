package org.example.startmenum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DirectoryMoveService {
    final String START_MENU = "C:\\ProgramData\\Microsoft\\Windows\\Start Menu\\Programs";//public
    static boolean autoRemoveEmpty=true;
    static boolean useCpFile=true;

    private final Path startMenu;
    private final Map<Path, Op> opMap;
    private final Map<String, Op> ops;

    DirectoryMoveService() {
        startMenu = Paths.get(START_MENU);
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

    public void moveStartMenu() throws IOException {
        System.out.println("可移动目录如下:");
        List<Path> subDirs = new ArrayList<>();
        //dirs
        addDirs(subDirs);
        if (subDirs.isEmpty()) {
            System.out.println("结束.");
            return;
        }

        System.out.println("开始操作操作说明:1 删除 2 移动 3 跳过 (exit 可退出)");
        //add op
        addOps(subDirs);
        //tasks
        printTasks();
        //run tasks
        runTasks();

        System.out.println("结束.");
    }

    void addDirs(List<Path> subDirs) throws IOException{
        Files.list(startMenu).forEach(path -> {
            if (path.toFile().isDirectory()) {
                boolean add=true;
                if(useCpFile){
                    add=!Config.isIgnore(path);
                }
                if(add){
                    System.out.println(path);
                    subDirs.add(path);
                }

            }
        });
    }

    void addOps(List<Path> subDirs) throws IOException{
        Scanner scanner = new Scanner(System.in);
        for (Path path : subDirs) {
            //files
            System.out.println(path.toString() + "下的子目录:");
            long count = Files.list(path).count();
            //skip ignore
            if (0 == count && autoRemoveEmpty && Config.isIgnore(path)) {
                opMap.put(path, getOp(Op.DEL_OP));
                continue;
            }
            AtomicBoolean exit=new AtomicBoolean(false);
            Files.list(path).forEach(file -> {
                //show all file or exe file
                if (!exit.get() && (Files.isExecutable(file) || Files.isRegularFile(file))) {
                    System.out.print("file:" + file.getFileName() + ",选择 操作:");
                    String op = scanner.nextLine();
                    while (!isValidOp(op) && isContinue(op)) {
                        op = scanner.nextLine();
                    }
                    if(isContinue(op)){
                        opMap.put(file, getOp(op));
                    }else {
                        exit.set(true);
                    }
                }
            });
            if(exit.get()){
                //exit
                System.exit(0);
            }

        }
    }

    Op getOp(String op) {
        return ops.get(op);
    }

    void printTasks() {
        if (opMap.isEmpty()) {
            System.out.println("未进行操作.");
            return;
        }
        System.out.println("将要执行任务:");
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
        }
    }

    List<String> supportOps = Arrays.asList(Op.DEL_OP, Op.MOVE_OP, Op.SKIP_OP);

    boolean isValidOp(String inputOp) {
        return supportOps.contains(inputOp);
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
            Path p = path.getParent();
            try {
                Files.move(path, p);
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
        new DirectoryMoveService().moveStartMenu();
    }
}
