/*
 * Copyright 2012,2013 Robert Huitema robert@42.co.nz
 *
 * This file is part of FreeBoard. (http://www.42.co.nz/freeboard)
 *
 *  FreeBoard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  FreeBoard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with FreeBoard.  If not, see <http://www.gnu.org/licenses/>.
 */
package nz.co.fortytwo.freeboard.installer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.signalk.maptools.KapObserver;
import org.signalk.maptools.KapProcessor;

/**
 * Processes charts into tile pyramids, and adds config to chartplotter javascript.
 * Currently only handles BSB/KAP
 *
 * @author robert
 *
 */
public class ChartProcessor {

	Logger logger = Logger.getLogger(ChartProcessor.class);
	//Properties config = null;
	private boolean manager=false;
	private JTextArea textArea;
	
	private File mapCacheDir;

	public ChartProcessor() throws Exception {
		//config=Util.getConfig(null);

	}
	public ChartProcessor(boolean manager,JTextArea textArea) throws Exception {
		//config=Util.getConfig(null);
		this.manager=manager;
		this.textArea=textArea;
	}

	public void processChart(String file, boolean reTile, String charset ) throws Exception {
		File chartFile = new File(mapCacheDir,file);
		processChart(chartFile, reTile, charset);
	}
	public void processChart(File chartFile, boolean reTile, String charset) throws Exception {
		//make a file
				mapCacheDir=chartFile.getParentFile();
				if(!chartFile.exists()){
//					if(manager){
//						logger.error("No file at "+chartFile.getAbsolutePath()+"\n");
//					}
					logger.error("No file at "+chartFile.getAbsolutePath());
				}
				//for WORLD.tif (Natural Earth shape file) we need
				//gdal_rasterize -of GTiff -co COMPRESS=DEFLATE -co PREDICTOR=2 -co ZLEVEL=9   -ot Byte  -a_nodata 255 -tr .1 .1 -burn 255 -burn 0 -burn 0  ne_10m_coastline.shp ./WORLD.tif
				//gdal_rasterize -of GTiff -co COMPRESS=DEFLATE -co PREDICTOR=2 -co ZLEVEL=9   -ot Byte  -a_nodata 255 -tr .01 .01 -burn 255 -burn 0 -burn 0  ne_10m_coastline.shp ./WORLD1.tif
				// v1.7 'gdal_translate -a_ullr -180.0 90.0 180.0 -90.0 -a_srs "EPSG:4326" -if","GTiff -of vrt  WORLD.tif temp.vrt'
				// v1.9 'gdal_translate -a_ullr -180.0 90.0 180.0 -90.0 -a_srs "EPSG:4326" -of vrt  WORLD.tif temp.vrt'
				//to add Georef info
				//if(chartFile.getName().toUpperCase().startsWith("WORLD")){
				//	processWorldChart(chartFile,reTile,"Natural Earth");
				//}else
				//we have a KAP file
				if(chartFile.getName().toUpperCase().endsWith("KAP")){
					processKapChart(chartFile,reTile, charset);
				}else{
					System.out.print("File "+chartFile.getAbsolutePath()+" not recognised so not processed\n");
				}

	}
/*	private void processWorldChart(File chartFile, boolean reTile, String attribution) throws Exception {
		String chartName = chartFile.getName();
		chartName = chartName.substring(0,chartName.lastIndexOf("."));
		File dir = new File(chartFile.getParentFile(),chartName);
		if(manager){
			System.out.print("Chart tag:"+chartName+"\n");
			System.out.print("Chart dir:"+dir.getPath()+"\n");
		}
		logger.debug("Chart tag:"+chartName);
		logger.debug("Chart dir:"+dir.getPath());
		String scales = "0-2";
		int min = 0;
		int max = 2;
		if("WORLD1".equals(chartName)){
			scales = "3-6";
			min=3;
			max=6;
		}
		//start by running the gdal script
		if(reTile){
			//win8.1 may need full paths
			//Arrays.asList("C:\\Python33\\python", "C:\\Program Files (x86)\\GDAL\\gdal2tiles.py", pathName +"\\" +"temp.vrt", pathName +"\\" + chartName));

			executeGdal(chartFile, chartName,
					//v1.7 Arrays.asList("gdal_translate", "-co","COMPRESS=PACKBITS", "-a_ullr","-180.0","90.0","180.0","-90.0","-a_srs","\"EPSG:4326\"", "-if","GTiff", "-of", "vrt", chartFile.getName(),"temp.vrt"),
					Arrays.asList("gdal_translate", "-co","COMPRESS=PACKBITS", "-a_ullr","-180","83.6431","180.0","-85.2659", "-of", "vrt", chartFile.getName(),"temp.vrt"),
					Arrays.asList("gdal2tiles.py", "-z", scales, "temp.vrt", chartName));
		}
		//write out freeboard.txt
		String text = "\n\tvar "+chartName+" = L.tileLayer(\"http://{s}.{server}:8080/mapcache/"+chartName+"/{z}/{x}/{y}.png\", {\n"+
    			"\t\tserver: host,\n"+
    			"\t\tsubdomains: 'abcd',\n"+
    			"\t\tattribution: '"+attribution+"',\n"+
    			"\t\tminZoom: "+min+",\n"+
    			"\t\tmaxZoom: "+max+",\n"+
    			"\t\ttms: true\n"+
    			"\t\t}).addTo(map);\n";
		if(manager){
			System.out.print(text+"\n");
		}
		File layers = new File(dir,"freeboard.txt");
        FileUtils.writeStringToFile(layers, text);
        System.out.print("Zipping directory...\n");
		ZipUtils.zip(dir, new File(dir.getParentFile(),chartName+".zip"));
		System.out.print("Zipping directory complete\n");
	}*/

