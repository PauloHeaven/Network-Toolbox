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
        binding = FragmentChatBinding.inflate(inflater, container, false); // Génération du lien vers les éléments graphiques

        binding.switchProtocol.setChecked(true); // Utilisation de TCP par défaut
        binding.switchProtocol.setOnCheckedChangeListener((buttonView, isChecked) -> isTcp = isChecked); // Écouteur sur le changement de position de l'interrupteur. isChecked est un booléen généré automatiquement par les interrupteurs, qui contient sa position

        binding.buttonSendMessage.setOnClickListener(this::onSendMessage); // Écouteur du bouton d'envoi

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void onSendMessage(View view) { // Récupère l'adresse IP et le contenu des champs de texte, et dirige vers la bonne fonction selon le protocole
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

    private void sendTcpMessage() { // Message TCP
        new Thread(() -> { // Thread
            try (Socket socket = new Socket(ipAddress, port)) { // Socket client TCP
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream()); // Objet d'écriture sur le sens émission du socket, qui prend en argument ce canal
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter); // Objet cache qui stocke ce qu'il va encoyer au streamWriter
                if (author.equals("")) { // Détection de présence de l'auteur
                    message = JSONConvert(message).toString();
                } else {
                    message = JSONConvert(message, author).toString();
                }
                bufferedWriter.write(message); // Écriture des informations sur le flux
                bufferedWriter.newLine(); // Insertion d'un retour chariot
                bufferedWriter.flush(); // Envoi et effacement du cache

                BufferedReader inputBuf = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Meme processus, mais pour le flux de réception
                String messageTxt = inputBuf.readLine(); // Lecture et stockage des données reçues
                JSONObject JSONstr = new JSONObject(messageTxt); // Conversion de la chaine de caractères en objet JSON pour en exploiter les données grace aux clés
                final String data = getString(R.string.json_author) + JSONstr.getString("author") + "\n" + getString(R.string.json_date) + JSONstr.getString("date") + "\n" + getString(R.string.json_content) + JSONstr.getString("content");

                requireActivity().runOnUiThread(() -> binding.textViewResponse.setText(data)); // Mise à jour du champ de texte avec l'accusé de réception

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
                DatagramSocket datagramSocket = new DatagramSocket(); // Socket d'émission. Pas de port à préciser, il choisira un port temporaire aléatoire, qui sera exploitable par le serveur pour répondre
                DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(ipAddress), port); // Conversion des données en ASCII, longueur du message, IP et port
                datagramSocket.send(datagramPacket); // Envoi
                byte[] responseBytes = new byte[1024]; // Tableau de caractères ASCII pour recevoir le datagramme. Il tient lieu du BufferedReader en TCP.
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length); // Instanciation d'un datagramme pour envoyer la réponse
                datagramSocket.receive(responsePacket);

                String messageTxt = new String(responseBytes, 0, responsePacket.getLength()); // Conversion ASCII vers chaine de caractères
                JSONObject JSONstr = new JSONObject(messageTxt); // Chaine vers dictionnaire JSON
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

    private JSONObject JSONConvert(String message, String author) throws JSONException { // Conversion du message en un dictionnaire JSON
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRANCE); // Constitution d'une date à un format habituel
        LocalDateTime now = LocalDateTime.now(); // Récupération de la date et l'heure courantes
        String date = dtf.format(now);
        JSONObject JSONstr = new JSONObject(); // Création de l'objet JSON
        JSONstr.put("content", message); // Ajout des clés et de leurs valeurs
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