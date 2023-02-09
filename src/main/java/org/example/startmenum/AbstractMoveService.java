package org.example.startmenum;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractMoveService {

    private final List<String> supportOps = Arrays.asList(DirectoryMoveService.Op.DEL_OP, DirectoryMoveService.Op.MOVE_OP, DirectoryMoveService.Op.SKIP_OP);

    abstract void before();

    abstract void end();

    protected boolean isValidOp(String inputOp) {
        return supportOps.contains(inputOp) || validSaveOp(inputOp);
    }

    protected boolean validSaveOp(String inputOp){
        return "3s".equals(inputOp);
    }

    protected boolean isContinue(String end){
        return !"exit".equals(end);
    }

}
