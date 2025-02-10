package com.example.winesommelier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RecommendationEngine {
    private List<SparseVector> tfidfMatrix;
    private List<Wine> wines; // Use Wine objects to get names, reviews, etc.

    // Modified constructor accepting a List of Wine objects
    public RecommendationEngine(List<SparseVector> tfidfMatrix, List<Wine> wines) {
        this.tfidfMatrix = tfidfMatrix;
        this.wines = wines;
    }

    // Compute cosine similarity between two sparse vectors.
    // You can also include this method inside SparseVector as a static helper.
    public static double cosineSimilarity(SparseVector a, SparseVector b) {
        double dot = 0.0;
        int i = 0, j = 0;
        while (i < a.indices.length && j < b.indices.length) {
            if (a.indices[i] == b.indices[j]) {
                dot += a.values[i] * b.values[j];
                i++;
                j++;
            } else if (a.indices[i] < b.indices[j]) {
                i++;
            } else {
                j++;
            }
        }
        if (a.norm == 0 || b.norm == 0) return 0.0;
        return dot / (a.norm * b.norm);
    }

    // Get top 'topN' recommendations (indices in the TFIDF matrix) for a reference index.
    public List<Integer> getRecommendations(int refIdx, int topN) {
        SparseVector refVector = tfidfMatrix.get(refIdx);
        List<Pair<Integer, Double>> simScores = new ArrayList<>();

        // Compute cosine similarity between the reference vector and all others.
        for (int i = 0; i < tfidfMatrix.size(); i++) {
            double sim = cosineSimilarity(refVector, tfidfMatrix.get(i));
            simScores.add(new Pair<>(i, sim));
        }

        // Sort by similarity in descending order.
        Collections.sort(simScores, new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return Double.compare(o2.second, o1.second);
            }
        });

        // Return top 'topN' indices.
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, simScores.size()); i++) {
            indices.add(simScores.get(i).first);
        }
        return indices;
    }
}
