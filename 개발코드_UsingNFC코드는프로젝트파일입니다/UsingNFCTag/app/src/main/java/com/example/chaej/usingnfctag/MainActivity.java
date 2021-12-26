package com.example.chaej.usingnfctag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

class Unit implements Serializable {
    List<String> ls = new ArrayList();

    Unit(List<String> p) {
        if (p.isEmpty() != true) {
            for (int i = 0; i < p.size(); i++) {
                ls.add(p.get(i));
            }
        }
    }

    public Unit() {

    }
}

class ConnectedThread extends Thread{

    List<String> list = new ArrayList<>();
    Unit asdf;
    String hostname;
    int port;
    Boolean flag; // 만약 서버로부터 받는 것이면 1 서버에 전송하는 것이면 0


    public ConnectedThread(String addr,int port, List<String> list){
        hostname = addr;
        for(int i=0;i<list.size();i++){
            this.list.add(list.get(i));
        }
        this.port = port;
        flag = true;
    }
    public ConnectedThread(String addr, int port){
        hostname = addr;
        this.port = port;
        flag = false;
    }

    public void run(){
        if(flag==false)
        {
            try {
                Socket s = new Socket(hostname, port);
                //서버로 출력에 이용할 출력처리용 객체를 생성합니다.
                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                // 네트웍으로 전송할 데이터 객체 생성
                // public class Unit implements Serializable
                asdf = (Unit)ois.readObject(); //미리 생성한 unit 객체
                //list = u.ls;
                for(int i=0; i<asdf.ls.size();i++){
                    list.add(asdf.ls.get(i));
                }
                s.close();
                //서버로 전송하기위해 버퍼에 저장
            }catch (Exception e) {
                System.err.println(e);
                System.err.println("사용법: java Client <hostname> <port:4000>");
            }
        }
    else{
        try {
            Socket sock = new Socket(hostname, port);
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            Unit u = new Unit(list);
            oos.writeObject(u);
            oos.flush();
            sock.close();
        } catch (Exception e) {
        }
    }
    }

}





public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    /*  *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   */
/*  *   *   *   *   *   *   *   *   *NFC 변수 선언 *   *   *   *   *   *   *   *   *   *   */
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    Tag myTag;
    Context context;
    boolean writeMode;
    String myText;
  //tts
      private TextToSpeech myTTS;

/*  *   *   *   *   *   *   *   *   효과음 출력 관련 객체 선언   *   *   *   *   *   *   *   *   *   */



    private SoundPool sp;
    private int soundID;
    TextView tvNFCContent; //없어도 됨.
    TextView message; //Write에 사용
    Button btnWrite;
    Button Send;
/*  *   *   *   *   *   *   *   * List 관련 객체 선언   *   *   *   *   *   *   *   *   *   *   */
    List<String> list = new ArrayList<String>();
    ArrayAdapter adapter;
    ListView ls;
    List<String> log= new ArrayList<String>();
    final String host = "125.176.22.119";    //서버의 IP주소
    //집에서 한다면 공유기 포트포워딩해서 포트번호 알아올 것.
    final int port = 41231; //서버가 사용하는 포트번호

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundID = sp.load(this, R.raw.beep, 1);

        tvNFCContent = (TextView) findViewById(R.id.nfc_contents);
        message = (TextView) findViewById(R.id.edit_message);
        btnWrite = (Button) findViewById(R.id.button);
        Send = (Button) findViewById(R.id.send);

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (myTag == null) {
                        Toast.makeText(context, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    } else {
                        write(message.getText().toString(), myTag);
                        Toast.makeText(context, WRITE_SUCCESS, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, WRITE_ERROR, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        /************************************************************************
         ****************************Sending List object to Server***************
         *************************************************************************/
        Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "ready to save", Toast.LENGTH_LONG).show();
                //     ConnectedThread ct = new ConnectedThread(host, port, list);
                //   ct.start();
       /*
        try {
            //Socket sock = new Socket("125.176.22.119", 41231);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream());
            Unit u = new Unit(list);
            oos.writeObject(u);
            //oos.flush();
            oos.close();
        } catch (Exception e) {
        }
        Toast.makeText(context, "save complete", Toast.LENGTH_LONG);
    }
 */
                String fileName = "list";
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < list.size(); i++) {
                    sb.append(list.get(i));
                    sb.append(";");
                }
                FileOutputStream fos = null;
                try {
                    final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/folderName/");

                    if (!dir.exists()) {
                        if (!dir.mkdirs()) {
                            Log.e("ALERT", "could not create the directories");
                        }
                    }

                    final File myFile = new File(dir, fileName + ".txt");

                    if (!myFile.exists()) {
                        myFile.createNewFile();
                    }

                    fos = new FileOutputStream(myFile);

                    fos.write(sb.toString().getBytes());
                    fos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Toast.makeText(context, "save complete", Toast.LENGTH_LONG).show();
            }
        });


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};

        ls = (ListView) findViewById(R.id.asdf);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);

        // list.add("코끼리");
        // list.add("칠면조");

        ls.setAdapter(adapter);

