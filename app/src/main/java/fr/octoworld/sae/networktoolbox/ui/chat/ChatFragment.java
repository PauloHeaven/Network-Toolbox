package fr.octoworld.sae.networktoolbox.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import fr.octoworld.sae.networktoolbox.R;
import fr.octoworld.sae.networktoolbox.databinding.FragmentChatBinding;

public class ChatFragment extends Fragment {

    private boolean isTcp = true;
    private String ipAddress;
    private int port;
    private String message;
    private String author;
    private FragmentChatBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);

        binding.switchProtocol.setChecked(true);
        binding.switchProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> isTcp = isChecked);

        binding.buttonSendMessage.setOnClickListener(this::onSendMessage);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onSendMessage(View view) {
        // Get the IP address, port, and message
        ipAddress = binding.editTextIpAddress.getText().toString();
        port = Integer.parseInt(binding.editTextPort.getText().toString());
        message = binding.editTextMessage.getText().toString();
        author = binding.chatAuthor.getText().toString();
            if (isTcp) {
                sendTcpMessage();
            } else {
                sendUdpMessage();
            }
    }

    private void sendTcpMessage() {
        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port)) {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                if (author.equals("")) {
                    message = JSONConvert(message).toString();
                } else {
                    message = JSONConvert(message, author).toString();
                }
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                BufferedReader inputBuf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String messageTxt = inputBuf.readLine();
                JSONObject JSONstr = new JSONObject(messageTxt);
                final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");

                requireActivity().runOnUiThread(() -> binding.textViewResponse.setText(data));

            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void sendUdpMessage() {
        new Thread(() -> {
            try {
                if (author.equals("")) {
                    message = JSONConvert(message).toString();
                } else {
                    message = JSONConvert(message, author).toString();
                }
                DatagramSocket datagramSocket = new DatagramSocket();
                DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(ipAddress), port);
                datagramSocket.send(datagramPacket);
                byte[] responseBytes = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length);
                datagramSocket.receive(responsePacket);

                String messageTxt = new String(responseBytes, 0, responsePacket.getLength());
                JSONObject JSONstr = new JSONObject(messageTxt);
                final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");

                requireActivity().runOnUiThread(() -> binding.textViewResponse.setText(data));
                datagramSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private JSONObject JSONConvert(String message, String author) throws JSONException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);
        JSONObject JSONstr = new JSONObject();
        JSONstr.put("content", message);
        JSONstr.put("date", date);
        JSONstr.put("author", author);

        return JSONstr;
    }

    private JSONObject JSONConvert(String message) throws JSONException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
        LocalDateTime now = LocalDateTime.now();
        String date = dtf.format(now);
        JSONObject JSONstr = new JSONObject();
        JSONstr.put("content", message);
        JSONstr.put("date", date);
        JSONstr.put("author", "default");

        return JSONstr;
    }
}