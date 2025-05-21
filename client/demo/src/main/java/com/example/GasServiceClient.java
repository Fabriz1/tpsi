package com.example;

// Salva come: GasClientProgetto/src/it/clientgas/GasServiceClient.java
package com.clientgas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson; // Importa Gson
import com.google.gson.reflect.TypeToken; // Per deserializzare liste
import java.lang.reflect.Type;
import java.util.List;
import java.util.Arrays; // Per Arrays.asList se non usi TypeToken

public class GasServiceClient {

    private final String baseUrl; // Es. "http://localhost:8080/serviziogas"
    private final Gson gson;

    public GasServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();
    }

    /**
     * Richiede l'elenco degli impianti da manutenere in una data zona.
     * @return Lista di oggetti RispostaImpianto o null in caso di errore.
     */
    public List<RispostaImpianto> getImpiantiDaManutenere(float latMin, float latMax, float lonMin, float lonMax) {
        String endpoint = "/impianti"; // Relativo al baseUrl
        try {
            String queryParams = String.format("?lat_min=%s&lat_max=%s&lon_min=%s&lon_max=%s",
                    URLEncoder.encode(String.valueOf(latMin), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(String.valueOf(latMax), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(String.valueOf(lonMin), StandardCharsets.UTF_8.name()),
                    URLEncoder.encode(String.valueOf(lonMax), StandardCharsets.UTF_8.name())
            );

            URL url = new URL(baseUrl + endpoint + queryParams);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json"); // Diciamo al server che accettiamo JSON

            int status = connection.getResponseCode();
            System.out.println("GET " + url.toString() + " - Status: " + status);


            if (status == HttpURLConnection.HTTP_OK) { // 200
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
                reader.close();

                String jsonResponse = responseBody.toString();
                System.out.println("Risposta JSON dal server (GET): " + jsonResponse);

                // Deserializza il JSON in una lista di RispostaImpianto
                Type listType = new TypeToken<List<RispostaImpianto>>(){}.getType();
                return gson.fromJson(jsonResponse, listType);
                // Alternativa più semplice se il JSON è sempre un array e non hai TypeToken:
                // RispostaImpianto[] impiantiArray = gson.fromJson(jsonResponse, RispostaImpianto[].class);
                // return Arrays.asList(impiantiArray);

            } else {
                System.err.println("Errore nella richiesta GET: " + status + " - " + connection.getResponseMessage());
                // Puoi leggere l'error stream per maggiori dettagli sull'errore dal server
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    System.err.println("Dettaglio errore server:");
                    while ((errorLine = errorReader.readLine()) != null) {
                        System.err.println(errorLine);
                    }
                } catch (IOException ex) {
                    // Ignora se l'error stream non è disponibile
                }
                return null;
            }
        } catch (IOException e) {
            System.err.println("Errore IOException durante la richiesta GET: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Registra l'avvenuta manutenzione di un impianto.
     * @param codiceImpianto Il codice dell'impianto da aggiornare.
     * @param dataOraManutenzione La data e ora della manutenzione in formato ISO ("YYYY-MM-DDTHH:mm:ss").
     * @return true se l'operazione ha successo (status 204), false altrimenti.
     */
    public boolean registraManutenzione(int codiceImpianto, String dataOraManutenzione) {
        String endpoint = "/impianti/" + codiceImpianto; // Es. /impianti/101

        // Crea l'oggetto per il corpo JSON
        // Per semplicità, creiamo una mappa o un oggetto anonimo se la struttura è piccola,
        // altrimenti si potrebbe creare una classe apposita 'ManutenzioneRequestData'.
        class ManutenzioneRequestBody { // Classe locale per il corpo JSON
            String data_ora;
            ManutenzioneRequestBody(String dataOra) { this.data_ora = dataOra; }
        }
        ManutenzioneRequestBody requestBodyObject = new ManutenzioneRequestBody(dataOraManutenzione);
        String jsonInputString = gson.toJson(requestBodyObject);

        try {
            URL url = new URL(baseUrl + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true); // Necessario per inviare un corpo nella richiesta PUT
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json"); // Opzionale, ma buona pratica

            System.out.println("PUT " + url.toString() + " con corpo: " + jsonInputString);

            try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonInputString);
                writer.flush();
            }

            int status = connection.getResponseCode();
            System.out.println("Risposta PUT - Status: " + status);

            if (status == HttpURLConnection.HTTP_NO_CONTENT) { // 204
                return true;
            } else {
                System.err.println("Errore nella richiesta PUT: " + status + " - " + connection.getResponseMessage());
                // Leggi l'error stream per dettagli
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    System.err.println("Dettaglio errore server:");
                    while ((errorLine = errorReader.readLine()) != null) {
                        System.err.println(errorLine);
                    }
                } catch (IOException ex) { /* ignora */ }
                return false;
            }

        } catch (IOException e) {
            System.err.println("Errore IOException durante la richiesta PUT: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    // Metodo main per testare il client
    public static void main(String[] args) {
        // Assicurati che il tuo GasControllerServlet sia deployato e in esecuzione
        // e che il context path sia corretto (es. "/serviziogas" come da context.xml)
        GasServiceClient client = new GasServiceClient("http://localhost:8080/serviziogas");

        System.out.println("--- Test GET Impianti Da Manutenere ---");
        // Modifica le coordinate per includere i tuoi dati di esempio che sono 'N'
        List<RispostaImpianto> impianti = client.getImpiantiDaManutenere(45.0f, 46.0f, 9.0f, 10.0f);
        if (impianti != null && !impianti.isEmpty()) {
            System.out.println("Impianti ricevuti:");
            for (RispostaImpianto impianto : impianti) {
                System.out.println(impianto);
            }
        } else if (impianti != null) { // Lista vuota, nessun impianto trovato
             System.out.println("Nessun impianto da manutenere trovato per i criteri specificati.");
        } else { // Errore durante la richiesta
            System.out.println("Errore nel recuperare gli impianti.");
        }

        System.out.println("\n--- Test PUT Registra Manutenzione ---");
        int codiceImpiantoDaAggiornare = 101; // Assicurati che questo impianto esista e sia 'N'
        String dataOraCorrente = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        boolean successoPut = client.registraManutenzione(codiceImpiantoDaAggiornare, dataOraCorrente);
        if (successoPut) {
            System.out.println("Manutenzione per impianto " + codiceImpiantoDaAggiornare + " registrata con successo il " + dataOraCorrente);
        } else {
            System.out.println("Fallimento registrazione manutenzione per impianto " + codiceImpiantoDaAggiornare);
        }

        // Test PUT su un impianto non esistente
        System.out.println("\n--- Test PUT Registra Manutenzione (Impianto Non Esistente) ---");
        successoPut = client.registraManutenzione(9999, dataOraCorrente); // Codice che non dovrebbe esistere
         if (successoPut) {
            System.out.println("Manutenzione per impianto 9999 registrata (IMPROBABILE, ERRORE ATTESO).");
        } else {
            System.out.println("Fallimento registrazione manutenzione per impianto 9999 (CORRETTO, ERRORE ATTESO).");
        }

        // Puoi aggiungere un altro GET per verificare che l'impianto 101 sia stato aggiornato
        System.out.println("\n--- Test GET Impianti Da Manutenere (Dopo PUT) ---");
        impianti = client.getImpiantiDaManutenere(45.0f, 46.0f, 9.0f, 10.0f);
         if (impianti != null) {
            System.out.println("Impianti ricevuti dopo PUT:");
            boolean trovatoAggiornato = false;
            for (RispostaImpianto impianto : impianti) {
                System.out.println(impianto);
                if(impianto.getCodice() == codiceImpiantoDaAggiornare && "S".equals(impianto.getStatoManutenzione())){
                    // Questo non dovrebbe accadere se il GET prende solo stato 'N'
                }
                 if(impianto.getCodice() == codiceImpiantoDaAggiornare){
                     trovatoAggiornato = true; // Se il GET prendesse tutti, qui controlleremmo lo stato
                 }
            }
            if(!trovatoAggiornato && successoPut){
                System.out.println("L'impianto " + codiceImpiantoDaAggiornare + " non è più nella lista 'N' (CORRETTO).");
            }
        } else {
            System.out.println("Errore nel recuperare gli impianti dopo PUT.");
        }
    }
}