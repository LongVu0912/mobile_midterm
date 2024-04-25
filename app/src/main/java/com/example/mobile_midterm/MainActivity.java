package com.example.mobile_midterm;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mobile_midterm.adapters.CollectionAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager2 viewPager2 = findViewById(R.id.pager);

        CollectionAdapter adapter = new CollectionAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager2.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            String text = "";
            if (position == 0) text = "DOWNLOAD";
            else if (position == 1) text = "VIEW";
            else if (position == 2) text = "UPLOAD";
            tab.setText(text);
        }).attach();
    }
}