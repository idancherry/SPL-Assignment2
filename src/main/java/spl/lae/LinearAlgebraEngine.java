package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        executor = new TiredExecutor(numThreads);

    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        computationRoot.associativeNesting();
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX){
            ComputationNode resolvableNode = computationRoot.findResolvable();
            if (resolvableNode==null){
                throw new IllegalStateException("No resolvable node found, " +
                        "but root node is not a matrix.");
            }
            resolvableNode.associativeNesting();
            loadAndCompute(resolvableNode);
        }
        try{
            executor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        List<ComputationNode> children = node.getChildren();
        if (children == null)
            throw new IllegalArgumentException("Operator node has no children.");

        ComputationNodeType type =node.getNodeType();
        if (children.size() == 1){
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
            switch (type){
                case NEGATE:
                    executor.submitAll(createNegateTasks());
                    break;

                case TRANSPOSE:
                    executor.submitAll(createTransposeTasks());
                    break;

                default:
                    throw new IllegalArgumentException("Invalid unary operation.");
            }
            node.resolve(leftMatrix.readRowMajor());
            return;
        }else if (children.size()==2){
            switch (type){
                case ADD:
                    leftMatrix.loadRowMajor(children.get(0).getMatrix());
                    rightMatrix.loadRowMajor(children.get(1).getMatrix());
                    executor.submitAll(createAddTasks());
                    break;

                case MULTIPLY:
                    leftMatrix.loadRowMajor(children.get(0).getMatrix());
                    rightMatrix.loadColumnMajor(children.get(1).getMatrix());
                    executor.submitAll(createMultiplyTasks());
                    break;

                default:
                    throw new IllegalArgumentException("Invalid binary operation: " + type);
            }
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        throw new IllegalArgumentException("Invalid arity: " + children.size());
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        isMatchDims();
        int rows = leftMatrix.length();
        List<Runnable> tasks = new ArrayList<>(rows);

        for(int i=0; i< rows; i++){
            final int rowIndex = i;
            tasks.add(() -> leftMatrix.get(rowIndex).add(rightMatrix.get(rowIndex)));
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        int[] a = leftMatrix.getDim();
        int[] b = rightMatrix.getDim();

        if (a[1] != b[0]) mismatchErr();

        int rows = leftMatrix.length();
        List<Runnable> tasks = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            tasks.add(() -> leftMatrix.get(rowIndex).vecMatMul(rightMatrix));
        }
        return tasks;
    }


    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        int rows = leftMatrix.length();
        List<Runnable> tasks = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            final int rowIndex = i;
            tasks.add(() -> leftMatrix.get(rowIndex).negate());
        }
        return tasks;
        
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        int outer = leftMatrix.length();
        List<Runnable> tasks = new ArrayList<>(outer);
        for (int i = 0; i < outer; i++) {
            final int idx = i;
            tasks.add(() -> leftMatrix.get(idx).transpose());
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }

    // helper
    private void mismatchErr(){
        throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
    }

    // helper
    private void isMatchDims(){
        int[] dims1 = leftMatrix.getDim();
        int[] dims2 = rightMatrix.getDim();
        if (dims1[0]!=dims2[0] || dims1[1]!=dims2[1]) mismatchErr();
    }
}
        
