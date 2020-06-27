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
	private static float[][] imageInfoRGB; //for decoder later as file


	public static float avgError;

	public static float getAvgError() {
		return avgError;
	}
	
	
	public static boolean isGreyScale(RasterImage input){
		for(int y=0;y<input.height;y++) {
			for(int x=0;x<input.width;x++) {
				int r = (input.argb[x+y*input.width] >> 16) & 0xff;
				int g = (input.argb[x+y*input.width] >> 8) & 0xff;
				int b =  input.argb[x+y*input.width] & 0xff;
				
				if(r != g || g != b || b != r) {
					return false;
				}

			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param input
	 * @param out
	 * @throws IOException
	 */
	public static RasterImage encode(RasterImage input,DataOutputStream out) throws IOException  {
		if(isGreyScale(input)) return encodeGrayScale(input,out);
		else return encodeRGB(input,out);
	}
	
	

	/**
	 * Applies fractal image compression to a given RasterImage.
	 * @param input RasterImage to be processed
	 * @return compressed RasterImage
	 */
	public static RasterImage encodeGrayScale(RasterImage input,DataOutputStream out) throws IOException  {
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
		imageInfo = new float[rangebloeckePerWidth*rangebloeckePerHeight][3];//for decoder later write to file
		for (int y = 0; y < dst.height; y += blockgroesse) {
			for (int x = 0; x < dst.width; x += blockgroesse) {

				int i = getDomainBlockIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);
				
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
		
		out.writeInt(0);
		//write out values to be read from decoder
		out.writeInt(input.width);
		out.writeInt(input.height);
		out.writeInt(blockgroesse);
		out.writeInt(widthKernel);

				
		for(int row=0;row<imageInfo.length;row++) {			
				out.writeInt((int)(imageInfo[row][0]));
				out.writeInt((int)(imageInfo[row][1]*100));
				out.writeInt((int)(imageInfo[row][2]));
		}		
		out.close();
	
		return getBestGeneratedCollage(input);
	}


	
	/**
	 * Applies fractal image compression to a given RasterImage.
	 * 
	 * @param input RasterImage to be processed
	 * @return compressed RasterImage
	 */
	public static RasterImage encodeRGB(RasterImage input,DataOutputStream out) throws IOException  {
		// calculate rangeblock per dimension
		int rangebloeckePerWidth = input.width / blockgroesse;
		int rangebloeckePerHeight = input.height / blockgroesse;

		// calculate domainblock per dimension
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

		// generate codebook to read domain blocks from
		int[][] codebuch = createCodebuchRGB(input);
		RasterImage dst = new RasterImage(input.width, input.height);

		int j = 0;
		imageInfoRGB = new float[rangebloeckePerWidth*rangebloeckePerHeight][5];//for decoder later write to file
		for (int y = 0; y < dst.height; y += blockgroesse) {
			for (int x = 0; x < dst.width; x += blockgroesse) {

				int i = getDomainBlockIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);
				
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
				imageInfoRGB[j] = getBestDomainblockRGB(domainKernel, getRangeblockRGB(x, y, input));	
				j++;
			}
		}
		
		out.writeInt(1);
		//write out values to be read from decoder
		out.writeInt(input.width);
		out.writeInt(input.height);
		out.writeInt(blockgroesse);
		out.writeInt(widthKernel);

				
		for(int row=0;row<imageInfoRGB.length;row++) {	
				out.writeInt((int)(imageInfoRGB[row][0]));
				out.writeInt((int)(imageInfoRGB[row][1]*1000000));
				out.writeInt((int)(imageInfoRGB[row][2]*100000));
				out.writeInt((int)(imageInfoRGB[row][3]*100000));
				out.writeInt((int)(imageInfoRGB[row][4]));


		}		
		out.close();
	
		return getBestGeneratedCollageRGB(input);
	}
	
	/**
	 * 
	 * @param encodedImage
	 * @return
	 */
	public static RasterImage getBestGeneratedCollage(RasterImage encodedImage) {
		
		RasterImage image = FractalCompression.generateGrayImage(encodedImage.width, encodedImage.height);
		
		calculateIndices(imageInfo, encodedImage.width, encodedImage.height, blockgroesse, widthKernel);

		// make iterations for image reconstruction
		for (int counter = 0; counter < 50; counter++) {
			int[][] codebuch = createCodebuch(image); // get codebook
			int i = 0;

			// iterate image per rangeblock
			for (int y = 0; y < image.height; y += blockgroesse) {
				for (int x = 0; x < image.width; x += blockgroesse) {
					// iterate rangeblock
					for (int ry = 0; ry < blockgroesse && y + ry < image.height; ry++) {
						for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
							int range = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value
																									// of rangeblock
							// get current value of best fit domainblock pixel
							int domain = codebuch[(int) imageInfo[i][0]][rx + ry * blockgroesse];
							int value = (int) (imageInfo[i][1] * domain + imageInfo[i][2]);

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
		}
		return image;	

		
	}
	
	
	/**
	 * 
	 * @param encodedImage
	 * @return
	 */
	public static RasterImage getBestGeneratedCollageRGB(RasterImage encodedImage) {
		
		RasterImage image = FractalCompression.generateGrayImage(encodedImage.width, encodedImage.height);
		
		calculateIndices(imageInfoRGB, encodedImage.width, encodedImage.height, blockgroesse, widthKernel);

		// make iterations for image reconstruction
		for (int counter = 0; counter < 50; counter++) {
			int[][] codebuch = createCodebuchRGB(image); // get codebook
			int i = 0;

			// iterate image per rangeblock
			for (int y = 0; y < image.height; y += blockgroesse) {
				for (int x = 0; x < image.width; x += blockgroesse) {
					// iterate rangeblock
					for (int ry = 0; ry < blockgroesse && y + ry < image.height; ry++) {
						for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
							int rangeR = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value of rangeblock
							int rangeG = (image.argb[x + rx + (y + ry) * image.width] >> 8) & 0xff; // get current value of rangeblock
							int rangeB = image.argb[x + rx + (y + ry) * image.width]  & 0xff; // get current value of rangeblock
							
							
							
							// get current value of best fit domainblock pixel
							int domain = codebuch[(int) imageInfoRGB[i][0]][rx + ry * blockgroesse];
							int domainR = (domain >> 16) & 0xff;
							int domainG = (domain >> 8) & 0xff;
							int domainB = domain & 0xff;
							
							int valueR = (int) (imageInfoRGB[i][1] * domainR + imageInfoRGB[i][2]);
							int valueG = (int) (imageInfoRGB[i][1] * domainG + imageInfoRGB[i][3]);
							int valueB = (int) (imageInfoRGB[i][1] * domainB + imageInfoRGB[i][4]);

							
							// apply thresshold
							valueR = applyThreshold(valueR);
							valueG = applyThreshold(valueG);
							valueB = applyThreshold(valueB);
							
							image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (valueR << 16) | (valueG << 8)| valueB;
						}
					}
					i++;
				}
			}
		}
		return image;	

		
	}
	
	
	
	
	
	
	
	
	/**
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static RasterImage decodeGreyScale(DataInputStream inputStream) throws Exception{
			int width = inputStream.readInt();
			int height = inputStream.readInt();
			
			RasterImage image = FractalCompression.generateGrayImage(width, height);
			
			int inputedBlockgroesse = inputStream.readInt();
			int widthKernel = inputStream.readInt();
			
			int rangebloeckePerWidth = width / inputedBlockgroesse;
			int rangebloeckePerHeight = height / inputedBlockgroesse;
			
			
			float[][] imgData = new float[rangebloeckePerWidth*rangebloeckePerHeight][3];
			
			while(inputStream.available() > 0) {		
					for(int rows=0; rows<imgData.length; rows++) {
						imgData[rows][0]=(float)inputStream.readInt();
						imgData[rows][1]=(float)inputStream.readInt()/100f;
						imgData[rows][2]=(float)inputStream.readInt();
					}}
		
			calculateIndices(imgData, width, height, inputedBlockgroesse, widthKernel);

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
	 * 
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	public static RasterImage decodeRGB(DataInputStream inputStream) throws Exception{
			int width = inputStream.readInt();
			int height = inputStream.readInt();
			
			RasterImage image = FractalCompression.generateGrayImage(width, height);
			
			int inputedBlockgroesse = inputStream.readInt();
			int widthKernel = inputStream.readInt();
			
			int rangebloeckePerWidth = width / inputedBlockgroesse;
			int rangebloeckePerHeight = height / inputedBlockgroesse;
			
			
			float[][] imgData = new float[rangebloeckePerWidth*rangebloeckePerHeight][5];
			
			while(inputStream.available() > 0) {		
					for(int rows=0; rows<imgData.length; rows++) {
						imgData[rows][0]=(float)inputStream.readInt();
						imgData[rows][1]=(float)inputStream.readInt()/1000000f;
						imgData[rows][2]=(float)inputStream.readInt()/100000f;
						imgData[rows][3]=(float)inputStream.readInt()/100000f;
						imgData[rows][4]=(float)inputStream.readInt();

					}}
					
			calculateIndices(imgData, width, height, inputedBlockgroesse, widthKernel);

			// make iterations for image reconstruction
			for (int counter = 0; counter < 50; counter++) {
				int[][] codebuch = createCodebuchRGB(image); // get codebook
				int i = 0;

				// iterate image per rangeblock
				for (int y = 0; y < image.height; y += inputedBlockgroesse) {
					for (int x = 0; x < image.width; x += inputedBlockgroesse) {
						// iterate rangeblock
						for (int ry = 0; ry < inputedBlockgroesse && y + ry < image.height; ry++) {
							for (int rx = 0; rx < inputedBlockgroesse && x + rx < image.width; rx++) {
								int rangeR = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff; // get current value of rangeblock
								int rangeG = (image.argb[x + rx + (y + ry) * image.width] >> 8) & 0xff; // get current value of rangeblock
								int rangeB = image.argb[x + rx + (y + ry) * image.width]  & 0xff; // get current value of rangeblock
								
								
								
								// get current value of best fit domainblock pixel
								int domain = codebuch[(int) imgData[i][0]][rx + ry * inputedBlockgroesse];
								int domainR = (domain >> 16) & 0xff;
								int domainG = (domain >> 8) & 0xff;
								int domainB = domain & 0xff;
								
								int valueR = (int) (imgData[i][1] * domainR + imgData[i][2]);
								int valueG = (int) (imgData[i][1] * domainG + imgData[i][3]);
								int valueB = (int) (imgData[i][1] * domainB + imgData[i][4]);

								//System.out.println(imgData[i][2] + " " + imgData[i][3] + " " + imgData[i][4]);
								//System.out.println(imgData[i][1]);

								// apply thresshold
								
								valueR = applyThreshold(valueR);
								valueG = applyThreshold(valueG);
								valueB = applyThreshold(valueB);
								
								image.argb[x + rx + (y + ry) * image.width] = 0xff000000 | (valueR << 16) | (valueG << 8)| valueB;

								avgError += (rangeR - valueR) * (rangeR - valueR) +
										    (rangeG - valueG) * (rangeG - valueG) +
										    (rangeB - valueB) * (rangeB - valueB); // calculate error
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
	private static int getDomainBlockIndex(int x, int y, int rangebloeckePerWidth, int rangebloeckePerHeight,
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

	public static RasterImage decode(DataInputStream inputStream) throws Exception{
		int isGreyScale = inputStream.readInt();
		if(isGreyScale == 0) return decodeGreyScale(inputStream);
		else return decodeRGB(inputStream);
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
	private static int[] getRangeblockRGB(int x, int y, RasterImage image) {
		int[] rangeblock = new int[blockgroesse * blockgroesse];
		int i = 0;

		// iterates range block and extracts grey values
		for (int ry = 0; ry < blockgroesse && y + ry < image.height; ry++) {
			for (int rx = 0; rx < blockgroesse && x + rx < image.width; rx++) {
				int value = image.argb[(x + rx) + (y + ry) * image.width];
				//value = (value >> 16) & 0xff; für grauwert
				rangeblock[i] = value;
				i++;
			}
		}
		return rangeblock;
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
			float[] ab = getLuminenceAndTransformFactor(domainblocks[i], rangeblock);
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
				// blockAdjusted[j] = domainValue;
				
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
	 * finds the best matching domainblock for the given rangeblock out of a given array of domainblocks
	 * 
	 * 
	 * @param domainblocks
	 * @param rangeblock
	 * @return
	 */
	private static float[] getBestDomainblockRGB(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		float[] bestBlock = { 0, 0, 0, 0, 0 };

		// iterate domain blocks
		for (int i = 0; i < domainblocks.length; i++) {
			// get Aopt and Bopt for currently visited domainblock
			float[] ab = getLuminenceAndTransformFactorRGB(domainblocks[i], rangeblock);
			//System.out.println(ab[0] + " " + ab[1] + " " + ab[2]+ " "+ ab[3]);

			float error = 0;
			int[] blockAdjusted = new int[blockgroesse * blockgroesse];

			for (int j = 0; j < blockgroesse * blockgroesse; j++) {//iterates through domainblock
				// get domain values adjusted by Aopt and Bopt for error calculation
				int domainValueR = (int) (ab[0] * ((domainblocks[i][j] >> 16) & 0xff) + ab[1]);
				int domainValueG = (int) (ab[0] * ((domainblocks[i][j]  >> 8) & 0xff) + ab[2]);
				int domainValueB = (int) (ab[0] * (domainblocks[i][j] & 0xff) + ab[3]);

				int rangeR = (rangeblock[j] >> 16) & 0xff;
				int rangeG = (rangeblock[j] >> 8) & 0xff;
				int rangeB = rangeblock[j] & 0xff;
				

				// apply threshold
				domainValueR = applyThreshold(domainValueR);
				domainValueG = applyThreshold(domainValueG);
				domainValueB = applyThreshold(domainValueB);


				error += (rangeR - domainValueR) * (rangeR - domainValueR) + 
						 (rangeG - domainValueG) * (rangeG - domainValueG) +
						 (rangeB - domainValueB) * (rangeB - domainValueB);
				
			}

			// check if current error smaller than previous errors
			if (error < smallestError) {
				smallestError = error;
				float[] temp = { i, ab[0], ab[1], ab[2], ab[3] };
				bestBlock = temp;
			}
		}
		return bestBlock;
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	private static int applyThreshold(int pixelValue) {
		if (pixelValue < 0)
			pixelValue = 0;
		else if (pixelValue > 255)
			pixelValue = 255; 
		return pixelValue;
	}
	
	/**
	 * Calculates a and b luminence factors
	 * 
	 * @param domain
	 * @param range
	 * @return
	 */
	private static float[] getLuminenceAndTransformFactor(int[] domain, int[] range) {
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
	 * Calculates a and b luminence factors
	 * 
	 * @param domain
	 * @param range
	 * @return
	 */
	private static float[] getLuminenceAndTransformFactorRGB(int[] domain, int[] range) {
		
		int[] domainR =  getRGB(domain,0); 
		int[] domainG =  getRGB(domain,1);
		int[] domainB =  getRGB(domain,2);

		int[] rangeR =   getRGB(range,0);
		int[] rangeG =   getRGB(range,1);
		int[] rangeB =   getRGB(range,2);
		
		
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
		float bR = getMittelwert(rangeR) - a * getMittelwert(domainR);
		
		//System.out.println(bR + " -> bR");
		float bG = getMittelwert(rangeG) - a * getMittelwert(domainG);
		
		//System.out.println(bG + " -> bG");

		float bB = getMittelwert(rangeB) - a * getMittelwert(domainB);
		
		//System.out.println(bB + " -> bB");


		float[] result = { a, bR, bG, bB };
		return result;
	}
	
	
	public static int[] getRGB(int[] argbBytes, int canal) {
		
		int[] temp = new int[argbBytes.length];
		
		//red
		if(canal == 0) {
			for(int i=0; i<argbBytes.length;i++) {
				temp[i] = (argbBytes[i] >> 16) & 0xff;
				//System.out.println(i + "   R  " + temp[i]);
			}
		}
		
		//green
		else if(canal == 1) {
			for(int i=0; i<argbBytes.length;i++) {
				temp[i] = (argbBytes[i] >> 8) & 0xff;
				//System.out.println(i + "   G  " + temp[i]);

			}
		}
		
		//blue
		else if(canal == 2) {
			for(int i=0; i<argbBytes.length;i++) {
				temp[i] = argbBytes[i]  & 0xff;
				//System.out.println(i + "   B  " + temp[i]);

			}
		}
		
		return temp;
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
	private static float[][] calculateIndices(float[][] imgData, int width, int height, int blockgroesse, int widthKernel) {
		int rangebloeckePerWidth = width / blockgroesse;
		int rangebloeckePerHeight = height / blockgroesse;

		// calculate domainblock per dimension
		int domainbloeckePerWidth = rangebloeckePerWidth * 2 - 3;
		int domainbloeckePerHeight = rangebloeckePerHeight * 2 - 3;

		int i = 0;
		for (int y = 0; y < height; y += blockgroesse) {
			for (int x = 0; x < width; x += blockgroesse) {
				int di = getDomainBlockIndex(x, y, rangebloeckePerWidth, rangebloeckePerHeight, domainbloeckePerWidth);

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
				int yd = (int) (imgData[i][0] / widthKernel);
				int xd = (int) (imgData[i][0] % widthKernel);

				// combine to index
				int result = xd + dx + (yd + dy) * domainbloeckePerWidth;

				imgData[i][0] = result;
				i++;
			}
		}
		return null;
	}

	
	/**
	 * Gets a RasterImage and scales it down by factor 2.
	 * 
	 * @param image RasterImage to be processed
	 * @return scaled RasterImage
	 */
	public static RasterImage scaleImageRGB(RasterImage image) {
		RasterImage scaled = new RasterImage(image.width / 2, image.height / 2);
		int i = 0;
		for (int y = 0; y < image.height; y += 2) {
			for (int x = 0; x < image.width; x += 2) {

				// Mittelwert bestimmen
				int mittelwertR = (image.argb[x + y * image.width] >> 16) & 0xff;
				int mittelwertG = (image.argb[x + y * image.width] >> 8) & 0xff;
				int mittelwertB = image.argb[x + y * image.width] & 0xff;



				// Randbehandlung-----
				if (x + 1 >= image.width) {
					mittelwertR += 128;
					mittelwertG += 128;
					mittelwertB += 128;

					
				} else {
					mittelwertR += (image.argb[x + 1 + y * image.width] >> 16) & 0xff;
					mittelwertG += (image.argb[x + 1 + y * image.width] >> 8) & 0xff;
					mittelwertB += image.argb[x + 1 + y * image.width] & 0xff;
				

				if (y + 1 >= image.height)  {
						mittelwertR += 128;
						mittelwertG += 128;
						mittelwertB += 128;		
			}
					else {
						mittelwertR += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
						mittelwertG += (image.argb[x + (y + 1) * image.width] >> 8) & 0xff;
						mittelwertB +=  image.argb[x + (y + 1) * image.width]  & 0xff;

				}
				}
				
				// Randbehandlung-----
				if (y + 1 >= image.height) {
					mittelwertR += 128;
					mittelwertG += 128;
					mittelwertB += 128;					}
				else {
					if (x + 1 >= image.height) {
						mittelwertR += 128;
					    mittelwertG += 128;
					    mittelwertB += 128;		
					}				
					else {
						mittelwertR += (image.argb[x + (y + 1) * image.width] >> 16) & 0xff;
						mittelwertG += (image.argb[x + (y + 1) * image.width] >> 8) & 0xff;
						mittelwertB +=  image.argb[x + (y + 1) * image.width]  & 0xff;
				} }
				// -----

				mittelwertR = mittelwertR / 4;
				mittelwertG = mittelwertG / 4;
				mittelwertB = mittelwertB / 4;

				scaled.argb[i] = 0xff000000 | (mittelwertR << 16) | (mittelwertG << 8) | mittelwertB;
				i++;
			}
		}
		return scaled;

	}
	
	
	
	
	
	
	
	
	
	

	/**
	 * Gets a RasterImage and scales it down by factor 2.
	 * 
	 * @param image RasterImage to be processed
	 * @return scaled RasterImage
	 */
	public static RasterImage scaleImage(RasterImage image) {
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
	 * Gets an RasterImage and returns a 2D of array containing a codebook
	 * 
	 * @param image RasterImage to be processed
	 * @return codebook array
	 */
	private static int[][] createCodebuchRGB(RasterImage image) {

		// scale image by factor 2
		image = scaleImageRGB(image);
		int abstand = blockgroesse / 4;

		// generated codebook size
		int[][] codebuch = new int[(image.width / abstand - 3) * (image.height / abstand - 3)][blockgroesse * blockgroesse];

		int i = 0;

		// iterate image
		for (int y = 0; y < image.height; y += abstand) {
			for (int x = 0; x < image.width; x += abstand) {
				int[] codebuchblock = new int[blockgroesse * blockgroesse];
				// iterate domainblock
				if (y + blockgroesse <= image.height && x + blockgroesse <= image.width) {
					for (int ry = 0; ry < blockgroesse; ry++) {
						for (int rx = 0; rx < blockgroesse; rx++) {

							int valueR = (image.argb[x + rx + (y + ry) * image.width] >> 16) & 0xff;
							int valueG = (image.argb[x + rx + (y + ry) * image.width] >> 8) & 0xff;
							int valueB = image.argb[x + rx + (y + ry) * image.width]  & 0xff;

							codebuchblock[rx + ry * blockgroesse] = 0xff000000 | (valueR << 16) | (valueG << 8) | valueB;
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
		int[][] codebuch = createCodebuchRGB(image);
		int i = 0;

		// generate image to display
		RasterImage codebuchImage = new RasterImage(domainbloeckePerWidth*blockgroesse+domainbloeckePerWidth,domainbloeckePerHeight*blockgroesse+domainbloeckePerHeight);

		// iterate image
		for (int y = 0; y < codebuchImage.height; y += blockgroesse+1) {
			for (int x = 0; x < codebuchImage.width; x += blockgroesse+1) {
				for (int ry = 0; ry < blockgroesse && y + ry < codebuchImage.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < codebuchImage.width; rx++) {
						//int value = codebuch[i][rx + ry * blockgroesse];
						
						int valueR = (codebuch[i][rx + ry * blockgroesse] >> 16) & 0xff;
						int valueG = (codebuch[i][rx + ry * blockgroesse] >> 8) & 0xff;
						int valueB = codebuch[i][rx + ry * blockgroesse] & 0xff;
						
						codebuchImage.argb[x + rx + (y + ry) * codebuchImage.width] = 0xff000000 | (valueR << 16)
								| (valueG << 8) | valueB;
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
	public static RasterImage generateGrayImage(int width, int height) {
		RasterImage image = new RasterImage(width, height);
		for (int i = 0; i < image.argb.length; i++) {
			image.argb[i] = 0xff000000 | (128 << 16) | (128 << 8) | 128;
		}
		return image;
	}
}