	/**
	 * Reads the .kap file, and the generated tilesresource.xml to get
	 * chart desc, bounding box, and zoom levels
	 * @param chartFile
	 * @param false 
	 * @throws Exception
	 */
	public void processKapChart(File chartFile, boolean reTile, String charset) throws Exception {
		//String chartPath = chartFile.getParentFile().getAbsolutePath();
		String chartName = chartFile.getName();
		chartName = chartName.substring(0,chartName.lastIndexOf("."));
		File dir = new File(chartFile.getParentFile(),chartName);
//		if(manager){
//			logger.info("Chart tag:"+chartName+"\n");
//			logger.info("Chart dir:"+dir.getPath()+"\n");
//		}
//		logger.info("Processing Chart tag:"+chartName);
		logger.info("Chart dir:"+dir.getPath());

		if(reTile){
			KapProcessor processor = new KapProcessor();
			processor.setObserver(new KapObserver() {
				public void appendMsg(final String message) {
					SwingUtilities.invokeLater(new Runnable() {
					    public void run() {
					    	textArea.append(message);
					    }
					  });
				}
			});
			processor.createTilePyramid(chartFile, mapCacheDir, false);
		}
		//process the layer data
		File xmlFile = new File(dir,"tilemapresource.xml");
		
		//read data from dirName/tilelayers.xml
		SAXReader reader = new SAXReader();
       
        Document document=reader.read(new InputStreamReader(new FileInputStream(xmlFile),charset));
      
        logger.info("KAP file using "+charset);
		//now get the Chart Name from the kap file
		InputStreamReader fileReader = new InputStreamReader(new FileInputStream(chartFile),charset);
		char[] chars = new char[4096];
		fileReader.read(chars);
		fileReader.close();
		String header = new String(new String(chars).getBytes(),"UTF-8");
		int pos=header.indexOf("BSB/NA=")+7;
		String desc = header.substring(pos,header.indexOf("\n",pos)).trim();
		//if(desc.endsWith("\n"))desc=desc.substring(0,desc.length()-1);
		logger.debug("Name:"+desc);
		//we cant have + or , or = in name, as its used in storing ChartplotterViewModel
		//US50_2 BERING SEA CONTINUATION,NU=2401,RA=2746,3798,DU=254
		desc=desc.replaceAll("\\+", " ");
		desc=desc.replaceAll(",", " ");
		desc=desc.replaceAll("=", "/");
		//limit length too
		if(desc.length()>40){
			desc=desc.substring(0,40);
		}
		
        //we need BoundingBox
        Element box = (Element) document.selectSingleNode( "//BoundingBox" );
        String minx = box.attribute("minx").getValue();
        String miny = box.attribute("miny").getValue();
        String maxx = box.attribute("maxx").getValue();
        String maxy = box.attribute("maxy").getValue();
//        if(manager){
//			logger.info("Box:"+minx+","+miny+","+maxx+","+maxy+"\n");
//		}
        logger.debug("Box:"+minx+", "+miny+", "+maxx+", "+maxy);

        //we need TileSets, each tileset has an href, we need first and last for zooms
        @SuppressWarnings("unchecked")
		List<Attribute> list = document.selectNodes( "//TileSets/TileSet/@href" );
        int minZoom = 18;
        int maxZoom = 0;
        for (Attribute attribute : list){
            int zoom = Integer.valueOf(attribute.getValue());
            if(zoom<minZoom)minZoom=zoom;
            if(zoom>maxZoom)maxZoom=zoom;
        }
//        if(manager){
//			System.out.print("Zoom:"+minZoom+"-"+maxZoom+"\n");
//		}
        logger.debug("Zoom:"+minZoom+"-"+maxZoom);
        
        //cant have - in js var name
        String chartNameJs = chartName.replaceAll("^[^a-zA-Z_$]|[^\\w$]","_");
        String snippet = "\n\tvar "+chartNameJs+" = L.tileLayer(\"http://{s}.{server}:8080/mapcache/"+chartName+"/{z}/{x}/{y}.png\", {\n"+
        			"\t\tserver: host,\n"+
        			"\t\tsubdomains: 'abcd',\n"+
        			"\t\tattribution: '"+chartName+" "+desc+"',\n"+
        			"\t\tminZoom: "+minZoom+ ",\n"+
        			"\t\tmaxNativeZoom: "+maxZoom+",\n"+
        			"\t\tmaxZoom: "+(maxZoom+3)+",\n"+
        			"\t\ttms: true\n"+
        			"\t\t}).addTo(map);\n";


//        if(manager){
//			System.out.print(snippet+"\n");
//		}
        logger.debug(snippet);
		//add it to local freeboard.txt
        File layers = new File(dir,"freeboard.txt");
        FileUtils.writeStringToFile(layers, snippet, StandardCharsets.UTF_8.name());
        //now zip the result
        logger.info("Zipping directory...");
		ZipUtils.zip(dir, new File(dir.getParentFile(),chartName+".zip"));
		logger.info("Zipping directory complete, in "+new File(dir.getParentFile(),chartName+".zip").getAbsolutePath());
	}




	/**
	 * First arg is chart filename, second is boolean reTile.
	 * reTile = true causes the tiles to be recreated,
	 * false just recreates the layers conf text.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		//arg0 = chartfile
		String chartFile = null;
		if(args!=null && args.length>0 && StringUtils.isNotBlank(args[0])){
			chartFile=args[0];
		}
		if(StringUtils.isBlank(chartFile)){
			System.out.print("No file provided");
			System.exit(1);
		}
		boolean reTile = true;
		if(args!=null && args.length>1 && StringUtils.isNotBlank(args[1])){
			reTile=Boolean.valueOf(args[1]);
		}
		//we have a file
		ChartProcessor chartProcessor = new ChartProcessor();
		chartProcessor.processChart(chartFile,reTile, "UTF-8");
	}




}
