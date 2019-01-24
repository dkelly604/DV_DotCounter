import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

/*
 * ImageJ Plugin to measure single dots in 2 colour (green and red) timelapse deltavision images
 * NOTE, this is a simple plugin and has no error checking for the images being acquired out of
 * order. If they aren't acquired green then red there will be problems.
 * Output is in the form of a text file which can be imported easily into Excel, R or Graphpad
 * Written by D. Kelly
 * September 2017
 */
public class DV_DotCounter_ implements PlugIn{
	ImagePlus Greeny;
	ImagePlus Reddy;
	int GreenyID;
	int ReddyID;
	double [] xval = new double[300];
	double [] yval = new double[300];
	double[] thegreenarea = new double[10];
	double[] thegreenmean = new double[10];
	double[] theredarea = new double[10];
	double[] theredmean = new double[10];
	int counter = 1;
	String filename;
	
	public void run(String arg) {
	
		IJ.run("Set Measurements...", "area mean min centroid redirect=None decimal=2");  //Set correct measurements
		new WaitForUserDialog("Open DV Image", "Open DV Images. SPLIT CHANNELS!").show(); //Ask user to open image
		IJ.run("Bio-Formats Importer");		//Open Bio_formats **Make sure its installed**
		
		/*
		 * Section to get the image name so that it can be added
		 * to the output file as identification for the results
		 */
		ImagePlus imp = WindowManager.getCurrentImage();
    	filename = imp.getTitle(); 	//Get file name
		int dotindex = filename.indexOf('.');		
		filename = filename.substring(0, dotindex + 4);
		
		/*
		 * Section to identify which channel is which, make sure 
		 * that the acquistion was performed green channel then
		 * red channel otherwise the script will measure the 
		 * dots from the wrong channel
		 */
		int[] Idlist = new int[2];
    	Idlist = WindowManager.getIDList(); //Add open images to list
    	IJ.selectWindow(Idlist[0]);  //Select first image in list
    	int maxLoop = Idlist.length; 
      	
    	for (int x=0; x<maxLoop; x++){	//Loop through the list identifying both images
    		int zproject = Idlist[x];
    		IJ.selectWindow(zproject);
    		imp = WindowManager.getCurrentImage();
    
    		//Identify green image based on acquisition
    		if(x==0){
    			Greeny = WindowManager.getCurrentImage();
    			GreenyID = Greeny.getID();
    			IJ.run(Greeny, "Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
    			IJ.run(Greeny, "Enhance Contrast", "saturated=0.35");
    		}
    		//Identify red image based on acquisition
    		if(x==1){
    			Reddy = WindowManager.getCurrentImage();
    			ReddyID = Reddy.getID();
    			IJ.run(Reddy, "Set Scale...", "distance=0 known=0 pixel=1 unit=pixel");
    			IJ.run(Reddy, "Enhance Contrast", "saturated=0.35");
    		}
    		
    	}
    	
    	SelectCells();
    	new WaitForUserDialog("Finished", "All Done").show();
	}
	
	public void SelectCells(){
		String response; 
		
		IJ.selectWindow(ReddyID); //Selects the red channel to identify the dot position
		IJ.setTool("freehand");
		 
		do{
			IJ.selectWindow(ReddyID);
			new WaitForUserDialog("Pick One", "Select Timepoint and Draw Round a Cell").show(); //User draws round a cell
			int tPoint = Reddy.getCurrentSlice();	//sets the timepoint
			IJ.run("ROI Manager...", "");	//Opens ROI Manager
			IJ.setAutoThreshold(Reddy, "Yen dark");  //Autothreshold within ROI using Yen
			RoiManager rm = new RoiManager(); //declare ROI manager variable type
			rm = RoiManager.getInstance();	//Initialise ROI manager variable
			IJ.run(Reddy, "Analyze Particles...", "size=3-Infinity pixel display clear add slice"); //Measure dot within ROI	
			int numROI = rm.getCount();		//Count how many dots were found (usually 1)
			ResultsTable rt = new ResultsTable(); //Declare Results table variable to access measurements
			rt = Analyzer.getResultsTable(); //Retrieve Results table measurements
			int numvals = rt.getCounter(); //Count how many rows are in the Results table
			/*
			 * Loop to place every value in every results table
			 * row into 4 different arrays. Arrays are for dot area,
			 * dot mean intensity, X and Y positions which are used
			 * for the label position
			 */
			for (int a=0; a<numvals; a++){
				//RED VALUES
				theredarea[a] = rt.getValueAsDouble(0, a);
				theredmean[a] = rt.getValueAsDouble(1, a);
				xval[counter] = rt.getValueAsDouble(6, 0);
				yval[counter] = rt.getValueAsDouble(7, 0);
			}
			
			//Calls method to label the cell with a number so that its not counted twice
			setImageNumbers();		
			
			//Apply the dot ROI from the red image to the green image
			IJ.selectWindow(GreenyID);
			Greeny.setZ(tPoint);	//Set same timepoint as in Red image
			IJ.setAutoThreshold(Greeny, "Default");	
			double theMax = Greeny.getDisplayRangeMax(); //Get maximum display range
			
			/*
			 * Loop to measure the green dot using the ROI
			 * from the red dot or dots if more than 1 was 
			 * found.
			 */
			for (int c = 0; c<numROI; c++){
					rm.select(c);	//Select the ROI if there is more than 1 it does them in sequence
					IJ.setThreshold(Greeny, 0, theMax); //Sets threshold based on maximum pixel value
					Greeny.unlock();
					IJ.run(Greeny, "Analyze Particles...", "size=0-Infinity display clear");	//Make measurements	
					//GREEN VALUES
					thegreenarea[c] = rt.getValueAsDouble(0, 0);
					thegreenmean[c] = rt.getValueAsDouble(1, 0);
			}
			outputinfo(numvals);  //Send measurement results to output method to produce text file
			counter++;  //Counter for the cell number
			response = JOptionPane.showInputDialog("Another y/n"); //User asked if there are any more cells to measure
		}while(response.equals("y"));
	}
	
	public void setImageNumbers(){
		/*
		 * Method to label each counted cell with a number 
		 * to prevent double counting. Numbers are only
		 * added to the red image as that is the image used for
		 * selecting the cells
		 */
		int numPoints = Reddy.getNChannels();
		IJ.setForegroundColor(255, 255, 255);  //Sets the text background box colour to black
		ImageProcessor ip = Reddy.getProcessor();	//Assigns imageprocessor so that image can be worked on
			Font font = new Font("SansSerif", Font.PLAIN, 18); //Sets font for text
			ip.setFont(font);
			ip.setColor(new Color(255, 255, 0)); //Sets text colour to red
			String cellnumber = String.valueOf(counter); //Sets text to the number of the cell counter
			int xpos = (int) xval[counter];  //X position of label
			int ypos = (int) yval[counter];  //Y position of label
			
			for (int g=1; g<numPoints+1; g++){
				Reddy.setC(g);
				ip.drawString(cellnumber, xpos, ypos);
				Reddy.updateAndDraw();
			}
	}
	
	public void outputinfo(int numvals){
		/*
		 * Method to produce the output text file
		 */
		String CreateName = "C:/Temp/Results.txt";
		String FILE_NAME = CreateName;
	
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
		
			for (int d = 0; d < numvals; d++){
			bufferedWriter.write(" File= " + filename + " Cell " + counter + " Green Area " + thegreenarea[d] + " Red Area " + theredarea[d] + " Green Intensity " + thegreenmean[d] + " Red Intensity " + theredmean[d]);
			bufferedWriter.newLine();
			}
			bufferedWriter.newLine();
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	} 

}
