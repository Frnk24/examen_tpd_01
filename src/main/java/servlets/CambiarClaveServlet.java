package servlets;

import dao.EstudianteswebJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Estudiantesweb;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.persistence.EntityManagerFactory; // No es necesario si el DAO lo maneja todo
import javax.persistence.Persistence;
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

@WebServlet(name = "CambiarClaveServlet", urlPatterns = {"/CambiarClaveServlet"})
public class CambiarClaveServlet extends HttpServlet {

    private EstudianteswebJpaController estudianteDAO;
    private static final Logger LOGGER = Logger.getLogger(CambiarClaveServlet.class.getName());
    private EntityManagerFactory emf; // Guardar EMF para cerrarlo en destroy

    @Override
    public void init() throws ServletException {
        super.init();
        LOGGER.info("CambiarClaveServlet: Iniciando inicialización...");
        try {
            // IMPORTANTE: Usa el mismo nombre de PU que en otros Servlets y persistence.xml
            this.emf = Persistence.createEntityManagerFactory("com.mycompany_examen_tdp_01_war_1.0-SNAPSHOTPU");
            this.estudianteDAO = new EstudianteswebJpaController(this.emf); // Pasa el EMF al DAO
            LOGGER.info("CambiarClaveServlet: EntityManagerFactory y EstudianteswebJpaController inicializados correctamente.");
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Error CRÍTICO durante la inicialización del EMF o DAO.", t);
            t.printStackTrace(System.err); 
            throw new ServletException("Error crítico al inicializar CambiarClaveServlet y la capa de persistencia.", t);
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
        LOGGER.info("CambiarClaveServlet: doPost() INVOCADO. Request URI: " + request.getRequestURI() + ", Method: " + request.getMethod());
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false); // No crear nueva sesión si no existe

        if (session == null || session.getAttribute("codiEstdWebLogueado") == null) {
            LOGGER.warning("CambiarClaveServlet: Intento de cambio de contraseña sin sesión válida o sin codiEstdWebLogueado.");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No autorizado. Por favor, inicie sesión de nuevo.");
            return;
        }

        Integer codiEstdWeb = null;
        try {
            codiEstdWeb = (Integer) session.getAttribute("codiEstdWebLogueado");
        } catch (ClassCastException cce) {
             LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Error de tipo en codiEstdWebLogueado de la sesión.", cce);
             sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de sesión interna.");
             return;
        }
        
        if (codiEstdWeb == null) { // Doble chequeo por si acaso
            LOGGER.warning("CambiarClaveServlet: codiEstdWebLogueado es null en la sesión.");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Error de sesión. Por favor, inicie sesión de nuevo.");
            return;
        }

        String currentPassword = null;
        String newPassword = null;
        
        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader bufferedReader = request.getReader()) {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: IOException al leer el cuerpo de la solicitud.", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al leer la solicitud.");
            return;
        }
        String requestBody = sb.toString();

        if (requestBody.trim().isEmpty()) {
            LOGGER.warning("CambiarClaveServlet: Cuerpo de la solicitud JSON vacío.");
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de la solicitud vacío. Se esperaba JSON.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(requestBody))) {
            JsonObject jsonObject = jsonReader.readObject();
            if (jsonObject.containsKey("currentPassword") && !jsonObject.isNull("currentPassword")) {
                currentPassword = jsonObject.getString("currentPassword");
            }
            if (jsonObject.containsKey("newPassword") && !jsonObject.isNull("newPassword")) {
                newPassword = jsonObject.getString("newPassword");
            }
        } catch (JsonParsingException e) {
            LOGGER.log(Level.WARNING, "CambiarClaveServlet: Error al parsear JSON. Cuerpo recibido: " + requestBody, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de solicitud JSON inválido.");
            return;
        }

        if (currentPassword == null || currentPassword.trim().isEmpty() ||
            newPassword == null || newPassword.trim().isEmpty()) {
            LOGGER.warning("CambiarClaveServlet: Contraseñas vacías después del parseo.");
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Todas las contraseñas son requeridas.");
            return;
        }

        if (newPassword.length() < 6) {
             sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "La nueva contraseña debe tener al menos 6 caracteres.");
             return;
        }

        Estudiantesweb estudiante;
        try {
            // El método findEstudiantesweb del DAO debería manejar su propio EntityManager
            estudiante = estudianteDAO.findEstudiantesweb(codiEstdWeb);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Error al buscar estudiante (ID: " + codiEstdWeb + ") para cambio de clave.", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno al obtener datos del usuario.");
            return;
        }

        if (estudiante == null) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Usuario de sesión (ID: " + codiEstdWeb + ") NO ENCONTRADO en BD durante cambio de contraseña.");
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado. Su sesión podría haber expirado o el usuario fue eliminado.");
            session.invalidate(); 
            return;
        }

        // --- IMPORTANTE: Comparación de Contraseñas y Hashing ---
        // ¡DEBES CAMBIAR ESTO PARA USAR BCRYPT O UN HASH SEGURO!
        if (estudiante.getPassEstd() != null && !estudiante.getPassEstd().equals(currentPassword)) {
        // Ejemplo con BCrypt:
        // if (estudiante.getPassEstd() != null && !BCrypt.checkpw(currentPassword, estudiante.getPassEstd())) {
            LOGGER.log(Level.WARNING, "CambiarClaveServlet: Contraseña actual INCORRECTA para usuario ID: " + codiEstdWeb);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "La contraseña actual es incorrecta.");
            return;
        }

        // Hashea la nueva contraseña antes de guardarla
        // ¡DEBES CAMBIAR ESTO PARA USAR BCRYPT O UN HASH SEGURO!
        String newPasswordToStore = newPassword; // ¡INSEGURO!
        // Ejemplo con BCrypt:
        // String newPasswordHashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        // String newPasswordToStore = newPasswordHashed;

        estudiante.setPassEstd(newPasswordToStore);

        try {
            estudianteDAO.edit(estudiante); // El método edit maneja su propio EM
            LOGGER.log(Level.INFO, "CambiarClaveServlet: Contraseña cambiada EXITOSAMENTE para usuario ID: " + codiEstdWeb);
            JsonObject successJson = Json.createObjectBuilder()
                    .add("success", true)
                    .add("message", "Contraseña cambiada exitosamente.")
                    .build();
            sendJsonResponse(response, successJson);
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Error al editar (NonexistentEntityException) durante cambio de contraseña para ID: " + codiEstdWeb, e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Error: Usuario no encontrado al intentar guardar la nueva contraseña.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CambiarClaveServlet: Error INESPERADO al guardar nueva contraseña para ID: " + codiEstdWeb, e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno al cambiar la contraseña.");
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (this.emf != null && this.emf.isOpen()) {
            this.emf.close();
            LOGGER.info("CambiarClaveServlet: EntityManagerFactory cerrada en destroy().");
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para cambio de contraseña de estudiantes";
    }
}