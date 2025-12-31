package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SharedVectorTest {

    private static void assertArrayEqualsTol(double[] expected, double[] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], 1e-9);
        }
    }

    @Test
    void constructor_clonesInput() {
        double[] arr = {1, 2, 3};
        SharedVector v = new SharedVector(arr, VectorOrientation.ROW_MAJOR);
        arr[0] = 999;
        assertEquals(1, v.get(0), 1e-9);
    }

    @Test
    void get_inBounds_returnsValue() {
        SharedVector v = new SharedVector(new double[]{7, 8, 9}, VectorOrientation.ROW_MAJOR);
        assertEquals(8, v.get(1), 1e-9);
    }

    @Test
    void get_outOfBounds_throws() {
        SharedVector v = new SharedVector(new double[]{7, 8, 9}, VectorOrientation.ROW_MAJOR);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> v.get(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> v.get(3));
    }

    @Test
    void length_returnsLength() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3, 4}, VectorOrientation.ROW_MAJOR);
        assertEquals(4, v.length());
    }

    @Test
    void getOrientation_returnsOrientation() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
    }

    @Test
    void transpose_togglesOrientation() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void add_sameLengthAndOrientation_addsElementwise() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.ROW_MAJOR);
        a.add(b);
        assertArrayEqualsTol(new double[]{11, 22, 33}, new double[]{a.get(0), a.get(1), a.get(2)});
    }

    @Test
    void add_lengthMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void add_orientationMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void negate_flipsSigns() {
        SharedVector v = new SharedVector(new double[]{1, -2, 3}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertArrayEqualsTol(new double[]{-1, 2, -3}, new double[]{v.get(0), v.get(1), v.get(2)});
    }

    @Test
    void dot_validRowDotCol_computes() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(140, row.dot(col), 1e-9);
    }

    @Test
    void dot_wrongSelfOrientation_throws() {
        SharedVector self = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
        SharedVector other = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> self.dot(other));
    }

    @Test
    void dot_sameOrientation_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }

    @Test
    void dot_lengthMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }

    @Test
    void vecMatMul_requiresRowMajorVector() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{{1}, {1}});
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMul_dimensionMismatch_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMul_multipliesCorrectly() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{
                {3, 4, 5},
                {10, 20, 30}
        });
        v.vecMatMul(m);
        assertArrayEqualsTol(new double[]{23, 44, 65}, new double[]{v.get(0), v.get(1), v.get(2)});
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertEquals(3, v.length());
    }

    @Test
    void vecMatMul_withEmptyMatrix_requiresZeroLengthVector_andResultsZeroLength() {
        SharedVector v = new SharedVector(new double[]{}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[0][]);
        v.vecMatMul(m);
        assertEquals(0, v.length());
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void vecMatMul_withEmptyMatrix_nonZeroVector_throws() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[0][]);
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }
}
