package com.dexels.proximitytracker.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.dexels.proximitytracker.ContactTracingManager;
import com.dexels.proximitytracker.R;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment {

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment, container, false);
        ((Switch) view.findViewById(R.id.toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    new ContactTracingManager(getContext()).startContactTracing(null).addOnCompleteListener(new OnCompleteListener<Integer>() {
                        @Override
                        public void onComplete(Task<Integer> task) {
                            System.out.println("done task: " + task);
                        }

                    });
                } else {
                    new ContactTracingManager(getContext()).stopContactTracing().addOnCompleteListener(new OnCompleteListener<Integer>() {
                        @Override
                        public void onComplete(Task<Integer> task) {
                            System.out.println("done task: " + task);
                        }

                    });
                }
            }
        });
        return view;
    }


}
