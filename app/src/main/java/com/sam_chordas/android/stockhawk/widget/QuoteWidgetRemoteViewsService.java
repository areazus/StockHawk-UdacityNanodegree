package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Created by ahmed on 3/6/2016.
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new QuoteWidgetRemoteViewsFactory(getApplicationContext(), intent);
    }
    public static class QuoteWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory, Loader.OnLoadCompleteListener<Cursor> {
        private Context context;
        private int appWidgetId;
        private CursorLoader loader;
        private Cursor cursor;

        public QuoteWidgetRemoteViewsFactory(Context context, Intent intent){
            this.context = context;
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

        }


        public void initLoader(){
            loader = new CursorLoader(context, QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);;
            loader.registerListener(appWidgetId, this);
            loader.startLoading();
        }

        @Override
        public void onCreate() {
            initLoader();
        }

        public static void updateWidgets(Context context){
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, QuoteWidgetProvider.class);
            int[] widgetIds = widgetManager.getAppWidgetIds(component);
            widgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_list);
        }

        @Override
        public void onDataSetChanged() {

        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            if(cursor != null){
                return cursor.getCount();
            }else{
                return 0;
            }
        }

        @Override
        public RemoteViews getViewAt(int position) {
            cursor.moveToPosition(position);
            String symbol = cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL));
            String price = cursor.getString(cursor.getColumnIndex(context.getString(R.string.price)));
            boolean isUp = cursor.getInt(cursor.getColumnIndex(context.getString(R.string.isUp)))==1?true:false;
            String change;
            if (Utils.showPercent){
                change = cursor.getString(cursor.getColumnIndex(context.getString(R.string.percent_change)));
            } else{
                change = cursor.getString(cursor.getColumnIndex(context.getString(R.string.change)));
            }

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_collection_item);
            rv.setTextViewText(R.id.stock_symbol, symbol);
            rv.setTextViewText(R.id.change, change);
            if(isUp){
                rv.setInt(R.id.change, "setBackgroundColor",
                        Color.GREEN);
            }else{
                rv.setInt(R.id.change, "setBackgroundColor",
                        Color.RED);
            }

            Bundle extras = new Bundle();
            extras.putInt(QuoteWidgetProvider.EXTRA_ITEM, position);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            // Make it possible to distinguish the individual on-click
            // action of a given item
            rv.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return getCount();
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            this.cursor = data;
            updateWidgets(context);
            Log.v("Cursor", "Cursor loaded with "+data.getCount()+" items");
        }

    }
}
