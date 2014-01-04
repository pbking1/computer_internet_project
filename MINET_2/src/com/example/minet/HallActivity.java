package com.example.minet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class HallActivity extends Activity {
	
	public static Socket osocket;
	public static String myIP = MainActivity.clientName;
	public static String otherIP;
	public static int otherPort = 10001;
	private String username = MainActivity.userName;
	private String version = "CS1.0";
	private ListView chatViewList;
	private EditText editmess;
	private Button send;
	private Button logout;
	private ListView mListView;
	private List<Map<String, Object>> mchatList = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> mDataList = new ArrayList<Map<String, Object>>();
	private Map<String, Object> map = null;
	private boolean leave = false;
	private DataOutputStream moutput;
	private DataInputStream minput;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO 自动生成的方法存根
		super.onCreate(savedInstanceState);
		setContentView(R.layout.hall);
		
		try {
			chatViewList = (ListView)findViewById(R.id.chatViewlist);
			editmess = (EditText)findViewById(R.id.editText1);
			send = (Button)findViewById(R.id.button1);
			logout = (Button)findViewById(R.id.logout);
			mListView = (ListView)findViewById(R.id.listView);
			
			minput = new DataInputStream(MainActivity.msocket.getInputStream());
			moutput = new DataOutputStream(MainActivity.msocket.getOutputStream());
			
			Bundle bundle = this.getIntent().getExtras();
			username = bundle.getString("userName");
			
			String getList = "";
			getList += getRequest("GETLIST");
			getList += getHead();
			getList += " ";
			moutput.writeUTF(getList);
			
			new readServer().start();
    		new beat().start();
    		new ListenerRequest().start();
    		
			mListView.setOnItemClickListener(mItemClickListener);
			send.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO 自动生成的方法存根
					if(!leave)
					{
						try {
							String userInput = "";
							userInput += getRequest("MESSAGE");
							userInput += getHead();
							userInput += editmess.getText().toString();
							editmess.setText("");
							moutput.writeUTF(userInput);
						} catch (IOException e) {
							// TODO 自动生成的 catch 块
							e.printStackTrace();
						}
					}
				}
			});
			logout.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO 自动生成的方法存根
					try {
						String logout = "";
						logout += getRequest("LEAVE");
						logout += getHead();
						logout += "LEAVE";
						moutput.writeUTF(logout);
						MainActivity.msocket.close();
						
						Intent intent = new Intent();
						intent.setClass(HallActivity.this, MainActivity.class);
						startActivity(intent);
						finish();
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
	public class readServer extends Thread
	{
		public void run()
		{
			String msg = "";
			try
			{
				while(!leave)
				{
					msg = minput.readUTF();
					decodemsg(msg);
				}
			}
			catch(Exception e) {
				System.out.println(e.toString());
			}
			System.out.println( "read thread out");
		}	
	}
	public class ListenerRequest extends Thread {
		ServerSocket server;

		public void run() {
			try {
				server = new ServerSocket(otherPort);
				while((osocket = server.accept())!=null){
					DataInputStream oinput = new DataInputStream(osocket.getInputStream());
					DataOutputStream ooutput = new DataOutputStream(osocket.getOutputStream());
					
					String hello;
					hello = oinput.readUTF();
					String type = hello.split(" ")[0];
					String rest = hello.split(" ")[1];
					String hostName = rest.split("\r\n")[0];
					
					if(type.equals("MINET")){
						String sayHello = "";
						sayHello += "MINET ";
						sayHello += username;
						sayHello += "\r\n";
						ooutput.writeUTF(sayHello);
					
						Bundle bundle = new Bundle();
						bundle.putString("userName", (String) username);
						bundle.putString("otherName", hostName);
						
						Intent intent = new Intent();
						intent.setClass(HallActivity.this, RoomActivity.class);
						intent.putExtras(bundle);
						startActivity(intent);
						finish();
					}
				}
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
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
			else if(str.equals("userList"))
				updateUserList();	
		}
	};
	public void updateUserList(){
		System.out.println(mDataList);
		SimpleAdapter listItemAdapter = new SimpleAdapter(this, 
				mDataList, 
				R.layout.user,
				new String[] { "name" },
				new int[] { R.id.item_name } 
		);
		mListView.setAdapter(listItemAdapter);
	}
	public void updateMessage(){
		System.out.println(mchatList);
		SimpleAdapter listItemAdapter = new SimpleAdapter(this, 
				mchatList, 
				R.layout.message,
				new String[] { "name", "mess", "time"},
				new int[] { R.id.name, R.id.mess, R.id.time } 
		);
		chatViewList.setAdapter(listItemAdapter);
	}
	private OnItemClickListener mItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			
			otherIP = (String) mDataList.get(arg2).get("ip");
			String otherName = (String) mDataList.get(arg2).get("name");
			try {
				osocket = new Socket(otherIP, otherPort);
				DataInputStream oinput = new DataInputStream(osocket.getInputStream());
				DataOutputStream ooutput = new DataOutputStream(osocket.getOutputStream());
				String sayHello = "";
				sayHello += "MINET ";
				sayHello += username;
				sayHello += "\r\n";
				ooutput.writeUTF(sayHello);
				
				String hello;
				hello = oinput.readUTF();
				String type = hello.split(" ")[0];
				String rest = hello.split(" ")[1];
				String hostName = rest.split("\r\n")[0];
				System.out.println(hello);
				if(type.equals("MINET") && otherName.equals(hostName)){
					Bundle bundle = new Bundle();
					bundle.putString("userName", username);
					bundle.putString("otherName", hostName);
					
					Intent intent = new Intent();
					intent.setClass(HallActivity.this, RoomActivity.class);
					intent.putExtras(bundle);
					startActivity(intent);
					finish();
				}	
			} catch (UnknownHostException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
	
		}
	};
	public void decodemsg(String msg)
	{
		String request = msg.split(" ")[1];
		String body = msg.split("\r\n\r\n")[1];
		if(request.equals("LOGOUT"))
		{
			if(body.indexOf("status succeed") != -1)
			{
				leave = true;
				Toast.makeText(this, "您已离线", Toast.LENGTH_SHORT).show();
			}
		}
		
		if(request.equals("MESSAGE"))
		{
			String user = msg.split("\r\n")[0].split(" ")[2];
			System.out.println(user);
			String headTime = msg.split("\r\n")[1];
			String time = headTime.split(" ")[2];
			System.out.println(time);
			String message = body.split(" ")[1];
			System.out.println("收到消息:"+ message + "\r\n");
			
			map = new HashMap<String, Object>();
			map.put("name", user);
			map.put("mess", message);
			map.put("time", time);
			mchatList.add(map);
			
			Message _msg = new Message();
			Bundle _bundle = new Bundle();
			_bundle.putString("do", "chatList");
			_msg.setData(_bundle);
			update.sendMessage(_msg);
		}
		
		if(request.equals("UPDATE"))
		{
			String name = body.split(" ")[1];
			String ip = body.split(" ")[2];
			String status = body.split(" ")[0];
			System.out.print(ip);
			System.out.print(name+" ");
			
			if(status.equals("LOGOUT")){
				System.out.println("下线啦！");
				//List<Map<String, Object>> 
				map = new HashMap<String, Object>();
				map.put("name", name);
				map.put("ip", ip);
				map.put("port", otherPort);
				mDataList.remove(map);
				Message _msg = new Message();
				Bundle _bundle = new Bundle();
				_bundle.putString("do", "userList");
				_msg.setData(_bundle);
				update.sendMessage(_msg);
			}
			else {
				System.out.println("上线啦！");
				map = new HashMap<String, Object>();
				map.put("name", name);
				map.put("ip", ip);
				map.put("port", otherPort);
				mDataList.add(map);
				Message _msg = new Message();
				Bundle _bundle = new Bundle();
				_bundle.putString("do", "userList");
				_msg.setData(_bundle);
				update.sendMessage(_msg);
			}
		}
		System.out.println(request);
		if(request.equals("GETLIST"))
		{
			String[] user = body.split("\r\n");
			System.out.println(body);
			mDataList.clear();
			for(int i= 0;i < user.length;i++)
			{
				String name = user[i].split(" ")[1];
				String ip = user[i].split(" ")[3];
				map = new HashMap<String, Object>();
				map.put("name", name);
				map.put("ip", ip);
				map.put("port", otherPort);
				mDataList.add(map);
				System.out.println(mDataList);
			}
			Message _msg = new Message();
			Bundle _bundle = new Bundle();
			_bundle.putString("do", "userList");
			_msg.setData(_bundle);
			update.sendMessage(_msg);
			//updateUserList();
		}
	}
	public class beat extends Thread
	{
		public void run()
		{
			
			try
			{
				while(!leave)
				{
					beat.sleep(10000);
					String str = "";
					str += getRequest("BEAT");
					str += getHead();
					str += "登广小JJ";
					moutput.writeUTF(str);
				}
			}
			catch (Exception e)
			{
				 System.out.println("leave is " + leave);
				 System.out.println("sorry");
			}
		}
	}
	public String getRequest(String controll){
		String request = "";
		request += version;
		request += " ";
		request += controll ;
		request += " ";
		request += username;
		request += "\r\n";
		return request;
	}
	
	public String getBody(String body){
		
		return body;
		
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
		String ip = myIP;
		head += ip;
		head += "\r\n";
		
		head += "Port ";
		int port = MainActivity.msocket.getPort();
		head += port;
		head += "\r\n";
		head += "\r\n";
		return head;
	}
}
