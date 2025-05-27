package servlets;

import dao.EstudianteswebJpaController;
import dto.Estudiantesweb;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.persistence.EntityManager; // Importar EntityManager
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
// Para hashing, descomenta y añade la dependencia de jBCrypt si la vas a usar
// import org.mindrot.jbcrypt.BCrypt; 

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private EstudianteswebJpaController estudianteDAO;
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private EntityManagerFactory emf; // Guardar EMF para poder cerrar EM correctamente

    @Override
    public void init() throws ServletException {
        super.init();
        LOGGER.info("LoginServlet: Iniciando inicialización...");
        try {
            // IMPORTANTE: Usa el mismo nombre de PU que en EstudianteswebServlet y persistence.xml
            this.emf = Persistence.createEntityManagerFactory("com.mycompany_examen_tdp_01_war_1.0-SNAPSHOTPU");
            this.estudianteDAO = new EstudianteswebJpaController(this.emf);
            LOGGER.info("LoginServlet: EntityManagerFactory y EstudianteswebJpaController inicializados correctamente.");
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "LoginServlet: Error CRÍTICO durante la inicialización del EMF o DAO.", t);
            // Imprimir traza a la consola del servidor para más detalles si el logger no está configurado
            t.printStackTrace(System.err); 
            throw new ServletException("Error crítico al inicializar LoginServlet y la capa de persistencia. Revise los logs del servidor.", t);
        }
    }

    private void sendJsonResponse(HttpServletResponse response, JsonObject json) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        JsonObject errorJson = Json.createObjectBuilder().add("error", message).build();
        sendJsonResponse(response, errorJson);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        LOGGER.info("LoginServlet: doPost() INVOCADO. Request URI: " + request.getRequestURI() + ", Method: " + request.getMethod());
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String ndniEstdWeb = null;
        String passEstd = null;
        
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader bufferedReader = request.getReader()) {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "LoginServlet: IOException al leer el cuerpo de la solicitud.", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al leer la solicitud.");
            return;
        }
        String requestBody = sb.toString();

        if (requestBody.trim().isEmpty()) {
            LOGGER.warning("LoginServlet: Cuerpo de la solicitud JSON vacío.");
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de la solicitud vacío. Se esperaba JSON.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(requestBody))) {
            JsonObject jsonObject = jsonReader.readObject();
            if (jsonObject.containsKey("ndniEstdWeb") && !jsonObject.isNull("ndniEstdWeb")) {
                ndniEstdWeb = jsonObject.getString("ndniEstdWeb").trim();
            }
            if (jsonObject.containsKey("passEstd") && !jsonObject.isNull("passEstd")) {
                passEstd = jsonObject.getString("passEstd"); // No hacer trim a la contraseña
            }
        } catch (JsonParsingException e) {
            LOGGER.log(Level.WARNING, "LoginServlet: Error al parsear JSON. Cuerpo recibido: " + requestBody, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de solicitud JSON inválido.");
            return;
        }

        if (ndniEstdWeb == null || ndniEstdWeb.isEmpty() || passEstd == null || passEstd.isEmpty()) {
            LOGGER.warning("LoginServlet: DNI o contraseña vacíos después del parseo.");
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Usuario y contraseña son requeridos.");
            return;
        }

        Estudiantesweb estudiante = null;
        EntityManager em = null; 
        try {
            // Usar el EMF guardado en init() para crear el EntityManager
            if (this.emf == null) {
                LOGGER.severe("LoginServlet: EntityManagerFactory (emf) es null en doPost. Fallo en init()?");
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de configuración del servidor (EMF nulo).");
                return;
            }
            em = this.emf.createEntityManager(); // Crear EntityManager
            
            // Asegúrate que el DAO EstudianteswebJpaController NO cierre este EM si lo pasas como argumento.
            // Si tu DAO crea y cierra su propio EM en cada método, esta forma es correcta.
            // O, si tienes un método en estudianteDAO como findByNdniEstdWeb(String dni), úsalo.
            // Ejemplo usando NamedQuery directamente aquí:
            TypedQuery<Estudiantesweb> query = em.createNamedQuery("Estudiantesweb.findByNdniEstdWeb", Estudiantesweb.class);
            query.setParameter("ndniEstdWeb", ndniEstdWeb);
            estudiante = query.getSingleResult();
            LOGGER.info("LoginServlet: Usuario encontrado en BD para DNI: " + ndniEstdWeb);

        } catch (NoResultException e) {
            LOGGER.log(Level.INFO, "LoginServlet: Intento de login fallido, usuario NO encontrado para DNI: " + ndniEstdWeb);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Credenciales inválidas (usuario no existe).");
            return;
        } catch (IllegalStateException ise) { // Puede ocurrir si EMF está cerrado
            LOGGER.log(Level.SEVERE, "LoginServlet: IllegalStateException al acceder a EntityManager. EMF podría estar cerrado.", ise);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de configuración del servidor (EMF cerrado).");
            return;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "LoginServlet: Error INESPERADO al buscar estudiante (DNI: " + ndniEstdWeb + ")", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor al procesar el login.");
            return;
        } finally {
            if (em != null && em.isOpen()) {
                em.close(); 
                LOGGER.fine("LoginServlet: EntityManager cerrado después de la búsqueda de usuario.");
            }
        }

        // La variable 'estudiante' aquí ya NO puede ser null si se pasó la NoResultException
        
        // --- IMPORTANTE: Comparación de Contraseñas ---
        // ¡DEBES CAMBIAR ESTO PARA USAR BCRYPT O UN HASH SEGURO!
        // El siguiente código es INSEGURO y SOLO para fines de desarrollo inicial.
        if (estudiante.getPassEstd() != null && estudiante.getPassEstd().equals(passEstd)) { 
        // Ejemplo con BCrypt (descomentar y usar cuando implementes hashing):
        // if (estudiante.getPassEstd() != null && BCrypt.checkpw(passEstd, estudiante.getPassEstd())) {

            HttpSession session = request.getSession(true); // Crear sesión si no existe
            session.setAttribute("codiEstdWebLogueado", estudiante.getCodiEstdWeb());
            String nombreCompleto = (estudiante.getNombEstdWeb() != null ? estudiante.getNombEstdWeb() : "") + 
                                  (estudiante.getAppaEstdWeb() != null ? " " + estudiante.getAppaEstdWeb() : "");
            session.setAttribute("nombreUsuarioLogueado", nombreCompleto.trim());
            
            // session.setMaxInactiveInterval(30*60); // Opcional: configurar tiempo de expiración de sesión (ej. 30 minutos)

            LOGGER.log(Level.INFO, "LoginServlet: LOGIN EXITOSO para usuario DNI: " + ndniEstdWeb + ", ID: " + estudiante.getCodiEstdWeb());
            
            JsonObjectBuilder responseBuilder = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Login exitoso.");
            if (!nombreCompleto.trim().isEmpty()) {
                responseBuilder.add("nombreUsuario", nombreCompleto.trim());
            }
            if (estudiante.getCodiEstdWeb() != null) {
                responseBuilder.add("codiEstdWeb", estudiante.getCodiEstdWeb());
            }
            sendJsonResponse(response, responseBuilder.build());

        } else {
            LOGGER.log(Level.WARNING, "LoginServlet: Intento de login fallido, CONTRASEÑA INCORRECTA para DNI: " + ndniEstdWeb);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Credenciales inválidas (contraseña incorrecta).");
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.emf != null && this.emf.isOpen()) {
            this.emf.close();
            LOGGER.info("LoginServlet: EntityManagerFactory cerrada en destroy().");
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet de autenticación de estudiantes Web";
    }
}