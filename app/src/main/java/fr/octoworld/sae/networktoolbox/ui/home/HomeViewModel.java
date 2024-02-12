package fr.octoworld.sae.networktoolbox.ui.home;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Locale;

/*public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Serveur TCP");
    }

    public LiveData<String> getText() {
        return mText;
    }
}*/

public class HomeViewModel extends ViewModel {

    private ServerSocket serverSocket;
    private DatagramSocket datagramSocket;
    private List<Socket> clients;

    private boolean isTcp = true;
    private EditText editTextPort;
    private TextView textViewMessage;

    public HomeViewModel(EditText editTextPort, TextView textViewMessage) {
        this.editTextPort = editTextPort;
        this.textViewMessage = textViewMessage;
    }
    public void onStartServer() {
        // Get port number
        int port = Integer.parseInt(editTextPort.getText().toString());

        // Create server socket
        try {
            if (isTcp) {
                serverSocket = new ServerSocket(port);
            } else {
                datagramSocket = new DatagramSocket(port);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Start a thread to listen for new clients
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // Accept a new client
                        Socket client;
                        if (isTcp) {
                            client = serverSocket.accept();
                        } else {
                            //DatagramSocket datagramSocket = (DatagramSocket) serverSocket;
                            DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
                            datagramSocket.receive(datagramPacket);
                            client = new Socket(datagramPacket.getAddress(), datagramPacket.getPort());
                        }

                        // Create a thread to handle the client
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Handle the client
                                    try {
                                        handleClient(client);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                } finally {
                                    // Close the client socket
                                    try {
                                        client.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void onStopServer() {
        // Close the server socket
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Stop all client threads
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket client) throws IOException {
        // Get the input and output streams
        InputStream inputStream = client.getInputStream();
        OutputStream outputStream = client.getOutputStream();

        // Read the message from the client
        byte[] messageBytes = new byte[1024];
        int bytesRead = inputStream.read(messageBytes);
        String message = new String(messageBytes, 0, bytesRead);

        // Display the message
        textViewMessage.setText(message);

        // Send a response to the client
        outputStream.write("Message reçu : ".getBytes());
        outputStream.write(message.getBytes());
        outputStream.flush();
    }
}