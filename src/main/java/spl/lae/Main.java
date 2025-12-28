package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: java Main <num_threads> <input_json> <output_json>");
        }
        int numThreads;
        try{
            numThreads= Integer.parseInt(args[0]);
        }catch(Exception e){
            throw new IllegalArgumentException("Illegal value for number of threads.");
        }
        if (numThreads<1){
            throw new IllegalArgumentException("Needs at least 1 thread to run.");
        }

        String inputPath = args[1];
        String outputPath = args[2];

        LinearAlgebraEngine laeEngine=null;
        try{

            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(inputPath);

            laeEngine = new LinearAlgebraEngine(numThreads);

            ComputationNode resultNode = laeEngine.run(root);
            double[][] resultMatrix = resultNode.getMatrix();
            OutputWriter.write(resultMatrix, outputPath);

        } catch (Exception e) {
            // write error to output file and print to stderr
            try{
                OutputWriter.write(e.getMessage(), outputPath);
                System.err.println("Error: " + e.getMessage());

            } catch (IOException exception){
                exception.printStackTrace();
            }
        }finally {
            if (laeEngine!=null) {
                try {
                    laeEngine.shutdown();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}