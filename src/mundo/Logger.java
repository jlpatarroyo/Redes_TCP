package mundo;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class Logger {
	
	
	public Logger(){}
	
	public void log(String mensaje, String rutaArchivo)
	{
        File myfile = new File(rutaArchivo);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now(); 
        try
        {
        	FileWriter writer = new FileWriter(myfile, true);
            writer.write("\n" + dtf.format(now) +  " " + mensaje);
            writer.close();
        }
        catch (Exception e) {
			// TODO: handle exception
        	e.printStackTrace();
		}
	}
	
}
