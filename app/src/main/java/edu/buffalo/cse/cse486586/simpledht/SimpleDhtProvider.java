package edu.buffalo.cse.cse486586.simpledht;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private Node head;
    private  Node myNodeInfo ;
    private  Node endNodeInfo;
    private static String myPortId;
    private static int noOfNodesJoined =0;
    private  static  final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");

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


        try
        {
            String fileName = values.getAsString("key");
            String value = values.getAsString("value");
            String hashedKey = helper.genHash(fileName);
            if (IsCorrectNode(hashedKey))
            {   FileOutputStream outputStream = null;
                outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                outputStream.flush();
                outputStream.close();
            }
            else
            {
                String passingMessage = "Insert:"+ fileName+":" + value +":" +myNodeInfo.successor.portId + ":" + myNodeInfo.portId;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, passingMessage);
            }
        }
        catch(IOException io)
        {
            io.printStackTrace();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally {
//            outputStream.close();
        }

        return uri;

    }


    private boolean IsCorrectNode(String hashedKey)
    {

            if ((myNodeInfo.predecessor.hashedId.compareTo(myNodeInfo.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) <= 0 && hashedKey.compareTo(myNodeInfo.predecessor.hashedId) < 0) ||
                    (myNodeInfo.predecessor.hashedId.compareTo(myNodeInfo.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.predecessor.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) > 0) ||
                    (hashedKey.compareTo(myNodeInfo.predecessor.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) <= 0))
            {
                return true;

            }
            return false;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
       ServerSocket serverSocket  = null;
        try
        {
             serverSocket = new ServerSocket(SERVER_PORT);
            TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            final String processId = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
            //String myPortId =String.valueOf (Integer.parseInt(processId)*2);
            myPortId = String.valueOf(Integer.parseInt(processId)*2);
            Log.e("*********","****MY PORT ID*****----"+ Integer.parseInt(processId)*2);

            if(myPortId.equals(masterNode))
            {
                  myNodeInfo = InsertSorted(masterNode, helper.genHash(masterNode));

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

    private Node InsertSorted(String portId,  String newNodeHshPort) {
        Log.i("In insert Sorted", "Inserting the new node in the ring-" + portId);
        Node curr;
        Node prev;
        Node newNode = new Node(portId);
        boolean ins = false;
        if (head== null)
        {
            newNode.successor = null;
            newNode.predecessor = null;
            head = newNode;
            endNodeInfo = newNode;
            //start = newNode;
            //end = start;
            return newNode;

        }
        else if ((newNodeHshPort.compareTo(head.hashedId)) <0)
            {
                newNode.predecessor = endNodeInfo;
                endNodeInfo.successor = newNode;
                head.predecessor = newNode;
                newNode.successor = head;
                head = newNode;
                return newNode;

            }
        else if ((newNodeHshPort.compareTo(endNodeInfo.portId)) > 0)
            {
                endNodeInfo.successor = newNode;
                newNode.predecessor = endNodeInfo;
                newNode.successor = head;
                head.predecessor = newNode;
                endNodeInfo = newNode;
                return newNode;
            }
        else
            {
                Node current = head;
                Node ptr = head.successor;
                while (ptr != null) {
                if ((newNodeHshPort.compareTo(current.hashedId) >0) && ((newNodeHshPort.compareTo(ptr.hashedId)) <0)) {
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
                        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                        Log.e("Acknowledge to ", messageFromClient.split(":")[1]);
                        outputStream.writeUTF("Acknowledged");
                        outputStream.flush();

                        String [] messageFromClientTokens = messageFromClient.split(":");
                        if( messageFromClientTokens[0].equals("Join me")) {
                            Log.i("Inside Server-" +myNodeInfo.portId," To add the node in a ring-" + messageFromClientTokens[1]);
                            Node insertedNode = InsertSorted(messageFromClientTokens[1], helper.genHash(messageFromClientTokens[1]));
                            Log.e("Inserted",insertedNode.portId);
                            noOfNodesJoined ++;
                            // Inform the joined node about its predecessor and successor
                            //if( noOfNodesJoined >2) {
                                UpdateLinks();
                            //}
                        }
                        else if (messageFromClientTokens[0].equals("Update"))
                        {
                            myNodeInfo = new Node(myPortId);
                            Log.e("At server" + myPortId,"Updating my successors and predecessor");
                            myNodeInfo.predecessor = new Node(messageFromClientTokens[1]);
                            Log.e("Updated predecessor at "+ myPortId, myNodeInfo.predecessor.portId);
                            myNodeInfo.successor = new Node(messageFromClientTokens[2]);
                            Log.e("Updated successor at "+ myPortId, myNodeInfo.successor.portId);

                        }
                        else if (messageFromClientTokens[0].equals("Insert"))
                        {
                            String keyToInsert = messageFromClientTokens[1];
                            String value = messageFromClientTokens[2];

                            ContentValues mContentValues = new ContentValues();
                            mContentValues.put("key", keyToInsert);
                            mContentValues.put("value", value);
                            getContext().getContentResolver().insert(CONTENT_URI, mContentValues);
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


    private void UpdateLinks()
    {
        try {

            Node current = head;
            Log.e("Current head", current.portId);
            Log.e("Current end", endNodeInfo.portId);
           for(int i =0 ; i < noOfNodesJoined;i++) {
                //if(current.portId.equals(myPortId)) continue;
               new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Update:" + current.predecessor.portId + ":" + current.successor.portId + ":" + current.portId);
               current = current.successor;

           }

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private class ClientTask extends  AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {
            try {
                String message = strings[0];
                String []messageTokens = strings[0].split(":");

                if(messageTokens.length == 3) {
                    Log.e("In client"+ myPortId,"Sending message to join me in the ring");
                    String toSendTOMasterPort = messageTokens[2];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(toSendTOMasterPort));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(message);
                    outputStream.flush();

                    DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                    String serverResponse = inputStream.readUTF();
                    if(serverResponse != null)
                    {
                        Log.e("Acknowledge received -",myPortId );
                    }
                }
                else if( messageTokens.length == 5)
                {
                    String successorPort = messageTokens[3];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(successorPort));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(message);
                    outputStream.flush();

                }

                else
                {

                    String deliveryPort= messageTokens[3];
                    Log.e("In client"+ myPortId," Sending message to update links at" + deliveryPort);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(deliveryPort));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(message);
                    outputStream.flush();
                }



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
