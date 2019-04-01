package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
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
    private final static String masterNode = "11108";
    private static List<Node> joinedNodes = new LinkedList<Node>();
    private final static int SERVER_PORT = 10000;
    private  Node myNodeInfo ;
    private  Node endNodeInfo;
    private static String myPortId;


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
            myPortId = String.valueOf(Integer.parseInt(processId)*2);
            Log.e("*********","****MY PORT ID*****----"+ Integer.parseInt(processId)*2);

            if(myPortId.equals(masterNode))
            {
                  Node some = InsertSorted(masterNode);

            }
            else
            {
                // Let master node know that I am a new node willing to join the ring
                try {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Join me:" + myPortId + ":" + masterNode);
                }
                catch( Exception e)
                {
                    e.printStackTrace();
                }
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

    private Node InsertSorted( String newNodePort) {
        Log.i("In insert Sorted", "Inserting the new node in the ring");
        Node curr;
        Node prev;
        Node newNode = new Node(newNodePort);
        boolean ins = false;
        if (myNodeInfo== null)
        {
            newNode.successor = null;
            newNode.predecessor = null;
            myNodeInfo = newNode;
            endNodeInfo = newNode;
            //start = newNode;
            //end = start;
            return newNode;

        }
        else if (Integer.parseInt(newNodePort) < Integer.parseInt(myNodeInfo.portId))
            {
                newNode.predecessor = endNodeInfo;
                endNodeInfo.successor = newNode;
                myNodeInfo.predecessor = newNode;
                newNode.successor = myNodeInfo;
                myNodeInfo = newNode;
                return newNode;

            }
        else if (Integer.parseInt(newNodePort) > Integer.parseInt(endNodeInfo.portId))
            {
                endNodeInfo.successor = newNode;
                newNode.predecessor = endNodeInfo;
                newNode.successor = myNodeInfo;
                myNodeInfo.predecessor = newNode;
                endNodeInfo = newNode;
                return newNode;
            }
        else
            {
                Node current = myNodeInfo;
                Node ptr = myNodeInfo.successor;
                while (ptr != null) {
                if (Integer.parseInt(newNodePort) > Integer.parseInt(current.portId) && (Integer.parseInt(newNodePort) < Integer.parseInt(ptr.portId))) {
                    current.successor = newNode;
                    newNode.predecessor = current;
                    newNode.successor = ptr;
                    ptr.predecessor = newNode;
                    ins = true;
                    return newNode;
                }
                else {
                    current = ptr;
                    ptr = ptr.successor;
                }
            }
            if (!ins) {
                current.successor = newNode;
                newNode.predecessor = current;
                return newNode;

            }
        }
        return null;
    }




//
//        while(curr != null &&  Integer.valueOf(curr.portId) < Integer.valueOf(newNodePort))
//        {
//            prev = curr;
//            curr = curr.successor;
//
//        }
//        if( prev == null)
//        {
//            head = newNode;
//        }
//        else
//        {
//            prev.successor = newNode;
//            newNode.predecessor = prev;
//        }
//        if( curr != null)
//        {
//            curr.predecessor = newNode;
//            newNode.successor = curr;
//        }
//    return newNode;
//    }


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
                        Log.i("At server " + myNodeInfo.portId," Accepting requests");

                        String [] messageFromClientTokens = messageFromClient.split(":");
                        if( messageFromClientTokens[0].equals("Join me")) {
                            Log.i("Inside Server-" +myNodeInfo.portId," To add the node in a ring-" + messageFromClientTokens[1]);
                            Node insertedNode = InsertSorted(messageFromClientTokens[1]);
                            Log.e("Inserted",insertedNode.portId);

                            // Inform the joined node about its predecessor and successor


                            Node current = myNodeInfo.successor;



                            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                            outputStream.writeUTF("Predecessor:" + insertedNode.predecessor.portId + ":Successor:" + insertedNode.successor.portId);
                            outputStream.flush();
                        }
                        else if (messageFromClientTokens[0].equals("Update"))
                        {
                            Log.e("At server" + myPortId,"Updating my successors and predecessor");
                            myNodeInfo.predecessor = new Node(messageFromClientTokens[1]);
                            myNodeInfo.successor = new Node(messageFromClientTokens[3]);

                        }
                }
            }
            catch(Exception ex)
            {
                Log.e("In server", " Something went wrong in the server code");
                ex.printStackTrace();
            }




            return null;
        }
    }

    private class ClientTask extends  AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {
            try {
                String message = strings[0];
                String []messageTokens = strings[0].split(":");
                String toSendTOMasterPort = messageTokens[2];
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(toSendTOMasterPort));
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF(message);
                outputStream.flush();

                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                String messageFromServer = inputStream.readUTF();
                String [] messageFromServerTokens = messageFromServer.split(":");
                String predecessor = messageFromServerTokens[1];
                String successor = messageFromServerTokens[3];
                Log.e(" My successor", successor);
                Log.e("My Predecessor", predecessor);
                myNodeInfo = new Node(myPortId, new Node(predecessor),new Node(successor));



            }catch(Exception ex)
            {
                Log.e("In client", " Something went wrong in client Async Task");
                ex.printStackTrace();
            }



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
