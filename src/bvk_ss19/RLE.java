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
		int y = 0;
		int x;
		for (y = 0; y < domain.height; y++) {
			for (x = 0; x < domain.width; x++) {
				int domainM = 0; //Summe der Grauwerte
				int rangeM = 0;
				int ry = 0;
				int rx = 0;

				int domainMin = 0;
				int rangeMin = 0;
				int sum = 0;

				int a = 0;
				int b = 0;

				

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
						//domainMin += greyD;

						int greyR = ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - rangeM;
						//rangeMin += greyR;
						varianz+=greyR*greyD;
						kovarianz+=greyD*greyD;
					}
				} 
//				if (domainMin == 0) {
//					a = 1;
//				}
//				else {
//					a = (domainMin * rangeMin)/ (domainMin * domainMin); 
//				}
//				if(kovarianz==0)kovarianz=1;
//				if(varianz==0)varianz=1;
				

				a= varianz/kovarianz;
				b = rangeM - a*domainM;
				
				//				for (ry=0;ry < blockgroesse && y + ry < domain.height; ry++) { // Rangeblöcke Grauwerte summieren
				//					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
				//						int greyD = ((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) - domainM;
				//						domainMin += greyD;
				//
				//						int greyR = ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - rangeM;
				//						rangeMin += greyR;
				//
				//					}
				//				} 
				//
				//				for (ry=0;ry < blockgroesse && y + ry < domain.height; ry++) { // Rangeblöcke Grauwerte summieren
				//					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
				//						int greyD = ((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) - domainM;
				//						domainMin += greyD;
				//
				//						int greyR = ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - rangeM;
				//						rangeMin += greyR;
				//
				//					}
				//				} 
				//				for (ry=0;ry < blockgroesse && y + ry < range.height; ry++) { // Rangeblöcke Grauwerte summieren
				//					for (rx = 0; rx < blockgroesse && x + rx < range.width; rx++) {
				//						sum+= ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - a*((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) -b;		                     
				//					}
				//				}        
				for (ry=0;ry < blockgroesse && y + ry < range.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (rx = 0; rx < blockgroesse && x + rx < range.width; rx++) {
						int value = a*((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) -b;
//						if(value<0)
//							value=0;
//						else if(value>255) value =255;
				
						range.argb[x + rx + (y + ry) * range.width] = 0xff000000 | (value << 16) | (value << 8) | value;


					}
				} 
				x += blockgroesse - 1;
			}
			y += blockgroesse - 1;
		}
		return range;
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

}
