package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.ThreadPoolExecutor;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    Helper helper = new Helper();
    private final static String masterNode = "11108";
    private static List<Node> joinedNodes = new ArrayList<Node>();
    private final static int SERVER_PORT = 10000;
    private Node head;
    private  Node myNodeInfo ;
    private  Node endNodeInfo;
    private static String myPortId;
    private static int noOfNodesJoined =0;
    private  static  final Uri CONTENT_URI = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
    private static boolean isSingleNode = true;
    private  MatrixCursor singleKeyResult;
    private static boolean isResultFound = false;
    private static  boolean isAllQueryResult=false;
    private MatrixCursor starKeyResult;
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            File filesDirectory = getContext().getFilesDir();
            File[] files = filesDirectory.listFiles();

            if (selection.equals("@")) {
                Log.e("No of files", "" + files.length);
                for (File file : files) {
                    file.delete();

                }
            }
            else if( selection.equals("*"))
            {

            }
            else{
                boolean isFileAvailable = false;
                for (File file : files){
                    if(file.getName().equals(selection))
                    {
                        isFileAvailable = true;
                        file.delete();
                        Log.e("In delete at "+ myPortId,"****FILE DELETED****");
                        break;
                    }
                }
                if( !isFileAvailable)
                {
                    Log.e("At delete" + myPortId,"I DONT HAVE THE FILE. REQUESTING--" + myNodeInfo.successor.portId);
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"Delete:" + selection + ":" + myPortId + ":" + myNodeInfo.successor.portId);

                }
            }



        }catch(Exception ex)
        {
            ex.printStackTrace();
        }

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
            Log.e("In Insert","Calling insert");
            String fileName = values.getAsString("key");
            String value = values.getAsString("value");
            String hashedKey = helper.genHash(fileName);
            Log.e("INSERT", "KEY TO INSERT"+ fileName);
            Log.e("INSERT", "VALUE TO INSERT" + value);
            if (IsCorrectNode(hashedKey))
            {
                Log.e("INSERT- "+ myPortId,"Inserting -" + fileName +"-" + value);
                FileOutputStream outputStream = null;
                outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
                outputStream.write(value.getBytes());
                outputStream.flush();
                outputStream.close();
            }
            else
            {
                String passingMessage = "Insert:"+ fileName+ "@@" + value +":" +myNodeInfo.portId + ":" +myNodeInfo.successor.portId ;
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
            if (isSingleNode) return true;

            if( myNodeInfo.predecessor != null && myNodeInfo.successor !=null) {
                if (myNodeInfo.predecessor.hashedId.compareTo(myNodeInfo.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) <= 0 && hashedKey.compareTo(myNodeInfo.predecessor.hashedId) < 0 ||
                        myNodeInfo.predecessor.hashedId.compareTo(myNodeInfo.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.predecessor.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) > 0 ||
                        hashedKey.compareTo(myNodeInfo.predecessor.hashedId) > 0 && hashedKey.compareTo(myNodeInfo.hashedId) <= 0) {
                    return true;

                }
            }
            else if(myNodeInfo.predecessor == null && myNodeInfo.successor ==null)
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
                 // myNodeInfo = InsertSorted(myPortId, helper.genHash(String.valueOf(Integer.parseInt(myPortId)/2)));
                    myNodeInfo = new Node(myPortId,String.valueOf(Integer.parseInt(myPortId)/2));
                    joinedNodes.add(myNodeInfo);

            }
            else
            {
                // Let master node know that I am a new node willing to join the ring
                try {
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Join me:" + myPortId + ":" + "dummy:" + masterNode);
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
        Node newNode = new Node(portId, String.valueOf(Integer.parseInt(portId)/2));
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
                        String [] messageFromClientTokens = messageFromClient.split(":");
                        if( messageFromClientTokens[0].equals("Join me")) {
                            Log.i("Inside Server-" +myNodeInfo.portId," To add the node in a ring-" + messageFromClientTokens[1]);
                            //Node insertedNode = InsertSorted(messageFromClientTokens[1], helper.genHash(String.valueOf(Integer.parseInt(messageFromClientTokens[1])/2)));
                            String toBeJoinedPort = messageFromClientTokens[1];
                            String toBeJoinedNodeId =String.valueOf(Integer.parseInt(messageFromClientTokens[1])/2);
                            Node newNode = new Node(toBeJoinedPort,toBeJoinedNodeId);

                            joinedNodes.add(newNode);
                            Collections.sort(joinedNodes, new NodeCompare());
                            Log.e("Inserted",toBeJoinedPort);
                            isSingleNode = false;
                            noOfNodesJoined ++;
                            UpdateLinks();

                        }
                      else if (messageFromClientTokens[0].equals("Update"))
                        {
                            isSingleNode = false;
                            myNodeInfo = new Node(myPortId, String.valueOf(Integer.parseInt(myPortId)/2));
                            Log.e("At server" + myPortId,"Updating my successors and predecessor");
                            myNodeInfo.predecessor = new Node(messageFromClientTokens[1], String.valueOf(Integer.parseInt(messageFromClientTokens[1])/2));
                            Log.e("Updated predecessor at "+ myPortId, myNodeInfo.predecessor.portId);
                            myNodeInfo.successor = new Node(messageFromClientTokens[2],String.valueOf(Integer.parseInt(messageFromClientTokens[2])/2));
                            Log.e("Updated successor at "+ myPortId, myNodeInfo.successor.portId);

                        }
                        else if (messageFromClientTokens[0].equals("Insert"))
                        {
                            Log.e("In server to insert", "received key as"+ messageFromClientTokens[1] );
                            String[] keyValueToInsert = messageFromClientTokens[1].split("@@");
                            String keyToInsert = keyValueToInsert[0];
                            String value = keyValueToInsert[1];

                            ContentValues contentValues = new ContentValues();
                            contentValues.put("key", keyToInsert);
                            contentValues.put("value", value);
                            getContext().getContentResolver().insert(CONTENT_URI, contentValues);
                        }
                        else if (messageFromClientTokens[0].equals("Retrieve"))
                        {
                            if(messageFromClientTokens[1].equals(messageFromClientTokens[3]) ) {
                                Log.e("In server" + myPortId, "Retrieved all the keys and values");
                                Log.e("ALl keys -", messageFromClientTokens[2]);

                                String allKeyValuePairs = messageFromClientTokens[2];
                                if (allKeyValuePairs.equals("")) {
                                    isAllQueryResult = true;
                                } else{
                                    String[] allKeyValuePairsTokens = allKeyValuePairs.split("@@");
                                int count = 0;

                                for (String keyValue : allKeyValuePairsTokens) {
                                    String[] keyAndValue = keyValue.split("-");

                                    starKeyResult.addRow(new String[]{keyAndValue[0], keyAndValue[1]});
                                    count++;
                                }
                                Log.e("No of keys retreived", String.valueOf(count));
                                isAllQueryResult = true;
                            }
                            }
                            else {
                                StringBuilder sb = new StringBuilder();
                                sb.append(messageFromClientTokens[2]);
                                Log.e("In retreive at server" + myPortId, " Predecessor's key--" + messageFromClientTokens[2]);
                                Cursor cursor = getContext().getContentResolver().query(CONTENT_URI, null, "@", null, null);
                                sb.append(KeyValuePairString(cursor));
                                Log.e("In server retrieve" + myPortId, "Key value pairs --" + sb.toString());
                                InformSuccessorToRetrievekeys(sb, messageFromClientTokens[1],  myNodeInfo.successor.portId);
                            }

                        }
                        else if(messageFromClientTokens[0].equals("Check Key") )
                        {
                            Log.e("In Check Key server at " + myPortId,messageFromClient);
                            String whoWantsKey = messageFromClientTokens[2];
                            Log.e("In Check Key server at " + myPortId,"Requested port" + whoWantsKey);
                             ReturnSingleKey(messageFromClientTokens[1], whoWantsKey);

                        }
                        else if(messageFromClientTokens[0].equals("Found Key") )
                        {
                            singleKeyResult = new MatrixCursor(new String[]{"key","value"});
                            singleKeyResult.addRow( new String[]{messageFromClientTokens[1],messageFromClientTokens[2]});
                            Log.e("Inside server -"+ myPortId, "Key Found");
                            isResultFound = true;

                        }
                        else if(messageFromClientTokens[0].equals("Delete"))
                        {
                            if(!messageFromClientTokens[2].equals(messageFromClientTokens[3]))
                            getContext().getContentResolver().delete(CONTENT_URI,messageFromClientTokens[1],null);

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

    private void InformSuccessorToRetrievekeys(StringBuilder alreadyRetreivedKeys,String requestedPort, String successorPort)
    {
        Log.e("In InformSuccessorKeys","Pinging successor to retireve keys. Successor-" + successorPort);
        Log.e("InformSuccessorKeys", " Already Retreiveed keys-" + alreadyRetreivedKeys);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Retrieve:" + requestedPort + ":" + alreadyRetreivedKeys + ":" + successorPort);
    }

    private void UpdateLinks()
    {
        try {

//            Node current = head;
//            Log.e("Current head", current.portId);
//            Log.e("Current end", endNodeInfo.portId);
//           for(int i =0 ; i <= noOfNodesJoined;i++) {
//               if( current.portId.equals(myPortId)) continue;
//                //if(current.portId.equals(myPortId)) continue;
//               Log.e("In update Links", "Current Port-" + current.portId +"- current successor id" + current.successor.portId);
//               new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Update:" + current.predecessor.portId  + ":" + current.successor.portId + ":" + current.portId);
//               current = current.successor;


            for(int i =0 ; i < joinedNodes.size();i++)
            {
                    String destinationPort = joinedNodes.get(i).portId;
                    String successorPort;
                    String predecessorPort;
                    if (i == 0) {
                        predecessorPort = joinedNodes.get(joinedNodes.size()-1).portId;
                    } else {
                        predecessorPort = joinedNodes.get(i-1).portId;
                    }
                    if (i == joinedNodes.size() - 1) {
                        successorPort =joinedNodes.get(0).portId;
                    } else {
                        successorPort = joinedNodes.get(i+1).portId;
                    }

                Log.e("Sending message to" + destinationPort,"Predecessor -" + predecessorPort + "- Successor" + successorPort);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Update:" + predecessorPort + ":" + successorPort + ":" + destinationPort);
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

                if( messageTokens[0].equals("Found Key"))
                {
                    Log.e("In client"+ myPortId,message);
                    String toSendTOMasterPort = messageTokens[4];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(toSendTOMasterPort));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    outputStream.writeUTF(message);
                    outputStream.flush();
                }
                else {
                    String toSendTOMasterPort= messageTokens[3];

                    Log.e("In client" + myPortId, message);

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(toSendTOMasterPort));
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
        Log.e("Inside","Query-----");
        // TODO Auto-generated method stub
        MatrixCursor cursor;
       try {
           if (selection.equals("@")) {
               return GetLocalKeys();
           }
           else if (selection.equals("*")) {
               if(isSingleNode) {
                Log.e("In query", "ITS A SINGLE NODE");
                  return  GetLocalKeys();
               }
               Log.e("Inside query", " **");
               return RetrieveAllKeys();

           }
           else
           {

               Log.e("Inside query"," RUNNING ELSE PART---- SINGLE KEY QUERY");
               try {

                   if( IsCorrectNode(helper.genHash(selection))) {
                       Log.e("Inside query"," SNGLE KEY EXISTS HERE");

                       FileInputStream inputstream = getContext().openFileInput(selection);
                       int res = 0;
                       // creating an object of StringBuilder to efficiently append the data which is read using read() function. Read() function returns a byte and hence we use while loop to read all the bytes. It returns -1 if its empty.
                       StringBuilder sb = new StringBuilder();
                       while ((res = inputstream.read()) != -1) {
                           sb.append((char) res);

                       }
                       MatrixCursor matrixCursor = new MatrixCursor(new String[]{"key", "value"});
                       matrixCursor.addRow(new String[]{selection, sb.toString()});
                       inputstream.close();
                       return  matrixCursor;
                   }
                   else
                   {
                       //singleKeyResult = new MatrixCursor(new String[]{"key", "value"});
                       new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Check Key:"+ selection + ":" +myPortId + ":" + myNodeInfo.successor.portId);
                       isResultFound = false;
                       Log.e("In query else", " Running the lock now");
                       while(!isResultFound)
                       {
                           Log.e("BLOCKING CALL","BLOCKING CALL");
                           //wait for result;
                       }
                       Log.e("EXIT BLOCKIING CALL","EXIT");
                       return singleKeyResult;

                   }

               }catch (Exception ex)
               {
                   ex.printStackTrace();
               }
           }

       }catch(Exception ex)
       {
           Log.e("In query","Someting went wrong");
           ex.printStackTrace();
       }
        return null;
    }


    private MatrixCursor GetLocalKeys() {
        Log.e("Inside query"," @ running");
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
        File filesDirectory = getContext().getFilesDir();
        File[] files = filesDirectory.listFiles();
        Log.e("No of files","" +files.length );
        try {
            for (File file : files) {

                    String key = file.getName();
                 //   Log.e("Inside query", "searching for file-" + key);
                    // Creating a stream object. Various other alternate byte/character streams can be used to read and write the data.
                    BufferedReader inputstream = new BufferedReader(new InputStreamReader(getContext().openFileInput(key)));
                    String value = inputstream.readLine();
                    Log.e("Key value-", key + "---" + value);
                    cursor.addRow(new String[]{key, value});
            }

            return cursor;
        }catch (Exception ex)
        {
            Log.e("In GetLocalKeys", "Something went wrong");
        }
        return null;
    }

    private MatrixCursor RetrieveAllKeys()
    {
        MatrixCursor cursor;
        try
        {
            starKeyResult = new MatrixCursor(new String [] {"key","value"});
            starKeyResult = GetLocalKeys();

                Node current = myNodeInfo;
                String toSendToPort = current.successor.portId;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(toSendToPort));
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());


                    outputStream.writeUTF("Retrieve:"+ myPortId +":" + KeyValuePairString(starKeyResult) +":" + current.successor.portId);
                    Log.e("In retreive all keys","Retrieve:"+ myPortId +":" + KeyValuePairString(starKeyResult) +":" + current.successor.portId);
                    outputStream.flush();

                    isAllQueryResult = false;
                    while(!isAllQueryResult)
                    {
                        Log.e("Blocking Call","*");
                    }
                    return starKeyResult;


     }catch (Exception ex)
        {
            Log.e("In RetrieveKeys","Something went wrong");
            ex.printStackTrace();
        }
        return null;
    }

    private String  KeyValuePairString(Cursor cursor)
    {
        StringBuilder sb = new StringBuilder();
        while ((cursor.moveToNext()))
        {
            String key = cursor.getString(cursor.getColumnIndex("key"));
            String value = cursor.getString(cursor.getColumnIndex("value"));
            sb.append(key +"-" + value +"@@");
        }
    return sb.toString();

    }
    private void ReturnSingleKey(String selection, String whoWantsKey)
    {
        try {

        if( IsCorrectNode(helper.genHash(selection))) {

            FileInputStream inputstream = getContext().openFileInput(selection);
            int res = 0;
            // creating an object of StringBuilder to efficiently append the data which is read using read() function. Read() function returns a byte and hence we use while loop to read all the bytes. It returns -1 if its empty.
            StringBuilder sb = new StringBuilder();
            while ((res = inputstream.read()) != -1) {
                sb.append((char) res);

            }

            singleKeyResult = new MatrixCursor(new String[]{"key","value"});
            singleKeyResult.addRow(new String[]{selection, sb.toString()});
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Found Key:" + selection +":" + sb.toString() +":" + myPortId + ":" + whoWantsKey);
            inputstream.close();

        }
        else
        {   Log.e("Return single key" + myPortId, "- I DONT HAVE TO KEY " + myPortId );
            Log.e("Return single key" + myPortId, "Passing request for search to my successor" +myNodeInfo.successor.portId);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Check Key:"+ selection + ":" +whoWantsKey + ":" + myNodeInfo.successor.portId);
        }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

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
