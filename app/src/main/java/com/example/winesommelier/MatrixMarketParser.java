package com.example.winesommelier;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatrixMarketParser {

    public static List<SparseVector> loadSparseMatrix(Context context, String fileName) {
        List<SparseVector> matrix = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;

            // Skip header lines starting with '%'
            while ((line = reader.readLine()) != null && line.startsWith("%")) {}

            if (line == null) {
                reader.close();
                return null;
            }

            // Read dimensions: rows, columns, and nonZeros
            String[] dims = line.trim().split("\\s+");
            int rows = Integer.parseInt(dims[0]);
            // int cols = Integer.parseInt(dims[1]); // can be used if needed
            int nonZeros = Integer.parseInt(dims[2]);

            // Create maps to store nonzero entries for each row
            Map<Integer, List<Integer>> rowIndices = new HashMap<>();
            Map<Integer, List<Double>> rowValues = new HashMap<>();

            for (int i = 0; i < nonZeros; i++) {
                line = reader.readLine();
                if (line == null || line.trim().isEmpty()) continue;
                String[] tokens = line.trim().split("\\s+");
                int row = Integer.parseInt(tokens[0]) - 1; // Convert to 0-indexed
                int col = Integer.parseInt(tokens[1]) - 1;
                double value = Double.parseDouble(tokens[2]);

                if (!rowIndices.containsKey(row)) {
                    rowIndices.put(row, new ArrayList<>());
                    rowValues.put(row, new ArrayList<>());
                }
                rowIndices.get(row).add(col);
                rowValues.get(row).add(value);
            }
            reader.close();

            // Build the matrix as a list of SparseVector objects
            matrix = new ArrayList<>(rows);
            for (int r = 0; r < rows; r++) {
                if (rowIndices.containsKey(r)) {
                    matrix.add(new SparseVector(rowIndices.get(r), rowValues.get(r)));
                } else {
                    // For rows with no nonzero entries, add an empty sparse vector
                    matrix.add(new SparseVector(new ArrayList<>(), new ArrayList<>()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matrix;
    }
}
