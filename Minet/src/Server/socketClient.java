package Server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;   
import java.io.DataOutputStream;   
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class socketClient
{
	private DataOutputStream output;
	private DataInputStream input;
	private String clientName;
	
	public static void main(String[] args)
	{
		//在main函数中，启动服务器的socket
		new socketClient().ConnectServer();
	}
	
	public void ConnectServer()
	{
		try
		{
			System.out.println( "testing " );
			Socket socket = new Socket("172.18.182.145",8888);
			clientName = socket.getInetAddress().toString();
			System.out.println( "testing " + clientName );
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
			
			new readServer().start();
			new writeServer().start();
		}
		catch(Exception e)	{System.out.println(e.toString());}
	}
	
	public class readServer extends Thread
	{
		private Socket client;

		public void run()
		{
			String msg;
			try
			{
				while(true)
				{
					msg = input.readUTF();
					if(msg!=null)
						System.out.println("收到消息：【"+clientName+"】 "+msg);	
				}
			}
			catch(Exception e) {System.out.println(e.toString());}
		}	
	}
	
	public class writeServer extends Thread
	{
		private Socket client;

		
		public void run()
		{
			try
			{
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				String userInput;
				while(true)
				{
					if(stdIn.ready())
					{
						userInput = stdIn.readLine();
						if(userInput!="exit")
						{
							output.writeUTF(userInput);
							System.out.println("已发送消息给【"+clientName+"】"+userInput + '\n');
						}
					}
				}
			}
			catch(Exception e) {System.out.println(e.toString());}
		}
	}
	
}