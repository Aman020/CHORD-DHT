package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.Comparator;

public class Node{
    String portId;
    String hashedId;
    Node predecessor;
    Node successor;
    Node(String portId)
    {
        this.portId = portId;
        try {
            hashedId =  new Helper().genHash(portId);
        }
        catch(Exception ex)
        {
            Log.e("Node creation"," Something went wrong while creating a node");
            ex.printStackTrace();
        }

    }
    Node(String portId, Node predecessor, Node successor)
    {
        this.portId = portId;
        this.successor = successor;
        this.predecessor = predecessor;

    }


}






class NodeCompare implements Comparator<Node> {

    @Override
    public int compare(Node lhs, Node rhs) {
        return lhs.portId.compareTo(rhs.portId);
    }
}