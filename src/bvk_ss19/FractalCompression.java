//BVK Ue1 SS2019 Vorgabe
//
// Copyright (C) 2018 by Klaus Jung
// All rights reserved.
// Date: 2018-03-28

package bvk_ss19;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class FractalCompression {

	private static int blockgroesse = 8;
	private static int widthKernel = 5;

	private static float[][] imageInfo; //for decoder later as file

	public static float avgError;

	public static float getAvgError() {
		return avgError;
	}

	/**
	 * Applies fractal image compression to a given RasterImage.
	 * 
	 * @param input RasterImage to be processed
	 * @return compressed RasterImage
	 */
	public static RasterImage encode(RasterImage input,DataOutputStream out) throws IOException  {
		// calculate rangeblock per dimension
		int rangebloeckePerWidth = input.width / blockgroesse;
		int rangebloeckePerHeight = input.height / blockgroesse;

		// calculate domainblock per dimension
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

		// generate codebook to read domain blocks from
		int[][] codebuch = createCodebuch(input);
		RasterImage dst = new RasterImage(input.width, input.height);

		int j = 0;
		imageInfo = new float[input.argb.length][3];//for decoder later write to file
		for (int y = 0; y < dst.height; y += blockgroesse) {
			for (int x = 0; x < dst.width; x += blockgroesse) {

				int i = getDomainIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);
				
				// create domainblock kernel-------------------
				//calculates start position of kernel + randbehandlung
				int dy = (int) (i / domainbloeckePerWidth) - widthKernel / 2;
				int dx = i % domainbloeckePerWidth - widthKernel / 2;
				if (dx < 0)
					dx = 0;
				if (dy < 0)
					dy = 0;
				if (dx + widthKernel >= domainbloeckePerWidth)
					dx = domainbloeckePerWidth - widthKernel;
				if (dy + widthKernel >= domainbloeckePerHeight)
					dy = domainbloeckePerHeight - widthKernel;

				//write codebuch entries into kernel array
				int[][] domainKernel = new int[widthKernel * widthKernel][blockgroesse * blockgroesse];
				int n = 0;			
				for (int ky = 0; ky < widthKernel; ky++) {
					for (int kx = 0; kx < widthKernel; kx++) {
						int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
						domainKernel[n] = codebuch[index];
						n++;
					}
				}
				//---------------
				
				// apply algorithm based on minimum error to find best fit domain block
				imageInfo[j] = getBestDomainblock(domainKernel, getRangeblock(x, y, input));	
				j++;
			}
		}
		
		//write out values to be read from decoder
		out.writeInt(input.width);
		out.writeInt(input.height);
		out.writeInt(blockgroesse);
		out.writeInt(imageInfo.length);
		out.writeInt(imageInfo[0].length);
		
		calculateIndices(input.width, input.height);
				
		for(int row=0;row<imageInfo.length;row++) {
			for(int column=0;column<imageInfo[0].length;column++) {
	         	int intBits =  Float.floatToIntBits(imageInfo[row][column]); 
	         	out.writeByte((byte) (intBits >> 24));
	         	out.writeByte((byte) (intBits >> 16));
	         	out.writeByte((byte) (intBits >> 8));
	         	out.writeByte((byte) (intBits));
			}
		}
		
		out.close();
	
		return dst;
	}
	
	/**
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static RasterImage decode(DataInputStream inputStream) throws Exception{
			int width = inputStream.readInt();
			int height = inputStream.readInt();
			
			RasterImage image = FractalCompression.getGreyImage(width, height);

			
			int inputedBlockgroesse = inputStream.readInt();

			int imgDatarows = inputStream.readInt();
			int imgDatacols = inputStream.readInt();
			
			float[][] imgData = new float[imgDatarows][imgDatacols];
			
			while(inputStream.available() > 0) {
		
					for(int rows=0; rows<imgDatarows; rows++) {
						for(int cols=0; cols<imgDatacols; cols++) {
							//int tmp = inputStream.readByte() & 0xff;
							
							int intBits = inputStream.readByte() << 24 | (inputStream.readByte() & 0xFF) << 16 
									| (inputStream.readByte() & 0xFF) << 8 | (inputStream.readByte() & 0xFF);
							float number = Float.intBitsToFloat(intBits);
							
//							int tmp = inputStream.readInt();
//			        	    float number = Float.intBitsToFloat(tmp);
							imgData[rows][cols] = number;
				} }}
		
			//calculateIndices(width, height);

			// make iterations for image reconstruction
			for (int counter = 0; counter < 50; counter++) {
				int[][] codebuch = createCodebuch(image); // get codebook
				int i = 0;

				// iterate image per rangeblock
				for (int y = 0; y < image.height; y += inputedBlockgroesse) {
					for (int x = 0; x < image.width; x += inputedBlockgroesse) {
						// iterate rangeblock
						for (int ry = 0; ry < inputedBlockgroesse && y + ry < image.height; ry++) {
							for (int rx = 0; rx < inputedBlockgroesse && x + rx < image.width; rx++) {
								int range = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value
																										// of rangeblock
								// get current value of best fit domainblock pixel
								int domain = codebuch[(int) imgData[i][0]][rx + ry * inputedBlockgroesse];
								int value = (int) (imgData[i][1] * domain + imgData[i][2]);

								// apply thresshold
								if (value < 0)
									value = 0;
								else if (value > 255)
									value = 255;

								image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (value << 16) | (value << 8)
										| value;

								avgError += (range - value) * (range - value); // calculate error
							}
						}
						i++;
					}
				}
				avgError = avgError / (float) (width * height);
				if (avgError < 1)
					break; // stop iterations when error drops below 1
				if (counter != 49)
					avgError = 0;
			}															// pixel
 
			return image;	
	}
	
	/**
	 * Calculates the index of the domainblock on top of the rangeblock
	 * 
	 * @param x,y,rangeblockePerWidth, rangeblockePerHeight
	 * @return index of domainblock
	 */
	private static int getDomainIndex(int x, int y, int rangebloeckePerWidth, int rangebloeckePerHeight,
			int domainbloeckePerWidth) {
		int xr = x / 8;
		int yr = y / 8;
		int i = 0;
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
		return i;
	}

	/**
	 * Gets positions x,y and returns the range block starting from these
	 * coordinates.
	 * 
	 * @param x     Position in x achse of the image
	 * @param y     Position in y achse of the image
	 * @param image Image to be processed
	 * @return in array containing the rangeblock values
	 */
	private static int[] getRangeblock(int x, int y, RasterImage image) {
		int[] rangeblock = new int[blockgroesse * blockgroesse];
		int i = 0;

		// iterates range block and extracts grey values
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
	 * finds the best matching domainblock for the given rangeblock out of a given array of domainblocks
	 * 
	 * 
	 * @param domainblocks
	 * @param rangeblock
	 * @return
	 */
	private static float[] getBestDomainblock(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		float[] bestBlock = { 0, 0, 0 };

		// iterate domain blocks
		for (int i = 0; i < domainblocks.length; i++) {
			// get Aopt and Bopt for currently visited domainblock
			float[] ab = getContrastAndBrightness(domainblocks[i], rangeblock);
			float error = 0;
			int[] blockAdjusted = new int[blockgroesse * blockgroesse];

			for (int j = 0; j < blockgroesse * blockgroesse; j++) {//iterates through domainblock
				// get domain values adjusted by Aopt and Bopt for error calculation
				int domainValue = (int) (ab[0] * domainblocks[i][j] + ab[1]);

				// apply threshold
				if (domainValue < 0)
					domainValue = 0;
				else if (domainValue > 255)
					domainValue = 255;

				error += (rangeblock[j] - domainValue) * (rangeblock[j] - domainValue);
				blockAdjusted[j] = domainValue;
			}

			// check if current error smaller than previous errors
			if (error < smallestError) {
				smallestError = error;
				float[] temp = { i, ab[0], ab[1] };
				bestBlock = temp;
			}
		}
		return bestBlock;
	}

	/**
	 * Calculates a and b luminence factors
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

		// iterate domain block
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

		// get b
		float b = rangeM - a * domainM;
		float[] result = { a, b };
		return result;
	}

	/**
	 * Gets an array of integers and returns the average value.
	 * 
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
	 * calculates the indices from the domain-Kernel to the codebook index
	 * 
	 * @param width, height of image
	 * @return
	 */
	private static void calculateIndices(int width, int height) {
		int rangebloeckePerWidth = width / blockgroesse;
		int rangebloeckePerHeight = height / blockgroesse;

		// calculate domainblock per dimension
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

		int i = 0;
		for (int y = 0; y < height; y += blockgroesse) {
			for (int x = 0; x < width; x += blockgroesse) {
				int di = getDomainIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);

				// calculate kernel start point
				int dy = (int) (di / domainbloeckePerWidth) - widthKernel / 2;
				int dx = di % domainbloeckePerWidth - widthKernel / 2;
				
				//Randbehandlung
				if (dx < 0)
					dx = 0;
				if (dy < 0)
					dy = 0;
				if (dx + widthKernel >= domainbloeckePerWidth)
					dx = domainbloeckePerWidth - widthKernel;
				if (dy + widthKernel >= domainbloeckePerHeight)
					dy = domainbloeckePerHeight - widthKernel;

				// calculate x and y from index of kernel
				int yd = (int) (imageInfo[i][0] / widthKernel);
				int xd = (int) (imageInfo[i][0] % widthKernel);

				// combine to index
				int result = xd + dx + (yd + dy) * domainbloeckePerWidth;

				imageInfo[i][0] = result;
				i++;
			}
		}
	}

