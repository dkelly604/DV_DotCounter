# DV_DotCounter
ImageJ plugin to analyse fluorescent dots in 3D timelapse images from a Deltavision microscope

INSTALL

1. Ensure that the ImageJ version is at least 1.5 and the installation has Java 1.8.0_60 (64bit) 
installed. If not download the latest version of ImageJ bundled with Java and install it.

2. The versions can be checked by opening ImageJ and clicking Help then About ImageJ.

3. Download the latest copy of Bio-Formats into the ImageJ plugin directory

4. Create a directory in the C: drive called Temp (case sensitive)

5. Using notepad save a blank .txt file called Results.txt into the Temp directory
you previously created (also case sensitive).

6. Place DV_DotCounter.jar into the plugins directory of your ImageJ installation.

7. If everything has worked DV DotCounter should be in the Plugins menu.

8. DV_DotCounter.java is the editable code for the plugin should improvements or changes be required.

USAGE

1. You will be prompted to Open DV Images. The plugin was written for 2 channel timelapse 
deltavision images acquired Green channel then Red Channel. It will probably work on 
non timelapse images but it will cause problems if the channel order is reversed.

2. When the Bio-Formats dialogue opens make sure that the only tick is in Split Channels,
nothing else should be ticked.

3. Once the images have opened you will be prompted to select a timepoint and draw round
a cell with an interesting dot in it. Draw round the cell and click OK. NOTE be careful not 
to change the active image to the green channel or you will measure the wrong channel.

4. The measurments will be made automatically and you will be asked whether or not you want 
to measure another cell. The cell numbers of all previously counted cells will be marked 
on the red image.

5. Results are saved to the text file you should have created in C:\Temp
