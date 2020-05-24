// BVK Ue1 SS2019 Vorgabe
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
		int numberOfColors=0;
		for(int i=0; i<image.width*image.height; i++) {
			int color= image.argb[i];		
			if(!colors.contains(color)){
				numberOfColors++;
				colors.add(color);
			}		
		}		
		out.writeInt(image.width);
		out.writeInt(image.height);		
		out.writeInt(numberOfColors);
		for(int color:colors) {
			out.writeInt(color);
		}
		
		int i=0;
		while( i<image.width*image.height) {	
			int color= image.argb[i];	
			int lauflaenge=0;
			for(;i< image.width*image.height && image.argb[i]==color && lauflaenge<255; lauflaenge++, i++);			
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
		for (int i=0; i<numberOfColors;i++) {
			colors[i]= in.readInt();
		}	
		int i=0;
		while(in.available()>0) {
			int index= in.readByte() & 0xff;
			int lauflaenge = in.readByte() & 0xff;
			int color= colors[index];		
			for(int j=0; j<lauflaenge; j++, i++) {
				image.argb[i]=color;			
			}
		}
		return image;
	}

}
