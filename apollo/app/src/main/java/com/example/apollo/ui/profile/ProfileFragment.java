package com.example.apollo.ui.profile;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.apollo.R;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //making clickable
        View rowName = view.findViewById(R.id.rowName);
        View rowUsername = view.findViewById(R.id.rowUsername);
        View rowEmail = view.findViewById(R.id.rowEmail);
        View rowPhone = view.findViewById(R.id.rowPhone);

        //click visual
        rowName.setClickable(true);
        rowUsername.setClickable(true);
        rowEmail.setClickable(true);
        rowPhone.setClickable(true);
    }
}
