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
import java.util.List;

public class RLE {
	private static int blockgroesse = 8;

	/**
	 * 
	 * @param image
	 * @param out
	 * @throws IOException
	 */
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

	/**
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
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
	public static RasterImage domainBlockApprox(RasterImage input) {
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

				int widthKernel = 5;

				int dy = (int) (i / domainbloeckePerWidth);
				int dx = i % domainbloeckePerWidth;

				if (dx == 0)
					dx = widthKernel / 2;
				if (dy == 0)
					dy = widthKernel / 2;
				if (dx + widthKernel >= domainbloeckePerWidth)
					dx = domainbloeckePerWidth - 3 - widthKernel / 2;
				if (dy + widthKernel >= domainbloeckePerHeight)
					dy = domainbloeckePerHeight - 3 - widthKernel / 2;
				int[][] domainKernel = new int[widthKernel * widthKernel][blockgroesse * blockgroesse];
				int n = 0;
				for (int ky = 0; ky < widthKernel; ky++) {
					for (int kx = 0; kx < widthKernel; kx++) {
						int index = dx + kx + (dy + ky) * domainbloeckePerWidth;
						domainKernel[n] = codebuch[index];
						n++;
					}

				}
				i = getBestDomainblock(domainKernel, getRangeblock(x, y, input));
				
				//TODO index ist vom kernel nicht vom codebuch --> umrechnen
				System.out.println(i);
				for (int ry = 0; ry < blockgroesse && y + ry < dst.height; ry++) {
					for (int rx = 0; rx < blockgroesse && x + rx < dst.width; rx++) {
						int value = codebuch[i][rx + ry * blockgroesse];
						dst.argb[x + rx + (y + ry) * dst.width] = 0xff000000 | (value << 16) | (value << 8) | value;
					}
				}
			}
		}
//		dst = adjustContrastBrightness(dst, input);
		return dst;
	}

	public static int[] getDomainblockKernel() {
		return null;

	}

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

	public static int getBestDomainblock(int[][] domainblocks, int[] rangeblock) {
		float smallestError = 10000000;
		int bestBlock = 0;
		for (int i = 0; i < domainblocks.length; i++) {
			float[] ab = getContrastAndBrightness(domainblocks[i], rangeblock);
			float error = 0;

			for (int j = 0; j < blockgroesse * blockgroesse; j++) { // Kontrast und Helligkeit anpassen
				
				int value = (int) (ab[0] * domainblocks[i][j] - ab[1]);
				if (value < 0)
					value = 0;
				else if (value > 255)
					value = 255;
				domainblocks[i][j] = value;
				System.out.println(rangeblock[i] - domainblocks[i][j]);

				error += (rangeblock[i] - domainblocks[i][j]) * (rangeblock[j] - domainblocks[i][j]);

			}
			//System.out.println(error);

			error = (1 / (blockgroesse * blockgroesse)) * error;
			if (error < smallestError) {
				smallestError = error;
				bestBlock = i;
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
	public static RasterImage adjustContrastBrightness(RasterImage domain, RasterImage range) {
		int blockgroesse = 8;
		int y, x;
		for (y = 0; y < domain.height; y++) {
			for (x = 0; x < domain.width; x++) {
				int domainM = 0; // Summe der Grauwerte
				int rangeM = 0;
				int ry = 0;
				int rx = 0;
				int[] rangeb = new int[blockgroesse * blockgroesse];
				int n = 0;
				for (ry = 0; ry < blockgroesse && y + ry < domain.height; ry++) { // Rangeblöcke Grauwerte summieren für
					// Mittelwert
					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
						int greyD = (domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff;
						domainM += greyD;

						int greyR = (range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff;
						rangeb[n] = greyR;
						n++;
						rangeM += greyR;
					}
				}
				domainM = domainM / (rx * ry); // Mittelwert
				rangeM = rangeM / (rx * ry);

				int[] rangeblock = getRangeblock(x, y, range);
				int m = getMittelwert(rangeblock);
				float varianz = 0;
				float kovarianz = 0;
				for (ry = 0; ry < blockgroesse && y + ry < domain.height; ry++) { // Summe Grauwert minus Mittelwert
					for (rx = 0; rx < blockgroesse && x + rx < domain.width; rx++) {
						float greyD = ((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) - domainM;
						float greyR = ((range.argb[x + rx + (y + ry) * range.width] >> 16) & 0xff) - rangeM;
						varianz += greyR * greyD;
						kovarianz += greyD * greyD;
					}
				}

				// Kontrast und Helligkeit
				float a;
				if (kovarianz == 0)
					a = 1;
				else
					a = varianz / kovarianz;
				// if (a == 0)
				// a = 1;
				if (a > 1)
					a = 1;
				if (a < -1)
					a = -1;
				float b = rangeM - a * domainM;

				for (ry = 0; ry < blockgroesse && y + ry < range.height; ry++) { // Kontrast und Helligkeit anpassen
					for (rx = 0; rx < blockgroesse && x + rx < range.width; rx++) {
						int value = (int) (a * ((domain.argb[x + rx + (y + ry) * domain.width] >> 16) & 0xff) - b);
						// if (value < 0)
						// value = 0;
						// else if (value > 255)
						// value = 255;
						domain.argb[x + rx + (y + ry) * domain.width] = 0xff000000 | (value << 16) | (value << 8)
								| value;
					}
				}
				x += blockgroesse - 1;
			}
			y += blockgroesse - 1;
		}
		return domain;
	}

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
	public static RasterImage decoder(RasterImage range) {

		RasterImage start = getGreyImage(range.width, range.height);

		RasterImage temp = adjustContrastBrightness(start, range);
		for (int i = 0; i < 2; i++) {
			temp = adjustContrastBrightness(temp, range);

		}
		return temp;
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