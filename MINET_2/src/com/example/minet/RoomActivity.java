package com.example.minet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class RoomActivity extends Activity {
	private String version = "P2P1.0";
	private String userName = null;
	private String otherName = null;
	
	private TextView _other = null;
	private Button back = null;
	private Button send = null;
	private EditText editmess = null;
	private ListView mListView;
	private List<Map<String, Object>> mDataList = new ArrayList<Map<String, Object>>();
	private Map<String, Object> map = null;
	private DataInputStream oinput = null;
	private DataOutputStream ooutput = null;
	private DataOutputStream moutput = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO 自动生成的方法存根
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room);
		
		_other = (TextView)findViewById(R.id.othername);
		back = (Button)findViewById(R.id.back);
		send = (Button)findViewById(R.id.send);
		editmess = (EditText)findViewById(R.id.editText1);
		mListView = (ListView)findViewById(R.id.chatView);
		Bundle bundle = this.getIntent().getExtras();
		userName = bundle.getString("userName");
		otherName = bundle.getString("otherName");
		
		_other.setText(otherName);
		
		try {
			oinput = new DataInputStream(HallActivity.osocket.getInputStream());
			ooutput = new DataOutputStream(HallActivity.osocket.getOutputStream());
			moutput = new DataOutputStream(MainActivity.msocket.getOutputStream());
			
			new Thread(new Listener()).start();
			new Thread(new beat()).start();
			back.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO 自动生成的方法存根
					try {
						Bundle bundle = new Bundle();
						bundle.putString("userName", userName);
						
						String str = "";
						str += getRequest("LEAVE");
						str += getHead();
						str += "LEAVE";
						ooutput.writeUTF(str);
						
						HallActivity.osocket.close();
						
						Intent intent = new Intent();
						intent.setClass(RoomActivity.this, HallActivity.class);
						intent.putExtras(bundle);
						startActivity(intent);
						finish();
					} catch (IOException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}
			});
			send.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO 自动生成的方法存根
					try {
						String userInput = "";
						userInput += getRequest("P2PMESSAGE");
						userInput += getHead();
						userInput += editmess.getText().toString();
						System.out.println(userInput);
						String user = userInput.split("\r\n")[0].split(" ")[2];
						String data = userInput.split("\r\n\r\n")[1];
						String head = userInput.split("\r\n\r\n")[0];
						String time = head.split("\r\n")[1].split(" ")[2];
						
						map = new HashMap<String, Object>();
						map.put("name", user);
						map.put("mess", data);
						map.put("time", time);
						mDataList.add(map);
						
						Message _msg = new Message();
						Bundle _bundle = new Bundle();
						_bundle.putString("do", "chatList");
						_msg.setData(_bundle);
						update.sendMessage(_msg);
						
						editmess.setText("");
						ooutput.writeUTF(userInput);
					} catch (IOException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
					}
				}
			});
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
	}
	public String getRequest(String controll){
		String request = "";
		request += version;
		request += " ";
		request += controll;
		request += " ";
		request += userName;
		request += "\r\n";
		return request;
	}
	public String getHead()
	{
		String head = "";
		head += "Time ";
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String Date = date.format(new Date());
        head += Date;
        head += "\r\n";
        
        head += "IpAddr ";
		head += HallActivity.myIP;
		head += "\r\n";
		
		head += "Port ";
		int port = HallActivity.osocket.getPort();
		head += port;
		head += "\r\n";
		head += "\r\n";
		return head;
	}
	class Listener implements Runnable {

		@Override
		public void run() {
			// TODO 自动生成的方法存根
			try {
				while(true){
					String msg;
					msg = oinput.readUTF();
					decodeMsg(msg);
				}
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
				
		}
		
	}
	public void decodeMsg(String msg){
		String data = msg.split("\r\n\r\n")[1];
		String head = msg.split("\r\n\r\n")[0];
		String request = head.split("\r\n")[0];
		String time = head.split("\r\n")[1].split(" ")[2];
		String method = request.split(" ")[1];
		String userName = request.split(" ")[2];
		System.out.println(msg);
		if ( method.equals("LEAVE") ){
			try {
				Bundle bundle = new Bundle();
				bundle.putString("userName", userName);
				
				String getList = "";
				getList += getRequest("GETLIST");
				getList += getHead();
				getList += " ";
				moutput.writeUTF(getList);
				
				Intent intent = new Intent();
				intent.setClass(RoomActivity.this, HallActivity.class);
				intent.putExtras(bundle);
				startActivity(intent);
				finish();
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
		}
		else if ( method.equals("P2PMESSAGE") ){
			System.out.println(data);
			System.out.println(time);
			map = new HashMap<String, Object>();
			map.put("name", userName);
			map.put("mess", data);
			map.put("time", time);
			mDataList.add(map);
			
			Message _msg = new Message();
			Bundle _bundle = new Bundle();
			_bundle.putString("do", "chatList");
			_msg.setData(_bundle);
			update.sendMessage(_msg);
		}
		else if( method.equals( "BEAT") ){
			//this.last_beat_time = System.currentTimeMillis();
		}
	}
	private Handler update = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Bundle dataBundle = msg.getData();
			String str = dataBundle.getString("do");
			if(str.equals("chatList"))
				updateMessage();
		}
	};
	public void updateMessage(){
		System.out.println(mDataList);
		SimpleAdapter listItemAdapter = new SimpleAdapter(this, 
				mDataList, 
				R.layout.message,
				new String[] { "name", "mess", "time"},
				new int[] { R.id.name, R.id.mess, R.id.time } 
		);
		mListView.setAdapter(listItemAdapter);
	}
	public class beat extends Thread
	{
		public void run()
		{
			try
			{
				while(true)
				{
					beat.sleep(10000);
					String str = "";
					str += getRequest("BEAT");
					str += getHead();
					str += "登广小JJ";
					ooutput.writeUTF(str);
			 }
			}
			catch (Exception e)
			{
				 System.out.println("sorry");
			}
		}
	}
}
