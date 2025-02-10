package com.example.winesommelier;

import java.util.List;

public class SparseVector {
    public int[] indices;
    public double[] values;
    public double norm;  // Precomputed norm for cosine similarity

    public SparseVector(List<Integer> idxList, List<Double> valueList) {
        int size = idxList.size();
        indices = new int[size];
        values = new double[size];
        double sumSq = 0.0;
        for (int i = 0; i < size; i++) {
            indices[i] = idxList.get(i);
            values[i] = valueList.get(i);
            sumSq += values[i] * values[i];
        }
        norm = Math.sqrt(sumSq);
    }
}
