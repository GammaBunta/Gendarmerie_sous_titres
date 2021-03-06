package subtitler.utils;


import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncoderProgressListener;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.MultimediaInfo;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class WaveForm {

	private static final double WAVEFORM_HEIGHT_COEFFICIENT = 1.3; // This fits the waveform to the swing node height
	private static final CopyOption[] options = new CopyOption[]{ COPY_ATTRIBUTES , REPLACE_EXISTING };
	private float[] resultingWaveform;
	private int[] wavAmplitudes;
	private String fileAbsolutePath;
	private final Random random = new Random();
	private Encoder encoder;
	private ConvertProgressListener listener = new ConvertProgressListener();
	int width;
	Pane pane;
	double startAt=0;
	double endAt;
	double timeMax;

	public void setTImeMax(double timeMax) {
		this.timeMax=timeMax;
	}

	public void setBounds(double startAt, double endAt) {
		this.startAt=startAt;
		this.endAt=endAt;
	}

	public WaveForm(String fileAbsolutePath, int width) {
		pane = new Pane();
		this.width =width;
		startService(fileAbsolutePath);
	}

	public void makeWaveForm() {
		pane.getChildren().clear();

		double start=(wavAmplitudes.length*startAt)/timeMax;
		double stop=(wavAmplitudes.length*endAt)/timeMax;

		double maxVal=0;
		for(double i=start;i<stop;i++) {
			if(wavAmplitudes[(int) i]>maxVal) {
				maxVal=wavAmplitudes[(int) i];
			}
		}

		for(double i=start;i<stop;i++) {
			Rectangle rec = new Rectangle();
			rec.setFill(Color.RED);
			rec.setWidth(1);
			rec.setHeight( pane.getPrefHeight()*(wavAmplitudes[(int)i]/maxVal) );
			rec.setLayoutX((i-start)*(pane.getPrefWidth()/(stop-start)));
			rec.setLayoutY(pane.getPrefHeight()-(wavAmplitudes[(int)i]/maxVal)*pane.getPrefHeight());
			pane.getChildren().add(rec);
		}
	}

	public Pane getPane() {
		return pane;
	}

	/**
	 * Start the external Service Thread.
	 *
	 * 
	 */
	public void startService(String fileAbsolutePath) {

		//Variables
		this.fileAbsolutePath = fileAbsolutePath;

		try {
			System.out.println("Return "+call());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected Boolean call() throws Exception {

		try {
			String fileFormat = "mp3";
			resultingWaveform = processFromNoWavFile(fileFormat);
		} catch (Exception ex) {
			ex.printStackTrace();
			if (ex.getMessage().contains("There is not enough space on the disk")) {
				System.err.println("Not enough disk space");
			}
			return false;
		}

		return true;

	}

	/**
	 * Try to process a Non Wav File
	 * 
	 * @param fileFormat
	 * @return
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws EncoderException
	 */
	private float[] processFromNoWavFile(String fileFormat) throws IOException , UnsupportedAudioFileException , EncoderException {
		int randomN = random.nextInt(99999);

		//Create temporary files
		File temporalDecodedFile = File.createTempFile("decoded_" + randomN, ".wav");
		File temporalCopiedFile = File.createTempFile("original_" + randomN, "." + fileFormat);

		//Delete temporary Files on exit
		temporalDecodedFile.deleteOnExit();
		temporalCopiedFile.deleteOnExit();

		//Create a temporary path
		Files.copy(new File(fileAbsolutePath).toPath(), temporalCopiedFile.toPath(), options);

		//Transcode to .wav
		transcodeToWav(temporalCopiedFile, temporalDecodedFile);

		//Avoid creating amplitudes again for the same file
		if (wavAmplitudes == null)
			wavAmplitudes = getWavAmplitudes(temporalDecodedFile);

		//Delete temporary files
		temporalDecodedFile.delete();
		temporalCopiedFile.delete();

		return processAmplitudes(wavAmplitudes);
	}

	/**
	 * Transcode to Wav
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 * @throws EncoderException
	 */
	private void transcodeToWav(File sourceFile , File destinationFile) throws EncoderException {
		//Attributes atters = DefaultAttributes.WAV_PCM_S16LE_STEREO_44KHZ.getAttributes()
		try {

			//Set Audio Attributes
			AudioAttributes audio = new AudioAttributes();
			audio.setCodec("pcm_s16le");
			audio.setChannels(2);
			audio.setSamplingRate(44100);

			//Set encoding attributes
			EncodingAttributes attributes = new EncodingAttributes();
			attributes.setFormat("wav");
			attributes.setAudioAttributes(audio);

			//Encode
			encoder = encoder != null ? encoder : new Encoder();
			encoder.encode(sourceFile, destinationFile, attributes, listener);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Get Wav Amplitudes
	 * 
	 * @param file
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
	private int[] getWavAmplitudes(File file) throws UnsupportedAudioFileException , IOException {
		//Get Audio input stream
		try (AudioInputStream input = AudioSystem.getAudioInputStream(file)) {
			AudioFormat baseFormat = input.getFormat();

			//Encoding
			Encoding encoding = AudioFormat.Encoding.PCM_UNSIGNED;
			float sampleRate = baseFormat.getSampleRate();
			int numChannels = baseFormat.getChannels();

			AudioFormat decodedFormat = new AudioFormat(encoding, sampleRate, 16, numChannels, numChannels * 2, sampleRate, false);
			int available = input.available();

			//Get the PCM Decoded Audio Input Stream
			try (AudioInputStream pcmDecodedInput = AudioSystem.getAudioInputStream(decodedFormat, input)) {
				final int BUFFER_SIZE = 4096; //this is actually bytes

				//Create a buffer
				byte[] buffer = new byte[BUFFER_SIZE];

				//Now get the average to a smaller array
				int maximumArrayLength = 100000;
				int[] finalAmplitudes = new int[maximumArrayLength];
				int samplesPerPixel = available / maximumArrayLength;

				//Variables to calculate finalAmplitudes array
				int currentSampleCounter = 0;
				int arrayCellPosition = 0;
				float currentCellValue = 0.0f;

				//Variables for the loop
				int arrayCellValue = 0;

				//Read all the available data on chunks
				while (pcmDecodedInput.read(buffer, 0, BUFFER_SIZE) > 0)
					for (int i = 0; i < buffer.length - 1; i += 2) {

						//Calculate the value
						arrayCellValue = (int) ( ( ( ( ( buffer[i + 1] << 8 ) | buffer[i] & 0xff ) << 16 ) / 32767 ) * WAVEFORM_HEIGHT_COEFFICIENT );

						//Tricker
						if (currentSampleCounter != samplesPerPixel) {
							++currentSampleCounter;
							currentCellValue += Math.abs(arrayCellValue);
						} else {
							//Avoid ArrayIndexOutOfBoundsException
							if (arrayCellPosition != maximumArrayLength)
								finalAmplitudes[arrayCellPosition] = finalAmplitudes[arrayCellPosition + 1] = (int) currentCellValue / samplesPerPixel;

							//Fix the variables
							currentSampleCounter = 0;
							currentCellValue = 0;
							arrayCellPosition += 2;
						}
					}

				return finalAmplitudes;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();

		}

		//You don't want this to reach here...
		return new int[1];
	}

	/**
	 * Process the amplitudes
	 * 
	 * @param sourcePcmData
	 * @return An array with amplitudes
	 */
	private float[] processAmplitudes(int[] sourcePcmData) {
		//The width of the resulting waveform panel
		float[] waveData = new float[width];
		int samplesPerPixel = sourcePcmData.length / width;

		//Calculate
		float nValue;
		for (int w = 0; w < width; w++) {

			//For performance keep it here
			int c = w * samplesPerPixel;
			nValue = 0.0f;

			//Keep going
			for (int s = 0; s < samplesPerPixel; s++) {
				nValue += ( Math.abs(sourcePcmData[c + s]) / 65536.0f );
			}

			//Set WaveData
			waveData[w] = nValue / samplesPerPixel;
		}

		System.out.println("Finished Processing amplitudes");
		return waveData;
	}

	public class ConvertProgressListener implements EncoderProgressListener {
		int current = 1;

		public ConvertProgressListener() {
		}

		public void message(String m) {
		}

		public void progress(int p) {

			double progress = p / 1000.00;
			System.out.println(progress);

		}

		public void sourceInfo(MultimediaInfo m) {
		}
	}

	public String getFileAbsolutePath() {
		return fileAbsolutePath;
	}

	public void setFileAbsolutePath(String fileAbsolutePath) {
		this.fileAbsolutePath = fileAbsolutePath;
	}

	public int[] getWavAmplitudes() {
		return wavAmplitudes;
	}

	public float[] getResultingWaveform() {
		return resultingWaveform;
	}

	public void setResultingWaveform(float[] resultingWaveform) {
		this.resultingWaveform = resultingWaveform;
	}

}
