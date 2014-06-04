package it.mathchallenger.server.admin;

import it.mathchallenger.server.communication.SocketServer;
import it.mathchallenger.server.controls.ranking.Ranking;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

public class AdminSocketService extends Thread {
	private Socket comm;
	private BufferedReader in;
	private OutputStream output;
	private final static int TIMEOUT_PING = 60000;
	private boolean logged=false;
	
	public AdminSocketService(Socket s){
		comm=s;
		try {
			in=new BufferedReader(new InputStreamReader(comm.getInputStream()));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			output=comm.getOutputStream();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void run(){
		int timer_ping = 0;
		while (!comm.isClosed() && comm.isBound()) {
			try {
				String str;
				if ((str=in.readLine())!=null) {
					String[] cmd=str.trim().split(" ");
					switch(cmd[0]){
						case "logout":
							if(cmd.length==1){
								if(!logged){
									OutputWrite("logout=error;message=Non sei loggato");
									break;
								}
								OutputWrite("logout=OK");
								comm.close();
								break;
							}
							else {
								OutputWrite("logout=error;message=Usage: logout");
							}
							break;
						case "login":
							if(cmd.length==3){
								String u=cmd[1];
								String p=cmd[2];
								if(login(u,p)){
									logged=true;
									OutputWrite("login=OK");
								}
								else {
									logged=false;
									OutputWrite("login=error;message=Invalid username or password");
								}
								break;
							}
							else {
								OutputWrite("login=errror;message=Usage: login username password");
								break;
							}
						case "ranking_reload":
							if(cmd.length==1){
								if(!logged){
									OutputWrite("ranking_reload=error;message=Non sei loggato");
									break;
								}
								Ranking.readProperties();
								OutputWrite("ranking_reload=OK");
								break;
							}
							else {
								OutputWrite("ranking_reload=error;message=Usage: ranking_reload");
							}
							break;
						case "list_users":
							if(cmd.length==1){
								if(!logged){
									OutputWrite("list_users=error;message=Non sei loggato");
									break;
								}
								StringBuilder resp=new StringBuilder("list_users=OK;loggati="+SocketServer.thread_utenti_attivi.activeCount());
								Thread[] list=new Thread[SocketServer.thread_utenti_attivi.activeCount()];
								SocketServer.thread_utenti_attivi.enumerate(list);
								for(int i=0;i<list.length;i++){
									resp.append(";utente="+list[i].getName());
								}
								OutputWrite(resp.toString());
								break;
							}
							else
								OutputWrite("list_users=error;message=Usage: list_users");
							break;
						case "kick_user":
							break;
						case "ban_user":
							break;
						case "delete_user":
							break;
					}
				}
				else {
					timer_ping += 200;
				}
			}
			catch(IOException e){
				
			}
			try {
				sleep(200);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(timer_ping>TIMEOUT_PING)
				break;
		}
		System.out.println("Thread admin terminato");
	}
	private void OutputWrite(String s) throws IOException {
		if (!s.endsWith("\n"))
			s += "\n";
		output.write(s.getBytes());
		output.flush();
	}
	private boolean login(String u, String p){
		Properties prop=new Properties();
		InputStream in;
		try {
			in = new FileInputStream("admin.properties");
			prop.load(in);
			in.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		boolean login;
		if(u.compareTo(prop.getProperty("username"))==0 && p.compareTo(prop.getProperty("password"))==0)
			login=true;
		else
			login = false;
		prop.clear();
		return login;
	}
}