package fr.octoworld.sae.networktoolbox.ui.home;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.widget.CompoundButton;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import fr.octoworld.sae.networktoolbox.R;
import fr.octoworld.sae.networktoolbox.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private boolean isRunning = false;
    private boolean isTcp = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.textViewIpAddress.setText(getIpAddress());

        // Set button listeners
        binding.buttonStartServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServer();
            }
        });

        binding.buttonStopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopServer();
            }
        });

        binding.switchProtocol.setChecked(true);
        binding.switchProtocol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isTcp = isChecked;
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startServer() {
        // Get port number
        if (getIpAddress().equals("0.0.0.0")) {
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_no_wifi), Toast.LENGTH_SHORT).show();
            return;
        }
        if (binding.editTextPort.getText().toString().equals("")) {
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_invalid_port), Toast.LENGTH_SHORT).show();
            return;
        }
        int port = Integer.parseInt(binding.editTextPort.getText().toString());
        if (port < 1024 || port > 65535) {
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_invalid_port), Toast.LENGTH_SHORT).show();
            return;
        }

        String socketData = getIpAddress() + ":" + port;

        binding.textViewIpAddress.setText(socketData);
        // Create server socket
        try {
            if (isTcp) {
                if (isRunning) {
                    Toast.makeText(getContext(), requireContext().getString(R.string.alert_server_running), Toast.LENGTH_SHORT).show();
                    return;
                }
                isRunning = true;
                startTcpServer(port);
                Toast.makeText(getContext(), requireContext().getString(R.string.tcp_server_start_success), Toast.LENGTH_SHORT).show();
            } else {
                if (isRunning) {
                    Toast.makeText(getContext(), requireContext().getString(R.string.alert_server_running), Toast.LENGTH_SHORT).show();
                    return;
                }
                isRunning = true;
                startUdpServer(port);
                Toast.makeText(getContext(), requireContext().getString(R.string.udp_server_start_success), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    private void startTcpServer(int port) throws IOException {
        // Start a thread to listen for new clients
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(port);
                    while(isRunning) {
                        Socket client = serverSocket.accept();
                        BufferedReader inputBuf = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String messageTxt = inputBuf.readLine();
                        JSONObject JSONstr = new JSONObject(messageTxt);
                        final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");
                        OutputStreamWriter serverOutput = new OutputStreamWriter(client.getOutputStream());
                        BufferedWriter bufferedWriter = new BufferedWriter(serverOutput);
                        bufferedWriter.write(messageTxt);
                        bufferedWriter.flush();
                        String connectionInfo = getString(R.string.server_connected_to) + client.getInetAddress() + ":" + client.getPort();
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.textViewMessage.setText(data);
                                binding.textServerClient.setText(connectionInfo);
                            }
                        });
                        client.close();
                    }
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void startUdpServer(int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageTxt;
                try {
                    datagramSocket = new DatagramSocket(port);
                    while (isRunning) {
                        byte[] buf = new byte[1024];
                        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                        datagramSocket.receive(datagramPacket);
                        String connectionInfo = getString(R.string.server_connected_to) + datagramPacket.getAddress() + ":" + datagramPacket.getPort();
                        messageTxt = new String(datagramPacket.getData());
                        JSONObject JSONstr = new JSONObject(messageTxt);
                        final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");
                        datagramPacket.setData(messageTxt.getBytes());
                        datagramSocket.send(datagramPacket);

                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.textViewMessage.setText(data);
                                binding.textServerClient.setText(connectionInfo);
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void stopServer() {
        // Close the server socket
        if (isTcp) {
            try {
                serverSocket.close();
                Toast.makeText(getContext(), requireContext().getString(R.string.tcp_server_stop_success), Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                datagramSocket.close();
                Toast.makeText(getContext(), requireContext().getString(R.string.udp_server_stop_success), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getIpAddress() {
        WifiManager wifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.FRANCE, "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }
}