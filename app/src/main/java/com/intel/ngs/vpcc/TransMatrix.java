package com.intel.ngs.vpcc;

public class TransMatrix {
    private double[][] matrix ={{0.9,0.1},{0.6,0.4}};
    public double[] TransMatrix(int i){
        if(i == 0)
            return matrix[0];
        else
            return matrix[1];
    }
}
