package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    Helper helper = new Helper();

    class Node{
        String portId;
        String hashedId;
        Node predecessor;
        Node successor;
        Node(String portId)
        {
            this.portId = portId;
            try {
                hashedId = helper.genHash(portId);
            }
            catch(Exception ex)
            {
                Log.e("Node creation"," Something went wrong while creating a node");
                ex.printStackTrace();
            }

        }


    }

    private final static String masterNode = "11108";
    private static List<Node> joinedNodes = new ArrayList<Node>();
    private final static int SERVER_PORT = 10000;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        try
        {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            final String processId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            String myPortId =String.valueOf (Integer.parseInt(processId)*2);
            Log.e("*********","****MY PORT ID*****----"+ Integer.parseInt(processId)*2);

            if(myPortId.equals(masterNode))
            {
                joinedNodes.add( new Node(masterNode));

            }
            else
            {
                // Let master node know that I am a new node willing to join the ring
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,myPortId, String.valueOf(Integer.valueOf(processId) * 2));

            }
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

        }
        catch(Exception ex)
        {
            Log.e("On Create", " Something went wrong in on create function");

            ex.printStackTrace();
        }

        return false;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void>
    {

        @Override
        protected Void doInBackground(ServerSocket... serverSockets) {
            try
            {

                    while (true) {

                        Socket socket =   serverSockets[0].accept();
                        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                        String messageFromClient = inputStream.readUTF();


                }
            }
            catch(Exception ex)
            {
                Log.e("In server", " Something went wrong in the server code");
            }




            return null;
        }
    }

    private class ClientTask extends  AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {
            return null;
        }
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
