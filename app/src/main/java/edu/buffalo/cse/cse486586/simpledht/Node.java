package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.Comparator;

public class Node{
    String portId;
    String hashedId;
    String nodeId;
    Node predecessor;
    Node successor;
    Node(String portId, String nodeId)
    {
        this.nodeId = nodeId;
        this.portId = portId;
        try {
            hashedId =  new Helper().genHash(nodeId);
        }
        catch(Exception ex)
        {
            Log.e("Node creation"," Something went wrong while creating a node");
            ex.printStackTrace();
        }

    }



}



class NodeCompare implements Comparator<Node> {

    @Override
    public int compare(Node lhs, Node rhs) {
        return lhs.hashedId.compareTo(rhs.hashedId);
    }
}