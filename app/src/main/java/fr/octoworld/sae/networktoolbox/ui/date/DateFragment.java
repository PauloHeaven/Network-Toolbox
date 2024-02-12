package fr.octoworld.sae.networktoolbox.ui.date;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Time;
import java.util.Objects;

import fr.octoworld.sae.networktoolbox.MainActivity;
import fr.octoworld.sae.networktoolbox.R;
import fr.octoworld.sae.networktoolbox.databinding.FragmentDateBinding;

public class DateFragment extends Fragment {

    private FragmentDateListener listener;
    private FragmentDateBinding binding;
    private String serverName = "time.nist.gov";
    private int serverPort = 13;

    public interface FragmentDateListener {
        void onInputDateSent(CharSequence input);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentDateBinding.inflate(inflater, container, false);

        binding.setOffset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                onClickSetOffset(view);
            }
        });

        binding.buttonGettime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickGetTime(view);
            }
        });

        binding.setOffsetSign.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onClickSetOffsetOperator(buttonView);
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private TextView time, date;
    private EditText offset;
    private int offsetValue = 0;
    private SwitchCompat sign;
    private String signValue = "-";

     public void onClickGetTime(View view){
        time = binding.textTime;
        date = binding.textDate;
        String serverName = "time.nist.gov";
        int serverPort = 13;
        DisplayTime runnable = new DisplayTime(serverName, serverPort, this.offsetValue);
        new Thread(runnable).start();
    }

    public void onClickSetOffsetOperator(View view){
        sign = binding.setOffsetSign;
        if (sign.isChecked()){
            this.signValue = "+";
            sign.setText("+");
        } else {
            this.signValue = "-";
            sign.setText("-");
        }
    }

    public void onClickSetOffset(View view){
        time = binding.textTime;
        date = binding.textDate;
        offset = binding.offset;
        this.offsetValue = Integer.parseInt(offset.getText().toString());
        if (this.signValue.equals("-")) {
            this.offsetValue = -this.offsetValue;
        }
        String serverName = "time.nist.gov";
        int serverPort = 13;
        DisplayTime runnable = new DisplayTime(serverName, serverPort, this.offsetValue);
        new Thread(runnable).start();
        Toast.makeText(getContext(), getString(R.string.date_offset_saved), Toast.LENGTH_SHORT).show();
    }

    private class DisplayTime implements Runnable{

        private String serverName;
        private int serverPort;
        private int offset;
        private String data;
        private String recTime;
        private String recDate;
        private String[] recDateUS;


        public DisplayTime(String serverName, int serverPort, int offset) {
            this.serverName = serverName;
            this.serverPort = serverPort;
            this.offset = offset;
        }

        public void setOffset(int offset){
            int recHour = Integer.parseInt(this.recTime.split(":")[0]);
            if (recHour + offset >= 24){
                recHour += offset;
                recHour -= 24;
            } else if (recHour + offset < 0) {
                recHour += offset;
                recHour += 24;
            } else {
                recHour += offset;
            }
            this.recTime = recHour + ":" + this.recTime.split(":", 2)[1];
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(serverName, serverPort);
                BufferedReader buf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                buf.readLine();
                this.data = buf.readLine();
                this.recTime = data.substring(15,23);
                this.recDateUS = data.substring(6, 14).split("-");
                this.recDate = this.recDateUS[2] + "/" + this.recDateUS[1] + "/20" + this.recDateUS[0];
                socket.close();
                if (this.offset != 0) {
                    setOffset(this.offset);
                }

                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        time.setText(recTime);
                        date.setText(recDate);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}