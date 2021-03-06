package com.example.latitude6410.nfcbuisness;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NfcAdapter.OnNdefPushCompleteCallback,
        NfcAdapter.CreateNdefMessageCallback {
    // The array lists to hold our messages
    private ArrayList<String> messagesToSendArray = new ArrayList<>();
    private ArrayList<String> messagesReceivedArray = new ArrayList<>();

    //Text boxes to add and display our messages
    private EditText txtBoxAddMessage;
    private TextView txtReceivedMessages;
    private TextView txtMessagesToSend;

    //Button to add message
    private Button btnAddMessage;


    private NfcAdapter mNfcAdapter;


    public void addMessage(View view) {

        // String object that contains text from the editable txtBoxAddMessage
        String newMessage = txtBoxAddMessage.getText().toString();

        // Add newMessage to the messagesToSend ArrayList
        messagesToSendArray.add(newMessage);

        txtBoxAddMessage.setText(null);
        updateTextViews();

        Toast.makeText(this, "Added Message", Toast.LENGTH_LONG).show();
    }

    private void updateTextViews() {
        txtMessagesToSend.setText("Messages To Send:\n");

        //Populate Our list of messages we want to sed
        if (messagesToSendArray.size() > 0) {
            for (int index = 0; index < messagesToSendArray.size(); index++) {
                txtMessagesToSend.append(messagesToSendArray.get(index));
                txtMessagesToSend.append("\n");
            }
        }

        txtReceivedMessages.setText("Messages Recieved:\n");

        // Populate our list of message we have received
        if (messagesReceivedArray.size() > 0) {
            for (int index = 0; index < messagesReceivedArray.size(); index++) {
                txtReceivedMessages.append(messagesReceivedArray.get(index));
                txtReceivedMessages.append("\n");
            }
        }
    }

    //Save our Array Lists of Messages for if the user navigates away
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstatnceState) {
        super.onSaveInstanceState(savedInstatnceState);
        savedInstatnceState.putStringArrayList("messagesToSend", messagesToSendArray);
        savedInstatnceState.putStringArrayList("lastMessagesReceived", messagesReceivedArray);
    }

    //Load our Array Lists of  Messages for when the user navigates back
    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        messagesToSendArray = savedInstanceState.getStringArrayList("messagesToSend");
        messagesReceivedArray = savedInstanceState.getStringArrayList("lastMessagesReceived");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtBoxAddMessage = (EditText) findViewById(R.id.txtBoxAddMessage);
        txtMessagesToSend = (TextView) findViewById(R.id.txtMessageToSend);
        txtReceivedMessages = (TextView) findViewById(R.id.txtMessagesReceived);
        btnAddMessage = (Button) findViewById(R.id.buttonAddMessage);

        btnAddMessage.setText("Add Message");
        updateTextViews();

        // Check if NFC is available on device
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            //This will refer back to createNdefMessage for what it will send
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            //This will be called if the message is sent successfully
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        } else {
            Toast.makeText(this, "NFC not available on this device",
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        //This is called when the system detaects that our NdefMessage was sent
        //successfully
        messagesToSendArray.clear();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        //This will be called when another NFC capable device is detected.
        if (messagesToSendArray.size() == 0)
            return null;

        //We'll write the createRecords() method in just a moment
        NdefRecord[] recordsToAttach = createRecords();

        //When creating an NdefMessage we need to provide an NdefRecord[]
        return new NdefMessage(recordsToAttach);
    }

    public NdefRecord[] createRecords() {
        NdefRecord[] records = new NdefRecord[messagesToSendArray.size() + 1];
        //To Create Messages Manually if API is less than
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            for (int i = 0; i < messagesToSendArray.size(); i++) {
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));
                NdefRecord record = new NdefRecord(
                        NdefRecord.TNF_WELL_KNOWN,      //Our 3-bit Type name format
                        NdefRecord.RTD_TEXT,            //Description of our payload
                        new byte[0],                    //The optional id for our Record
                        payload);                       //Our payload for the Record

                records[i] = record;
            }
        }
        //Api is high enough that we can use createMime, which is preferred.
        else {
            for (int i = 0; i < messagesToSendArray.size(); i++) {
                byte[] payload = messagesToSendArray.get(i).
                        getBytes(Charset.forName("UTF-8"));

                NdefRecord record = NdefRecord.createMime("text/plain", payload);
                records[i] = record;
            }
        }
        records[messagesToSendArray.size()] =
                NdefRecord.createApplicationRecord(getPackageName());
        return records;
    }

    private void handleNfcIntent (Intent NfcIntent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(NfcIntent.getAction())) {
            Parcelable[] recievedArray =
                    NfcIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(recievedArray != null) {
                messagesReceivedArray.clear();
                NdefMessage receivedMessage = (NdefMessage) recievedArray[0];
                NdefRecord[] attachedRecords = receivedMessage.getRecords();

                for (NdefRecord record:attachedRecords) {
                    String string = new String(record.getPayload());

                    //Make sure we don't pass along our Android Application Record
                    if (string.equals(getPackageName()))
                        continue;
                    messagesReceivedArray.add(string);
                }
                Toast.makeText(this, "Received " + messagesReceivedArray.size() +
                        " Messages", Toast.LENGTH_LONG).show();
                updateTextViews();
            }
            else {
                Toast.makeText(this, "Received Blank Parcel", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTextViews();
        handleNfcIntent(getIntent());
    }

}