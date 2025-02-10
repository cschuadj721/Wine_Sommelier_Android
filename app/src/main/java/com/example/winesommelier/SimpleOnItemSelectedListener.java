package com.example.winesommelier;

import android.view.View;
import android.widget.AdapterView;

public abstract class SimpleOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        onItemSelected();
    }
    public abstract void onItemSelected();
}
