package io.github.donnyyu.cleandash;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/** Icon and name rows for the app drawer grid and the "choose an app" list. */
final class AppAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final int layout;
    private final List<AppList.App> apps;

    AppAdapter(Context context, int layout, List<AppList.App> apps) {
        this.inflater = LayoutInflater.from(context);
        this.layout = layout;
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public AppList.App getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) view = inflater.inflate(layout, parent, false);
        AppList.App app = apps.get(position);
        ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(app.icon);
        ((TextView) view.findViewById(R.id.app_label)).setText(app.label);
        return view;
    }
}
