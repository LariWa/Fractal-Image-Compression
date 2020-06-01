//BVK Ue1 SS2019 Vorgabe
//
// Copyright (C) 2018 by Klaus Jung
// All rights reserved.
// Date: 2018-03-28

package bvk_ss19;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RLE {
	private static int blockgroesse=8;
	public static void encodeImage(RasterImage image, DataOutputStream out) throws IOException {

		// TODO: write RLE data to DataOutputStream
		ArrayList<Integer> colors = new ArrayList<Integer>();
		int numberOfColors = 0;
		for (int i = 0; i < image.width * image.height; i++) {
			int color = image.argb[i];
			if (!colors.contains(color)) {
				numberOfColors++;
				colors.add(color);
			}
		}
		out.writeInt(image.width);
		out.writeInt(image.height);
		out.writeInt(numberOfColors);
		for (int color : colors) {
			out.writeInt(color);
		}

		int i = 0;
		while (i < image.width * image.height) {
			int color = image.argb[i];
			int lauflaenge = 0;
			for (; i < image.width * image.height && image.argb[i] == color && lauflaenge < 255; lauflaenge++, i++)
				;
			out.writeByte(colors.indexOf(color));
			out.writeByte(lauflaenge);
		}
	}

	public static RasterImage decodeImage(DataInputStream in) throws IOException {

		// TODO: read width and height from DataInputStream
		int width = in.readInt();
		int height = in.readInt();

		// create RasterImage to be returned
		RasterImage image = new RasterImage(width, height);
		int numberOfColors = in.readInt();
		int[] colors = new int[numberOfColors];
		for (int i = 0; i < numberOfColors; i++) {
			colors[i] = in.readInt();
		}
		int i = 0;
		while (in.available() > 0) {
			int index = in.readByte() & 0xff;
			int lauflaenge = in.readByte() & 0xff;
			int color = colors[index];
			for (int j = 0; j < lauflaenge; j++, i++) {
				image.argb[i] = color;
			}
		}
		return image;
	}

	public static RasterImage createRangebloecke(RasterImage base) {
		int blockgroesse = 8;
		int width = base.width;
		int height = base.height;

		// create RasterImage to be returned
		RasterImage image = new RasterImage(width, height);
		int y = 0;
		int x;
		for (y = 0; y < base.height; y++) {
			for (x = 0; x < base.width; x++) {
				int b = 0; //Summe der Grauwerte
				int rx=0;
				int ry;

				for (ry=0;ry < blockgroesse && y + ry < base.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						int grey = (base.argb[x + rx + (y + ry) * base.width] >> 16) & 0xff;
						b += grey;
					}
				}
				b = b / (rx * ry); // Mittelwert

				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { // Mittelwerte ins Bild schreiben, später im Decoder
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						image.argb[x + rx + (y + ry) * base.width] = 0xff000000 | (b << 16) | (b << 8) | b;
					}
				}
				x += blockgroesse - 1;
			}
			y += blockgroesse - 1;
		}
		return image;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	public static RasterImage domainApprox(RasterImage input){
		int blockgroesse = 8;

		RasterImage src = input; //createRangebloecke(input);
		RasterImage dst = copy(src);

		//start iterating through src image from y = blockgrosse -1 because first 
		//line of range blocks has no blocks above it
		for (int y = blockgroesse -1 ; y < dst.height-1; y++) {
			for (int x = 0; x < dst.width-1; x++) {
				dst.argb[y*dst.width+x] = src.argb[(y-blockgroesse + 1)*src.width +x];
			}				
		}
		RasterImage temp = adjustContrastBrightness(src,dst);
		//		for(int i = 0; i<200; i++) {
		//			temp =  adjustContrastBrightness(src,temp);
		//
		//		}
		return temp;
	}

	/**
	 * 
	 * @param domain
	 * @param range
	 * @return
	 */
	public static RasterImage adjustContrastBrightness(RasterImage domain, RasterImage range) {
		int blockgroesse = 8;
		int y, x;
		for (y = 0; y < domain.height; y++) {
			for (x = 0; x < domain.width; x++) {
				int domainM = 0; //Summe der Grauwerte
				int rangeM = 0;
				int ry = 0;
				int rx = 0;		

				for (ry=0;ry < blockgroesse && y + ry < domain.height; ry++) { // Rangeblöcke Grauwerte summieren für Mittelwert
					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
						int greyD = (domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff;
						domainM += greyD;

						int greyR = (range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff;
						rangeM += greyR;
					}
				}
				domainM = domainM / (rx * ry); // Mittelwert
				rangeM = rangeM / (rx * ry);

				int varianz =0;
				int kovarianz=0;
				for (ry=0;ry < blockgroesse && y + ry < domain.height; ry++) { // Summe Grauwert minus Mittelwert
					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
						int greyD = ((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) - domainM;
						int greyR = ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - rangeM;
						varianz+=greyR*greyD;
						kovarianz+=greyD*greyD;
					}
				} 

				//Kontrast und Helligkeit
				int a;
				if(kovarianz==0) a=1;
				else
					a= varianz/kovarianz;
				if(a==0)a=1;
				int b = rangeM - a*domainM;				


				for (ry=0;ry < blockgroesse && y + ry < range.height; ry++) { // Kontrast und Helligkeit anpassen
					for (rx = 0; rx < blockgroesse && x + rx < range.width; rx++) {
						int value = a*((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) -b;
						if(value<0)value=0;
						else if(value>255) value =255;				
						domain.argb[x + rx + (y + ry) * domain.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
				} 
				x += blockgroesse - 1;
			}
			y += blockgroesse - 1;
		}
		return domain;
	}
	public static RasterImage decoder(RasterImage range) {

		RasterImage start = getGreyImage(range.width, range.height);

		RasterImage temp = adjustContrastBrightness(start,range);
		for(int i = 0; i<2; i++) {
			temp =  adjustContrastBrightness(temp,range);

		}
		return temp;	
	}
	public static RasterImage scaleImage(RasterImage image) {
		//scale image
		// create RasterImage to be returned
		RasterImage scaled = new RasterImage(image.width/2, image.height/2);
		int y, x;
		int i=0;

		for (y = 0; y < image.height; y+=2) {
			for (x = 0; x < image.width; x+=2) {

				//Mittelwert bestimmen
				int mittelwert = (image.argb[x + y * image.width]>> 16) & 0xff;	
				if(x+1>=image.width) {mittelwert+=128;}
				else {
					mittelwert +=(image.argb[x +1+ y * image.width]>> 16) & 0xff;	 	
					if(y+1>=image.height) mittelwert+=128;
					else mittelwert +=(image.argb[x + (y+1) * image.width]>> 16) & 0xff;	 		
				}

				if(y+1>=image.height) mittelwert+=128;
				else {
					if(x+1>=image.height)mittelwert+=128;
					else mittelwert +=(image.argb[x+1 + (y+1) * image.width] >> 16) & 0xff;	
				}

				mittelwert =mittelwert/4;
				scaled.argb[i]=0xff000000 | (mittelwert << 16) | (mittelwert << 8) | mittelwert;
				i++;
			}


		}
		return scaled;

	}

	public static int[][] createCodebuch(RasterImage image) {
		image = scaleImage(image);
		int abstand=4;
		System.out.println(((image.width/blockgroesse)*2-1)*((image.height/blockgroesse)*2-1));
		int[][] codebuch = new int[2000][64];//TODO real size
		int i=0;
		for (int y = 0; y < image.height; y+=abstand) {
			for (int x = 0; x < image.width; x+=abstand) {
				int[] codebuchblock= new int[64];
				for (int ry=0;ry < blockgroesse && y + ry < image.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
						codebuchblock[rx+ry*blockgroesse] = (image.argb[x+rx+(y+ry)*image.width]>>16) & 0xff;
					}
				}
				codebuch[i]=codebuchblock;
				i++;
			}	
		}
		return codebuch;
	}
	
	public static RasterImage showCodebuch(RasterImage image) {
		int[][] codebuch = createCodebuch(image);
		int i =0;
		RasterImage codebuchImage = new RasterImage(image.width*2+image.width/4,image.height*2+image.height/4);//TODO adjust width and height
		for (int y = 0; y < codebuchImage.height; y+=9) {
			for (int x = 0; x < codebuchImage.width; x+=9) {
				for (int ry=0;ry < blockgroesse && y + ry < image.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
						int value = codebuch[i][rx+ry*blockgroesse];
						codebuchImage.argb[x + rx + (y + ry) * codebuchImage.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
					codebuchImage.argb[x + 8 + (y + ry) * codebuchImage.width] = 0xff000000 | (255 << 16) | (255 << 8) | 255;
				}
				
				
				i++;
			}}
			
		return codebuchImage;
	}


	/**
	 * Method to copy a Raster image to another Raster Image
	 * @param src
	 * @return
	 */
	public static RasterImage copy(RasterImage src) {
		RasterImage dst = new RasterImage(src.width,src.height);

		for (int i = 0; i < src.argb.length; i++) {
			dst.argb[i] = src.argb[i];
		}
		return dst;
	}

	public static RasterImage getGreyImage(int width, int height){
		RasterImage image = new RasterImage(width, height);
		for (int i=0; i< image.argb.length; i++) {
			image.argb[i]= 0xff000000 | (128 << 16) | (128 << 8) | 128;
		}
		return image;
	}

}
