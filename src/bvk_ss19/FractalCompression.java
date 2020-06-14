//BVK Ue1 SS2019 Vorgabe
//
// Copyright (C) 2018 by Klaus Jung
// All rights reserved.
// Date: 2018-03-28

package bvk_ss19;

import java.text.DecimalFormat;

public class FractalCompression {

	private static int blockgroesse = 8;
	private static float[][] imageInfo;

	/**
	 * Takes an image and splits it in range blocks, 
	 * after wards
	 * @param base RasterImage to be processed
	 * @return RasterImage split into range blocks
	 */
	static RasterImage createRangebloecke(RasterImage base) {
		int width = base.width;
		int height = base.height;

		// create RasterImage to be returned
		RasterImage image = new RasterImage(width, height);
		int y = 0;
		int x;
		for (y = 0; y < base.height; y++) {
			for (x = 0; x < base.width; x++) {
				int sum = 0; // Summe der Grauwerte
				int rx = 0;
				int ry;

				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { // Rangeblöcke Grauwerte summieren
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						int grey = (base.argb[x + rx + (y + ry) * base.width] >> 16) & 0xff;
						sum += grey;
					}
				}
				sum = sum / (rx * ry); // Mittelwert

				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { // Mittelwerte ins Bild schreiben,
					// später im Decoder
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						image.argb[x + rx + (y + ry) * base.width] = 0xff000000 | (sum << 16) | (sum << 8) | sum;
					}
				}
				x += blockgroesse - 1;
			}
			y += blockgroesse - 1;
		}
		return image;
	}


   /**
    * Applies fractal image compression to a given RasterImage.
    * 
    * @param input RasterImage to be processed
    * @return compressed RasterImage
    */
	public static RasterImage encode(RasterImage input) {
		int rangebloeckePerWidth = input.width / blockgroesse;
		int rangebloeckePerHeight = input.height / blockgroesse;
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
		return dst;
	}

    /**
     * Gets positions x,y and returns the range block starting
     * from these coordinates.
     * @param x Position in x achse of the image
     * @param y Position in y achse of the image
     * @param image Image to be processed
     * @return in array containing the rangeblock values
     */
	private static int[] getRangeblock(int x, int y, RasterImage image) {
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

   /**
    * 
    * @param domainblocks
    * @param rangeblock
    * @return
    */
	private static float[] getBestDomainblock(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		float[]  bestBlock= {0,0,0};
		for (int i = 0; i < domainblocks.length; i++) {
			float[] ab = getContrastAndBrightness(domainblocks[i], rangeblock);
			float error = 0;
			int[] blockAdjusted= new int[blockgroesse*blockgroesse];

			for (int j = 0; j < blockgroesse * blockgroesse; j++) { // Kontrast und Helligkeit anpassen
				int domainValue = (int) (ab[0] * domainblocks[i][j] + ab[1]);
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
	

    /**
     * 
     * @param domain
     * @param range
     * @return
     */
	private static float[] getContrastAndBrightness(int[] domain, int[] range) {
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
	 * Gets an array of integers and
	 * returns the average value.
	 * @param values
	 * @return
	 */
	private static int getMittelwert(int[] values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum / values.length;
	}

	

	/**
	 * Gets a random image and starts generated the compressed image
	 * based on the codebook, the domain indexes for each rangeblock
	 * and the a,b parameters
	 * @param range
	 * @return
	 */
	public static RasterImage decoder(RasterImage start) {

		start = FractalCompression.getGreyImage(start.width,start.height); 
		
		float avgError = 0;
		int rangeBlockPerWidth = start.width/blockgroesse;
		int rangeBlockPerHeight = start.height/blockgroesse;
		
		for(int counter = 0; counter < 10; counter ++) {
			
			int[][] codebuch = createCodebuch(start);	
			int i = 0;		
			float tmp = 0;
			
			for(int y=0; y< start.height; y+=blockgroesse) {
				for(int x=0; x<start.width; x+=blockgroesse) {
					float error = 0;

					for (int ry = 0; ry < blockgroesse && y + ry < start.height; ry++) {
						for (int rx = 0; rx < blockgroesse && x + rx < start.width; rx++) {
							int domain = codebuch[(int) imageInfo[i][0]][rx + ry * blockgroesse];
						    int	value = (int) (imageInfo[i][1]*domain+ imageInfo[i][2]);		
							int range = start.argb[x + rx + (y + ry) * start.width];

						    
							if (value < 0)
								value = 0;
							else if (value > 255)
								value = 255;
	
							start.argb[x + rx + (y + ry) * start.width] = 0xff000000 | (value << 16) | (value << 8) | value;
							
							
							//calculate error
							int domainValues = (int) (imageInfo[i][1]*domain + imageInfo[i][2]);	
							error += (range - value)*(range - value);
						}
					}
					avgError += error/(blockgroesse*blockgroesse);
					i++;
	
				}
			}
			avgError = avgError/(rangeBlockPerWidth*rangeBlockPerHeight);
			//System.out.println(avgError);
			DecimalFormat df = new DecimalFormat("#.000000000000");
			System.out.println(df.format(avgError));
			avgError = 0;

		}
		return start;

	}


	/**
	 * Gets a RasterImage and scales it down by factor 2.
	 * @param image RasterImage to be processed
	 * @return scaled RasterImage
	 */
	private static RasterImage scaleImage(RasterImage image) {
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
	 * Gets an RasterImage and returns a 2D of array containing
	 * a codebook image
	 * @param image RasterImage to be processed
	 * @return codebook array
	 */
	private static int[][] createCodebuch(RasterImage image) {
		image = scaleImage(image);
		int abstand = 2;
		int[][] codebuch = new int[(image.width / 2 - 3) * (image.height / 2 - 3)][blockgroesse*blockgroesse];
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
	 * Gets a RasterImage and displays the codebook image generated by it.
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
	 * Generates a grey RasterImage from given 
	 * width and height.
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