//	/**
//	 *decodes an image based on codebook indices, brightness and contrast values
//	 * 
//	 * @param width, height
//	 * @return
//	 */
//	public static RasterImage decoder(int width, int height) {
//		// start from grey image
//		RasterImage image = FractalCompression.getGreyImage(width, height);
//
//		calculateIndices(width, height);
//
//		// make iterations for image reconstruction
//		for (int counter = 0; counter < 50; counter++) {
//			int[][] codebuch = createCodebuch(image); // get codebook
//			int i = 0;
//
//			// iterate image per rangeblock
//			for (int y = 0; y < image.height; y += blockgroesse) {
//				for (int x = 0; x < image.width; x += blockgroesse) {
//					// iterate rangeblock
//					for (int ry = 0; ry < blockgroesse && y + ry < image.height; ry++) {
//						for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
//							int range = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value
//																									// of rangeblock
//																									// pixel
//
//							// get current value of best fit domainblock pixel
//							int domain = codebuch[(int) imageInfo[i][0]][rx + ry * blockgroesse];
//							int value = (int) (imageInfo[i][1] * domain + imageInfo[i][2]);
//
//							// apply thresshold
//							if (value < 0)
//								value = 0;
//							else if (value > 255)
//								value = 255;
//
//							image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (value << 16) | (value << 8)
//									| value;
//
//							avgError += (range - value) * (range - value); // calculate error
//						}
//					}
//					i++;
//				}
//			}
//			avgError = avgError / (float) (width * height);
//			if (avgError < 1)
//				break; // stop iterations when error drops below 1
//			if (counter != 49)
//				avgError = 0;
//		}
//		return image;
//	}

	/**
	 * Gets a RasterImage and scales it down by factor 2.
	 * 
	 * @param image RasterImage to be processed
	 * @return scaled RasterImage
	 */
	private static RasterImage scaleImage(RasterImage image) {
		RasterImage scaled = new RasterImage(image.width / 2, image.height / 2);
		int i = 0;
		for (int y = 0; y < image.height; y += 2) {
			for (int x = 0; x < image.width; x += 2) {

				// Mittelwert bestimmen
				int mittelwert = (image.argb[x + y * image.width] >> 16) & 0xff;

				// Randbehandlung-----
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
				// -----

				mittelwert = mittelwert / 4;
				scaled.argb[i] = 0xff000000 | (mittelwert << 16) | (mittelwert << 8) | mittelwert;
				i++;
			}
		}
		return scaled;

	}

	/**
	 * Gets an RasterImage and returns a 2D of array containing a codebook
	 * 
	 * @param image RasterImage to be processed
	 * @return codebook array
	 */
	private static int[][] createCodebuch(RasterImage image) {

		// scale image by factor 2
		image = scaleImage(image);
		int abstand = blockgroesse / 4;

		// generated codebook size
		int[][] codebuch = new int[(image.width / abstand - 3) * (image.height / abstand - 3)][blockgroesse
				* blockgroesse];

		int i = 0;

		// iterate image
		for (int y = 0; y < image.height; y += abstand) {
			for (int x = 0; x < image.width; x += abstand) {
				int[] codebuchblock = new int[blockgroesse * blockgroesse];
				// iterate domainblock
				if (y + blockgroesse <= image.height && x + blockgroesse <= image.width) {
					for (int ry = 0; ry < blockgroesse; ry++) {
						for (int rx = 0; rx < blockgroesse; rx++) {

							codebuchblock[rx + ry * blockgroesse] = (image.argb[x + rx + (y + ry) * image.width] >> 16)
									& 0xff;
						}
					}
					// map domainblock pixel values to domainblock index
					codebuch[i] = codebuchblock;
					i++;
				}
			}
		}
		return codebuch;
	}

	/**
	 * Gets a RasterImage and displays the codebook image generated by it.
	 * 
	 * @param image
	 * @return
	 */
	public static RasterImage showCodebuch(RasterImage image) {
		int rangebloeckePerWidth = image.width / blockgroesse;
		int rangebloeckePerHeight = image.height / blockgroesse;

		// calculate domainblock per dimension
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;
		// generate codebook
		int[][] codebuch = createCodebuch(image);
		int i = 0;

		// generate image to display
		RasterImage codebuchImage = new RasterImage(domainbloeckePerWidth*blockgroesse+domainbloeckePerWidth,domainbloeckePerHeight*blockgroesse+domainbloeckePerHeight);

		// iterate image
		for (int y = 0; y < codebuchImage.height; y += blockgroesse+1) {
			for (int x = 0; x < codebuchImage.width; x += blockgroesse+1) {
				for (int ry = 0; ry < blockgroesse && y + ry < codebuchImage.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < codebuchImage.width; rx++) {
						int value = codebuch[i][rx + ry * blockgroesse];
						codebuchImage.argb[x + rx + (y + ry) * codebuchImage.width] = 0xff000000 | (value << 16)
								| (value << 8) | value;
					}
				}
				i++;
			}
		}
		return codebuchImage;
	}

	/**
	 * Generates a grey RasterImage from given width and height.
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
