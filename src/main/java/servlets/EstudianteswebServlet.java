package servlets;

import dao.EstudianteswebJpaController;
import dao.exceptions.NonexistentEntityException;
import dto.Estudiantesweb;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "EstudianteswebServlet", urlPatterns = {"/EstudianteswebServlet"})
public class EstudianteswebServlet extends HttpServlet {

    private EstudianteswebJpaController estudianteDAO;
    private static final Logger LOGGER = Logger.getLogger(EstudianteswebServlet.class.getName());
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    static {
        DATE_FORMATTER.setLenient(false);
    }

    @Override
    public void init() throws ServletException {
        super.init();
        // IMPORTANTE: Reemplaza "EstudiantesPU" con el nombre real de tu Unidad de Persistencia
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("com.mycompany_examen_tdp_01_war_1.0-SNAPSHOTPU");
        estudianteDAO = new EstudianteswebJpaController(emf);
    }

    private JsonObject estudianteToJson(Estudiantesweb estudiante) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (estudiante.getCodiEstdWeb()!=null) {
            builder.add("codiEstdWeb",estudiante.getCodiEstdWeb());
        }else{
            builder.addNull("codiEstdWeb");
        }
        if (estudiante.getNdniEstdWeb()!=null) {
            builder.add("ndniEstdWeb",estudiante.getNdniEstdWeb());
        }else{
            builder.addNull("ndniEstdWeb");
        }
       if (estudiante.getAppaEstdWeb()!=null) {
            builder.add("appaEstdWeb",estudiante.getAppaEstdWeb());
        }else{
            builder.addNull("appaEstdWeb");
        }
        if (estudiante.getApmaEstdWeb()!=null) {
            builder.add("apmaEstdWeb",estudiante.getApmaEstdWeb());
        }else{
            builder.addNull("apmaEstdWeb");
        }
       if (estudiante.getNombEstdWeb()!=null) {
            builder.add("nombEstdWeb",estudiante.getNombEstdWeb());
        }else{
            builder.addNull("nombEstdWeb");
        }
        
        if (estudiante.getFechNaciEstdWeb() != null) {
            builder.add("fechNaciEstdWeb", DATE_FORMATTER.format(estudiante.getFechNaciEstdWeb()));
        } else {
            builder.addNull("fechNaciEstdWeb");
        }
        if (estudiante.getLogiEstd()!=null) {
            builder.add("logiEstd",estudiante.getLogiEstd());
        }else{
            builder.addNull("logiEstd");
        }
        
