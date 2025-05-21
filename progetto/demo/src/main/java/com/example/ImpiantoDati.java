
package com.example; 

public class ImpiantoDati {
    private int codice;
    private String denominazione;
    private float latitudine;
    private float longitudine;
    private String statoManutenzione; // 'N' o 'S'
    private String dataOraManutenzione; // Stringa in formato ISO (es. "2024-05-21T10:30:00") o null

    // Costruttore vuoto (importante per alcune librerie come Gson)
    public ImpiantoDati() {
    }

    // Getters e Setters
    public int getCodice() { return codice; }
    public void setCodice(int codice) { this.codice = codice; }

    public String getDenominazione() { return denominazione; }
    public void setDenominazione(String denominazione) { this.denominazione = denominazione; }

    public float getLatitudine() { return latitudine; }
    public void setLatitudine(float latitudine) { this.latitudine = latitudine; }

    public float getLongitudine() { return longitudine; }
    public void setLongitudine(float longitudine) { this.longitudine = longitudine; }

    public String getStatoManutenzione() { return statoManutenzione; }
    public void setStatoManutenzione(String statoManutenzione) { this.statoManutenzione = statoManutenzione; }

    public String getDataOraManutenzione() { return dataOraManutenzione; }
    public void setDataOraManutenzione(String dataOraManutenzione) { this.dataOraManutenzione = dataOraManutenzione; }

    @Override
    public String toString() { // Utile per il debug
        return "ImpiantoDati{" + "codice=" + codice + ", denominazione='" + denominazione + '\'' + '}';
    }
}