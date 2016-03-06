package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailActivityFragment extends Fragment {
    public final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    public final static DateFormat dateFormatForView = new SimpleDateFormat("MM/dd");

    public final Map<String, Data> dataMap = new HashMap<String, Data>();
    private GraphView graph;
    DataStats dataStats = new DataStats();
    LineChartView lineChartView;

    public StockDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Intent intent  = getActivity().getIntent();
        if(intent!= null){
            String stockID = intent.getStringExtra(getString(R.string.symbol));
            if(stockID != null && stockID.trim().length() > 0){
                new FetchStockInfo().execute(stockID);
            }
        }


        RelativeLayout rootView = (RelativeLayout)inflater.inflate(R.layout.fragment_stock_detail, container, false);
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.list_item_quote_view);
        initializeStockInfoView(layout);

        graph = (GraphView) rootView.findViewById(R.id.stock_graph);
        return rootView;
    }

    public void initializeStockInfoView(LinearLayout linearLayout){
        Intent intent = getActivity().getIntent();
        String symbol = intent.getStringExtra(getString(R.string.symbol));
        String price = intent.getStringExtra(getString(R.string.price));
        boolean isUp = intent.getBooleanExtra(getString(R.string.isUp), false);
        String change =intent.getStringExtra(getString(R.string.change));;

        linearLayout.setBackgroundColor(Color.LTGRAY);
        ((TextView) linearLayout.findViewById(R.id.stock_symbol)).setText(symbol);
        ((TextView)linearLayout.findViewById(R.id.bid_price)).setText(price);
        TextView changeView = ((TextView)linearLayout.findViewById(R.id.change));
        changeView.setText(change);
        int sdk = Build.VERSION.SDK_INT;
        if(isUp) {
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                changeView.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.percent_change_pill_green));
            } else {
                changeView.setBackground(
                        getResources().getDrawable(R.drawable.percent_change_pill_green));
            }
        }else{
            if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
                changeView.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.percent_change_pill_red));
            } else {
                changeView.setBackground(
                        getResources().getDrawable(R.drawable.percent_change_pill_red));
            }
        }
    }

    public void initView2(){
        if(true)
            return;
        Data[] dataPoints = new Data[dataMap.size()];
        dataMap.values().toArray(dataPoints);
        LineSet set = new LineSet();
        for(Data d : dataPoints){
            set.addPoint(new Point(dateFormatForView.format(d.date), (float) d.price));
        }
        lineChartView.addData(set);
        lineChartView.show();
    }

    public void initView(){
        Data[] dataPoints = new Data[dataMap.size()];
        dataMap.values().toArray(dataPoints);
        Arrays.sort(dataPoints);

        LineGraphSeries<Data> series = new LineGraphSeries<Data>(dataPoints);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    Data data = dataMap.get(((int) value) + "");
                    if (data == null) {
                        return null;
                    } else {
                        return dateFormatForView.format(data.date);
                    }
                } else {
                    return "$" + super.formatLabel(value, isValueX);
                }
            }
        });
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getActivity(), ""+dataPoint.getY(), Toast.LENGTH_SHORT).show();
            }
        });
        graph.addSeries(series);
        graph.getViewport().setScalable(true);
        graph.getViewport().setMinX(dataStats.minX);
        graph.getViewport().setMaxX(dataStats.maxX);

    }
    private class FetchStockInfo  extends AsyncTask<String, Void, Boolean> {
        private OkHttpClient client = new OkHttpClient();

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String stockID = params[0];
                String jsonRespone = fetchJsonData(stockID);
                JSONObject jsonObject = new JSONObject(jsonRespone).getJSONObject("query")
                        .getJSONObject("results");

                JSONArray array = jsonObject.getJSONArray("quote");
                int length = array.length();
                for(int i=0; i<length; i++){
                    JSONObject stockDayData = array.getJSONObject(i);
                    Date date = dateFormat.parse(stockDayData.getString("Date"));
                    double price = Double.parseDouble(stockDayData.getString("Close"));
                    if(i>dataStats.maxX){
                        dataStats.maxX = i;
                    }
                    if(i<dataStats.minX){
                        dataStats.minX = i;
                    }

                    if(price>dataStats.maxY){
                        dataStats.maxY = price;
                    }
                    if(price<dataStats.minY){
                        dataStats.minY = price;
                    }
                    int id = length-1-i;

                    dataMap.put(id+"", new Data(id, date, price));
                }
                return true;
            }catch (Exception e){
                Log.e("Error", e.getMessage(), e);
            }
            return false;
        }


        private String fetchJsonData(String stockID) throws IOException {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            String endDate = dateFormat.format(cal.getTime());  //Yesterdays date is the end date
            cal.add(Calendar.DATE, -30);
            String startDate = dateFormat.format(cal.getTime());  //About one month of data
            String query = "select * from yahoo.finance.historicaldata where symbol = \""
                    +stockID+"\" and startDate = \""
                    +startDate+"\" and endDate = \""+endDate+"\"";
            StringBuilder urlStringBuilder = new StringBuilder();
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
            Log.e("To Run", urlStringBuilder.toString());

            Request request = new Request.Builder()
                    .url(urlStringBuilder.toString())
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }


        @Override
        protected void onPostExecute(Boolean changed){
            if(changed){
                initView();
                initView2();
            }
        }

    }

    private class DataStats{
        int minX=Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        double minY=Double.MIN_VALUE, maxY= Integer.MAX_VALUE;
    }

    private class Data implements DataPointInterface, Comparable<Data> {
        public final double id;
        public final Date date;
        public final double price;

        public Data(double id, Date date, double price){
            this.id = id;
            this.date = date;
            this.price = price;
        }

        @Override
        public double getX() {
            return id;
        }

        @Override
        public double getY() {
            return price;
        }


        @Override
        public int compareTo(Data another) {
            return this.date.compareTo(another.date);
        }
    }
}
