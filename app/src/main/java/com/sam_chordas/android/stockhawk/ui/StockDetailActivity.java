package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.sam_chordas.android.stockhawk.R;

public class StockDetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new StockDetailActivityFragment())
                    .commit();
        }    }

}
