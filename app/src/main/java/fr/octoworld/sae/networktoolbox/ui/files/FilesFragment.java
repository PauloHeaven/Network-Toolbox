package fr.octoworld.sae.networktoolbox.ui.files;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.octoworld.sae.networktoolbox.databinding.FragmentFilesBinding;

public class FilesFragment extends Fragment {

    private FragmentFilesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FilesViewModel filesViewModel =
                new ViewModelProvider(this).get(FilesViewModel.class);

        binding = FragmentFilesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textFiles;
        filesViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}