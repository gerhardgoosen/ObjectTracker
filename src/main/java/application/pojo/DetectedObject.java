package application.pojo;

import org.opencv.core.Scalar;

public class DetectedObject {


	private int xPos, yPos;
	private String type;
	private Scalar HSVmin, HSVmax;
	private Scalar Color;
	private Scalar fillColor;

	public DetectedObject( ){

	}

	public DetectedObject(Scalar color){
		setColor(color);
		setType("Custom");
	}
	public DetectedObject(String name){
		setType(name);


		if(name=="blue"){

			//TODO: use "calibration mode" to find HSV min
			//and HSV max values


//			setHSVmin(new Scalar(80,52.6,0));
//			setHSVmax(new Scalar(128.5,254,255));

			setHSVmin(new Scalar(75,194,31));
			setHSVmax(new Scalar(107,256,256));

			//BGR value for Blue:
			setColor( new Scalar(255,0,0));
			setFillColor(new Scalar(255,0,0,0.5));

		}


//		if(name=="blue"){
//
//			//TODO: use "calibration mode" to find HSV min
//			//and HSV max values
//
//			setHSVmin(new Scalar(92,0,0));
//			setHSVmax(new Scalar(124,256,256));
//
//			//BGR value for Blue:
//			setColor(new Scalar(255,0,0));
//			setFillColor(new Scalar(255,0,0,0.5));
//
//		}

		if(name=="green"){

			//TODO: use "calibration mode" to find HSV min
			//and HSV max values

//			setHSVmin(new Scalar(34,50,50));
//			setHSVmax(new Scalar(80,220,200));

			setHSVmin(new Scalar(55,80,7));
			setHSVmax(new Scalar(87,176,256));

			//BGR value for Green:
			setColor(new Scalar(0,255,0));
			setFillColor(new Scalar(0,255,0,50));

		}

		if(name=="yellow"){

			//TODO: use "calibration mode" to find HSV min
			//and HSV max values

		    //setHSVmin(new Scalar(20,124,123));
			//setHSVmax(new Scalar(30,256,256));

			setHSVmin(new Scalar(20,60,123));
			setHSVmax(new Scalar(50,200,256));

			//BGR value for Yellow:
			setColor(new Scalar(0,255,255));
			setFillColor(new Scalar(0,255,255,0));

		}

		if(name=="red"){

			//TODO: use "calibration mode" to find HSV min
			//and HSV max values

//			setHSVmin(new Scalar(0,200,0));
//			setHSVmax(new Scalar(19,255,255));

			setHSVmin(new Scalar(132,127,78));
			setHSVmax(new Scalar(180,255,113));

			//BGR value for Red:
			setColor(new Scalar(0,0,255,255));
			setFillColor(new Scalar(0,0,255,50));
		}
	}




	public int getXPos() {
		return xPos;
	}
	public void setXPos(int xPos) {
		this.xPos = xPos;
	}
	public int getYPos() {
		return yPos;
	}
	public void setYPos(int yPos) {
		this.yPos = yPos;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Scalar getHSVmin() {
		return HSVmin;
	}
	public void setHSVmin(Scalar hSVmin) {
		HSVmin = hSVmin;
	}
	public Scalar getHSVmax() {
		return HSVmax;
	}
	public void setHSVmax(Scalar hSVmax) {
		HSVmax = hSVmax;
	}
	public Scalar getColor() {
		return Color;
	}
	public void setColor(Scalar color) {
		Color = color;
	}




	public Scalar getFillColor() {
		return fillColor;
	}




	public void setFillColor(Scalar fillColor) {
		this.fillColor = fillColor;
	}







}
