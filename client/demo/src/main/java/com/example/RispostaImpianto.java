// Salva come: GasClientProgetto/src/it/clientgas/RispostaImpianto.java
package com.example;

public class RispostaImpianto {
    private int codice;
    private String denominazione;
    private float latitudine;
    private float longitudine;
    private String statoManutenzione;
    private String dataOraManutenzione; // JSON usa 'dataOraManutenzione'

    // Getters (i setters potrebbero non essere necessari se lo usi solo per leggere risposte)
    public int getCodice() { return codice; }
    public String getDenominazione() { return denominazione; }
    public float getLatitudine() { return latitudine; }
    public float getLongitudine() { return longitudine; }
    public String getStatoManutenzione() { return statoManutenzione; }
    public String getDataOraManutenzione() { return dataOraManutenzione; }

    @Override
    public String toString() {
        return "Impianto Letto: {" +
               "codice=" + codice +
               ", denominazione='" + denominazione + '\'' +
               ", lat=" + latitudine +
               ", lon=" + longitudine +
               ", stato='" + statoManutenzione + '\'' +
               ", dataManutenzione='" + (dataOraManutenzione != null ? dataOraManutenzione : "N/A") + '\'' +
               '}';
    }
}