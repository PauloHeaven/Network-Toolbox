package fr.octoworld.sae.networktoolbox.ui.date;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DateViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public DateViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is date fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}