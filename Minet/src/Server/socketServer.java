package Server;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.io.DataInputStream;   
import java.io.DataOutputStream;   
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class socketServer {
	ArrayList<User> clientList = new ArrayList();
	//private DataOutputStream output;
	//private DataInputStream input;
	//private String clientName;
	//private Socket socket;
	private String head_time = "Time ";
	private String head_port = "Port ";
	private String head_ip_addr = "IpAddr ";
	private String head_protocol_version = "CS1.0";
	private ServerSocket server;
	public static void main(String[] args)
	{
		//在main函数中，启动服务器的socket
		new socketServer().OpenServer();
		
	}
	
	public void OpenServer()
	{
		try
		{
			server = new ServerSocket(8888);
			
			Socket socket;
			while((socket = server.accept())!=null)
			{
				User tempUsr = new User( socket, System.currentTimeMillis() );
				tempUsr.start();
				clientList.add( tempUsr );
			}
		}
		catch(Exception e) {System.out.println(e.toString());}
	}
	
	public void sendMsgToAllUser(String msg,String method_name, String userName,String time, Socket socket){
		String res_msg = head_protocol_version + ' ' +  method_name + ' ' +  userName + "\r\n";
		String date;
		if( time == null )
		{
			SimpleDateFormat date_fmt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
			date = date_fmt.format(new Date() );
		}
		else
			date = time.split(" ")[1] + ' ' + time.split(" ")[2];
		res_msg = res_msg + head_time +  date + "\r\n" + head_ip_addr + socket.getLocalAddress().toString().split("/")[1] + "\r\n" + head_port + server.getLocalPort() + "\r\n"  + "\r\n";
		res_msg += msg;
		for( User temp : clientList ){
			try{
				if( temp.getUserName().equals("") == false )
				{
					DataOutputStream output = new DataOutputStream( temp.getSocket().getOutputStream());
					System.out.println( "res: "+ res_msg );
					output.writeUTF( res_msg);
				}
			}
			catch(Exception e){
				System.out.println(e.toString());
			}
		}
	}
	
	public boolean userLogIN(Socket socket, String userName ){
		User temp_user = null;
		for( User tempUsr : clientList ){
			if( tempUsr.getUserName().equals(userName) ){
				return false;
			}
			if( tempUsr.getSocket() == socket ){
				temp_user = tempUsr;
			}
		}
		if( temp_user == null ) return false;
		if( temp_user.getUserName().equals("") ){
			sendMsgToAllUser("LOGIN " + userName, "UPDATE", userName, null, socket);
			temp_user.setUserName( userName );
			return true;
		}
		return false;
	}
	
	public void userLogOut(User user){
		String name = user.getUserName();
		boolean tag = false;
		for( User tempUsr : clientList ){
			if( tempUsr == user ){
				tag = true;
			}
		}
		if( tag ){
			clientList.remove( user );
			sendMsgToAllUser("LOGOUT " + name, "UPDATE", name, null, user.getSocket());
		}
		
	}
	
	public String getUserList( String user_name){
		String res = "";
		for( User tempUsr : clientList ){
			if( tempUsr.getUserName().equals("") ){
				continue;
			}
			res = res + "user_name " + tempUsr.getUserName() + ' '+ "user_ipAddr " + tempUsr.getSocket().getInetAddress().toString().split("/")[1] + "\r\n";
		}
		return res;
	}
	
	
	public class User extends Thread
	{
		private Socket socket;
		private boolean isLogIn = false;
		private long last_beat_time ;
		private String user_name;
		
		public User(Socket socket, long start_time) {
			// TODO Auto-generated constructor stub
			this.socket = socket;
			this.last_beat_time = start_time;
			this.user_name = "";
		}
		
		public void setUserName(String userName){
			this.user_name = userName;
			isLogIn = true;
		}
		public String getUserName(){
			return user_name;
		}
		
		public Socket getSocket(){
			return socket;
		}
		
		public void decodeMsg(String msg){
			if( msg.indexOf("MINET") != -1 ){
				sayHello();
				return;
			}
			String data = msg.split("\r\n\r\n")[1];
			String head = msg.split("\r\n\r\n")[0];
			String request = head.split("\r\n")[0];
			String time = head.split("\r\n")[1];
			String method = request.split(" ")[1];
			String userName = request.split(" ")[2];
			
			if( method.equals("LOGIN") ){
				if( userLogIN(socket, userName) ){
					sendMsg("LOGIN","Status Succeed");
					last_beat_time = System.currentTimeMillis();
				}
				else{
					sendMsg("LOGIN","Status Failed");
				}
			}
			else if ( method.equals("LEAVE") ){
				userLogOut( this );
				sendMsg("LOGOUT", "status succeed");
			}
			else if( method.equals("GETLIST") ){
				String res = getUserList(user_name);
				sendMsg("GETLIST", res);
			}
			else if ( method.equals( "MESSAGE") ){
				sendMsgToAllUser("message " + data , "MESSAGE", user_name, time, socket);
			}
			else if( method.equals( "BEAT") ){
				this.last_beat_time = System.currentTimeMillis();
			}
			else if( method.equals("PHOTO") ){
				sendMsgToAllUser(data, "PHOTO", user_name, time , socket);
			}
		}
		
		public void sayHello(){
			try{
				DataOutputStream output = new DataOutputStream(socket.getOutputStream());
				String msg = "MIRO " + socket.getLocalAddress().toString().split("/")[1] +"\r\n"; 
				output.writeUTF( msg );
			}
			catch( Exception e){
				
			}
		}
		
		public void sendMsg(String method_name, String msg){
			try{
				DataOutputStream output = new DataOutputStream(socket.getOutputStream());
				String res_msg = head_protocol_version + ' ' +  method_name + ' ' +  user_name + "\r\n";
				SimpleDateFormat date_fmt = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
				String date = date_fmt.format(new Date() );
				res_msg = res_msg + head_time +  date + "\r\n" + head_ip_addr + socket.getLocalAddress().toString().split("/")[1] +"\r\n" + head_port + server.getLocalPort() + "\r\n"  + "\r\n";
				res_msg += msg;
				System.out.println( res_msg );
				output.writeUTF( res_msg);
			}
			catch( Exception e){
				
			}
		}
		public void run()
		{
			String msg;
			try
			{
				//String clientName = socket.getInetAddress().toString();
				DataInputStream input = new DataInputStream(socket.getInputStream());
				while( !isLogIn || ( System.currentTimeMillis() - last_beat_time <= 10000 ) )
				{
					msg = input.readUTF();
					System.out.println("recieve msg: " + msg );
					decodeMsg(msg);
				}
			}
			catch(Exception e){
				System.out.println("program ends unexpectedly");
				userLogOut(this);
				System.out.println(e.toString());}
			System.out.println( isLogIn + " the time tag is " + ( System.currentTimeMillis() - last_beat_time ));
		}
	}
	
	
}
