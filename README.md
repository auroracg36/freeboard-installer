Freeboard Installer

Installer to aid in freeboard setup tasks. It loads the arduinos, calibrates the IMU, and converts charts.

Its a first cut, and probably going to have some problems. In particular its only tested in a basic way on my Linux dev systems, 
so it may not work as well, or at all on Windows or Mac. If you can test for me that would be appreciated :-)

  For charts, You will need gdal installed on you PC.
  For linux:
    'sudo apt-get install gdal-bin python-gdal imagemagick'

  For windows:
    Seems to be a difficult one :-(
  I dont have a windows PC to test on but this link seems to be the best starting point
    http://trac.osgeo.org/osgeo4w/wiki
  You will probably need to mess with installing it, and email me for help with the details - sorry.
 

Basic process is:

1. Download and install the current Arduino IDE from http://http://arduino.cc/en/Main/Software. 
Install in a directory that does NOT have spaces!! (Same for all the freeboard software)

2. Download the freeboard-installer.jar (above) into a suitable directory

3. Download the HEX files for your Arduino Mega (https://github.com/rob42/freeboardPLC/blob/master/Release1280/FreeBoardPLC.hex for Mega 1280, 
 https://github.com/rob42/freeboardPLC/blob/master/Release2560/FreeBoardPLC.hex for 2560)
 
4. Download the ArduIMU hex file (https://github.com/rob42/FreeIMU-20121106_1323/blob/master/FreeBoardIMU/target/FreeBoardIMU.cpp.hex)

5. Plug in your Mega or ArduIMU

6. Start the freeboard-installer:
	* You may be able to double-click the jar file
	OR
	* from the command line: "java -jar freeboard-installer.jar"   

7. Follow the notes on each tab

Feedback welcome:-)
