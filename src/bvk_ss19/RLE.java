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
import java.util.HashMap;
import java.util.List;

public class RLE {

	private static int blockgroesse = 8;
	private static float[][] imageInfo;

	/**
	 * 
	 * @param base
	 * @return
	 */
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
				int b = 0; // Summe der Grauwerte
				int rx = 0;
				int ry;

				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						int grey = (base.argb[x + rx + (y + ry) * base.width] >> 16) & 0xff;
						b += grey;
					}
				}
				b = b / (rx * ry); // Mittelwert

				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { // Mittelwerte ins Bild schreiben,
					// später im Decoder
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
	public static HashMap<Integer, float[]> domainBlockApproxAufgabe2(RasterImage input) {
		HashMap<Integer, float[]> indexToAB = new HashMap<Integer, float[]>();
		int rangeBlockIndex = 0;

		int blockgroesse = 8;
		int rangebloeckePerWidth = input.width / 8;
		int rangebloeckePerHeight = input.height / 8;
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;

		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;


		int[][] codebuch = createCodebuch(input);
		RasterImage dst = new RasterImage(input.width, input.height);

		int i = 0;
		for (int y = 0; y < dst.height; y += blockgroesse) {
			for (int x = 0; x < dst.width; x += blockgroesse) {
				int xr = x / 8;
				int yr = y / 8;

				// Randbehandlung -------------------//
				if (yr == 0)
					yr = 1;
				if (xr == 0)
					xr = 1;
				if (yr == rangebloeckePerHeight - 1)
					yr = yr - 1;
				if (xr == rangebloeckePerWidth - 1)
					xr = xr - 1;
				// ---------------------------------//

				// get domainblock index
				if (xr > 1) {
					if (yr == 0)
						i = xr;
					else
						i = (xr * 2) - 2 + (yr + yr - 1) * domainbloeckePerWidth;
				} else if (xr == 1) {
					if (yr == 0)
						i = xr;
					else
						i = xr + (yr + yr - 1) * domainbloeckePerWidth;
				}

				//System.out.println(i);
				float[] ab=  getContrastAndBrightness(codebuch[i], getRangeblock(x, y, input));
				indexToAB.put(rangeBlockIndex, ab);
				rangeBlockIndex++;
			}}

		return indexToAB;
	}


	public static RasterImage encode(RasterImage input) {
		int blockgroesse = 8;
		int rangebloeckePerWidth = input.width / 8;
		int rangebloeckePerHeight = input.height / 8;
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

		int[][] codebuch = createCodebuch(input);
		RasterImage dst = new RasterImage(input.width, input.height);

		int i = 0;
		int j=0;
		imageInfo= new float[input.argb.length][3];
		for (int y = 0; y < dst.height; y += blockgroesse) {
			for (int x = 0; x < dst.width; x += blockgroesse) {
				int xr = x / 8;
				int yr = y / 8;

				// Randbehandlung -------------------//
				if (yr == 0)
					yr = 1;
				if (xr == 0)
					xr = 1;
				if (yr == rangebloeckePerHeight - 1)
					yr = yr - 1;
				if (xr == rangebloeckePerWidth - 1)
					xr = xr - 1;
				// ---------------------------------//

				// get domainblock index von Domainblock über Rangeblock
				if (xr > 1) {
					if (yr == 0)
						i = xr;
					else
						i = (xr * 2) - 2 + (yr + yr - 1) * domainbloeckePerWidth;
				} else if (xr == 1) {
					if (yr == 0)
						i = xr;
					else
						i = xr + (yr + yr - 1) * domainbloeckePerWidth;
				}


				//create domainblock kernel
				int widthKernel = 5;
				int dy = (int) (i / domainbloeckePerWidth)-widthKernel / 2;
				int dx = i % domainbloeckePerWidth -widthKernel / 2;
				if(dx<0)dx=0;
				if(dy<0)dy=0;
				if (dx + widthKernel >= domainbloeckePerWidth)
					dx = domainbloeckePerWidth - widthKernel;
				if (dy + widthKernel >= domainbloeckePerHeight)
					dy = domainbloeckePerHeight  - widthKernel ;

				int[][] domainKernel = new int[widthKernel * widthKernel][blockgroesse * blockgroesse];
				int n = 0;
				for (int ky = 0; ky < widthKernel; ky++) {
					for (int kx = 0; kx < widthKernel; kx++) {
						int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
						domainKernel[n] = codebuch[index];
						n++;
					}
				}
				imageInfo[j] = getBestDomainblock(domainKernel, getRangeblock(x, y, input));

				int yd = (int) (imageInfo[j][0] / widthKernel);
				int xd = (int) (imageInfo[j][0] % widthKernel);
				int di = xd+dx+(yd+dy)*domainbloeckePerWidth;
				imageInfo[j][0]= di;
				j++;

				for (int ry = 0; ry < blockgroesse && y + ry < dst.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < dst.width; rx++) {
						int value = codebuch[i][rx + ry * blockgroesse];
						dst.argb[x + rx + (y + ry) * dst.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
				}
			}
		}
		//	dst = adjustContrastBrightness(dst, input);
		return dst;
	}


	/**
	 * 
	 * @param input
	 * @return
	 */
	//	public static RasterImage domainBlockApprox(RasterImage input) {
	//		int blockgroesse = 8;
	//		int rangebloeckePerWidth = input.width / 8;
	//		int rangebloeckePerHeight = input.height / 8;
	//		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
	//		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;
	//
	//		int[][] codebuch = createCodebuch(input);
	//		RasterImage dst = new RasterImage(input.width, input.height);
	//
	//		int i = 0;
	//		for (int y = 0; y < dst.height; y += blockgroesse) {
	//			for (int x = 0; x < dst.width; x += blockgroesse) {
	//				int xr = x / 8;
	//				int yr = y / 8;
	//
	//				// Randbehandlung -------------------//
	//				if (yr == 0)
	//					yr = 1;
	//				if (xr == 0)
	//					xr = 1;
	//				if (yr == rangebloeckePerHeight - 1)
	//					yr = yr - 1;
	//				if (xr == rangebloeckePerWidth - 1)
	//					xr = xr - 1;
	//				// ---------------------------------//
	//
	//				// get domainblock index von Domainblock über Rangeblock
	//				if (xr > 1) {
	//					if (yr == 0)
	//						i = xr;
	//					else
	//						i = (xr * 2) - 2 + (yr + yr - 1) * domainbloeckePerWidth;
	//				} else if (xr == 1) {
	//					if (yr == 0)
	//						i = xr;
	//					else
	//						i = xr + (yr + yr - 1) * domainbloeckePerWidth;
	//				}
	//
	//				
	//				//create domainblock kernel
	//				int widthKernel = 3;
	//				int dy = (int) (i / domainbloeckePerWidth)-widthKernel / 2;
	//				int dx = i % domainbloeckePerWidth -widthKernel / 2;
	//				if(dx<0)dx=0;
	//				if(dy<0)dy=0;
	//				if (dx + widthKernel >= domainbloeckePerWidth)
	//					dx = domainbloeckePerWidth - widthKernel;
	//				if (dy + widthKernel >= domainbloeckePerHeight)
	//					dy = domainbloeckePerHeight  - widthKernel ;
	//
	//				int[][] domainKernel = new int[widthKernel * widthKernel][blockgroesse * blockgroesse];
	//				int n = 0;
	//				for (int ky = 0; ky < widthKernel; ky++) {
	//					for (int kx = 0; kx < widthKernel; kx++) {
	//						int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
	//						domainKernel[n] = codebuch[index];
	//						n++;
	//					}
	//				}
	//				
	//				//int[] domainblock= getBestDomainblock(domainKernel, getRangeblock(x, y, input));
	//
	//
	//				for (int ry = 0; ry < blockgroesse && y + ry < dst.height; ry++) {
	//					for (int rx = 0; rx < blockgroesse && x + rx < dst.width; rx++) {
	//						int value = domainblock[rx + ry * blockgroesse];
	//						dst.argb[x + rx + (y + ry) * dst.width] = 0xff000000 | (value << 16) | (value << 8) | value;
	//					}
	//				}
	//			}
	//		}
	//		return dst;
	//	}


	public static int[] getRangeblock(int x, int y, RasterImage image) {
		int[] rangeblock = new int[blockgroesse * blockgroesse];
		int i = 0;

		for (int ry = 0; ry < blockgroesse && y + ry < image.height; ry++) {
			for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {

				int value = image.argb[(x + rx) + (y + ry) * image.width];
				value = (value >> 16) & 0xff;
				rangeblock[i] = value;
				i++;
			}
		}
		return rangeblock;

	}


	public static float[] getBestDomainblock(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		float[]  bestBlock= {0,0,0};
		for (int i = 0; i < domainblocks.length; i++) {
			float[] ab = getContrastAndBrightness(domainblocks[i], rangeblock);
			float error = 0;
			int[] blockAdjusted= new int[blockgroesse*blockgroesse];

			for (int j = 0; j < blockgroesse * blockgroesse; j++) { // Kontrast und Helligkeit anpassen
				int domainValue = (int) (ab[0] * domainblocks[i][j] - ab[1]);
				if (domainValue < 0)
					domainValue = 0;
				else if (domainValue > 255)
					domainValue = 255;
				error += (rangeblock[j] - domainValue) * (rangeblock[j] - domainValue);
				blockAdjusted[j]=domainValue;

			}

			if (error < smallestError) {
				smallestError = error;
				float[] temp = {i, ab[0], ab[1]};
				bestBlock=temp;
			}			
		}

		return bestBlock;
	}



	/*	
	public static RasterImage adjustContrastBrightness(RasterImage domain, RasterImage range) {
		int blockgroesse = 8;
		int y, x;

		for (y = 0; y < range.height; y++) {
			for (x = 0; x < range.width; x++) {
				int domainM = 0; // Summe der Grauwerte
				int rangeM = 0;
				int ry = 0;
				int rx = 0;
				int[] rangeb = new int[blockgroesse * blockgroesse];
				int n = 0;
				int rangeBlockIndex = 0;
				float[] ab = getContrastAndBrightness();

				int[][] domainKernel = new int[widthKernel * widthKernel][blockgroesse * blockgroesse];
				int n = 0;
				for (int ky = 0; ky < widthKernel; ky++) {
					for (int kx = 0; kx < widthKernel; kx++) {
						int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
						domainKernel[n] = codebuch[index];
						n++;
					}
				}

				int[] domainblock= getBestDomainblock(domainKernel, getRangeblock(x, y, input));


				for (int ry = 0; ry < blockgroesse && y + ry < dst.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < dst.width; rx++) {
						int value = domainblock[rx + ry * blockgroesse];
						dst.argb[x + rx + (y + ry) * dst.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
				}
		}
		return domain;
	} */

	public static float[] getContrastAndBrightness(int[] domain, int[] range) {
		int domainM = getMittelwert(domain);
		int rangeM = getMittelwert(range);

		float varianz = 0;
		float kovarianz = 0;
		for (int i = 0; i < domain.length; i++) {
			float greyD = domain[i] - domainM;
			float greyR = range[i] - rangeM;
			varianz += greyR * greyD;
			kovarianz += greyD * greyD;
		}

		float a = varianz / kovarianz;
		if (a > 1)
			a = 1;
		if (a < -1)
			a = -1;
		float b = rangeM - a * domainM;
		float[] result = { a, b };
		return result;
	}

	/**
	 * 
	 * @param values
	 * @return
	 */
	public static int getMittelwert(int[] values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum / values.length;
	}

	/**
	 * 
	 * @param range
	 * @return
	 */
	public static RasterImage decoder() {

		RasterImage start = getGreyImage(256,256); //TODO width, height übertragen

		int[][] codebuch = createCodebuch(start);	
		int i = 0;				
		for(int y=0; y< start.height; y+=blockgroesse) {
			for(int x=0; x<start.width; x+=blockgroesse) {				
				for (int ry = 0; ry < blockgroesse && y + ry < start.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < start.width; rx++) {
						int value = codebuch[(int) imageInfo[i][0]][rx + ry * blockgroesse];
						value = (int) (imageInfo[i][1]*value+ imageInfo[i][2]);						
						if (value < 0)
							value = 0;
						else if (value > 255)
							value = 255;

						start.argb[x + rx + (y + ry) * start.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
				}
				i++;

			}
		}
		return start;

	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public static RasterImage scaleImage(RasterImage image) {
		// scale image
		// create RasterImage to be returned
		RasterImage scaled = new RasterImage(image.width / 2, image.height / 2);
		int y, x;
		int i = 0;

		for (y = 0; y < image.height; y += 2) {
			for (x = 0; x < image.width; x += 2) {

				// Mittelwert bestimmen
				int mittelwert = (image.argb[x + y * image.width] >> 16) & 0xff;
				if (x + 1 >= image.width) {
					mittelwert += 128;
				} else {
					mittelwert += (image.argb[x + 1 + y * image.width] >> 16) & 0xff;
					if (y + 1 >= image.height)
						mittelwert += 128;
					else
						mittelwert += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
				}

				if (y + 1 >= image.height)
					mittelwert += 128;
				else {
					if (x + 1 >= image.height)
						mittelwert += 128;
					else
						mittelwert += (image.argb[x + 1 + (y + 1) * image.width] >> 16) & 0xff;
				}

				mittelwert = mittelwert / 4;
				scaled.argb[i] = 0xff000000 | (mittelwert << 16) | (mittelwert << 8) | mittelwert;
				i++;
			}

		}
		return scaled;

	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public static int[][] createCodebuch(RasterImage image) {
		image = scaleImage(image);
		int abstand = 2;
		int[][] codebuch = new int[(image.width / 2 - 3) * (image.height / 2 - 3)][64];
		int i = 0;
		for (int y = 0; y < image.height; y += abstand) {
			for (int x = 0; x < image.width; x += abstand) {
				int[] codebuchblock = new int[64];
				if (y + blockgroesse <= image.height && x + blockgroesse <= image.width) {
					for (int ry = 0; ry < blockgroesse; ry++) { // Rangeblöcke Grauwerte summieren
						for (int rx = 0; rx < blockgroesse; rx++) {
							codebuchblock[rx + ry * blockgroesse] = (image.argb[x + rx + (y + ry) * image.width] >> 16)
									& 0xff;
						}
					}
					codebuch[i] = codebuchblock;
					i++;
				}
			}

		}
		return codebuch;
	}

	/**
	 * 
	 * @param image
	 * @return
	 */
	public static RasterImage showCodebuch(RasterImage image) {
		int[][] codebuch = createCodebuch(image);
		int i = 0;
		RasterImage codebuchImage = new RasterImage(image.width * 2 + image.width / 4,
				image.height * 2 + image.height / 4);
		for (int y = 0; y < codebuchImage.height; y += 9) {
			for (int x = 0; x < codebuchImage.width; x += 9) {
				for (int ry = 0; ry < blockgroesse && y + ry < codebuchImage.height; ry++) { // Rangeblöcke Grauwerte
					// summieren
					for (int rx = 0; rx < blockgroesse && x + rx < codebuchImage.width; rx++) {
						int value = codebuch[i][rx + ry * blockgroesse];
						codebuchImage.argb[x + rx + (y + ry) * codebuchImage.width] = 0xff000000 | (value << 16)
								| (value << 8) | value;
					}
					codebuchImage.argb[x + 8 + (y + ry) * codebuchImage.width] = 0xff000000 | (255 << 16) | (255 << 8)
							| 255;
				}

				i++;
			}
		}

		return codebuchImage;
	}

	/**
	 * Method to copy a Raster image to another Raster Image
	 * 
	 * @param src
	 * @return
	 */
	public static RasterImage copy(RasterImage src) {
		RasterImage dst = new RasterImage(src.width, src.height);
		for (int i = 0; i < src.argb.length; i++) {
			dst.argb[i] = src.argb[i];
		}
		return dst;
	}

	/**
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public static RasterImage getGreyImage(int width, int height) {
		RasterImage image = new RasterImage(width, height);
		for (int i = 0; i < image.argb.length; i++) {
			image.argb[i] = 0xff000000 | (128 << 16) | (128 << 8) | 128;
		}
		return image;
	}



}
