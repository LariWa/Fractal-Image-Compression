//BVK Ue1 SS2019 Vorgabe
//
// Copyright (C) 2018 by Klaus Jung
// All rights reserved.
// Date: 2018-03-28

package bvk_ss19;

import java.text.DecimalFormat;

public class FractalCompression {

	private static int blockgroesse = 4;
	private static float[][] imageInfo;
	
	public static float avgError;
	
	
	public static float getAvgError() {
		return avgError;
	}

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
		
		//start iterating image
		for (y = 0; y < base.height; y++) {
			for (x = 0; x < base.width; x++) {
				int sum = 0; // Summe der Grauwerte
				int rx = 0;
				int ry;

				//start iterating from x to x+rangeblock
				//and from y to y+rangeblock 
				for (ry = 0; ry < blockgroesse && y + ry < base.height; ry++) { 
					for (rx = 0; rx < blockgroesse && x + rx < base.width; rx++) {
						int grey = (base.argb[x + rx + (y + ry) * base.width] >> 16) & 0xff; // Rangeblöcke Grauwerte summieren
						sum += grey;
					}
				}
				sum = sum / (rx * ry); // Mittelwert
				
				//replace block with average grey value
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
		// calculate rangeblock per dimention
		int rangebloeckePerWidth = input.width / blockgroesse;
		int rangebloeckePerHeight = input.height / blockgroesse;
		
		// calculate domainblock per dimention
		int domainbloeckePerWidth = rangebloeckePerWidth * (blockgroesse/4) - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * (blockgroesse/4) - 3;

		//generate codebook to read domain blocks from
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
				
				//apply algorithm based on minimum error to find best fit domain block
				imageInfo[j] = getBestDomainblock(domainKernel, getRangeblock(x, y, input));

				int yd = (int) (imageInfo[j][0] / widthKernel);
				int xd = (int) (imageInfo[j][0] % widthKernel);
				int di = xd+dx+(yd+dy)*domainbloeckePerWidth;
				imageInfo[j][0]= di;
				j++;

				//iterate rangeblock and replace values
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

		//iterates range block and extracts grey values
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
    * Applies aaverageValuelgortithm, which iterates domainblocks in the codebook
    * and chooses the fest fit by picking the one with minimun error
    * @param domainblocks
    * @param rangeblock
    * @return
    */
	private static float[] getBestDomainblock(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		float[]  bestBlock= {0,0,0};
		
		//iterate domain blocks
		for (int i = 0; i < domainblocks.length; i++) {
			
			// get Aopt and Bopt for currently visited domainblock
			float[] ab = getContrastAndBrightness(domainblocks[i], rangeblock);
			float error = 0;
			int[] blockAdjusted= new int[blockgroesse*blockgroesse];

			for (int j = 0; j < blockgroesse * blockgroesse; j++) { 
				
				//get domain values adjusted by Aopt and Bopt for error calculation
				int domainValue = (int) (ab[0] * domainblocks[i][j] + ab[1]);
				
				// apply threshold
				if (domainValue < 0)
					domainValue = 0;
				else if (domainValue > 255)
					domainValue = 255;
				
				
				error += (rangeblock[j] - domainValue) * (rangeblock[j] - domainValue);
				blockAdjusted[j]=domainValue;

			}

			// check if current error smaller than previous errors
			if (error < smallestError) {
				smallestError = error;
				float[] temp = {i, ab[0], ab[1]};
				bestBlock=temp;
			}			
		}
		return bestBlock;
	}
	

    /**
     * Calculates a and b luminence factors
     * @param domain
     * @param range
     * @return
     */
	private static float[] getContrastAndBrightness(int[] domain, int[] range) {
		int domainM = getMittelwert(domain);
		int rangeM = getMittelwert(range);

		float varianz = 0;
		float kovarianz = 0;
		
		//iterate domain block
		for (int i = 0; i < domain.length; i++) {
			
			// subtract average value from current value
			float greyD = domain[i] - domainM;
			float greyR = range[i] - rangeM;
			
			// calculate variance, covariance
			varianz += greyR * greyD;
			kovarianz += greyD * greyD;
		}

		// get a
		float a = varianz / kovarianz;
		
		// apply threshold
		if (a > 1)
			a = 1;
		if (a < -1)
			a = -1;
		
		//get b
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

		//start from random grey image
		start = FractalCompression.getGreyImage(start.width,start.height); 
		
		//float avgError = 0;
		int rangeBlockPerWidth = start.width/blockgroesse;
		int rangeBlockPerHeight = start.height/blockgroesse;
		
		
		// make iterations for image reconstruction
		for(int counter = 0; counter < 50; counter ++) {
			
			//get codebook
			int[][] codebuch = createCodebuch(start);	
			int i = 0;		
			float tmp = 0;
			
			//iterate image
			for(int y=0; y< start.height; y+=blockgroesse) {
				for(int x=0; x<start.width; x+=blockgroesse) {
					float error = 0;

					//iterate rangeblock
					for (int ry = 0; ry < blockgroesse && y + ry < start.height; ry++) {
						for (int rx = 0; rx < blockgroesse && x + rx < start.width; rx++) {
							
							//get current value of best fit domainblock pixel
							int domain = codebuch[(int) imageInfo[i][0]][rx + ry * blockgroesse];
						    int	value = (int) (imageInfo[i][1]*domain+ imageInfo[i][2]);		
						    
						    //get current value of rangeblock pixel
						    int range = (start.argb[x + rx + (y + ry) * start.width]>> 16)& 0xff;

						    
						    // apply thresshold
							if (value < 0)
								value = 0;
							else if (value > 255)
								value = 255;
	
							start.argb[x + rx + (y + ry) * start.width] = 0xff000000 | (value << 16) | (value << 8) | value;
							
							
							//calculate error
							error += (range - value)*(range - value);
						}
					}
					
					avgError += error;
					i++;
	
				}
			}
			avgError = avgError/(rangeBlockPerWidth*rangeBlockPerHeight);
			
			// stop iterations when error drops below 1
			if (avgError < 1) break;
			
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

				// randbehandlung
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
		
		// scale image by factor 2
		image = scaleImage(image);
		int abstand = blockgroesse/4;
		
		// generated codebook size
		int[][] codebuch = new int[(image.width / abstand - 3) * (image.height / abstand - 3)][blockgroesse*blockgroesse];
		
		int i = 0;
		
		//iterate image
		for (int y = 0; y < image.height; y += abstand) {
			for (int x = 0; x < image.width; x += abstand) {
				int[] codebuchblock = new int[blockgroesse*blockgroesse];
				
				// iterate domainblock
				if (y + blockgroesse <= image.height && x + blockgroesse <= image.width) {
					for (int ry = 0; ry < blockgroesse; ry++) { 
						for (int rx = 0; rx < blockgroesse; rx++) {
							

							codebuchblock[rx + ry * blockgroesse] = (image.argb[x + rx + (y + ry) * image.width] >> 16)
									& 0xff;
						}
					}
					//map domainblock pixel values to domainblock index
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
		// generate codebook
		int[][] codebuch = createCodebuch(image);
		int i = 0;
		
		// generate image to display
		RasterImage codebuchImage = new RasterImage(image.width * 2 + image.width / 4,image.height * 2 + image.height / 4);
		
		// iterate image
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
