package mundo;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cliente
{
	private static final int PUERTO = 8080;
	private String ip;
	private boolean corriendo;
	private Logger logger;
	private String nombreCliente;

	//Protocolos
	private static final String SALUDO = "Hola";
	private static final String ENVIADO = "ENVIADO: ";
	private static final String RECIBIDO = "RECIBIDO: ";
	private static final String OK = "Ok";
	private static final String PREPARADO = "Preparado";
	private static final String LONGITUD = "Longitud:";
	private static final String RECIBIENDO = "Recibiendo";
	private static final String CHECKSUM = "Checksum:";
	private static final String RUTA_FINAL_ARCHIVO = "./data/Descargado.txt";
	private static final String CONSISTENTE = "Consistente";
	private static final String INCONSISTENTE = "Inconsistente";
	private static final String CERRAR = "Cerrar";
	private static final String TIEMPO = "Tiempo:";
	
	private static final String RUTA_LOG = "./data/logs/log_";
	private static final String TXT = ".txt";
	private static final String ESPACIO = "-----------------------------------";

	public Cliente()
	{
		logger =  new Logger();
	}
	
	public void peticion()
	{
		this.ip = "52.90.111.73";
		try
		{
			Socket socketCliente = new Socket(InetAddress.getByName(ip),PUERTO);
			PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
			long tFinal = 0;
			
			//Saludo inicial
			out.println(SALUDO);
			System.out.println(ENVIADO + SALUDO);
			corriendo = true;
			String data = null;

			while(corriendo)
			{
				while(corriendo && (data = in.readLine()) != null)
				{
					if(data.contains(OK))
					{
						String cliente = data.split(":")[1];
						logger.log(ESPACIO, RUTA_LOG + cliente + TXT);
						logger.log(RECIBIDO + data, RUTA_LOG + cliente + TXT);
						System.out.println(RECIBIDO + data);
						//Confirmación de estado de espera
						out.println(PREPARADO);
						logger.log(ENVIADO + PREPARADO, RUTA_LOG + cliente + TXT);
						System.out.println(ENVIADO + PREPARADO);
					}
					if(data.contains(LONGITUD))
					{
						int longitudArchivo = Integer.parseInt(data.split(":")[1]);
						String cliente = data.split(":")[2];
						logger.log(RECIBIDO + "Longitud de archivo = " + longitudArchivo + "bytes", 
								RUTA_LOG + cliente + TXT);
						System.out.println(RECIBIDO + "Longitud de archivo = " + longitudArchivo);
						logger.log(RECIBIENDO , 
								RUTA_LOG + cliente + TXT);
						out.println(RECIBIENDO);
						System.out.println(ENVIADO + RECIBIENDO);
						//Se descarga el archivo
						tFinal = descargarArchivo(socketCliente,longitudArchivo);
					}
					if(data.contains(CHECKSUM))
					{
						System.out.println(RECIBIDO + CHECKSUM);
						//Se calcula el hash del archivo
						String  miChecksum = checkSum(RUTA_FINAL_ARCHIVO);
						String checksumOriginal = data.split(":")[1];
						System.out.println("INFO: Calculando Checksum \n" + "Original: " + checksumOriginal + "\n" 
								+ "Actual: " + miChecksum);
						String cliente = data.split(":")[2];
						if(miChecksum.equals(checksumOriginal))
						{
							out.println(CONSISTENTE + ":" + tFinal);
							logger.log(CONSISTENTE + ":" + tFinal, RUTA_LOG + cliente + TXT);
							System.out.println(ENVIADO + CONSISTENTE);
						}
						else
						{
							out.println(INCONSISTENTE);
							logger.log(INCONSISTENTE + ":" + tFinal, RUTA_LOG + cliente + TXT);
							System.out.println(ENVIADO + INCONSISTENTE);
							corriendo = false;
						}
					}
					if(data.contains(TIEMPO))
					{
						long tiempo = Long.parseLong(data.split(":")[1]);
						String cliente = data.split(":")[2];
						logger.log(RECIBIDO + "Tiempo de transferencia = " + (double)(tiempo/1000) + "s", RUTA_LOG + cliente + TXT);
						System.out.println(RECIBIDO + "Tiempo de transferencia = " + (double)(tiempo/1000) + "s");
						logger.log(CERRAR, RUTA_LOG + cliente + TXT);
						out.println(CERRAR);
						System.out.println(ENVIADO + CERRAR);
						corriendo = false;
					}
				}
			}
			out.flush();
			out.close();
			socketCliente.close();
		}
		catch (Exception e) {
			System.out.println("ERROR: No se pudo crear el cliente");
			//e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		//String ip = "52.90.111.73";
		String ip = args[0];
		Cliente cliente = new Cliente();
		cliente.peticion();

	}

	private long descargarArchivo(Socket socketCliente, int tamanioArchivo) throws IOException 
	{
		System.out.println("INFO: Descargando Archivo..." );
		DataInputStream dis = new DataInputStream(socketCliente.getInputStream());
		FileOutputStream fos = new FileOutputStream(RUTA_FINAL_ARCHIVO);
		byte[] buffer = new byte[4096];

		//		int filesize = 3307868; // Send file size in separate msg
		int leer = 0;
		int totalLeido = 0;
		int remaining = tamanioArchivo;
		while((leer = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
			totalLeido += leer;
			remaining -= leer;
			//System.out.println("read " + totalLeido + " bytes.");
			fos.write(buffer, 0, leer);
		}
		long tFinal = System.currentTimeMillis();
		fos.close();
		return tFinal;
		//dis.close();
	}
	
	private String checkSum(String path)
	{
        String checksum = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            MessageDigest md = MessageDigest.getInstance("MD5");
          
            //Using MessageDigest update() method to provide input
            byte[] buffer = new byte[8192];
            int numOfBytesRead;
            while( (numOfBytesRead = fis.read(buffer)) > 0){
                md.update(buffer, 0, numOfBytesRead);
            }
            byte[] hash = md.digest();
            checksum = new BigInteger(1, hash).toString(16); //don't use this, truncates leading zero
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
          
       return checksum;
    }



}
