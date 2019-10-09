package mundo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Servidor {

	private boolean escuchando;
	private ServerSocket socketServidor;
	private static final int PUERTO = 8080;
	private int contadorClientes;
	private File archivo;
	private Logger logger;

	private static final String SALUDO = "Hola";
	private static final String OK = "Ok";
	private static final String ENVIADO = "ENVIADO: ";
	private static final String RECIBIDO = "RECIBIDO: ";
	private static final String PREPARADO = "Preparado";
	private static final String RUTA_ARCHIVO = "./data/200MB_Sample.txt";
	private static final String LONGITUD = "Longitud:";
	private static final String RECIBIENDO = "Recibiendo";
	private static final String CHECKSUM = "Checksum:";
	private static final String CONSISTENTE = "Consistente";
	private static final String INCONSISTENTE = "Inconsistente";
	private static final String TIEMPO = "Tiempo:";
	private static final String CERRAR = "Cerrar";
	private static final String RUTA_LOG = "./data/logs/log_servidor.txt";

	public Servidor()
	{
		this.logger =  new Logger();
		logger.log("--------------------------------------------------------" + "\n", RUTA_LOG);
	}
	
	public void escuchar(String ip)
	{
		escuchando = true;
		try
		{
			socketServidor = new ServerSocket(PUERTO, 1, InetAddress.getByName(ip));
			contadorClientes = 0;
			while(escuchando)
			{
				System.out.println("INFO: Esperando conexion de cliente...");
				Socket socketCliente = socketServidor.accept();
				contadorClientes++;
				String nombreCliente = "cliente" + (contadorClientes);
				ServidorThread servidorThread = new ServidorThread(nombreCliente, socketCliente);
				servidorThread.start();
			}
		}
		catch (Exception e) 
		{
			System.out.println("ERROR: No se pudo crear el servidor");
		}
	}
	
	private synchronized void log(String nombreCliente, String mensaje)
	{
		logger.log(nombreCliente + " - " + mensaje, RUTA_LOG);
	}

	public static void main(String[] args)
	{
		String ip = args[0];
		Servidor servidor = new Servidor();
		servidor.escuchar(ip);
	}


	public class ServidorThread extends Thread
	{
		private String nombreCliente;
		private Socket socketCliente;
		private boolean corriendo;

		public ServidorThread(String nombreCliente, Socket socketCliente)
		{
			
			this.nombreCliente = nombreCliente;
			this.socketCliente = socketCliente;
			System.out.println("INFO: se ha creado un hilo para " + nombreCliente);
			//inicializarLogger();
			archivo = new File(RUTA_ARCHIVO);
		}


		@Override
		public void run() 
		{
			try
			{
				PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true);
				corriendo = true;
				System.out.println("INFO: se inicio el proceso para el cliente " + nombreCliente);
				long tInicio=0;
				long tFinal=0;
				while(corriendo)
				{

					BufferedReader in = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
					String data = in.readLine();
					if(data != null)
					{
						//Se acepta la conexion
						if(data.equals(SALUDO))
						{
							log(darNombreCliente(), "RECIBIDO: " + data);
							System.out.println("RECIBIDO: " + data);
							out.println(OK + ":" + darNombreCliente());
							System.out.println(ENVIADO + OK);				
						}
						//Se recibe la conexion para recibir archivos
						if(data.equals(PREPARADO))
						{
							log(darNombreCliente(), RECIBIDO + PREPARADO);
							System.out.println(RECIBIDO + PREPARADO);
							//Se envia la longitud del archivo

							int tamanioArchivo = (int) archivo.length();
							out.println(LONGITUD + tamanioArchivo + ":" + darNombreCliente());
							log(darNombreCliente(), ENVIADO + "Longitud del archivo = " + tamanioArchivo + "bytes");
							System.out.println(ENVIADO + "Longitud del archivo = " + tamanioArchivo);
						}
						if(data.equals(RECIBIENDO))
						{
							//Se envia el archivo
							tInicio = enviarArchivo(archivo);
							//Se envia el checksum del archivo
							String checksum = checkSum(RUTA_ARCHIVO);
							out.println(CHECKSUM + checksum+ ":" + darNombreCliente());
							log(darNombreCliente(), ENVIADO + CHECKSUM + checksum);
							System.out.println(ENVIADO + CHECKSUM + checksum);
						}
						if(data.contains(CONSISTENTE))
						{
							//Calculo de tiempo transcurrido
							log(darNombreCliente(), RECIBIDO + CONSISTENTE);
							System.out.println(RECIBIDO + CONSISTENTE);
							tFinal = Long.parseLong(data.split(":")[1]);
							System.out.println("*** " + tInicio);
							System.out.println("*** " + tFinal);

							long tTranscurrido = (tFinal - tInicio);
							out.println(TIEMPO + tTranscurrido+ ":" + darNombreCliente());
							log(darNombreCliente(), ENVIADO + TIEMPO + tTranscurrido/1000 + "s");
							System.out.println(ENVIADO + TIEMPO + tTranscurrido);
						}
						if(data.equals(INCONSISTENTE))
						{
							//Termina la conexion
							log(darNombreCliente(), RECIBIDO + INCONSISTENTE);
							System.out.println(RECIBIDO + INCONSISTENTE);
							out.println(CERRAR+ ":" + darNombreCliente());
							corriendo = false;
							in.close();
							out.flush();
							out.close();
							socketCliente.close();
							contadorClientes--;
						}
						if(data.equals(CERRAR))
						{
							//Termina la conexion
							log(darNombreCliente(), RECIBIDO + CERRAR);
							System.out.println(RECIBIDO + CERRAR);
							corriendo = false;
							in.close();
							out.flush();
							out.close();
							socketCliente.close();
							contadorClientes--;
						}
					}
				}	
			}
			catch (Exception e)
			{
				System.out.println("ERROR: No se pudo crear la conexion dedicada");
				contadorClientes--;
				e.printStackTrace();
			}
		}


		public String darNombreCliente() 
		{
			return nombreCliente;
		}
		

		private synchronized long enviarArchivo(File archivo) throws IOException
		{
			long tInicial = System.currentTimeMillis();
			DataOutputStream dos = new DataOutputStream(socketCliente.getOutputStream());
			FileInputStream fis = new FileInputStream(archivo);
			byte[] buffer = new byte[4096];

			while (fis.read(buffer) > 0) {
				dos.write(buffer);
			}
			fis.close();
			return tInicial;
			//dos.close();	
		}

		private synchronized String checkSum(String path)
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

}
