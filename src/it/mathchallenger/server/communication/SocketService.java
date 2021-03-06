package it.mathchallenger.server.communication;

import it.mathchallenger.server.controls.Bot;
import it.mathchallenger.server.controls.DBAccount;
import it.mathchallenger.server.controls.DBPartita;
import it.mathchallenger.server.controls.DBStatistiche;
import it.mathchallenger.server.controls.GestionePartite;
import it.mathchallenger.server.controls.ranking.Ranking;
import it.mathchallenger.server.controls.version.VersionCheck;
import it.mathchallenger.server.entities.Account;
import it.mathchallenger.server.entities.Domanda;
import it.mathchallenger.server.entities.Partita;
import it.mathchallenger.server.entities.Statistiche;
import it.mathchallenger.server.errors.ListaErrori;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class SocketService extends Thread {
	private Socket		comm;
	private InputStream   input;
	private OutputStream  output;
	private static int	PING_TIMEOUT = 60000;
	private static int TIME_PING = 200;
	
	private Account	   account;
	private boolean	   client_version_ok=false;
	private int timer_ping = 0;
	private byte[] buffer;
	private String str_buffer;
	
	private final static int LIST_LAST_USER_SIZE=5;
	public static final long TIME_KEEP_USER_VISIBLE = 30 * 60 * 1000; //30 minuti
	private ArrayList<Account> ultime_partite;

	public SocketService(Socket com) {
		comm = com;
		try {
			input = com.getInputStream();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			output = com.getOutputStream();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		ConnectionsCount.connectionEnabled();
	}
	
	private boolean closeConnection() {
		if(comm==null || comm.isClosed())
			return true;
		try {
			comm.shutdownInput();
			comm.shutdownOutput();
			comm.close();
			buffer=null;
			str_buffer=null;
			if(ultime_partite!=null)
				ultime_partite.clear();
			ultime_partite=null;
			rand=null;
			ConnectionsCount.connectionClosed();
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void OutputWrite(String s) throws IOException {
		if (!s.endsWith("\n"))
			s += "\n";
		output.write(s.getBytes());
		output.flush();
	}
	private String readLine(byte[] buffer) throws IOException{
		if(input.available()>0){
			int read=input.read(buffer);
			if(read>0){
				return new String(buffer, 0, read);
			}
		}
		return null;
	}

	private Random rand = new Random();
	private int[] randomInteri(int s) {
		ArrayList<Integer> resps = new ArrayList<Integer>(s);
		for (int i = 0; i < s; i++)
			resps.add(i);
		int[] rand = new int[s];
		int i = 0;
		while (!resps.isEmpty()) {
			rand[i] = resps.remove(this.rand.nextInt(resps.size()));
			i++;
		}
		return rand;
	}
	
	public String getUsername(){
		if(account == null)
			return "NonLoggato";
		return account.getUsername();
	}
	private String data(){
		String data=null;
		Calendar d=Calendar.getInstance();
		int anno=d.get(Calendar.YEAR);
		int mese=d.get(Calendar.MONTH)+1;
		int giorno=d.get(Calendar.DAY_OF_MONTH);
		int ore=d.get(Calendar.HOUR_OF_DAY);
		int minuti=d.get(Calendar.MINUTE);
		int secondi=d.get(Calendar.SECOND);
		int milli=d.get(Calendar.MILLISECOND);
		String milli_s="";
		if(milli<10)
			milli_s="00"+milli;
		else if(milli<100)
			milli_s="0"+milli;
		else
			milli_s=""+milli;
		data=(anno+"-"+(mese<10?"0"+mese:mese)+"-"+(giorno<10?"0"+giorno:giorno)+" "+(ore<10?"0"+ore:ore)+":"+(minuti<10?"0"+minuti:minuti)+":"+(secondi<10?"0"+secondi:secondi)+"."+milli_s);
		return data;
	}
	public void run() {
		str_buffer = null;
		buffer = new byte[1024];
		boolean commandOK=true;
		while (!comm.isClosed() && comm.isBound()) {
			try {
				if ((str_buffer=readLine(buffer))!=null) {
					str_buffer = str_buffer.trim();
					//System.out.println("["+data()+"]"+"Comando arrivato [len="+str_buffer.length()+"]: "+str_buffer);
					String[] cmd = str_buffer.split(" ");
					commandOK=true;
					switch (cmd[0]) {
						case "validateVersion":
							validateVersion(cmd);
							break;
						case "ping":
							ping(cmd);
							break;
						case "exit":
							exit(cmd);
							break;
						case "login":
							login(cmd);
							break;
						case "login-authcode":
							loginAuthcode(cmd);
							break;
						case "logout":
							logout(cmd);
							break;
						case "change-psw":
							change_psw(cmd);
							break;
						case "reset-psw":
							reset_psw(cmd);
							break;
						case "register":
							register(cmd);
							break;
						case "getPartiteInCorso":
							getPartiteInCorso(cmd);
							break;
						case "newgame":
							newGame(cmd);
							break;
						case "newgame-random":
							newGameRandom(cmd);
							break;
						case "getDettagliPartita":
							getDettagliPartita(cmd);
							break;
						case "abandon":
							abandon(cmd);
							break;
						case "answer":
							answer(cmd);
							break;
						case "addfriend":
							addFriend(cmd);
							break;
						case "removefriend":
							removeFriend(cmd);
							break;
						case "getMyFriends":
							getMyFriends(cmd);
							break;
						case "search-user":
							searchUser(cmd);
							break;
						case "getStatistiche":
							getStatistiche(cmd);
							break;
						case "getDomande":
							getDomande(cmd);
							break;
						case "":
						default:
							commandOK=false;
							break;
					}
					str_buffer=null;
					
					if(commandOK)
						timer_ping=0;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				break;
			}
			
			//System.out.print("["+data()+"]");
			//System.out.println("Current time ping: "+timer_ping+"/"+PING_TIMEOUT);
			try {
				//System.out.print("["+data()+"]");
				//System.out.println("sleep_"+TIME_PING);
				Thread.sleep(TIME_PING);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			
			timer_ping += TIME_PING;
			//System.out.println("Incremento il ping: "+timer_ping);
			
			if (timer_ping > PING_TIMEOUT) {
				break;
			}
		}
		
		System.out.print("["+data()+"]");
		if (account != null) {
			System.out.println("Termine thread: " + account.getUsername());
			//GestionePartite.getInstance().esceUtente(account);
		}
		else {
			System.out.println("Termine thread connessione");
		}
		closeConnection();
		if(account!=null){
			Thread t = new ThreadQuit(account);
			t.start();
		}
	}
	
	private void validateVersion(String[] cmd) throws IOException{
		if(cmd.length==2){
			int versione=Integer.parseInt(cmd[1]);
			if(VersionCheck.getInstance().isVersionOK(versione)){
				client_version_ok=true;
				OutputWrite("validateVersion=OK");
			}
			else
				OutputWrite("validateVersion=error;message="+ListaErrori.VERSIONE_NON_VALIDA);					
		}
		else
			OutputWrite("validateVersion=error;message=Usage: validateVersion version");
	}
	private void ping(String[] cmd) throws IOException{
		if (cmd.length == 1) {
			timer_ping = 0;
			OutputWrite("ping=ok");
		}
	}
	private void exit(String[] cmd) throws IOException {
		if (cmd.length == 1) {
			if (account == null) {
				OutputWrite("exit=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				OutputWrite("exit=OK");
				//GestionePartite.getInstance().esceUtente(account);
				closeConnection();
			}
		}
		else
			OutputWrite("exit=error;message=Usage: exit");
	}
	private void login(String[] cmd) throws IOException {
		if (cmd.length == 3) {
			if (!client_version_ok){
				OutputWrite("login=error;message="+ListaErrori.VERSIONE_NON_VALIDA);
			}
			if (account != null) {
				OutputWrite("login=error;message="+ListaErrori.SEI_LOGGATO);
			}
			else {
				timer_ping = 0;
				String user = cmd[1].trim();
				String pass = cmd[2].trim();
				account = DBAccount.getInstance().login(user, pass);
				if (account != null) {
					GestionePartite.getInstance().entraUtente(account);
					ultime_partite=new ArrayList<Account>(LIST_LAST_USER_SIZE);
					OutputWrite("login=OK;authcode=" + account.getAuthCode() + ";id=" + account.getID());
				}
				else
					OutputWrite("login=error");
			}
		}
		else
			OutputWrite("login=error;message=Usage: login username password");
	}
	private void loginAuthcode(String[] cmd) throws IOException{
		if (cmd.length == 3) {
			if (!client_version_ok){
				OutputWrite("login=error;message="+ListaErrori.VERSIONE_NON_VALIDA);
			}
			
			if (account != null) {
				OutputWrite("login=error;message="+ListaErrori.SEI_LOGGATO);
			}
			else {
				timer_ping = 0;
				int id = Integer.parseInt(cmd[1].trim());
				String auth = cmd[2].trim();
				account = DBAccount.getInstance().login(id, auth);
				if (account != null) {
					GestionePartite.getInstance().entraUtente(account);
					ultime_partite=new ArrayList<Account>(LIST_LAST_USER_SIZE);
					OutputWrite("login=OK");
				}
				else
					OutputWrite("login=error;message="+ListaErrori.INVALID_AUTHCODE);
			}
		}
		else
			OutputWrite("login=error;message=Usage: login-auth id authcode");
	}
	private void logout(String[] cmd) throws IOException {
		if (cmd.length == 1) {
			if (account == null) {
				OutputWrite("logout=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
				
			}
			else {
				GestionePartite.getInstance().esceUtente(account);
				/*boolean logout = */DBAccount.getInstance().logout(account.getUsername(), account.getAuthCode());
				/*
				if (logout) {
					GestionePartite.getInstance().esceUtente(account);
					OutputWrite("logout=OK");
					closeConnection();
				}
				else
					OutputWrite("logout=error");
				*/
				OutputWrite("logout=OK");
				closeConnection();
			}
		}
		else
			OutputWrite("logout=error;message=Usage: logout");
	}
	private void change_psw(String[] cmd) throws IOException {
		if (cmd.length == 3) {
			if (account == null) {
				OutputWrite("change-psw=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				String oldPass = cmd[1].trim();
				String newPass = cmd[2].trim();
				Account acc = DBAccount.getInstance().login(account.getUsername(), oldPass);
				if (acc == null) {
					OutputWrite("change-psw=error;message="+ListaErrori.VECCHIA_PASSWORD_ERRATA);
				}
				else {
					boolean change = DBAccount.getInstance().changePassword(account, newPass);
					if (change)
						OutputWrite("change-psw=OK");
					else
						OutputWrite("change-psw=error");
				}
			}
		}
		else
			OutputWrite("change-psw=error;message=Usage: change-psw newpassword");
	}
	private void reset_psw(String[] cmd) throws IOException{
		if (cmd.length == 2) {
			boolean reset = DBAccount.getInstance().resetPasswordByUsername(cmd[1]);
			OutputWrite("reset-psw=" + (reset == true ? "OK" : "error"));
		}
		else
			OutputWrite("reset-psw=error;message=Usage: reset-psw username");
	}
	private void register(String[] cmd) throws IOException{
		if (cmd.length == 4) {
			if (account != null) {
				OutputWrite("register=error;message="+ListaErrori.SEI_LOGGATO);
			}
			else {
				String user = cmd[1];
				String pass = cmd[2];
				String email = cmd[3];
				DBAccount dba = DBAccount.getInstance();
				if (dba.isAccountExist(user))
					OutputWrite("register=error;message="+ListaErrori.USERNAME_IN_USO);
				else {
					account = dba.registra(user, pass, email);
					if (account != null) {
						GestionePartite.getInstance().entraUtente(account);
						OutputWrite("register=OK;authcode=" + account.getAuthCode() + ";id=" + account.getID());
					}
					else
						OutputWrite("register=error");
				}
			}
		}
		else
			OutputWrite("register=error;message=Usage: register username password email");
	}
	private void getPartiteInCorso(String[] cmd) throws IOException {
		if (cmd.length == 1) {
			if (account == null) {
				OutputWrite("getPartiteInCorso=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				ArrayList<Partita> partite = DBPartita.getInstance().getPartiteByUser(account.getID());
				StringBuilder res = new StringBuilder("getPartiteInCorso=OK");
				if (partite.size() > 0) {
					for (int i = 0; i < partite.size(); i++) {
						Partita p = partite.get(i);
						Account sfidato = null;
						if (p.getIDUtente2() <= 0) {
							sfidato = GestionePartite.getInstance().getBotRandom();
						}
						else {
							int id_s = account.getID() == p.getIDUtente1() ? p.getIDUtente2() : p.getIDUtente1();
							sfidato = DBAccount.getInstance().getAccountByID(id_s);
						}
						if (sfidato == null)
							continue;
						boolean inAttesa=false;
						if(p.getIDUtente1()==account.getID()){
							if(p.hasUtente1Risposto())
								inAttesa=true;
						}
						else {
							if(p.hasUtente2Risposto())
								inAttesa=true;
						}
						res.append(";partita=" + p.getIDPartita() + "," + sfidato.getID() + "," + sfidato.getUsername() + "," + p.getStatoPartita()+","+inAttesa);
					}
				}
				OutputWrite(res.toString());
			}
		}
		else {
			OutputWrite("getPartiteInCorso=error;message=Usage: getPartiteInCorso");
		}
	}
	private void newGame(String[] cmd) throws IOException{
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("newgame=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				Integer id_utente_sfidato = Integer.parseInt(cmd[1]);
				if (id_utente_sfidato == account.getID() || id_utente_sfidato == 0) {
					OutputWrite("newgame=error;message="+ListaErrori.NON_PUOI_SFIDARE_QUESTO_UTENTE);
				}
				else {
					Partita partita = DBPartita.getInstance().creaPartita(account.getID(), id_utente_sfidato);
					OutputWrite("newgame=OK;id=" + partita.getIDPartita());
				}
			}
		}
		else
			OutputWrite("newgame=error;message=Usage: newgame idutentesfidato");
	}
	private void newGameRandom(String[] cmd) throws IOException{
		if (cmd.length == 1) {
			if (account == null) {
				OutputWrite("newgame-random=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				Account acc_sfidante = GestionePartite.getInstance().accountRandom(account.getID(), ultime_partite);
				int id_s = acc_sfidante.getID();
				Partita p = DBPartita.getInstance().creaPartita(account.getID(), id_s < 0 ? 0 : id_s);
				if (acc_sfidante instanceof Bot) {
					((Bot) acc_sfidante).aggiungiPartita(p.getIDPartita());
				}
				aggiungiUtenteAUltimePartite(acc_sfidante);
				OutputWrite("newgame-random=OK;id=" + p.getIDPartita());
			}
		}
		else
			OutputWrite("newgame-random=error;message=Usage: newgame-random");
	}
	private void aggiungiUtenteAUltimePartite(Account acc){
		if(ultime_partite.size()==LIST_LAST_USER_SIZE){
			ultime_partite.remove(0);
		}
		ultime_partite.add(acc);
	}
	private void getDettagliPartita(String[] cmd) throws IOException {
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("getDettagliPartita=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				int id_partita = Integer.parseInt(cmd[1]);
				Partita p = DBPartita.getInstance().getPartitaByID(id_partita);
				if (p != null) {
					boolean playerOK = p.getIDUtente1() == account.getID() || p.getIDUtente2() == account.getID();
					if (playerOK) {
						boolean risposto1 = p.hasUtente1Risposto();
						boolean risposto2 = p.hasUtente2Risposto();
						int utente_n = p.getIDUtente1() == account.getID() ? 1 : 2;
						int stato = p.getStatoPartita();
						StringBuilder r = new StringBuilder();
						switch (utente_n) {
							case 1:
								r.append("getDettagliPartita=OK;domande=" + p.getNumeroDomande() + ";stato_partita=" + stato + ";utente=1;hai_risposto=" + (risposto1 ? 1 : 0) + ";tue_risposte=");
								for (int i = 0; i < p.getNumeroDomande(); i++) {
									r.append(p.getDomanda(i).getUser1Risposto());
									if (i < p.getNumeroDomande() - 1)
										r.append(",");
								}
								r.append(";avversario_risposto=" + (risposto2 ? 1 : 0) + ";avversario_risposte=");
								for (int i = 0; i < p.getNumeroDomande(); i++) {
									r.append(p.getDomanda(i).getUser2Risposto());
									if (i < p.getNumeroDomande() - 1)
										r.append(",");
								}
								break;
							case 2:
								r.append("getDettagliPartita=OK;domande=" + p.getNumeroDomande() + ";stato_partita=" + stato + ";utente=2;hai_risposto=" + (risposto2 ? 1 : 0) + ";tue_risposte=");
								for (int i = 0; i < p.getNumeroDomande(); i++) {
									r.append(p.getDomanda(i).getUser2Risposto());
									if (i < p.getNumeroDomande() - 1)
										r.append(",");
								}
								r.append(";avversario_risposto=" + (risposto1 ? 1 : 0) + ";avversario_risposte=");
								for (int i = 0; i < p.getNumeroDomande(); i++) {
									r.append(p.getDomanda(i).getUser1Risposto());
									if (i < p.getNumeroDomande() - 1)
										r.append(",");
								}
								break;
						}
						OutputWrite(r.toString());
					}
					else {
						OutputWrite("getDettagliPartita=error;message="+ListaErrori.NON_E_UNA_TUA_PARTITA);
					}
				}
				else {
					OutputWrite("getDettagliPartita=error;message="+ListaErrori.PARTITA_NON_TROVATA);
				}
			}
		}
		else
			OutputWrite("getDettagliPartita=error;message=Usage: getDettagliPartita idPartita");
	}
	private void abandon(String[] cmd) throws IOException{
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("abandon=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				Integer id_partita = Integer.parseInt(cmd[1]);
				if (DBPartita.getInstance().abbandonaPartita(id_partita, account.getID())) {
					OutputWrite("abandon=OK");
				}
				else {
					OutputWrite("abandon=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
				}
			}
		}
		else
			OutputWrite("abandon=error;message=Usage: abandon idpartita");
	}
	private void answer(String[] cmd) throws IOException{
		if (cmd.length == 8) {
			if (account == null) {
				OutputWrite("answer=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				int id = Integer.parseInt(cmd[1]);
				float[] risposte = new float[6];
				for (int i = 2, j = 0; i < 8; i++, j++) {
					float f = Float.parseFloat(cmd[i]);
					risposte[j] = f;
				}
				DBPartita.getInstance().rispondiDomande(id, account.getID(), risposte);
				OutputWrite("answer=OK");
			}
		}
		else {
			OutputWrite("answer=error;message=Usage: answer idpartita r1 r2 r3 r4 r5 r6");
		}
	}
	private void addFriend(String[] cmd) throws IOException{
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("addfriend=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				int idAmico = Integer.parseInt(cmd[1]);
				if (idAmico == 0 || idAmico == account.getID()) {
					OutputWrite("addfriend=error;message="+ListaErrori.NON_PUOI_AGGIUNGERE_QUESTO_ACCOUNT_AGLI_AMICI);
				}
				else {
					DBAccount.getInstance().addFriend(account, idAmico);
					OutputWrite("addfriend=OK");
				}
			}
		}
		else
			OutputWrite("addfriend=error;message=Usage: addfriend idamico");
	}
	private void removeFriend(String[] cmd) throws IOException{
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("removefriend=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				int id_amico = Integer.parseInt(cmd[1]);
				DBAccount.getInstance().removeFriend(account, id_amico);
				OutputWrite("removefriend=OK");
			}
		}
		else
			OutputWrite("removefriend=error;message=Usage: removefriend idamico");
	}
	private void getMyFriends(String[] cmd) throws IOException{
		if (cmd.length == 1) {
			if (account == null) {
				OutputWrite("getMyFriends=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				ArrayList<Account> amici = DBAccount.getInstance().getListaAmici(account);
				StringBuilder res = new StringBuilder("getMyFriends=OK;trovati=" + amici.size());
				for (int i = 0; i < amici.size(); i++) {
					Account acc = amici.get(i);
					res.append(";account=" + acc.getID() + "," + acc.getUsername());
				}
				OutputWrite(res.toString());
			}
		}
		else
			OutputWrite("getMyFriends=error;message=Usage: getMyFriends");
	}
	private void searchUser(String[] cmd) throws IOException {
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("search-user=error;messagge="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				ArrayList<Account> results = DBAccount.getInstance().searchUser(account, cmd[1]);
				StringBuilder out = new StringBuilder("search-user=OK;trovati=" + results.size());
				for (int i = 0; i < results.size(); i++) {
					Account acc = results.get(i);
					out.append(";utente=" + acc.getUsername() + "," + acc.getID());
				}
				results.clear();
				results = null;
				OutputWrite(out.toString());
			}
		}
		else
			OutputWrite("search-user=error;message=Usage: search-user nomeutente");
	}
	private void getStatistiche(String[] cmd) throws IOException{
		if(cmd.length==1){
			if(account==null){
				OutputWrite("getStatistiche=error;messagge="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				Statistiche stat=DBStatistiche.getInstance().getStatisticheByID(account.getID());
				if(stat!=null){
					OutputWrite("getStatistiche=OK;url_classifica="+Ranking.getInstance().getURLRanking()+";giocate="+stat.getPartite_giocate()+";vinte="+stat.getVittorie()+";perse="+stat.getSconfitte()+";pareggi="+stat.getPareggi()+";abbandoni="+stat.getAbbandonate()+";punti="+stat.getPunti());
				}
				else {
					OutputWrite("getStatistiche=error;messagge="+ListaErrori.RIPROVA_PIU_TARDI);
				}
			}
		}
		else
			OutputWrite("getStatistiche=error;message=Usage: getStatistiche");
	}
	private void getDomande(String[] cmd) throws IOException {
		if (cmd.length == 2) {
			if (account == null) {
				OutputWrite("getDomande=error;message="+ListaErrori.DEVI_ESSERE_LOGGATO);
			}
			else {
				int idP = Integer.parseInt(cmd[1]);
				Partita p = DBPartita.getInstance().getPartitaByID(idP);
				if (p.getIDUtente1() == account.getID() || p.getIDUtente2() == account.getID()) {
					StringBuilder res = new StringBuilder("getDomande=OK");
					for (int i = 1; i <= p.getNumeroDomande(); i++) {
						Domanda d = p.getDomanda(i - 1);
						int[] r = randomInteri(4);
						res.append(";domanda" + i + "=" + d.getDomanda() + ";risposta" + i + "=");
						for (int j = 0; j < r.length; j++) {
							int index = r[j];
							switch (index) {
								case 0:
									res.append(d.getRispostaEsatta());
									break;
								case 1:
								case 2:
								case 3:
									res.append(d.getRispostaErrata(index));
									break;
							}
							if (j < r.length - 1)
								res.append(',');
						}
					}
					OutputWrite(res.toString());
				}
				else {
					OutputWrite("getDomande=error;message="+ListaErrori.NON_E_UNA_TUA_PARTITA);
				}
			}
		}
		else
			OutputWrite("getDomande=error;message=Usage: getDomande id_partita");
	}
}
class ThreadQuit extends Thread {
	private Account acc;
	public ThreadQuit(Account a){
		acc=a;
	}
	public void run() {
		//System.out.println("ThreadQuit avviato");
		try {
			//System.out.println("ThreadQuit sleeping");
			sleep(SocketService.TIME_KEEP_USER_VISIBLE);
			//System.out.println("ThreadQuit sleep end");
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		GestionePartite.getInstance().esceUtente(acc);
		//System.out.println("ThreadQuit - Esce "+acc.getUsername());
		acc=null;
	}
}
