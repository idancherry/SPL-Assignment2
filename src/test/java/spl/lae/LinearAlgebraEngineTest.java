package spl.lae;

import memory.SharedMatrix;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTest {

    private static SharedMatrix getMatrixField(LinearAlgebraEngine engine, String fieldName) {
        try {
            Field f = LinearAlgebraEngine.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            return (SharedMatrix) f.get(engine);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setLeftRight(LinearAlgebraEngine engine, double[][] left, double[][] right) {
        SharedMatrix lm = getMatrixField(engine, "leftMatrix");
        SharedMatrix rm = getMatrixField(engine, "rightMatrix");
        lm.loadRowMajor(left);
        if (right != null) rm.loadRowMajor(right);
    }

    private static void runAll(List<Runnable> tasks) {
        for (Runnable r : tasks) r.run();
    }

    private static void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertNotNull(actual[i]);
            assertEquals(expected[i].length, actual[i].length);
            for (int j = 0; j < expected[i].length; j++) {
                assertEquals(expected[i][j], actual[i][j], 1e-9);
            }
        }
    }

    @Test
    void createAddTasks_addsRowWiseIntoLeftMatrix() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, 2},
                {3, 4}
        };
        double[][] b = {
                {10, 20},
                {30, 40}
        };
        setLeftRight(engine, a, b);

        runAll(engine.createAddTasks());

        SharedMatrix lm = getMatrixField(engine, "leftMatrix");
        assertMatrixEquals(new double[][]{
                {11, 22},
                {33, 44}
        }, lm.readRowMajor());
    }

    @Test
    void createAddTasks_dimensionMismatch_throws() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, 2, 3}
        };
        double[][] b = {
                {1, 2}
        };
        setLeftRight(engine, a, b);

        assertThrows(IllegalArgumentException.class, engine::createAddTasks);
    }

    @Test
    void createNegateTasks_negatesLeftMatrixRows() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, -2, 3},
                {-4, 5, 0}
        };
        setLeftRight(engine, a, null);

        runAll(engine.createNegateTasks());

        SharedMatrix lm = getMatrixField(engine, "leftMatrix");
        assertMatrixEquals(new double[][]{
                {-1, 2, -3},
                {4, -5, -0}
        }, lm.readRowMajor());
    }

    @Test
    void createTransposeTasks_transposesLogicalMatrix() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };
        setLeftRight(engine, a, null);

        runAll(engine.createTransposeTasks());

        SharedMatrix lm = getMatrixField(engine, "leftMatrix");
        assertMatrixEquals(new double[][]{
                {1, 4},
                {2, 5},
                {3, 6}
        }, lm.readRowMajor());
    }

    @Test
    void createMultiplyTasks_multipliesLeftByRight_rowWise() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };
        double[][] b = {
                {7, 8},
                {9, 10},
                {11, 12}
        };
        setLeftRight(engine, a, b);

        runAll(engine.createMultiplyTasks());

        SharedMatrix lm = getMatrixField(engine, "leftMatrix");
        assertMatrixEquals(new double[][]{
                {58, 64},
                {139, 154}
        }, lm.readRowMajor());
    }

    @Test
    void createMultiplyTasks_dimensionMismatch_throws() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] a = {
                {1, 2, 3}
        };
        double[][] b = {
                {1, 2},
                {3, 4}
        };
        setLeftRight(engine, a, b);

        assertThrows(IllegalArgumentException.class, engine::createMultiplyTasks);
    }
}
