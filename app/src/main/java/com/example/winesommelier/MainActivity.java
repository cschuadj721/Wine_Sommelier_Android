package com.example.winesommelier;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
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

    // Adapters for auto-complete
    ArrayAdapter<String> autoCompleteAdapter;

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
                // Populate the filter spinners using the recommended wines and static ranges
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

    // Populate the filter spinners with range options for price, alcohol, and rating,
    // and unique values for winery and category.
    private void populateFilterSpinners() {
        // --- Price range options ---
        List<String> priceList = new ArrayList<>();
        priceList.add("All");
        priceList.add("$0 - $10");
        priceList.add("$10 - $20");
        priceList.add("$20 - $30");
        priceList.add("$30 - $40");
        priceList.add("$40+");
        spinnerPrice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priceList));

        // --- Alcohol range options ---
        List<String> alcoholList = new ArrayList<>();
        alcoholList.add("All");
        alcoholList.add("Below 12%");
        alcoholList.add("12% - 13%");
        alcoholList.add("13% - 14%");
        alcoholList.add("Above 14%");
        spinnerAlcohol.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alcoholList));

        // --- Rating range options ---
        List<String> ratingList = new ArrayList<>();
        ratingList.add("All");
        ratingList.add(">= 5");
        ratingList.add(">= 6");
        ratingList.add(">= 7");
        ratingList.add(">= 8");
        ratingList.add(">= 9");
        spinnerRating.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ratingList));

        // --- Winery: Unique values ---
        Set<String> winerySet = new HashSet<>();
        for (Wine w : recommendedWines) {
            winerySet.add(w.winery);
        }
        List<String> wineryList = new ArrayList<>();
        wineryList.add("All");
        wineryList.addAll(winerySet);
        spinnerWinery.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wineryList));

        // --- Category: Unique values ---
        Set<String> categorySet = new HashSet<>();
        for (Wine w : recommendedWines) {
            categorySet.add(w.category);
        }
        List<String> categoryList = new ArrayList<>();
        categoryList.add("All");
        categoryList.addAll(categorySet);
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
    // The filtering logic uses numeric comparisons for price, alcohol, and rating.
    private void updateFilteredResults() {
        if (recommendedWines == null) return;
        String selPrice = spinnerPrice.getSelectedItem().toString();
        String selAlcohol = spinnerAlcohol.getSelectedItem().toString();
        String selRating = spinnerRating.getSelectedItem().toString();
        String selWinery = spinnerWinery.getSelectedItem().toString();
        String selCategory = spinnerCategory.getSelectedItem().toString();

        Log.d("MainActivity", "Filtering with - Price: " + selPrice + ", Alcohol: " + selAlcohol +
                ", Rating: " + selRating + ", Winery: " + selWinery + ", Category: " + selCategory);

        List<Wine> filtered = new ArrayList<>();
        for (Wine w : recommendedWines) {
            // --- Price filtering ---
            if (!selPrice.equals("All")) {
                try {
                    double priceValue = Double.parseDouble(w.price.replaceAll("[$]", ""));
                    if (selPrice.equals("$0 - $10") && !(priceValue >= 0 && priceValue < 10)) continue;
                    else if (selPrice.equals("$10 - $20") && !(priceValue >= 10 && priceValue < 20)) continue;
                    else if (selPrice.equals("$20 - $30") && !(priceValue >= 20 && priceValue < 30)) continue;
                    else if (selPrice.equals("$30 - $40") && !(priceValue >= 30 && priceValue < 40)) continue;
                    else if (selPrice.equals("$40+") && !(priceValue >= 40)) continue;
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // --- Alcohol filtering ---
            if (!selAlcohol.equals("All")) {
                try {
                    double alcValue = Double.parseDouble(w.alcohol.replaceAll("[%]", ""));
                    if (selAlcohol.equals("Below 12%") && !(alcValue < 12)) continue;
                    else if (selAlcohol.equals("12% - 13%") && !(alcValue >= 12 && alcValue < 13)) continue;
                    else if (selAlcohol.equals("13% - 14%") && !(alcValue >= 13 && alcValue < 14)) continue;
                    else if (selAlcohol.equals("Above 14%") && !(alcValue >= 14)) continue;
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // --- Rating filtering ---
            if (!selRating.equals("All")) {
                try {
                    double rateValue = Double.parseDouble(w.rating);
                    if (selRating.equals(">= 5") && !(rateValue >= 5)) continue;
                    else if (selRating.equals(">= 6") && !(rateValue >= 6)) continue;
                    else if (selRating.equals(">= 7") && !(rateValue >= 7)) continue;
                    else if (selRating.equals(">= 8") && !(rateValue >= 8)) continue;
                    else if (selRating.equals(">= 9") && !(rateValue >= 9)) continue;
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // --- Winery filtering (exact match) ---
            if (!selWinery.equals("All") && !w.winery.equals(selWinery)) continue;

            // --- Category filtering (exact match) ---
            if (!selCategory.equals("All") && !w.category.equals(selCategory)) continue;

            filtered.add(w);
        }

        Log.d("MainActivity", "Filtered list size: " + filtered.size());
        updateListView(filtered);
    }

    // Update the ListView with the given list of wines, showing at most 10 items.
    private void updateListView(List<Wine> wines) {
        int maxItems = 10;
        int sizeToDisplay = Math.min(wines.size(), maxItems);
        List<Wine> truncatedWines = new ArrayList<>(wines.subList(0, sizeToDisplay));
        Log.d("MainActivity", "Updating ListView with " + truncatedWines.size() +
                " wines (truncated from " + wines.size() + ").");

        // Use the custom adapter to inflate each wine as a CardView.
        WineAdapter adapter = new WineAdapter(this, truncatedWines);
        recommendationListView.setAdapter(adapter);
    }

    // Custom adapter class that inflates wine_item.xml for each wine.
    public class WineAdapter extends ArrayAdapter<Wine> {

        public WineAdapter(Context context, List<Wine> wines) {
            super(context, 0, wines);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null){
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.wine_item, parent, false);
            }
            Wine wine = getItem(position);
            TextView tvWineName = convertView.findViewById(R.id.tvWineName);
            TextView tvWinery = convertView.findViewById(R.id.tvWinery);
            TextView tvPrice = convertView.findViewById(R.id.tvPrice);
            TextView tvAlcohol = convertView.findViewById(R.id.tvAlcohol);
            TextView tvRating = convertView.findViewById(R.id.tvRating);
            TextView tvReview = convertView.findViewById(R.id.tvReview);

            tvWineName.setText("Wine: " + wine.name);
            tvWinery.setText("Winery: " + wine.winery);
            tvPrice.setText("Price: " + wine.price);
            tvAlcohol.setText("Alcohol: " + wine.alcohol);
            tvRating.setText("Rating: " + wine.rating);
            tvReview.setText("Review: " + wine.review);
            return convertView;
        }
    }
}
