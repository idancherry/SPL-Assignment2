package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedMatrixTest {

    // helpers
    private static void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual, "actual matrix is null");
        assertEquals(expected.length, actual.length, "row count mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertNotNull(actual[i], "actual row " + i + " is null");
            assertEquals(expected[i].length, actual[i].length, "col count mismatch at row " + i);
            for (int j = 0; j < expected[i].length; j++) {
                assertEquals(expected[i][j], actual[i][j], 1e-9,
                        "value mismatch at [" + i + "][" + j + "]");
            }
        }
    }

    private static double[][] m2x3() {
        return new double[][]{
                {1, 2, 3},
                {4, 5, 6}
        };
    }

    @Test
    void newMatrix_hasZeroLength_andDims00() {
        SharedMatrix sm = new SharedMatrix();
        assertEquals(0, sm.length());
        assertArrayEquals(new int[]{0, 0}, sm.getDim());
        assertEquals(VectorOrientation.ROW_MAJOR, sm.getOrientation());
        assertMatrixEquals(new double[0][], sm.readRowMajor());
    }

    @Test
    void constructor_loadsRowMajor() {
        double[][] input = m2x3();
        SharedMatrix sm = new SharedMatrix(input);

        assertEquals(2, sm.length());
        assertArrayEquals(new int[]{2, 3}, sm.getDim());
        assertEquals(VectorOrientation.ROW_MAJOR, sm.getOrientation());

        assertMatrixEquals(input, sm.readRowMajor());
    }

    @Test
    void loadRowMajor_thenReadRowMajor_returnsSameMatrix() {
        double[][] input = m2x3();
        SharedMatrix sm = new SharedMatrix();
        sm.loadRowMajor(input);

        assertArrayEquals(new int[]{2, 3}, sm.getDim());
        assertEquals(VectorOrientation.ROW_MAJOR, sm.getOrientation());
        assertMatrixEquals(input, sm.readRowMajor());
    }

    @Test
    void loadColumnMajor_thenReadRowMajor_returnsOriginalMatrix() {
        double[][] input = m2x3();
        SharedMatrix sm = new SharedMatrix();
        sm.loadColumnMajor(input);

        assertEquals(3, sm.length());
        assertArrayEquals(new int[]{2, 3}, sm.getDim());
        assertEquals(VectorOrientation.COLUMN_MAJOR, sm.getOrientation());

        assertMatrixEquals(input, sm.readRowMajor());
    }

    @Test
    void loadEmptyMatrix_resultsInZeroVectors_andReadEmpty() {
        SharedMatrix sm = new SharedMatrix();
        sm.loadRowMajor(new double[0][]);

        assertEquals(0, sm.length());
        assertArrayEquals(new int[]{0, 0}, sm.getDim());
        assertMatrixEquals(new double[0][], sm.readRowMajor());
    }

    @Test
    void loadRowMajor_clonesRows_modifyingInputAfterLoadDoesNotAffectMatrix() {
        double[][] input = m2x3();
        SharedMatrix sm = new SharedMatrix();
        sm.loadRowMajor(input);

        input[0][0] = 999;
        double[][] actual = sm.readRowMajor();

        assertEquals(1, actual[0][0], 1e-9, "matrix should not change after input mutation");
    }

    @Test
    void loadColumnMajor_copiesValues_modifyingInputAfterLoadDoesNotAffectMatrix() {
        double[][] input = m2x3();
        SharedMatrix sm = new SharedMatrix();
        sm.loadColumnMajor(input);

        input[1][2] = -123;
        double[][] actual = sm.readRowMajor();

        assertEquals(6, actual[1][2], 1e-9, "matrix should not change after input mutation");
    }

    //error handling
    @Test
    void loadRowMajor_nullMatrix_throws() {
        SharedMatrix sm = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> sm.loadRowMajor(null));
    }

    @Test
    void loadRowMajor_matrix0Null_throws() {
        SharedMatrix sm = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> sm.loadRowMajor(new double[][]{null}));
    }

    @Test
    void loadRowMajor_rowNull_throwsWithRowIndexMessage() {
        SharedMatrix sm = new SharedMatrix();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                sm.loadRowMajor(new double[][]{
                        {1, 2},
                        null
                })
        );
        assertTrue(ex.getMessage().contains("Matrix[1] is null"));
    }

    @Test
    void loadRowMajor_raggedRows_dimensionMismatch_throws() {
        SharedMatrix sm = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> sm.loadRowMajor(new double[][]{
                {1, 2, 3},
                {4, 5}
        }));
    }

    @Test
    void loadColumnMajor_raggedRows_dimensionMismatch_throws() {
        SharedMatrix sm = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> sm.loadColumnMajor(new double[][]{
                {1, 2, 3},
                {4, 5}
        }));
    }

    @Test
    void get_outOfBounds_throwsArrayIndexOutOfBounds() {
        SharedMatrix sm = new SharedMatrix(m2x3());
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sm.get(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sm.get(2));
    }
}
