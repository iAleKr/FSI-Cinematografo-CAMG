
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class Conexion {

    public Connection con;

    //-------------------------------------------------------------------------------
    public int conectar(String servidor, String base, String usuario, String clave) {
        try {
            // Se utiliza el driver moderno com.mysql.cj.jdbc.Driver si usas MySQL 8+
            String url = "jdbc:mysql://" + servidor + ":3306/" + base + "?useSSL=false&serverTimezone=UTC";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                Class.forName("com.mysql.jdbc.Driver");
            }
            con = DriverManager.getConnection(url, usuario, clave);
            return 1;
        } catch (Exception e) {
            System.out.println("Error de conexión: " + e.getMessage());
            return 0;
        }
    }
    //-------------------------------------------------------------------------------

    public int desconectar() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
            return 1;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return 0;
        }
    }

    //--------------------------------------------------------------------------
    //-------- Muestra el resultado de una consulta en una JTable 
    //--------------------------------------------------------------------------
    public int entablar(String consulta, JTable malla) {
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(consulta);
            ResultSetMetaData rsm = rs.getMetaData();
            DefaultTableModel modelo = (DefaultTableModel) malla.getModel();

            // Limpiar tabla
            modelo.setColumnCount(0);
            modelo.setRowCount(0);

            // Agregar las columnas del resultado con su nombre
            for (int k = 1; k <= rsm.getColumnCount(); k++) {
                modelo.addColumn(rsm.getColumnName(k).toUpperCase());
            }

            // Configurar renderizadores de columnas de imagen fuera del bucle
            for (int k = 1; k <= rsm.getColumnCount(); k++) {
                int tipo = rsm.getColumnType(k);
                if (tipo == Types.BLOB || tipo == Types.LONGVARBINARY || tipo == Types.VARBINARY || tipo == Types.BINARY) {
                    int w = malla.getColumnModel().getColumn(k - 1).getWidth();
                    int h = malla.getRowHeight();
                    malla.getColumnModel().getColumn(k - 1).setCellRenderer(new ImageRenderer(w, h));
                }
            }

            // Agregar renglones con los datos
            int renglon = 0;
            while (rs.next()) {
                modelo.insertRow(renglon, new Object[rsm.getColumnCount()]);
                for (int k = 1; k <= rsm.getColumnCount(); k++) {
                    int tipo = rsm.getColumnType(k);
                    if (tipo == Types.BLOB || tipo == Types.LONGVARBINARY || tipo == Types.VARBINARY || tipo == Types.BINARY) {
                        byte[] bytes = rs.getBytes(k);
                        if (bytes != null && bytes.length > 0) {
                            modelo.setValueAt(new ImageIcon(bytes), renglon, k - 1);
                        } else {
                            modelo.setValueAt(null, renglon, k - 1);
                        }
                    } else { // Cualquier otro tipo que se puede manejar como cadena
                        modelo.setValueAt(rs.getString(k), renglon, k - 1);
                    }
                }
                renglon++;
            }

            return 1; // 👈 Retorna 1 si se ejecutó correctamente

        } catch (SQLException ex) {
            System.out.println("Error en entablar: " + ex.getMessage());
            return 0;
        }
    }

    //--------------------------------------------------------------------------
    //-------- Muestra el resultado de una consulta en una JTable 
    //-------- adaptando el ancho de las columnas
    //--------------------------------------------------------------------------
    public int entablar(String consulta, JTable malla, double... porcentajes) {
        int res = entablar(consulta, malla);
        if (res == 1) {
            setAnchoColumnas(malla, porcentajes);
        }
        return res;
    }

    //--------------------------------------------------------------------------
    //-------- Cambia el ancho de las columnas de una tabla en porcentajes
    //--------------------------------------------------------------------------
    public static void setAnchoColumnas(JTable tabla, double... porcentajes) {
        double total = 0;
        int anchoTabla = tabla.getWidth();

        for (int i = 0; i < tabla.getColumnModel().getColumnCount(); i++) {
            if (i < porcentajes.length) {
                total += porcentajes[i];
            }
        }

        if (total == 0) {
            return;
        }

        for (int i = 0; i < tabla.getColumnModel().getColumnCount(); i++) {
            if (i < porcentajes.length) {
                TableColumn column = tabla.getColumnModel().getColumn(i);
                column.setPreferredWidth((int) (anchoTabla * (porcentajes[i] / total)));
            }
        }
    }

    //--------------------------------------------------------------------------
    //-------- Regresa el valor de una consulta de un solo dato
    //--------------------------------------------------------------------------
    public String obtenerDato(String consulta) {
        String resultado = "";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(consulta);
            if (rs.next()) {
                resultado = rs.getString(1);
            }
            return resultado;
        } catch (Exception e) {
            return resultado;
        }
    }

    //--------------------------------------------------------------------------
    //-------- Regresa el valor de un campo que coincida con un dato dado
    //--------------------------------------------------------------------------
    public String buscarDato(String tabla, String columnaDeseada, String campoBuscado, String valorBuscado) {
        String consulta = "SELECT " + columnaDeseada + " FROM " + tabla
                + " WHERE " + campoBuscado + " = ?";
        String resultado = "";

        try (PreparedStatement ps = con.prepareStatement(consulta)) {
            ps.setString(1, valorBuscado);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                resultado = rs.getString(columnaDeseada);
            }
            return resultado;
        } catch (Exception e) {
            return resultado;
        }
    }

    //--------------------------------------------------------------------------
    //-------- Muestra los datos de un campo en un combobox
    //--------------------------------------------------------------------------
    public void seleccionar(String consulta, JComboBox box) {
        ArrayList<String> datos = new ArrayList<>();
        try {
            Statement ps = con.createStatement();
            ResultSet rs = ps.executeQuery(consulta);
            while (rs.next()) {
                datos.add(rs.getString(1));
            }
            if (datos.size() > 0) {
                DefaultComboBoxModel cbm = new DefaultComboBoxModel(datos.toArray());
                box.setModel(cbm);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //--------------------------------------------------------------------------
    //-------- Regresa una lista con los registros del resultado de la consulta
    //--------------------------------------------------------------------------
    public ArrayList<ArrayList<String>> consultar(String consulta) {
        ArrayList<ArrayList<String>> datos = new ArrayList<>();
        try {
            Statement ps = con.createStatement();
            ResultSet rs = ps.executeQuery(consulta);
            ResultSetMetaData rsm = rs.getMetaData();
            while (rs.next()) {
                ArrayList<String> renglon = new ArrayList<>();
                for (int k = 1; k <= rsm.getColumnCount(); k++) {
                    renglon.add(rs.getString(k));
                }
                datos.add(renglon);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return datos;
    }

    //--------------------------------------------------------------------------
    //-------- Regresa un arreglo bidimensional con los registros de la consulta
    //--------------------------------------------------------------------------
    public String[][] consultar2Array(String consulta) {
        ArrayList<ArrayList<String>> datos = consultar(consulta);
        String[][] resultado = null;
        if (!datos.isEmpty()) {
            resultado = new String[datos.size()][];
            for (int r = 0; r < datos.size(); r++) {
                ArrayList<String> ren = datos.get(r);
                resultado[r] = ren.toArray(new String[0]); // 👈 Arreglado casting de Object[] a String[]
            }
        }
        return resultado;
    }

    //--------------------------------------------------------------------------
    //-------- Obtener fecha y hora del sistema
    //--------------------------------------------------------------------------
    public String ObtenerFechaHora() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND),
                c.get(Calendar.MILLISECOND));
    }

    //--------------------------------------------------------------------------
    //-------- Obtener la fecha del sistema
    //--------------------------------------------------------------------------
    public String ObtenerFecha() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    //--------------------------------------------------------------------------
    //-------- Obtener la hora del sistema
    //--------------------------------------------------------------------------
    public String ObtenerHora() {
        Calendar c = Calendar.getInstance();
        return String.format("%02d:%02d:%02d.%03d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND),
                c.get(Calendar.MILLISECOND));
    }

    //--------------------------------------------------------------------------
    //-------- Convierte un ImageIcon a InputStream (en memoria, sin guardar en disco)
    //--------------------------------------------------------------------------
    public InputStream ImagenBytes(ImageIcon imagen) {
        if (imagen == null) {
            return null;
        }

        try {
            BufferedImage buff = new BufferedImage(
                    imagen.getIconWidth(),
                    imagen.getIconHeight(),
                    BufferedImage.TYPE_INT_RGB);

            Graphics2D g2 = buff.createGraphics();
            g2.drawImage(imagen.getImage(), 0, 0, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buff, "jpg", baos);
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (IOException ex) {
            System.out.println("Error al procesar imagen: " + ex.getMessage());
            return null;
        }
    }

    //--------------------------------------------------------------------------
    //-------- Cambia el tamaño de una imagen
    //--------------------------------------------------------------------------
    public static Image escalarImagen(Image srcImg, int ancho, int alto) {
        if (srcImg == null || ancho <= 0 || alto <= 0) {
            return null;
        }

        BufferedImage imagenEscalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagenEscalada.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, ancho, alto, null);
        g2.dispose();

        return imagenEscalada;
    }
}

//------------------------------------------------------------------------------
//---- Clase para manejar imágenes en las celdas de una JTable
//------------------------------------------------------------------------------
class ImageRenderer extends DefaultTableCellRenderer {

    private int ancho, alto;

    public ImageRenderer(int ancho, int alto) {
        setHorizontalAlignment(SwingConstants.CENTER);
        this.ancho = ancho;
        this.alto = alto;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        lbl.setText("");

        if (value instanceof String) {
            lbl.setText((String) value);
        } else if (value instanceof ImageIcon) {
            ImageIcon icon = (ImageIcon) value;
            Image imgEscalada = Conexion.escalarImagen(icon.getImage(), ancho, alto);
            if (imgEscalada != null) {
                lbl.setIcon(new ImageIcon(imgEscalada));
            }
        } else {
            lbl.setIcon(null);
        }

        return lbl;
    }
}
