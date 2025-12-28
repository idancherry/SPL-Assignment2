package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java Main <input_json> <output_json> <num_threads>");
            return;
        }

        int numThreads = Integer.parseInt(args[0]);
        String inputPath = args[1];
        String outputPath = args[2];

        try{

            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(inputPath);

            LinearAlgebraEngine laeEngine = new LinearAlgebraEngine(numThreads);

            ComputationNode resultNode = laeEngine.run(root);
            double[][] resultMatrix = resultNode.getMatrix();
            OutputWriter.write(resultMatrix, outputPath);

            laeEngine.shutdown();


        } catch (Exception e) {
            // write error to output file and print to stderr
            try{
                OutputWriter.write(e.getMessage(), outputPath);
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();

            } catch (IOException exception){
                exception.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}