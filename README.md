                                          DISTRIBUTED HASH TABLE USING CHORD
                            
## Goal:

To implement a distributed hash table using Chord. Since its a simple version of Chord, we don't need to implement finger table and finger based routing. Following are the things to be implemented in this project.
1. ID space partitioning/re-partitioning
2. Ring-based routing
3. Node Joins

## Chord DHT:

It is basically a distributed hash table using consistent hashing.It organizes node as a ring.
In a chord ring, the hash key space is represented as a virtual ring instead of a table. It uses one of the hash functions (eg. SHA-1) that evenly distributes items over the hash. THe nodes are mapped in the same ring.
![alt text](https://www.cs.rutgers.edu/~pxk/417/notes/images/dht-dynamo-vnode.png)


## Requirements:

## Step 1: Writing the Content Provider
```
1. This content provider should implement all DHT functionalities. For example, it should create server and client threads (if this is what you decide to implement), open sockets, and respond to incoming requests; it should also implement a simplified version of the Chord routing protocol; lastly, it should also handle node joins. The following are the requirements for your content provider:
2. The content provider should implement all DHT functionality. This includes all communication using sockets as well as mechanisms to handle insert/query requests and node joins.
3. Each content provider instance should have a node id derived from its emulator port. This node id should be obtained by applying the above hash function (i.e., genHash()) to the emulator port. For example, the node id of the content provider instance running on emulator-5554 should be, node_id = genHash(“5554”). This is necessary to find the correct position of each node in the Chord ring.
4. Our content provider should implement insert(), query(), and delete(). The basic interface definition is the same as the previous assignment, which allows a client app to insert arbitrary <”key”, “value”> pairs where both the key and the value are strings.
5. For delete(URI uri, String selection, String[] selectionArgs), you only need to use use the first two parameters, uri & selection.  This is similar to what you need to do with query().
6. However, please keep in mind that this “key” should be hashed by the above genHash() before getting inserted to your DHT in order to find the correct position in the Chord ring.
7. For your query() and delete(), you need to recognize two special strings for the selection parameter.
8. If * (not including quotes, i.e., “*” should be the string in your code) is given as the selection parameter to query(), then you need to return all <key, value> pairs stored in your entire DHT.
9. Similarly, if * is given as the selection parameter to delete(), then you need to delete all <key, value> pairs stored in your entire DHT.
10. If @ (not including quotes, i.e., “@” should be the string in your code) is given as the selection parameter to query() on an AVD, then you need to return all <key, value> pairs stored in your local partition of the node, i.e., all <key, value> pairs stored locally in the AVD on which you run query().
11. Similarly, if @ is given as the selection parameter to delete() on an AVD, then you need to delete all <key, value> pairs stored in your local partition of the node, i.e., all <key, value> pairs stored locally in the AVD on which you run delete().
12. An app that uses your content provider can give arbitrary <key, value> pairs, e.g., <”I want to”, “store this”>; then your content provider should hash the key via genHash(), e.g., genHash(“I want to”), get the correct position in the Chord ring based on the hash value, and store <”I want to”, “store this”> in the appropriate node.
13. Your content provider should implement ring-based routing. Following the design of Chord, your content provider should maintain predecessor and successor pointers and forward each request to its successor until the request arrives at the correct node. Once the correct node receives the request, it should process it and return the result (directly or recursively) to the original content provider instance that first received the request.
14. Your content provider do not need to maintain finger tables and implement finger-based routing. This is not required.
15. Your content provider should handle new node joins. For this, you need to have the first emulator instance (i.e., emulator-5554) receive all new node join requests. Your implementation should not choose a random node to do that. Upon completing a new node join request, affected nodes should have updated their predecessor and successor pointers correctly.
16. Your content provider do not need to handle node leaves/failures. This is not required.
Your app should open one server socket that listens on 10000.
  You need to use run_avd.py and set_redir.py to set up the testing environment.
  The grading will use 5 AVDs. The redirection ports are 11108, 11112, 11116, 11120, and 11124.
  You should just hard-code the above 5 ports and use them to set up connections.
  Please use the code snippet provided in PA1 on how to determine your local AVD.
  emulator-5554: “5554”
  emulator-5556: “5556”
  emulator-5558: “5558”
  emulator-5560: “5560”
  emulator-5562: “5562”
17. Your content provider’s URI should be: “content://edu.buffalo.cse.cse486586.simpledht.provider”, which means that any app should be able18.  to access your content provider using that URI. Your content provider does not need to match/support any other URI pattern.
19. As with the previous assignment, Your provider should have two columns.
20. The first column should be named as “key” (an all lowercase string without the quotation marks). This column is used to store all keys.
21. The second column should be named as “value” (an all lowercase string without the quotation marks). This column is used to store all values.
22. All keys and values that your provider stores should use the string data type.
Note that your content provider should only store the <key, value> pairs local to its own partition.
23. Any app (not just your app) should be able to access (read and write) your content provider. As with the previous assignment, please do not include any permission to access your content provider.

```
## Testing:


We have testing programs to help you see how your code does with our grading criteria. If you find any rough edge with the testing programs, please report it on Piazza so the teaching staff can fix it. The instructions are the following:
Download a testing program for your platform. If your platform does not run it, please report it on Piazza.
Windows: We’ve tested it on 32- and 64-bit Windows 8.
Linux: We’ve tested it on 32- and 64-bit Ubuntu 12.04.
OS X: We’ve tested it on 32- and 64-bit OS X 10.9 Mavericks.
Before you run the program, please make sure that you are running five AVDs. python run_avd.py 5 will do it.
Run the testing program from the command line.
On your terminal, it will give you your partial and final score, and in some cases, problems that the testing program finds.


