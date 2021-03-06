package it.mathchallenger.server.entities;

public class Statistiche {
	private int id_utente;
	private int vittorie, sconfitte, pareggi, punti, abbandonate, partite_giocate;
	
	public Statistiche(int id){
		setIDUtente(id);
		setVittorie(0);
		setSconfitte(0);
		setPareggi(0);
		setPunti(0);
		setAbbandonate(0);
		setPartite_giocate(0);
	}

	public int getVittorie() {
		return vittorie;
	}

	public void setVittorie(int vittorie) {
		this.vittorie = vittorie;
	}

	public int getSconfitte() {
		return sconfitte;
	}

	public void setSconfitte(int sconfitte) {
		this.sconfitte = sconfitte;
	}

	public int getPareggi() {
		return pareggi;
	}

	public void setPareggi(int pareggi) {
		this.pareggi = pareggi;
	}

	public int getPunti() {
		return punti;
	}

	public void setPunti(int punti) {
		this.punti = punti;
	}

	public int getAbbandonate() {
		return abbandonate;
	}

	public void setAbbandonate(int abbandonate) {
		this.abbandonate = abbandonate;
	}

	public int getPartite_giocate() {
		return partite_giocate;
	}

	public void setPartite_giocate(int partite_giocate) {
		this.partite_giocate = partite_giocate;
	}

	public int getIDUtente() {
		return id_utente;
	}

	public void setIDUtente(int id_utente) {
		this.id_utente = id_utente;
	}
}
