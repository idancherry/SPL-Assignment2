package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector.clone();
        this.orientation=orientation;
    }

    public double get(int index) {
        readLock();
        try {
            if (index<0 || index >= vector.length) {
                throw new ArrayIndexOutOfBoundsException("Index out off bounds.");
            }
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        readLock();
        try {
            return orientation;
        } finally {
            readUnlock();
        }
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() {
        writeLock();
        try{
            if (orientation==VectorOrientation.COLUMN_MAJOR){
                orientation=VectorOrientation.ROW_MAJOR;
            }else{
                orientation = VectorOrientation.COLUMN_MAJOR;
            }
        }finally {
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        double[] otherArr;
        VectorOrientation orr;
        other.readLock();
        try{
            orr= other.orientation;
            otherArr = other.vector.clone();
        }finally {
            other.readUnlock();
        }
        writeLock();
        try{
            int len = vector.length;
            if (len!=otherArr.length || orr!=orientation){
                mismatchErr();
            }
            for (int i=0; i<len; i++){
                vector[i]+= otherArr[i];
            }
        }finally {
            writeUnlock();
        }
    }

    public void negate() {
        writeLock();
        try{
            int len = vector.length;
            for (int i=0; i<len; i++){
                vector[i]= -vector[i];
            }
        }finally {
            writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        if (orientation!=VectorOrientation.ROW_MAJOR) mismatchErr();
        double[] otherArr;
        VectorOrientation orr;
        other.readLock();
        double co=0;
        try{
            orr = other.orientation;
            otherArr = other.vector.clone();
        }finally {
            other.readUnlock();
        }
        readLock();
        try{
            int len = vector.length;
            if (len!=otherArr.length || orr!=orientation){
                mismatchErr();
            }
            for (int i=0; i<len; i++){
                co+=vector[i]*otherArr[i];
            }
        }finally {
            readUnlock();
        }
        return co;
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("Expects a ROW_MAJOR vector.");
        }
        double[][] mat = matrix.readRowMajor();
        int rows = mat.length;
        if (rows == 0) {
            writeLock();
            try {
                if (vector.length != 0) mismatchErr();
                vector = new double[0];
                orientation = VectorOrientation.ROW_MAJOR;
            } finally {
                writeUnlock();
            }
            return;
        }
        int cols = mat[0].length;
        writeLock();
        try {
            if (vector.length != rows) mismatchErr();
            double[] result = new double[cols];
            for (int j = 0; j < cols; j++) {
                double sum = 0.0;
                for (int i = 0; i < rows; i++) {
                    if (mat[i].length != cols) mismatchErr();
                    sum += vector[i] * mat[i][j];
                }
                result[j] = sum;
            }

            this.vector = result;
            this.orientation = VectorOrientation.ROW_MAJOR;
        } finally {
            writeUnlock();
        }
    }

    private void mismatchErr(){
        throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
    }
}
