package modularization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Properties;

import parallelization.CallbackReceiver;

public class FileReaderModule extends ModuleImpl {
	
	public static final String PROPERTYKEY_INPUTFILE = "inputfile";
	private File file;
	
	public FileReaderModule(CallbackReceiver callbackReceiver, Properties properties) throws Exception {
		super(callbackReceiver,properties);
		
		// set I/O -- no other inputs allowed here (we'll read the file)
		this.setInputReader(null);
		this.setInputStream(null);
		
		// Add description for properties
		this.getPropertyDescriptions().put(PROPERTYKEY_INPUTFILE, "Path to the input file");
	}

	@Override
	public boolean process() throws Exception {
		
		/*
		 * write to both output channels (stream/writer) -- if the first one
		 * is not connected to another module's input, it will throw an
		 * exception (that we will catch)
		 */
		try {
			// Instanciate a new input stream
			FileInputStream fileInputStream = new FileInputStream(this.file);
			
			// Define input buffer
			byte[] buffer = new byte[1024];
			
			// Read file data into buffer and write to outputstream
			int readbytes = fileInputStream.read(buffer);
			while (readbytes>0){
				this.getOutputStream().write(buffer);
				readbytes = fileInputStream.read(buffer);
			}
			
			// close relevant I/O instances
			fileInputStream.close();
			this.getOutputStream().close();
			
		} catch (Exception e){
			/* The inputstream does not seem to be connected -- try reader/writer instead */
			
			// Instanciate a new reader
			FileReader fileReader = new FileReader(this.file);
			
			// Define input buffer
			char[] buffer = new char[1024];
			
			// Read file data into buffer and output to writer
			while(fileReader.ready()){
				fileReader.read(buffer);
				this.getOutputWriter().write(buffer);
			}
			
			// close relevant I/O instances
			fileReader.close();
			this.getOutputWriter().close();
		}
		
		// Success
		return true;
	}

	@Override
	protected void applyProperties() throws Exception {
		if (this.getProperties().containsKey(PROPERTYKEY_INPUTFILE))
			this.file = new File(this.getProperties().getProperty(PROPERTYKEY_INPUTFILE));
		super.applyProperties();
	}

}
