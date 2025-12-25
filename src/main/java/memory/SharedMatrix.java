package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        load(true, matrix);
    }

    public void loadColumnMajor(double[][] matrix) {
        load(false, matrix);
    }

    public double[][] readRowMajor() {
        SharedVector[] local = vectors;
        acquireAllVectorReadLocks(local);
        try {
            double[][] matrix;
            int outer = local.length;
            if (outer==0) return new double[0][];
            int rows;
            int cols;
            VectorOrientation ori = local[0].getOrientation();
            for (int k = 1; k < outer; k++) {
                if (local[k].getOrientation() != ori) mismatchErr();
            }
            int inner = local[0].length();
            if (ori ==VectorOrientation.ROW_MAJOR){
                rows = outer;
                cols= inner;
                matrix = new double[rows][cols];
                for (int i=0; i<rows; i++){
                    if (local[i].length()!=cols) mismatchErr();
                    for (int j=0; j<cols; j++){
                        matrix[i][j] = local[i].get(j);
                    }
                }
                return matrix;
            }else{
                cols= outer;
                rows = inner;
                matrix = new double[rows][cols];
                for (int j = 0; j < cols; j++) {
                    if (local[j].length() != rows) mismatchErr();
                    for (int i = 0; i < rows; i++) {
                        matrix[i][j] = local[j].get(i);
                    }
                }
                return matrix;
            }
        } finally {
            releaseAllVectorReadLocks(local);
        }
    }

    public SharedVector get(int index) {
        SharedVector[] local = vectors;
        if (index < 0 || index >= local.length) throw new ArrayIndexOutOfBoundsException();
        return local[index];
    }

    public int length() {
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        SharedVector[] local = vectors;
        acquireAllVectorReadLocks(local);
        try {
            int outer = local.length;
            if (outer == 0) return VectorOrientation.ROW_MAJOR;
            VectorOrientation ori = local[0].getOrientation();
            for (int i = 1; i < outer; i++) {
                if (local[i].getOrientation() != ori) mismatchErr();
            }
            return ori;
        } finally {
            releaseAllVectorReadLocks(local);
        }
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++){
            vecs[i].readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (int i = vecs.length - 1; i >= 0; i--) {
            vecs[i].readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = 0; i < vecs.length; i++){
            vecs[i].writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for (int i = vecs.length - 1; i >= 0; i--) {
            vecs[i].writeUnlock();
        }
    }

    private void mismatchErr(){
        throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
    }

    private void load(boolean r_c, double[][] matrix){
        if (matrix == null) throw new IllegalArgumentException("Matrix is null.");
        int rows = matrix.length;
        if (rows==0){
            vectors = new SharedVector[0];
            return;
        }
        if (matrix[0] == null) throw new IllegalArgumentException("Matrix[0] is null.");
        int cols = matrix[0].length;
        for (int r = 1; r < rows; r++) {
            if (matrix[r] == null) throw new IllegalArgumentException("Matrix[" + r + "] is null");
            if (matrix[r].length != cols) mismatchErr();
        }

        SharedVector[] newVecs;
        VectorOrientation ori = r_c?
                VectorOrientation.ROW_MAJOR:
                    VectorOrientation.COLUMN_MAJOR;
        if (r_c){
            newVecs = new SharedVector[rows];
            for (int i=0; i<rows; i++){
                newVecs[i] = new SharedVector(matrix[i].clone(), ori);
            }
        }else{
            newVecs = new SharedVector[cols];
            for (int i=0; i<cols; i++){
                double[] vec = new double[rows];
                for (int j=0; j<rows; j++){
                    vec[j] = matrix[j][i];
                }
                newVecs[i] = new SharedVector(vec, ori);
            }
        }
        vectors = newVecs;
    }

    public int[] getDim(){
        SharedVector[] local = vectors;
        acquireAllVectorReadLocks(local);
        try{
            int outer = local.length;
            if (outer==0) return new int[]{0,0};
            int inner = local[0].length();
            VectorOrientation ori = local[0].getOrientation();
            for (int i = 1; i < outer; i++) {
                if (local[i].length() != inner || local[i].getOrientation() != ori) {
                    mismatchErr();
                }
            }
            return (ori == VectorOrientation.ROW_MAJOR)
                    ? new int[]{outer, inner}
                    : new int[]{inner, outer};
        }finally {
            releaseAllVectorReadLocks(local);
        }
    }
}
