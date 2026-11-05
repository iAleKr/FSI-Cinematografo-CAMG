
import java.io.Serializable;
import java.sql.PreparedStatement;
import javax.swing.ImageIcon;

public class Peliculas implements Serializable {

    // Paso 5: campos de la tabla Peliculas
    private String clave, titulo;
    private ImageIcon cartel;

    public Peliculas(String clave, String titulo, ImageIcon cartel) {
        this.clave = clave;
        this.titulo = titulo;
        this.cartel = cartel;
    }

    // Paso 6: función insertar
    public int insertar(Conexion cnx) {
        try {
            String sql = "INSERT INTO peliculas VALUES(?,?,?)";
            PreparedStatement ps = cnx.con.prepareStatement(sql);
            ps.setString(1, clave);
            ps.setString(2, titulo);
            ps.setBinaryStream(3, cnx.ImagenBytes(cartel));
            return ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
            return 0;
        }
    }
}
