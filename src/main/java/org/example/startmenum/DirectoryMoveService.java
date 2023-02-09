package org.example.startmenum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
        if (subDirs.isEmpty()) {
            System.out.println("结束.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("开始操作操作说明:1 删除 2 移动 3 跳过.3");
        for (Path path : subDirs) {
            //files
            System.out.println(path.toString() + "下的子目录:");
            long count = Files.list(path).count();
            if (0 == count && autoRemoveEmpty) {
                opMap.put(path, getOp(Op.DEL_OP));
                continue;
            }
            Files.list(path).forEach(file -> {
                if (file.toFile().isFile()) {
                    System.out.println("file:" + file.getFileName() + ",选择 操作:");
                    String op = scanner.nextLine();
                    while (!isValidOp(op)) {
                        op = scanner.nextLine();
                    }
                    opMap.put(file, getOp(op));
                }
            });
        }

        //tasks
        printTasks();
        //run tasks
        runTasks();

        System.out.println("结束.");
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
