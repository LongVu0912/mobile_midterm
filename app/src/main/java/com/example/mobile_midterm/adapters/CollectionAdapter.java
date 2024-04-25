package com.example.mobile_midterm.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.mobile_midterm.fragments.DownloadFragment;
import com.example.mobile_midterm.fragments.UploadFragment;
import com.example.mobile_midterm.fragments.ViewFragment;

public class CollectionAdapter extends FragmentStateAdapter {
    public CollectionAdapter(FragmentManager fragmentManager, Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new DownloadFragment();
                break;
            case 1:
                fragment = new ViewFragment();
                break;
            case 2:
                fragment = new UploadFragment();
                break;
            default:
                fragment = new DownloadFragment();
                break;
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 3;
    }

}
