package com.example.minet;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Bundle;
import android.os.StrictMode;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	private EditText username = null;
	private Button login = null;
	
	public static String serverIP = "172.18.158.237";
	public static int serverPort = 8888;
	public static Socket msocket;
	private DataOutputStream output;
	private DataInputStream input;
	public static String clientName;
	public static String userName;
	//private boolean leave = false;
	private BufferedReader stdIn;
	private String version = "CS1.0";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		
		username = (EditText)findViewById(R.id.loginName);
		login = (Button)findViewById(R.id.login);
		
		try {
			boolean  connect = false;
			while(!connect){
				msocket = new Socket(serverIP,serverPort);
				System.out.println("connect succeed!");
				
				clientName = msocket.getLocalAddress().toString();
				clientName = clientName.substring(1);
				input = new DataInputStream(msocket.getInputStream());
				output = new DataOutputStream(msocket.getOutputStream());
				stdIn = new BufferedReader(new InputStreamReader(System.in));
				
				String sayHello = "";
				sayHello += "MINET ";
				sayHello += clientName;
				sayHello += "\r\n";
				output.writeUTF(sayHello);
				String hello;
				hello = input.readUTF();
				if(Hello(hello)){
					connect = true;
					Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
				}
			}
			login.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View arg0) {
					// TODO 自动生成的方法存根
					Login();
				}
			});
		} catch (UnknownHostException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	public void Login(){
		if(!username.getText().toString().equals("")){
	    	try {
	    		String login = "";
				userName =  username.getText().toString();
				login += getRequest("LOGIN");
		    	login += getHead();
		    	login += " ";
		    	System.out.println(login);
		    	output.writeUTF(login);
		    	
		    	String respose = "";
		    	respose = input.readUTF();
		    	System.out.println(respose);
		    	if(login(respose))
		    	{
		    		Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
		    		Bundle bundle = new Bundle();
					bundle.putString("userName", userName);
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, HallActivity.class);
					intent.putExtras(bundle);
					startActivity(intent);
					finish();
		    	}
		    	else{
		    		Toast.makeText(this, "登录失败 请修改用户名", Toast.LENGTH_SHORT).show();
		    	}
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
	    	
		} else {
			System.out.println("用户名不能为空");
			Toast.makeText(MainActivity.this, "用户名不能为空", Toast.LENGTH_SHORT).show();
		}
	}
	public boolean Hello(String hello)
	{
		String type = hello.split(" ")[0];
		String rest = hello.split(" ")[1];
		String hostName = rest.split("\r\n")[0];
		System.out.println(clientName);
		System.out.println(hello);
		if(type.equals("MIRO") && hostName.equals(serverIP))
			return true;
		else
			return false;
	}
	public boolean login(String respose)
	{                                                                              
		String body = respose.split("\r\n\r\n")[1];
		//System.out.println(body); 
		if(body.indexOf("Status Succeed") != -1)
			return true;
		else
			return false;
	}
	public String getRequest(String controll){
		String request = "";
		request += version;
		request += " ";
		request += controll ;
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
		String ip = clientName;
		head += ip;
		head += "\r\n";
		
		head += "Port ";
		int port = msocket.getPort();
		head += port;
		head += "\r\n";
		head += "\r\n";
		return head;
	}
}
