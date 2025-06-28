package com.example.evento;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class customer_interface1 extends AppCompatActivity {

    private FragmentManager fm;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_interface1);

        // Edge‑to‑edge padding (optional)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom);
            return insets;
        });

        fm = getSupportFragmentManager();
        bottomNav = findViewById(R.id.BottomNav);

        // Show all labels
        bottomNav.setLabelVisibilityMode(
                NavigationBarView.LABEL_VISIBILITY_LABELED
        );

        // Default load Home
        if (savedInstanceState == null) {
            loadFragment(new Homefragment());
        }

        // IMPORTANT: switch on item.getItemId(), NOT on the item itself!
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                loadFragment(new Homefragment());
            }
            else if (id == R.id.venue) {
                loadFragment(new Venue());
            }
            else if (id == R.id.caterer) {
                loadFragment(new caterer());
            }
            else if (id == R.id.decorator) {
                loadFragment(new decorator());
            }
            else if (id == R.id.events) {
                loadFragment(new Myevents());
            }
            else {
                return false;
            }

            return true;
        });


    }

    /** Swap the given fragment into the R.id.frame_layout container */
    private boolean loadFragment(Fragment frag) {
        if (frag == null) return false;
        FragmentTransaction tx = fm.beginTransaction();
        tx.replace(R.id.frame_layout, frag);
        tx.commit();
        return true;
    }
}
