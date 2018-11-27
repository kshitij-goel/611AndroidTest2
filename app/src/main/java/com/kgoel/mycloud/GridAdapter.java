package com.kgoel.mycloud;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class GridAdapter extends BaseAdapter {
    private final Context mContext;
    private final Devices[] devices;

    // 1
    public GridAdapter(Context context, Devices[] devices) {
        this.mContext = context;
        this.devices = devices;
    }

    // 2
    @Override
    public int getCount() { return devices.length; }

    // 3
    @Override
    public long getItemId(int position) {
        return 0;
    }

    // 4
    @Override
    public Object getItem(int position) {
        return null;
    }

    // 5
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Devices device = devices[position];

        // 2
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.linearlayout_devices, null);
        }

        // 3
        final ImageView imageView = (ImageView)convertView.findViewById(R.id.imageview_cover);
        final TextView nameTextView = (TextView)convertView.findViewById(R.id.textview_image);

        // 4
        imageView.setImageResource(device.getImageResource());
        nameTextView.setText(mContext.getText(device.getName()));

        return convertView;
    }
}