/********************************************************************************
 *******************************S e r v e r *************************************
 *********************************************************************************/
        // final String host = "asdf";
        // final int port = 4000;\
        //Toast.makeText(context, "서버에 연결중..", Toast.LENGTH_LONG);
        //ConnectedThread ct = new ConnectedThread(host, port);
        //ct.start();
        /*
        if(list.isEmpty()==true){
            for(int i=0; i<ct.asdf.ls.size();i++){
                list.add(ct.asdf.ls.get(i));
            }
            Toast.makeText(context, "Load 성공", Toast.LENGTH_LONG);
        }
        */
/*
        try {
           // Socket s = new Socket(host, port);
            //서버로 출력에 이용할 출력처리용 객체를 생성합니다.
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream("dat.list"));
            // 네트웍으로 전송할 데이터 객체 생성
            // public class Unit implements Serializable
            Unit u = new Unit(list);
            u = (Unit)ois.readObject();
            //list = u.ls;
            if(u.ls.isEmpty()){

            }else{
                for(int i=0;i<u.ls.size();i++){
                    list.add(u.ls.get(i));
                }
            }

            //서버로 전송하기위해 버퍼에 저장
        }catch (Exception e) {
            System.err.println(e);
            System.err.println("사용법: java Client <hostname> <port:4000>");
        }
*/
/*

// ****************************SAVE*********************/
        final String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/folderName/list.txt";
        StringBuffer strBuffer = new StringBuffer();
        try {
            InputStream is = new FileInputStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = "";
            while ((line = reader.readLine()) != null) {
                strBuffer.append(line + "\n");
            }

            reader.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            strBuffer = new StringBuffer("");
        }
        String str = strBuffer.toString();
        str = str.substring(0, str.length() - 1);
        StringTokenizer st = new StringTokenizer(str, ";");
        for (int i = 0; st.hasMoreTokens(); i++) {
            String temp = st.nextToken();
            temp = temp.trim();
            if (temp != "")
                list.add(temp);
        }
        /*
        File file = new File("list.txt") ;
        FileReader fr = null ;
        BufferedReader bufrd = null ;
        StringBuffer sb =new StringBuffer();
        char ch ;

        try {
            // open file.
            fr = new FileReader(file) ;
            bufrd = new BufferedReader(fr) ;

            // read 1 char from file.
            while ((ch = ((char) bufrd.read())) != -1) {
             //   System.out.println("char : " + ch) ;
                sb.append(ch);
            }

            // close file.
            bufrd.close() ;
            fr.close() ;
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        StringTokenizer st = new StringTokenizer(sb.toString(), ";");
        for(int i=0;st.hasMoreTokens();i++){
            list.add(st.nextToken());
        }
        });
        */


    }
/*  *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   */
/*  *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   */
/*  *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   *   */





    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }



        tvNFCContent.setText("NFC Content: " + text);
        TAGdetected(text);

    }
/**********************************************************************
 * ******************tag detected and add or delete to/from listview****
*********************************************************************** */
    public void TAGdetected(String e){
        long now = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        sp.play(soundID, 1,1,0,0,0.5f);
        Date nowdate = new Date(now);
        String getTime = sdf.format(nowdate);

        char lastName = e.charAt(e.length() - 1);
        int index= (lastName - 0xAC00) % 28;

        if(e!=""){
            if(list.contains(e)==true){

                list.remove(list.indexOf(e)  );
                log.add("item :"+e+"removed on "+getTime);
                Toast.makeText(context, e+" removed successfully.   "+getTime, Toast.LENGTH_LONG).show();
                myText = e +(index>0?"이":"가") +"제거되었습니다.";
            }
            else{
                list.add(e);
                log.add("item :"+e+"added on "+getTime);
                Toast.makeText(context, e+" added successfully.     "+getTime, Toast.LENGTH_LONG).show();
                myText = e + (index>0?"이":"가") + "추가되었습니다.";
            }
            adapter.notifyDataSetChanged();
        }
        else{
            Toast.makeText(context, "NFC CONTENT is empty!!", Toast.LENGTH_LONG).show();
        }

        myTTS = new TextToSpeech(this, this);

    }




    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }



    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    //TTS
    public void onInit(int status) {
        myTTS.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
     //   myTTS.speak(myText2, TextToSpeech.QUEUE_ADD, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myTTS.shutdown();
    }
}



//}