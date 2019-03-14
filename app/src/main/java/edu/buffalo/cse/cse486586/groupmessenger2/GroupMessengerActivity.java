package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = new String[]{"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    private ContentResolver mContentResolver;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    PriorityQueue<Entry> priorityQueue = new PriorityQueue();
    int key = -1;
    int myId = 0;
    String uniqueString = "";
    int uniqueId = 0;
    double maxKey = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        switch(Integer.parseInt(myPort)){
            case(11108): myId = 1; uniqueString="A"; break;
            case(11112): myId = 2; uniqueString="B"; break;
            case(11116): myId = 3; uniqueString="C"; break;
            case(11120): myId = 4; uniqueString="D"; break;
            case(11124): myId = 5; uniqueString="E"; break;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText editText = (EditText) findViewById(R.id.editText1);
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }



    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                while(true){
                    Socket sc = serverSocket.accept();
                    /*https://stackoverflow.com/questions/28187038/tcp-client-server-program-datainputstream-dataoutputstream-issue*/
                    DataInputStream input = new DataInputStream(sc.getInputStream());
                    String recStr = input.readUTF();
                    //System.out.println("Server1:"+recStr);
                    /*http://developer.android.com/reference/android/os/AsyncTask.html*/
                    double keyToStore =0.0;
                    String[] msgStrs = recStr.split("~~",4);
                    //System.out.println("Server02:"+msgStrs[0]);
                    Log.v("Server Rcvd msgStrs:",recStr);

                    if("requestKey".equalsIgnoreCase(msgStrs[0])){
                        Entry maxEntry = max();
                        keyToStore = (int)maxEntry.getKey()+1+0.1*myId;
                        String msgId = msgStrs[2];
                        if(keyToStore > maxKey){
                            maxKey = keyToStore;
                        }
                        Log.v("Server ShowQueue Rec1: ", showPQueue());
                        Entry newEntry =new Entry(keyToStore,msgStrs[3],msgId,false);
                        priorityQueue.add(newEntry);
                        Log.v("Server ShowQueue Rec2: ", showPQueue());

                        String resStr = "response~~"+keyToStore+"~~"+msgId;
                        DataOutputStream ackOut = new DataOutputStream(sc.getOutputStream());
                        ackOut.writeUTF(resStr);
                        Log.v("Server Resp to Client:",resStr);
                        ackOut.close();
                    }else if("ackKey".equalsIgnoreCase(msgStrs[0])){

                        keyToStore = Double.parseDouble(msgStrs[1]);
                        String msgId = msgStrs[2];

                        Log.v("Server ShowQueue Upd1: ", showPQueue());
                        Entry updateEntry =new Entry(keyToStore,msgStrs[3],msgId,true);
                        updateKey(updateEntry);

                        Log.v("Server ShowQueue Upd2: ", showPQueue());

                        DataOutputStream ackOut = new DataOutputStream(sc.getOutputStream());
                        ackOut.writeUTF("received~~"+msgId);
                        ackOut.close();

                        while(priorityQueue.size() != 0){
                            Entry firstEntry = priorityQueue.peek();
                            if(priorityQueue.peek().isAck()){
                                Log.v("Server ShowQueue 2If: ", showPQueue());
                                publishProgress(firstEntry.getValue(),firstEntry.getKey()+"", firstEntry.getuId());
                                Log.v("Server ShowQueue 2Rm1: ", showPQueue());
                                priorityQueue.poll();
                                Log.v("Server ShowQueue 2Rm2: ", showPQueue());
                            }else{
                                Log.v("Server ShowQueue Else: ", showPQueue());
                                break;
                            }
                        }
                    }
                    input.close();
                    sc.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }


        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            String keyFinal = strings[1].trim();
            String msgId = strings[2].trim();
            key +=1;

            String text = msgId+"~"+keyFinal+"~"+key+"~"+strReceived + "\t\n";
            //String text = key+"~"+strReceived + "\t\n";
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(text);
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            Log.v("Publish Key and Msg:",text);

            ContentValues cv = new ContentValues();
            cv.put(KEY_FIELD, Integer.toString(key));
            cv.put(VALUE_FIELD, strReceived);


            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            mContentResolver = getContentResolver();
            mContentResolver.insert(mUri, cv);

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Entry me = max();
                double keyReq = (int)me.getKey()+1+0.1*myId;
                double keyFinal = keyReq;
                String msg = msgs[0];
                Log.v("Client MyPort:",msgs[1]);
                String msgId = uniqueString+(++uniqueId);
                for( String port : REMOTE_PORT){
                    String remotePort = port;
                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    String msgToSend = "requestKey~~"+keyReq+"~~"+msgId+"~~"+msg;
                    Log.v("Client Request:",msgToSend+" Port:"+remotePort);
                    DataOutputStream output = new DataOutputStream(socket1.getOutputStream());
                    output.writeUTF(msgToSend);

                    DataInputStream ackRec = new DataInputStream(socket1.getInputStream());
                    String ackStr = ackRec.readUTF();
                    Log.v("Client Ack from Server:",ackStr+" Port:"+remotePort);
                    String[] msgStrs = ackStr.split("~~",3);
                    double recKey = Double.parseDouble(msgStrs[1]);
                    if(msgStrs[0].equals("response") && msgId.equalsIgnoreCase(msgStrs[2])){
                        if(recKey > keyFinal){
                            keyFinal = recKey;
                        }
                        output.close();
                        ackRec.close();
                        socket1.close();
                    }
                }

                Log.v("Client FinalKey:",keyFinal+"~ Id:"+msgId);

                for( String port : REMOTE_PORT){
                    String remotePort = port;
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    String msgToSend = "ackKey~~"+keyFinal+"~~"+msgId+"~~"+msg;
                    Log.v("Client Final key msg:",msgToSend+" Port:"+remotePort);
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msgToSend);

                    DataInputStream ackRec = new DataInputStream(socket.getInputStream());
                    String ackStr = ackRec.readUTF();
                    Log.v("Client Final Ack Srvr:",ackStr);
                    String[] msgStrs = ackStr.split("~~",2);
                    if(msgStrs[0].equals("received")){
                        output.close();
                        ackRec.close();
                        socket.close();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }

    }


    //https://stackoverflow.com/questions/29872664/add-key-and-value-into-an-priority-queue-and-sort-by-key-in-java
    public class Entry implements Comparable<Entry>  {

        private double key;
        private String value;
        private String uId;
        private boolean ack = false;
        public Entry() {
        }

        public Entry(double key, String value, String uId, boolean ack) {
            this.key = key;
            this.value = value;
            this.uId = uId;
            this.ack = ack;
        }

        public double getKey() {
            return key;
        }

        public void setKey(double key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getuId() {  return uId; }

        public void setuId(String uId) { this.uId = uId; }

        public boolean isAck() {
            return ack;
        }

        public void setAck(boolean ack) {
            this.ack = ack;
        }

        @Override
        public int compareTo(Entry other) {
            if(this.key < other.key)
                return -1;
            else
                return 1;
        }
    }


    public Entry max(){
        Entry maxEntry =new Entry(maxKey,"","",false);

        for(Entry e: priorityQueue){
            if(e.getKey() > maxEntry.getKey()){
                if(maxKey >e.getKey()){
                    maxEntry.setKey(maxKey);
                    Log.v("Max Key1:",maxKey+"");
                }else{
                    maxEntry.setKey(e.getKey());
                    maxKey = e.getKey();
                    Log.v("Max Key2:",maxKey+"");
                }
                maxEntry.setValue(e.getValue());
                maxEntry.setuId(e.getuId());
                maxEntry.setAck(e.isAck());

            }
        }
        return maxEntry;
    }

    public boolean updateKey(Entry entry){
        Log.v("UpdateEntry:",entry.key+"#"+entry.value+"#"+entry.getuId()+"#"+entry.isAck());
        if(entry.getKey() > maxKey){
            maxKey = entry.getKey();
        }
        for(Entry e: priorityQueue){
            if(e.getuId().equalsIgnoreCase(entry.getuId())){
                priorityQueue.remove(e);
                priorityQueue.add(entry);
                Log.v("UpdatedEntry:",e.key+"#"+e.value+"#"+e.getuId()+"#"+e.isAck());
                return true;
            }
        }
        return false;
    }

    public String showPQueue(){
        String testKeys="";
        for(Entry q : priorityQueue){
            testKeys += q.isAck()+"~"+q.getuId()+"~"+q.getKey()+"~"+q.getValue()+"^^";
        }
        return testKeys;
    }


}
