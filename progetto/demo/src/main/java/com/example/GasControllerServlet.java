
package com.example; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//devo vedere la versione mi sono scordato ops
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServlet;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class GasControllerServlet extends HttpServlet {

    // --- Configurazione Database ---
    private final String DB_DRIVER = "com.mysql.cj.jdbc.Driver"; // Per MySQL 8+
    private final String DB_CONNECTION_URL_PREFIX = "jdbc:mysql://127.0.0.1:3306/"; // Host e porta MySQL
    private final String DB_NAME = "gas";
    private final String DB_USER = "root"; // Cambia se il tuo utente MySQL è diverso
    private final String DB_PASSWORD = "";   // Cambia se hai impostato una password per MySQL
    // Parametri aggiuntivi per la connessione a MySQL (possono risolvere problemi comuni)
    private final String DB_CONNECTION_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private Connection dbConnection;
    private boolean dbConnected;
    private Gson gson;

    // --- Metodi Ciclo di Vita Servlet ---
    @Override
    public void init() throws ServletException {
        String fullDbUrl = DB_CONNECTION_URL_PREFIX + DB_NAME + DB_CONNECTION_PARAMS;
        gson = new Gson(); // Inizializza Gson una sola volta

        try {
            Class.forName(DB_DRIVER);
            dbConnection = DriverManager.getConnection(fullDbUrl, DB_USER, DB_PASSWORD);
            dbConnected = true;
            log("SUCCESS: Connessione al database '" + DB_NAME + "' stabilita.");
        } catch (ClassNotFoundException e) {
            dbConnected = false;
            log("FATAL ERROR: Driver JDBC MySQL (" + DB_DRIVER + ") non trovato. Assicurati che mysql-connector-j.jar sia in WEB-INF/lib.", e);
            throw new ServletException("Driver JDBC non trovato", e);
        } catch (SQLException e) {
            dbConnected = false;
            log("FATAL ERROR: Impossibile connettersi al database '" + DB_NAME + "' all'URL: " + fullDbUrl + ". Verifica che MySQL sia in esecuzione e le credenziali siano corrette.", e);
            throw new ServletException("Errore di connessione al database", e);
        }
    }

    @Override
    public void destroy() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
                log("Connessione al database chiusa.");
            }
        } catch (SQLException e) {
            log("ERRORE: Durante la chiusura della connessione al database.", e);
        }
        dbConnected = false;
    }

    // --- Gestione Richieste HTTP ---
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log("Ricevuta richiesta GET a: " + request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));

        if (!dbConnected) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Servizio database temporaneamente non disponibile.");
            return;
        }

        // Estrazione e validazione parametri per la ricerca impianti
        String latMinStr = request.getParameter("lat_min");
        String latMaxStr = request.getParameter("lat_max");
        String lonMinStr = request.getParameter("lon_min");
        String lonMaxStr = request.getParameter("lon_max");

        if (latMinStr == null || latMaxStr == null || lonMinStr == null || lonMaxStr == null ||
            latMinStr.trim().isEmpty() || latMaxStr.trim().isEmpty() || lonMinStr.trim().isEmpty() || lonMaxStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parametri mancanti o vuoti: 'lat_min', 'lat_max', 'lon_min', 'lon_max' sono tutti richiesti.");
            return;
        }

        float latMin, latMax, lonMin, lonMax;
        try {
            latMin = Float.parseFloat(latMinStr);
            latMax = Float.parseFloat(latMaxStr);
            lonMin = Float.parseFloat(lonMinStr);
            lonMax = Float.parseFloat(lonMaxStr);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato non valido per i parametri di latitudine/longitudine. Devono essere numeri.");
            return;
        }

        List<ImpiantoDati> impiantiTrovati = new ArrayList<>();
        String sql = "SELECT codice, denominazione, latitudine, longitudine, stato_manutenzione, data_ora " +
                     "FROM impianto " +
                     "WHERE stato_manutenzione = 'N' " +
                     "AND latitudine BETWEEN ? AND ? " + // BETWEEN è inclusivo
                     "AND longitudine BETWEEN ? AND ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setFloat(1, latMin);
            pstmt.setFloat(2, latMax);
            pstmt.setFloat(3, lonMin);
            pstmt.setFloat(4, lonMax);

            log("Esecuzione SQL GET: " + pstmt.toString()); // Utile per vedere la query (senza valori reali per PreparedStatement)

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ImpiantoDati impianto = new ImpiantoDati();
                    impianto.setCodice(rs.getInt("codice"));
                    impianto.setDenominazione(rs.getString("denominazione"));
                    impianto.setLatitudine(rs.getFloat("latitudine"));
                    impianto.setLongitudine(rs.getFloat("longitudine"));
                    impianto.setStatoManutenzione(rs.getString("stato_manutenzione"));
                    Timestamp ts = rs.getTimestamp("data_ora");
                    if (ts != null) {
                        impianto.setDataOraManutenzione(ts.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    } else {
                        impianto.setDataOraManutenzione(null);
                    }
                    impiantiTrovati.add(impianto);
                }
            }
            log("Impianti trovati: " + impiantiTrovati.size());
        } catch (SQLException e) {
            log("ERRORE SQL in GET: " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore del server durante la ricerca degli impianti.");
            return;
        }

        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(impiantiTrovati));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log("Ricevuta richiesta PUT a: " + request.getRequestURI());

        if (!dbConnected) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Servizio database temporaneamente non disponibile.");
            return;
        }

        String pathInfo = request.getPathInfo(); // Es. "/123" se l'URL è /.../impianti/123
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.length() < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codice impianto mancante nell'URL. Formato atteso: /impianti/CODICE_IMPIANTO");
            return;
        }

        int codiceImpianto;
        try {
            codiceImpianto = Integer.parseInt(pathInfo.substring(1)); // Rimuove il primo "/"
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codice impianto nell'URL non è un numero valido: '" + pathInfo.substring(1) + "'");
            return;
        }

        // Lettura del corpo JSON
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log("ERRORE: Durante la lettura del corpo della richiesta PUT.", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore durante la lettura dei dati della richiesta.");
            return;
        }
        String jsonBody = sb.toString();
        log("Corpo JSON ricevuto per PUT (impianto " + codiceImpianto + "): " + jsonBody);


        String dataOraString = null;
        try {
            JsonObject jsonObject = gson.fromJson(jsonBody, JsonObject.class);
            if (jsonObject != null && jsonObject.has("data_ora") && jsonObject.get("data_ora").isJsonPrimitive()) {
                if (!jsonObject.get("data_ora").isJsonNull()) {
                    dataOraString = jsonObject.get("data_ora").getAsString();
                } else {
                     response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Il campo 'data_ora' nel JSON non può essere esplicitamente nullo.");
                     return;
                }
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            log("ERRORE: Parsing del corpo JSON per PUT fallito. JSON ricevuto: '" + jsonBody + "'", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Corpo JSON malformato o il campo 'data_ora' non è una stringa.");
            return;
        }


        if (dataOraString == null || dataOraString.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Campo 'data_ora' mancante, vuoto o non valido nel corpo JSON.");
            return;
        }

        LocalDateTime dataOraManutenzione;
        try {
            dataOraManutenzione = LocalDateTime.parse(dataOraString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            log("ERRORE: Formato data_ora non valido: '" + dataOraString + "'. Atteso formato ISO YYYY-MM-DDTHH:mm:ss.", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Formato 'data_ora' non valido. Utilizzare il formato YYYY-MM-DDTHH:mm:ss (es. 2024-05-21T10:30:00).");
            return;
        }

        String sql = "UPDATE impianto SET stato_manutenzione = 'S', data_ora = ? WHERE codice = ?";
        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(dataOraManutenzione));
            pstmt.setInt(2, codiceImpianto);

            log("Esecuzione SQL PUT: UPDATE impianto SET stato_manutenzione = 'S', data_ora = '" + dataOraManutenzione + "' WHERE codice = " + codiceImpianto);
            int righeModificate = pstmt.executeUpdate();

            if (righeModificate > 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // Successo!
                log("SUCCESS: Impianto " + codiceImpianto + " aggiornato. Manutenzione registrata il " + dataOraManutenzione);
            } else {
                // Se nessuna riga è stata modificata, l'impianto potrebbe non esistere
                boolean impiantoEsiste = checkIfImpiantoExists(codiceImpianto);
                if (impiantoEsiste) {
                     log("WARN: Impianto " + codiceImpianto + " trovato ma non aggiornato (forse già 'S' o condizione non soddisfatta).");
                     response.sendError(HttpServletResponse.SC_CONFLICT, "Impianto " + codiceImpianto + " trovato ma non necessitava di aggiornamento (possibile stato già 'S')."); // 409 Conflict
                } else {
                    log("WARN: Tentativo di aggiornare impianto non esistente: " + codiceImpianto);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Impianto con codice " + codiceImpianto + " non trovato.");
                }
            }
        } catch (SQLException e) {
            log("ERRORE SQL in PUT per impianto " + codiceImpianto + ": " + e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Errore del server durante l'aggiornamento dell'impianto.");
        }
    }

    private boolean checkIfImpiantoExists(int codiceImpianto) throws SQLException {
        // Questo metodo non dovrebbe lanciare eccezioni al chiamante se la query fallisce,
        // ma loggarle e ritornare false, o il chiamante deve gestire SQLException.
        // Per semplicità, qui la lasciamo propagare dato che il chiamante è già in un try-catch SQL.
        String checkSql = "SELECT 1 FROM impianto WHERE codice = ? LIMIT 1"; // Più efficiente di COUNT(*)
        try (PreparedStatement checkPstmt = dbConnection.prepareStatement(checkSql)) {
            checkPstmt.setInt(1, codiceImpianto);
            try (ResultSet rs = checkPstmt.executeQuery()) {
                return rs.next(); // Ritorna true se esiste almeno una riga
            }
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet per la gestione della manutenzione degli impianti a gas";
    }
}