        // No incluir passEstd en las respuestas JSON generales
        return builder.build();
    }

    private void sendJsonResponse(HttpServletResponse response, JsonStructure json) throws IOException {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("codiEstdWeb");

        try {
            if (idParam != null && !idParam.isEmpty()) {
                Integer id = Integer.parseInt(idParam);
                Estudiantesweb estudiante = estudianteDAO.findEstudiantesweb(id);
                if (estudiante != null) {
                    sendJsonResponse(response, estudianteToJson(estudiante));
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado.");
                }
            } else {
                List<Estudiantesweb> estudiantes = estudianteDAO.findEstudianteswebEntities();
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (Estudiantesweb estudiante : estudiantes) {
                    arrayBuilder.add(estudianteToJson(estudiante));
                }
                sendJsonResponse(response, arrayBuilder.build());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de estudiante inválido: " + idParam, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en doGet", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor al obtener estudiantes.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        try (JsonReader jsonReader = Json.createReader(new StringReader(request.getReader().lines().reduce("", (s1, s2) -> s1 + s2)))) {
            JsonObject jsonObject = jsonReader.readObject();
            Estudiantesweb nuevoEstudiante = new Estudiantesweb();

            // Validar y asignar campos. No se maneja codiEstdWeb (es autogenerado) ni passEstd.
            if (!jsonObject.containsKey("ndniEstdWeb") || jsonObject.isNull("ndniEstdWeb") || jsonObject.getString("ndniEstdWeb").trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'ndniEstdWeb' es obligatorio.");
                return;
            }
            nuevoEstudiante.setNdniEstdWeb(jsonObject.getString("ndniEstdWeb"));

            if (!jsonObject.containsKey("appaEstdWeb") || jsonObject.isNull("appaEstdWeb") || jsonObject.getString("appaEstdWeb").trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'appaEstdWeb' es obligatorio.");
                return;
            }
            nuevoEstudiante.setAppaEstdWeb(jsonObject.getString("appaEstdWeb"));
            
            if (!jsonObject.containsKey("apmaEstdWeb") || jsonObject.isNull("apmaEstdWeb") || jsonObject.getString("apmaEstdWeb").trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'apmaEstdWeb' es obligatorio.");
                return;
            }
            nuevoEstudiante.setApmaEstdWeb(jsonObject.getString("apmaEstdWeb"));

            if (!jsonObject.containsKey("nombEstdWeb") || jsonObject.isNull("nombEstdWeb") || jsonObject.getString("nombEstdWeb").trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombEstdWeb' es obligatorio.");
                return;
            }
            nuevoEstudiante.setNombEstdWeb(jsonObject.getString("nombEstdWeb"));
            
            if (jsonObject.containsKey("fechNaciEstdWeb") && !jsonObject.isNull("fechNaciEstdWeb")) {
                try {
                    Date fechaNacimiento = DATE_FORMATTER.parse(jsonObject.getString("fechNaciEstdWeb"));
                    nuevoEstudiante.setFechNaciEstdWeb(fechaNacimiento);
                } catch (ParseException e) {
                    LOGGER.log(Level.WARNING, "Formato de fecha inválido para fechNaciEstdWeb", e);
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fecha inválido para Fecha de Nacimiento. Usar YYYY-MM-DD.");
                    return;
                }
            } else {
                 sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'fechNaciEstdWeb' es obligatorio.");
                return;
            }

            if (!jsonObject.containsKey("logiEstd") || jsonObject.isNull("logiEstd") || jsonObject.getString("logiEstd").trim().isEmpty()) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'logiEstd' es obligatorio.");
                return;
            }
            nuevoEstudiante.setLogiEstd(jsonObject.getString("logiEstd"));

            // IMPORTANTE: passEstd NO se maneja aquí. Se debe establecer un valor por defecto o manejarlo en el login/registro inicial.
            // Para este CRUD, asumimos que ya existe o se manejará por separado.
            // Si es un nuevo estudiante, quizás necesites generar una contraseña temporal aquí
            // o tener un flujo de "registro" donde sí se pida.
            // Por ahora, si la entidad lo requiere como @NotNull, debemos poner algo.
            // Si el flujo es: login -> mantenimiento, entonces el passEstd ya existe.
            // Si es "agregar nuevo estudiante", se necesita una estrategia para passEstd.
            // Para que el JPA no falle si es @NotNull, se podría asignar un valor temporal si se está creando un nuevo registro
            // que luego el usuario deberá cambiar. Aquí asumo que el caso de uso es modificar estudiantes existentes
            // o agregar nuevos donde el passEstd se manejará en otro flujo (ej. se envía un default desde el cliente no editable).
            // La entidad lo tiene como NotNull, así que si se crea desde cero, es un problema.
            // Para este ejemplo, si se crea un nuevo estudiante desde esta interfaz, le asignaremos un valor por defecto a passEstd.
            // Esto DEBE ser revisado según tu lógica de negocio para nuevos estudiantes.
            if (nuevoEstudiante.getCodiEstdWeb() == null) { // Solo si es un nuevo estudiante
                 if (jsonObject.containsKey("passEstd") && !jsonObject.isNull("passEstd") && !jsonObject.getString("passEstd").trim().isEmpty()) {
                    nuevoEstudiante.setPassEstd(jsonObject.getString("passEstd")); // Idealmente, esto se hashea
                 } else {
                    // Opción 1: Error si no se provee (mejor si es un campo visible en "agregar")
                    // sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "La contraseña es obligatoria para nuevos estudiantes.");
                    // return;
                    // Opción 2: Valor por defecto (menos seguro, pero cumple con not-null)
                    nuevoEstudiante.setPassEstd("defaultPassword123"); // ¡CAMBIAR ESTO EN PRODUCCIÓN!
                 }
            }


            estudianteDAO.create(nuevoEstudiante);
            response.setStatus(HttpServletResponse.SC_CREATED);
            sendJsonResponse(response, estudianteToJson(nuevoEstudiante));

        } catch (JsonParsingException e) {
            LOGGER.log(Level.WARNING, "Error parseando JSON en doPost", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en doPost", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor al crear estudiante.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("codiEstdWeb");
        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante (codiEstdWeb) es requerido como parámetro en la URL para actualizar.");
            return;
        }

        try (JsonReader jsonReader = Json.createReader(new StringReader(request.getReader().lines().reduce("", (s1, s2) -> s1 + s2)))) {
            JsonObject jsonObject = jsonReader.readObject();
            Integer id = Integer.parseInt(idParam);
            Estudiantesweb estudianteExistente = estudianteDAO.findEstudiantesweb(id);

            if (estudianteExistente == null) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado para actualizar.");
                return;
            }

            // Actualizar campos. No se actualiza codiEstdWeb ni passEstd.
            if (jsonObject.containsKey("ndniEstdWeb")) {
                if (jsonObject.isNull("ndniEstdWeb") || jsonObject.getString("ndniEstdWeb").trim().isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'ndniEstdWeb' no puede estar vacío.");
                    return;
                }
                estudianteExistente.setNdniEstdWeb(jsonObject.getString("ndniEstdWeb"));
            }
            if (jsonObject.containsKey("appaEstdWeb")) {
                 if (jsonObject.isNull("appaEstdWeb") || jsonObject.getString("appaEstdWeb").trim().isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'appaEstdWeb' no puede estar vacío.");
                    return;
                }
                estudianteExistente.setAppaEstdWeb(jsonObject.getString("appaEstdWeb"));
            }
            if (jsonObject.containsKey("apmaEstdWeb")) {
                 if (jsonObject.isNull("apmaEstdWeb") || jsonObject.getString("apmaEstdWeb").trim().isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'apmaEstdWeb' no puede estar vacío.");
                    return;
                }
                estudianteExistente.setApmaEstdWeb(jsonObject.getString("apmaEstdWeb"));
            }
            if (jsonObject.containsKey("nombEstdWeb")) {
                 if (jsonObject.isNull("nombEstdWeb") || jsonObject.getString("nombEstdWeb").trim().isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'nombEstdWeb' no puede estar vacío.");
                    return;
                }
                estudianteExistente.setNombEstdWeb(jsonObject.getString("nombEstdWeb"));
            }
            if (jsonObject.containsKey("fechNaciEstdWeb")) {
                if (jsonObject.isNull("fechNaciEstdWeb")) {
                     sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'fechNaciEstdWeb' no puede ser nulo si se provee.");
                     return;
                }
                try {
                    Date fechaNacimiento = DATE_FORMATTER.parse(jsonObject.getString("fechNaciEstdWeb"));
                    estudianteExistente.setFechNaciEstdWeb(fechaNacimiento);
                } catch (ParseException e) {
                    LOGGER.log(Level.WARNING, "Formato de fecha inválido para fechNaciEstdWeb en PUT", e);
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato de fecha inválido para Fecha de Nacimiento. Usar YYYY-MM-DD.");
                    return;
                }
            }
             if (jsonObject.containsKey("logiEstd")) {
                 if (jsonObject.isNull("logiEstd") || jsonObject.getString("logiEstd").trim().isEmpty()) {
                    sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "El campo 'logiEstd' no puede estar vacío.");
                    return;
                }
                estudianteExistente.setLogiEstd(jsonObject.getString("logiEstd"));
            }
            
            // passEstd no se modifica aquí, se hace a través de un flujo dedicado "cambiar contraseña"

            estudianteDAO.edit(estudianteExistente);
            sendJsonResponse(response, estudianteToJson(estudianteExistente));

        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de estudiante inválido: " + idParam, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante inválido.");
        } catch (JsonParsingException e) {
            LOGGER.log(Level.WARNING, "Error parseando JSON en doPut", e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Formato JSON inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "Intento de editar entidad no existente", e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado para actualizar.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en doPut", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor al actualizar estudiante.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("codiEstdWeb");
        if (idParam == null || idParam.isEmpty()) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante (codiEstdWeb) es requerido como parámetro en la URL para eliminar.");
            return;
        }

        try {
            Integer id = Integer.parseInt(idParam);
            estudianteDAO.destroy(id);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // O SC_OK con un mensaje de éxito
            // Si quieres enviar un mensaje de éxito:
            // JsonObject successJson = Json.createObjectBuilder().add("message", "Estudiante eliminado exitosamente.").build();
            // sendJsonResponse(response, successJson);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "ID de estudiante inválido para eliminar: " + idParam, e);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "ID de estudiante inválido.");
        } catch (NonexistentEntityException e) {
            LOGGER.log(Level.WARNING, "Intento de eliminar entidad no existente", e);
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Estudiante no encontrado para eliminar.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en doDelete", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor al eliminar estudiante.");
        }
    }

    @Override
    public String getServletInfo() {
        return "Servlet para CRUD de Estudiantesweb";
    }
}