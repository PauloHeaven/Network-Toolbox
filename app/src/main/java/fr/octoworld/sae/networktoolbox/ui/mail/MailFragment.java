package fr.octoworld.sae.networktoolbox.ui.mail;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.octoworld.sae.networktoolbox.databinding.FragmentMailBinding;

public class MailFragment extends Fragment {

    private FragmentMailBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MailViewModel galleryViewModel =
                new ViewModelProvider(this).get(MailViewModel.class);

        binding = FragmentMailBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}