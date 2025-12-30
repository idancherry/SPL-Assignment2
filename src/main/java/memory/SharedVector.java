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

    // getter
    public double get(int index) {
        readLock();
        try {
            if (index<0 || index >= vector.length) {
                throw new ArrayIndexOutOfBoundsException("Index out of bounds.");
            }
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    // returns length of vector
    public int length() {
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

    // returns orientation of the vector (ROW/COLUMN)
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

    // flips vector's orientation
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

    // assigns the vector with the sum of itself and passed vector
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

    // negates all the numbers in the vector
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

    // returns the product of vector (ROW) and passed vector (COLUMN)
    public double dot(SharedVector other) {
        this.readLock();
        other.readLock();
        try {
            int len = this.vector.length;
            if (len != other.vector.length) mismatchErr();

            if (this.orientation != VectorOrientation.ROW_MAJOR) mismatchErr();
            if (other.orientation != VectorOrientation.COLUMN_MAJOR) mismatchErr();

            double sum = 0.0;
            double[] a = this.vector;
            double[] b = other.vector;
            for (int i = 0; i < len; i++) {
                sum += a[i] * b[i];
            }
            return sum;
        } finally {
            other.readUnlock();
            this.readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        if (getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("Expects a ROW_MAJOR vector.");
        }

        boolean colmatrix = (matrix.getOrientation() == VectorOrientation.COLUMN_MAJOR);
        int[] dim = matrix.getDim();
        int rows = dim[0];
        int cols = dim[1];

        writeLock();
        try {
            if (vector.length != rows) mismatchErr();

            if (rows == 0) {
                vector = new double[0];
                orientation = VectorOrientation.ROW_MAJOR;
                return;
            }

            double[] result = new double[cols];

            if (colmatrix) {
                for (int j = 0; j < cols; j++) {
                    SharedVector col = matrix.get(j);
                    result[j] = this.dot(col);
                }
            }else{
                for (int i=0; i<cols; i++){
                    double sum=0;
                    for (int j=0; j<rows; j++){
                        sum+=vector[j]*matrix.get(j).get(i);
                    }
                    result[i]=sum;
                }
            }
            vector = result;
            orientation = VectorOrientation.ROW_MAJOR;
        } finally {
            writeUnlock();
        }
    }


    // helper
    private void mismatchErr(){
        throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
    }
}