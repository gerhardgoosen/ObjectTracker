package application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;

import application.pojo.DetectedObject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ObjectRecogController {

	@FXML
	private Button cameraButton, saveColorButton;
	@FXML
	private ImageView originalFrame, maskImage, morphImage;

	@FXML
	private CheckBox fill, fineTune, detectColors;

	@FXML
	private Slider hueStart, hueStop, saturationStart, saturationStop, valueStart, valueStop;
	@FXML
	private Label hueCurrentValue, satCurrentValue, valCurrentValue;

	@FXML
	private RadioButton radRed, radBlue, radGreen, radYellow;

	@FXML
	private Label hueStartLabel, hueStopLabel, satStartLabel, satStopLabel, valStartLabel, valStopLabel;

	// a timer for acquiring the video stream
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	private VideoCapture capture = new VideoCapture();
	// a flag to change the button behavior
	private boolean cameraActive;

	// property for object binding
	private ObjectProperty<String> hueValueProp, satValueProp, valValueProp;

	int FRAME_WIDTH = 800;
	int FRAME_HEIGHT = 600;
	// max number of objects to be detected in frame
	int MAX_NUM_OBJECTS = 20;
	// minimum and maximum object area
	int MIN_OBJECT_AREA = 20 * 20;
	int MAX_OBJECT_AREA = ((Double) (FRAME_HEIGHT * FRAME_WIDTH / 1.5)).intValue();
	private HashMap<String, DetectedObject> colors;

	public void init() {
		colors = new HashMap<>();
		colors.put("red", new DetectedObject("red"));
		colors.put("blue", new DetectedObject("blue"));
		colors.put("green", new DetectedObject("green"));
		colors.put("yellow", new DetectedObject("yellow"));

	}

	@FXML
	private void toggleFineTune() {
		boolean selected = fineTune.isSelected();
		System.out.println("toggleFineTune ... " + selected);

		showRadioButtons(selected);
		showHSVSliders(selected);
		showHSVValueLabels(selected);

		resetSliders();
	}



	/**
	 * The action triggered by pushing the button on the GUI
	 */
	@FXML
	private void startCamera() {
		// bind a text property with the string containing the current range of
		// HSV values for object detection
		hueValueProp = new SimpleObjectProperty<>();
		satValueProp = new SimpleObjectProperty<>();
		valValueProp = new SimpleObjectProperty<>();
		this.hueCurrentValue.textProperty().bind(hueValueProp);
		this.satCurrentValue.textProperty().bind(satValueProp);
		this.valCurrentValue.textProperty().bind(valValueProp);

		// set a fixed width for all the image to show and preserve image ratio
		this.imageViewProperties(this.originalFrame, 800);
		this.imageViewProperties(this.maskImage, 400);
		this.imageViewProperties(this.morphImage, 400);

		if (!this.cameraActive) {
			// start the video capture
			this.capture.open(0);
			// is the video stream available?
			if (this.capture.isOpened()) {
				this.cameraActive = true;

				// grab a frame every 33 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {

					@Override
					public void run() {
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);
					}
				};

				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

				// update the button content
				this.cameraButton.setText("Stop Camera");
			} else {
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
		} else {
			// the camera is not active at this point
			this.cameraActive = false;
			// update again the button content
			this.cameraButton.setText("Start Camera");

			// stop the timer
			this.stopAcquisition();
		}
	}

	/**
	 * Get a frame from the opened video stream (if any)
	 *
	 * @return the {@link Image} to show
	 */
	private Mat grabFrame() {
		Mat frame = new Mat();

		// check if the capture is open
		if (this.capture.isOpened()) {
			try {
				// read the current frame
				this.capture.read(frame);

				// if the frame is not empty, process it
				if (!frame.empty() && detectColors.isSelected()) {
					// init
					Mat blurredImage = new Mat();
					Mat hsvImage = new Mat();
					Mat mask = new Mat();
					Mat morphOutput = new Mat();

					// remove some noise
					Imgproc.blur(frame, blurredImage, new Size(7, 7));

					if (fineTune.isSelected()) {

						// get thresholding values from the UI
						// remember: H ranges 0-180, S and V range 0-255
						Scalar minValues = new Scalar(this.hueStart.getValue(), this.saturationStart.getValue(),
								this.valueStart.getValue());
						Scalar maxValues = new Scalar(this.hueStop.getValue(), this.saturationStop.getValue(),
								this.valueStop.getValue());

						Utils.onFXThread(this.hueValueProp,
								"Hue range: " + Math.floor(minValues.val[0]) + " / " + Math.floor(maxValues.val[0]));
						Utils.onFXThread(this.satValueProp,
								"Saturation range: " + Math.floor( minValues.val[1]) + " / " + Math.floor(maxValues.val[1]));
						Utils.onFXThread(this.valValueProp,
								"Value range: " + Math.floor(minValues.val[2]) + " / " + Math.floor(maxValues.val[2]));

						// Imgproc.cvtColor(blurredImage, frame,
						// Imgproc.COLOR_BGR2GRAY);

						Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

						// show the partial output
						Core.inRange(hsvImage, minValues, maxValues, mask);
						this.updateImageView(this.maskImage, Utils.mat2Image(mask));

						DetectedObject custom = new DetectedObject();
						custom.setType("FineTune Color");
						custom.setHSVmin(minValues);
						custom.setHSVmax(maxValues);
						custom.setColor(minValues);
						custom.setFillColor(maxValues);

						this.matchAndDrawColor(custom, frame, hsvImage, mask, morphOutput, fill.isSelected());

					} else {
						// create some temp fruit objects so that
						// we can use their member functions/information
						DetectedObject blue, yellow, red, green;
						blue = new DetectedObject("blue");
						yellow = new DetectedObject("yellow");
						red = new DetectedObject("red");
						green = new DetectedObject("green");

						Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

						for (String key : colors.keySet()) {
							// threshold HSV image to select tennis balls
							this.matchAndDrawColor(colors.get(key), frame, hsvImage, mask, morphOutput,
									fill.isSelected());
						}

					}

				}

			} catch (Exception e) {
				// log the (full) error
				System.err.print("Exception during the image elaboration...");
				e.printStackTrace();
			}
		}

		return frame;
	}

	private void matchAndDrawColor(DetectedObject pojo, Mat frame, Mat hsvImage, Mat mask, Mat morphOutput,
			boolean fill) {
		// show the partial output
		Core.inRange(hsvImage, pojo.getHSVmin(), pojo.getHSVmax(), mask);
		this.updateImageView(this.maskImage, Utils.mat2Image(mask));

		// morphological operators
		// dilate with large element, erode with small ones
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

		Imgproc.erode(mask, morphOutput, erodeElement);
		Imgproc.erode(morphOutput, morphOutput, erodeElement);

		Imgproc.dilate(morphOutput, morphOutput, dilateElement);
		Imgproc.dilate(morphOutput, morphOutput, dilateElement);

		// show the partial output
		this.updateImageView(this.morphImage, Utils.mat2Image(morphOutput));
		// find the tennis ball(s) contours and show them frame =

		this.findAndDrawObjects(pojo, morphOutput, frame, fill);
	}

	/**
	 * Given a binary image containing one or more closed surfaces, use it as a
	 * mask to find and highlight the objects contours
	 *
	 * @param maskedImage
	 *            the binary image to be used as a mask
	 * @param frame
	 *            the original {@link Mat} image to be used for drawing the
	 *            objects contours
	 * @return the {@link Mat} image with the objects contours framed
	 */
	private Mat findAndDrawObjects(DetectedObject pojo, Mat maskedImage, Mat frame, boolean fill) {
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();

		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {

			if (fill) {
				Imgproc.drawContours(frame, contours, 0, pojo.getColor(), 2);
				for (MatOfPoint currentContour : contours) {
					Imgproc.fillConvexPoly(frame, currentContour, pojo.getFillColor());
				}
			}

			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {

				Moments moment = Imgproc.moments(maskedImage);
				double area = moment.m00;

				if (area > MIN_OBJECT_AREA) {
					Imgproc.drawContours(frame, contours, idx, pojo.getColor());
				}

				if (area > MIN_OBJECT_AREA) {

					pojo.setXPos(((Double) (moment.m10 / area)).intValue());
					pojo.setYPos(((Double) (moment.m01 / area)).intValue());

					drawObject(pojo, frame);

				}

			}
		}

		return frame;
	}

	private void drawObject(DetectedObject theObject, Mat frame) {

		Imgproc.circle(frame, new Point(theObject.getXPos(), theObject.getYPos()), 10, new Scalar(0, 0, 255), 2);

		Imgproc.putText(frame, (theObject.getXPos()) + " , " + (theObject.getYPos()),
				new Point(theObject.getXPos(), theObject.getYPos() + 20), 1, 1, new Scalar(0, 0, 255));
		// , theObject.getColor());

		Imgproc.putText(frame, theObject.getType(), new Point(theObject.getXPos(), theObject.getYPos() - 30), 1, 2
		// , theObject.getColor());
				, new Scalar(0, 0, 255));
	}

	// private void drawObjects(List<DetectedObject> theObjects,
	// List<MatOfPoint> contours, Mat frame) {
	//
	// for (int i = 0; i < theObjects.size(); i++) {
	//
	// Imgproc.circle(frame, new Point(theObjects.get(i).getXPos(),
	// theObjects.get(i).getYPos()), 10,
	// new Scalar(0, 0, 255));
	// Imgproc.putText(frame, (theObjects.get(i).getXPos()) + " , " +
	// (theObjects.get(i).getYPos()),
	// new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos() + 20),
	// 1, 1,
	// new Scalar(0, 255, 0));
	// Imgproc.putText(frame, theObjects.get(i).getType(),
	// new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos() - 30),
	// 1, 2,
	// theObjects.get(i).getColor());
	// }
	// }
	//
	// private void drawObjects(List<DetectedObject> theObjects, Mat frame,
	// List<MatOfPoint> contours, Mat hierarchy) {
	//
	// for (int i = 0; i < theObjects.size(); i++) {
	//
	// Imgproc.circle(frame, new Point(theObjects.get(i).getXPos(),
	// theObjects.get(i).getYPos()), 5,
	// theObjects.get(i).getColor());
	// Imgproc.putText(frame, (theObjects.get(i).getXPos()) + " , " +
	// (theObjects.get(i).getYPos()),
	// new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos() + 20),
	// 1, 1,
	// theObjects.get(i).getColor());
	// Imgproc.putText(frame, theObjects.get(i).getType(),
	// new Point(theObjects.get(i).getXPos(), theObjects.get(i).getYPos() - 20),
	// 1, 2,
	// theObjects.get(i).getColor());
	// }
	// }

	/**
	 * Set typical {@link ImageView} properties: a fixed width and the
	 * information to preserve the original image ration
	 *
	 * @param image
	 *            the {@link ImageView} to use
	 * @param dimension
	 *            the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension) {
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}

	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition() {
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}

		if (this.capture.isOpened()) {
			// release the camera
			this.capture.release();
		}
	}

	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 *
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image) {
		Utils.onFXThread(view.imageProperty(), image);
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed() {
		this.stopAcquisition();
	}

	private void showRadioButtons(boolean show) {
		System.out.println("showRadioButtons -> " + show);
		editColorDefinition();

		saveColorButton.setVisible(show);
		radRed.setVisible(show);
		radGreen.setVisible(show);
		radBlue.setVisible(show);
		radYellow.setVisible(show);

	}

	private void showHSVSliders(boolean show) {
		System.out.println("showHSVSliders -> " + show);
		hueStartLabel.setVisible(show);
		hueStopLabel.setVisible(show);
		satStartLabel.setVisible(show);
		satStopLabel.setVisible(show);
		valStartLabel.setVisible(show);
		valStopLabel.setVisible(show);

		hueStart.setVisible(show);
		hueStop.setVisible(show);
		saturationStart.setVisible(show);
		saturationStop.setVisible(show);
		valueStart.setVisible(show);
		valueStop.setVisible(show);

	}

	private void showHSVValueLabels(boolean show) {
		System.out.println("showHSVValueLabels -> " + show);
		hueCurrentValue.setVisible(show);
		satCurrentValue.setVisible(show);
		valCurrentValue.setVisible(show);

	}

	private void editColorDefinition() {
		System.out.println("editColorDefinition -> ");
		final ToggleGroup colorGroup = new ToggleGroup();

		radRed.setToggleGroup(colorGroup);
		radRed.setSelected(false);
		radBlue.setToggleGroup(colorGroup);
		radBlue.setSelected(false);
		radGreen.setToggleGroup(colorGroup);
		radGreen.setSelected(false);
		radYellow.setToggleGroup(colorGroup);
		radYellow.setSelected(false);

	}

	@FXML
	public void setHSVSliderRanges() {
		System.out.println("setHSVSliderRanges -> ");

		if (radRed.isSelected()) {
			System.out.println("red");
			this.setSliderRangeForColor(colors.get("red"));
		}

		if (radGreen.isSelected()) {
			System.out.println("green");
			this.setSliderRangeForColor(colors.get("green"));
		}

		if (radBlue.isSelected()) {
			System.out.println("blue");
			this.setSliderRangeForColor(colors.get("blue"));
		}

		if (radYellow.isSelected()) {
			System.out.println("yellow");
			this.setSliderRangeForColor(colors.get("yellow"));

		}

	}

	private void setSliderRangeForColor(DetectedObject yellow) {
		Scalar hsVmin = yellow.getHSVmin();
		double hmin = hsVmin.val[0];
		double smin = hsVmin.val[1];
		double vmin = hsVmin.val[2];
		Scalar hsVmax = yellow.getHSVmax();
		double hmax = hsVmax.val[0];
		double smax = hsVmax.val[1];
		double vmax = hsVmax.val[2];

		hueStart.setValue(hmin);
		hueStop.setValue(hmax);

		saturationStart.setValue(smin);
		saturationStop.setValue(smax);

		valueStart.setValue(vmin);
		valueStop.setValue(vmax);
	}


	private void resetSliders() {
		hueStart.setValue(0.0);
		saturationStart.setValue(0.0);
		valueStart.setValue(0.0);

		hueStop.setValue(0.0);
		saturationStop.setValue(0.0);
		valueStop.setValue(0.0);

	}

	@FXML
	public void saveColor() {
		System.out.println("saveColor");

		Scalar hSVmin = new Scalar(this.hueStart.getValue(),
								   this.saturationStart.getValue(),
								   this.valueStart.getValue());
		Scalar hSVmax = new Scalar(this.hueStop.getValue(),
								   this.saturationStop.getValue(),
								   this.valueStop.getValue());

		if (radRed.isSelected()) {
			System.out.println("red");
			colors.get("red").setHSVmin(hSVmin);
			colors.get("red").setHSVmax(hSVmax);
		}

		if (radGreen.isSelected()) {
			System.out.println("green");
			colors.get("green").setHSVmin(hSVmin);
			colors.get("green").setHSVmax(hSVmax);
		}

		if (radBlue.isSelected()) {
			System.out.println("blue");
			colors.get("blue").setHSVmin(hSVmin);
			colors.get("blue").setHSVmax(hSVmax);
		}

		if (radYellow.isSelected()) {
			System.out.println("yellow");
			colors.get("yellow").setHSVmin(hSVmin);
			colors.get("yellow").setHSVmax(hSVmax);

		}
	}
}