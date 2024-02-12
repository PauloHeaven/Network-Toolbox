package fr.octoworld.sae.networktoolbox.ui.home;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

import fr.octoworld.sae.networktoolbox.R;
import fr.octoworld.sae.networktoolbox.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding; // Initialisation des variables et des vues
    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private boolean isRunning = false;
    private boolean isTcp = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.textViewIpAddress.setText(getIpAddress()); // Récupération de l'adresse IP de la carte Wi-Fi à chaque affichage de l'onglet

        // Set button listeners
        binding.buttonStartServer.setOnClickListener(view -> startServer()); // Écouteurs sur les boutons

        binding.buttonStopServer.setOnClickListener(view -> stopServer());

        binding.switchProtocol.setChecked(true); // TCP par défaut
        binding.switchProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> isTcp = isChecked); // Écouteur sur l'interrupteur

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startServer() {
        // Get port number
        if (getIpAddress().equals("0.0.0.0")) { // 0.0.0.0 est remonté par l'API si le téléphone n'est pas connecté à un réseau Wi-Fi.
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_no_wifi), Toast.LENGTH_SHORT).show(); // Dans ce cas, on ne démarre pas le serveur.
            return;
        }
        if (binding.editTextPort.getText().toString().equals("")) { // Vérification du champ de port vide, pour éviter un plantage
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_invalid_port), Toast.LENGTH_SHORT).show();
            return;
        }
        int port = Integer.parseInt(binding.editTextPort.getText().toString());
        if (port < 1024 || port > 65535) { // Si le champ n'est pas vide, on vérifie tout de meme la validité du port
            Toast.makeText(getContext(), requireContext().getString(R.string.alert_invalid_port), Toast.LENGTH_SHORT).show();
            return;
        }

        String socketData = getIpAddress() + ":" + port; // Si le serveur démarre, on ajoute le port à droite de l'adresse IP

        binding.textViewIpAddress.setText(socketData);
        // Create server socket
        try {
            if (isTcp) {
                if (isRunning) { // Si le serveur est déjà démarré, on sort de la fonction
                    Toast.makeText(getContext(), requireContext().getString(R.string.alert_server_running), Toast.LENGTH_SHORT).show();
                    return;
                }
                isRunning = true;
                startTcpServer(port); // Démarrage du serveur
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
        }
    }
    private void startTcpServer(int port) throws IOException { // Serveur TCP
        // Start a thread to listen for new clients
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port); // Socket serveur TCP
                while(isRunning) { // Du moment que le serveur fonctionne, on accepte les connexions et les traite
                    Socket client = serverSocket.accept(); // Acceptation automatique de nouveaux clients
                    BufferedReader inputBuf = new BufferedReader(new InputStreamReader(client.getInputStream())); // Lecture du flux entrant
                    String messageTxt = inputBuf.readLine();
                    JSONObject JSONstr = new JSONObject(messageTxt); // Conversion de la chaine de caractères reçue en objet JSON pour etre plus facilement utilisable
                    final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");
                    OutputStreamWriter serverOutput = new OutputStreamWriter(client.getOutputStream()); // Activation du générateur de flux sortant pour envoyer l'accusé de réception
                    BufferedWriter bufferedWriter = new BufferedWriter(serverOutput);
                    bufferedWriter.write(messageTxt); // Envoi de l'accusé
                    bufferedWriter.flush();
                    String connectionInfo = getString(R.string.server_connected_to) + client.getInetAddress() + ":" + client.getPort(); // Récupération des informations de connexion du client distant
                    requireActivity().runOnUiThread(() -> {
                        binding.textViewMessage.setText(data);
                        binding.textServerClient.setText(connectionInfo);
                    });
                    client.close();
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void startUdpServer(int port) { // Serveur UDP
        new Thread(() -> {
            String messageTxt;
            try {
                datagramSocket = new DatagramSocket(port); // Socket serveur UDP
                while (isRunning) {
                    byte[] buf = new byte[1024]; // Tableau de réception du message
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length); // Création de l'objet datagramme pour recevoir le contenu
                    datagramSocket.receive(datagramPacket); // Réception
                    String connectionInfo = getString(R.string.server_connected_to) + datagramPacket.getAddress() + ":" + datagramPacket.getPort();
                    messageTxt = new String(datagramPacket.getData());
                    JSONObject JSONstr = new JSONObject(messageTxt);
                    final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");
                    datagramPacket.setData(messageTxt.getBytes()); // Avec UDP, on peut conserver le meme datagramme pour poursuivre l'échange, et se contenter d'y injecter les nouvelles données. Cela permet de ne pas avoir à récupérer l'adresse, le port individuellement, un tableau et recréer un datagramme, si on répond à une connexion qui s'est déjà produite
                    datagramSocket.send(datagramPacket); // Envoi

                    requireActivity().runOnUiThread(() -> {
                        binding.textViewMessage.setText(data);
                        binding.textServerClient.setText(connectionInfo);
                    });
                }
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void stopServer() { // Arret du serveur
        // Close the server socket
        if (isTcp) {
            try {
                serverSocket.close();
                Toast.makeText(getContext(), requireContext().getString(R.string.tcp_server_stop_success), Toast.LENGTH_SHORT).show(); // Notification

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

    private String getIpAddress() { // Récupération et formatage de l'adresse IP
        WifiManager wifiManager = (WifiManager) requireActivity().getSystemService(Context.WIFI_SERVICE); // Objet exploitant une API système pour récupérer les informations de l'interface réseau
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.FRANCE, "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), // Conversion binaire en décimal des octets
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }
}