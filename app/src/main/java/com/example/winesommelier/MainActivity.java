package com.example.winesommelier;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // UI elements
    AutoCompleteTextView wineAutoComplete;
    Button btnRecommend;
    ListView recommendationListView;
    Spinner spinnerPrice, spinnerAlcohol, spinnerRating, spinnerWinery, spinnerCategory;

    // Adapters for auto-complete and list view
    ArrayAdapter<String> autoCompleteAdapter;
    ArrayAdapter<String> recommendationAdapter;

    // Data storage
    List<Wine> allWines;           // All wines loaded from CSV
    List<Wine> recommendedWines;   // Top 100 recommended wines
    List<SparseVector> tfidfMatrix; // Loaded TFIDF matrix (sparse representation)
    RecommendationEngine engine;   // Engine to compute cosine similarity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        wineAutoComplete = findViewById(R.id.wineAutoComplete);
        btnRecommend = findViewById(R.id.btnRecommend);
        recommendationListView = findViewById(R.id.recommendationListView);
        spinnerPrice = findViewById(R.id.spinnerPrice);
        spinnerAlcohol = findViewById(R.id.spinnerAlcohol);
        spinnerRating = findViewById(R.id.spinnerRating);
        spinnerWinery = findViewById(R.id.spinnerWinery);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // Load wine data from CSV (in assets folder)
        allWines = loadWineData();

        // Setup auto-complete for wine names
        List<String> wineNames = new ArrayList<>();
        for (Wine w : allWines) {
            wineNames.add(w.name);
        }
        autoCompleteAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, wineNames);
        wineAutoComplete.setAdapter(autoCompleteAdapter);

        // Load TFIDF matrix and initialize the recommendation engine on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                tfidfMatrix = MatrixMarketParser.loadSparseMatrix(MainActivity.this, "wine_review_TFIDF.mtx");
                // Now the engine constructor takes a List<Wine> instead of separate lists
                engine = new RecommendationEngine(tfidfMatrix, allWines);
                Log.d("MainActivity", "TFIDF matrix and recommendation engine loaded.");
            }
        }).start();

        // When the user taps the button, compute the top 100 recommendations
        btnRecommend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String selectedWine = wineAutoComplete.getText().toString().trim();
                if (selectedWine.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter a wine name", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Find the index of the selected wine from allWines (ignoring case)
                int refIdx = -1;
                for (int i = 0; i < allWines.size(); i++) {
                    if (allWines.get(i).name.equalsIgnoreCase(selectedWine)) {
                        refIdx = i;
                        break;
                    }
                }
                if (refIdx == -1) {
                    Toast.makeText(MainActivity.this, "Wine not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (engine == null) {
                    Toast.makeText(MainActivity.this, "Engine is still loading. Please try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Get top 101 indices (assuming the first is the input wine itself)
                List<Integer> indices = engine.getRecommendations(refIdx, 101);
                Log.d("MainActivity", "Indices returned: " + indices.size());
                // Build the recommended wines list, excluding the input wine (index 0)
                recommendedWines = new ArrayList<>();
                for (int i = 1; i < indices.size(); i++) {
                    int idx = indices.get(i);
                    recommendedWines.add(allWines.get(idx));
                }
                // Populate the filter spinners using the recommended wines
                populateFilterSpinners();

                // Display only the top 10 wines on initial recommendation
                List<Wine> top10Wines = new ArrayList<>();
                int limit = Math.min(10, recommendedWines.size());
                for (int i = 0; i < limit; i++) {
                    top10Wines.add(recommendedWines.get(i));
                }
                updateListView(top10Wines);
            }
        });


    }

    // Loads wine data from CSV (assumes the file "preprocessed_reviews.csv" is in assets)
    private List<Wine> loadWineData() {
        List<Wine> wines = new ArrayList<>();
        try {
            InputStream is = getAssets().open("preprocessed_reviews.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {  // Skip header line
                    firstLine = false;
                    continue;
                }
                // Use a regex split that ignores commas inside quotes
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (tokens.length < 12) continue; // Ensure all columns exist
                Wine wine = new Wine(
                        tokens[0].trim(),  // wine name
                        tokens[1].trim(),  // winery
                        tokens[2].trim(),  // category
                        tokens[3].trim(),  // designation
                        tokens[4].trim(),  // varietal
                        tokens[5].trim(),  // appellation
                        tokens[6].trim(),  // alcohol
                        tokens[7].trim(),  // price
                        tokens[8].trim(),  // rating
                        tokens[9].trim(),  // reviewer
                        tokens[10].trim(), // review
                        tokens[11].trim()  // global_review_count
                );
                wines.add(wine);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wines;
    }

    // Populate the five filter spinners with unique values from recommendedWines
    private void populateFilterSpinners() {
        Set<String> prices = new HashSet<>();
        Set<String> alcohols = new HashSet<>();
        Set<String> ratings = new HashSet<>();
        Set<String> wineries = new HashSet<>();
        Set<String> categories = new HashSet<>();
        for (Wine w : recommendedWines) {
            prices.add(w.price);
            alcohols.add(w.alcohol);
            ratings.add(w.rating);
            wineries.add(w.winery);
            categories.add(w.category);
        }
        List<String> priceList = new ArrayList<>();
        priceList.add("All");
        priceList.addAll(prices);
        List<String> alcoholList = new ArrayList<>();
        alcoholList.add("All");
        alcoholList.addAll(alcohols);
        List<String> ratingList = new ArrayList<>();
        ratingList.add("All");
        ratingList.addAll(ratings);
        List<String> wineryList = new ArrayList<>();
        wineryList.add("All");
        wineryList.addAll(wineries);
        List<String> categoryList = new ArrayList<>();
        categoryList.add("All");
        categoryList.addAll(categories);

        // Create and set adapters for each spinner
        spinnerPrice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priceList));
        spinnerAlcohol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alcoholList));
        spinnerRating.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ratingList));
        spinnerWinery.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wineryList));
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList));

        // Set listeners so that any spinner selection change triggers filtering.
        spinnerPrice.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected() { updateFilteredResults(); }
        });
        spinnerAlcohol.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected() { updateFilteredResults(); }
        });
        spinnerRating.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected() { updateFilteredResults(); }
        });
        spinnerWinery.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected() { updateFilteredResults(); }
        });
        spinnerCategory.setOnItemSelectedListener(new SimpleOnItemSelectedListener() {
            @Override
            public void onItemSelected() { updateFilteredResults(); }
        });
    }

    // Filters the recommendedWines based on spinner selections and updates the ListView.
    private void updateFilteredResults() {
        if (recommendedWines == null) return;
        String selPrice = spinnerPrice.getSelectedItem().toString();
        String selAlcohol = spinnerAlcohol.getSelectedItem().toString();
        String selRating = spinnerRating.getSelectedItem().toString();
        String selWinery = spinnerWinery.getSelectedItem().toString();
        String selCategory = spinnerCategory.getSelectedItem().toString();

        List<Wine> filtered = new ArrayList<>();
        for (Wine w : recommendedWines) {
            if ((selPrice.equals("All") || w.price.equals(selPrice)) &&
                    (selAlcohol.equals("All") || w.alcohol.equals(selAlcohol)) &&
                    (selRating.equals("All") || w.rating.equals(selRating)) &&
                    (selWinery.equals("All") || w.winery.equals(selWinery)) &&
                    (selCategory.equals("All") || w.category.equals(selCategory))) {
                filtered.add(w);
            }
        }
        updateListView(filtered);
    }

    // Update the ListView with the given list of wines.
    private void updateListView(List<Wine> wines) {
        List<String> display = new ArrayList<>();
        for (Wine w : wines) {
            display.add("Wine: " + w.name + "\nWinery: " + w.winery +
                    "\nPrice: " + w.price + "\nAlcohol: " + w.alcohol +
                    "\nRating: " + w.rating + "\nReview: " + w.review);
        }
        recommendationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, display);
        recommendationListView.setAdapter(recommendationAdapter);
    }
}
