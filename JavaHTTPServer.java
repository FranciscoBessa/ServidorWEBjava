import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class JavaHTTPServer implements Runnable{ 
	
	static final File WEB_ROOT = new File("C:/caminho/do/meu/site");
	static final String DEFAULT_FILE = "pagina_1.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "menu.html";
	// porta para conexão
	static final int PORT = 8080;
	
	// variavel VER mostra o que esta sendo retornado
	static final boolean ver = true;
	
	// Conexão com o cliente via Socket
	private Socket connect;
	
	public JavaHTTPServer(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListando as conexões na porta : " + PORT + " ...\n");
			
			// fica escudando a conexão do servidor
			while (true) {
				JavaHTTPServer myServer = new JavaHTTPServer(serverConnect.accept());
				
				if (ver) {
					System.out.println("Connecton opened.");//variavel para exibição no  servidor com status da conexao
				}
				
				// criar a tread e a conexão do cliente
				Thread thread = new Thread(myServer);
				thread.start();
			}
			
		} catch (IOException e) {
			System.err.println("Erro na coneção com o servidor : " + e.getMessage());
		}
	}

	@Override
	public void run() {
		//gerencia a conexão particular com o cliente
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		
		try {
			// lemos os caracteres do cliente via fluxo de entrada no socket
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			// obtem o fluxo de saída de caractere para o cliente (para cabeçalhos)
			out = new PrintWriter(connect.getOutputStream());
			// obter fluxo de saída binária para o cliente (para dados solicitados)
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			// obter a primeira linha do pedido do cliente
			String input = in.readLine();
			// analisa a solicitação com uma string
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); //obtemos o método HTTP do cliente
			// receber o arquivo solicitado
			fileRequested = parse.nextToken().toLowerCase();
			
			// apenas os métodos GET sera utilizado
			if (!method.equals("GET")) {
				if (ver) {
					System.out.println("501 Not Implemented : " + method + " method.");
				}
				
				//retorna arquivo para o browser em caso de erro
				File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//conteúdo que retorna para a cliente
				byte[] fileData = readFileData(file, fileLength);
					
				// cabeçalhos HTTP com dados para o cliente
				out.println("HTTP/1.1 501 Not Implemented");
				out.println("Server: Java HTTP Server from SSaurel : 1.0");
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); //linha entre cabeçalhos e conteúdo, muito importante!
				out.flush(); // liberar buffer de fluxo de saída de caractere
				// arquivo
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				
			} else {
				// metodo GET
				if (fileRequested.endsWith("/")) {
					fileRequested += DEFAULT_FILE;
				}
				
				File file = new File(WEB_ROOT, fileRequested);
				int fileLength = (int) file.length();
				String content = getContentType(fileRequested);
				
				if (method.equals("GET")) { //metodo GET retorno do conteúdo
					byte[] fileData = readFileData(file, fileLength);
					
					//envio do cabeçalho HTTP
					out.println("HTTP/1.1 200 OK");
					out.println("Server: Java HTTP Server from SSaurel : 1.0");
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println(); // linha entre cabeçalhos e conteúdo, muito importante!
					out.flush(); // liberar buffer de fluxo de saída de caractere
					
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
					
				}
				
				if (ver) {
					System.out.println("File " + fileRequested + " do tipo " + content);
				}			
			}
			
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Erro arquivo não encontrado : " + ioe.getMessage());
			}
			
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // fecha a conexão socket
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 			
		}
	}
	
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;// retorna o arquivo no formato de bytes que sera lido pelo browser
	}
	
	// retorna tipos suportados
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "img/jpg";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); //linha em branco entre cabeçalho!
		out.flush(); //  fluxo de saída de caractere
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		
		if (ver) {
			System.out.println("File " + fileRequested + " not found");
		}
	}
	
}