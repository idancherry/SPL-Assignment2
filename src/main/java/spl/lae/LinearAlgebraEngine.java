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
        return null;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition

        List<Runnable> tasks = new ArrayList<>();

        if (leftMatrix.length() != rightMatrix.length())
            throw new IllegalArgumentException("Illegal operation: dimensions mismatch");

        for(int i=0; i< leftMatrix.length(); i++){
            
            final int rowIndex = i;

            if(leftMatrix.get(rowIndex).length()!=rightMatrix.get(rowIndex).length())
                throw new IllegalArgumentException("Illegal operation: dimensions mismatch");

            Runnable task = new Runnable() {
            public void run() {leftMatrix.get(rowIndex).add(rightMatrix.get(rowIndex));}
            };

            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        List<Runnable> tasks = new ArrayList<>();

        double [][] newMatrix = new double[leftMatrix.length()][rightMatrix.get(1).length()];
        

        for(int i=0; i< leftMatrix.length(); i++){

            final int rowIndex = i;

            Runnable task = new Runnable() {

                

            public void run() {leftMatrix.get(rowIndex).dot(rightMatrix.get());}
            };

            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows

        List<Runnable> tasks = new ArrayList<>();


        for(int i=0; i< leftMatrix.length(); i++){

            final int rowIndex = i;

            Runnable task = new Runnable() {
            public void run() {leftMatrix.get(rowIndex).negate();}
            };

            tasks.add(task);
        }
        return tasks;
        
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();


        for(int i=0; i< leftMatrix.length(); i++){

            final int rowIndex = i;

            Runnable task = new Runnable() {
            public void run() {leftMatrix.get(rowIndex).transpose();}
            };

            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}
