package bvk_ss19;

import java.util.Arrays;

public class Domainblock {

	
	public int[] argb;	
    public float variance;
    public int mittelWert;
    
    public boolean isRGB;
    
    public int mittelWertR;
    public int mittelWertG;
    public int mittelWertB;
    
    public float varianceR;
    public float varianceG;
    public float varianceB;


	public Domainblock(int[] argb, boolean isRGB) {
		// creates an empty RasterImage of given size
		this.argb = argb;
		if(!isRGB) 	{
			this.mittelWert = setMittelwert(argb);
			this.variance = setVarianz(mittelWert, argb);
		}
		else {
			this.mittelWert = setMittelwert(argb);
			
			this.mittelWertR = setMittelwert(extractRGB(argb,0));
			this.varianceR = setVarianz(mittelWertR, extractRGB(argb,0));

			this.mittelWertG = setMittelwert(extractRGB(argb,1));
			this.varianceG = setVarianz(mittelWertG, extractRGB(argb,1));

			this.mittelWertB= setMittelwert(extractRGB(argb,2));
			this.varianceB = setVarianz(mittelWertB, extractRGB(argb,2));
		}
	}
	
	
	/**
	 * 
	 * @param argbBytes
	 * @param canal
	 * @return
	 */
	public  int[] extractRGB(int[] argbBytes, int canal) {
		
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
	private  int setMittelwert(int[] values) {
		int sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum / values.length;
	}
	
	/**
	 * 		
	 * @param mittelWert
	 * @param block
	 * @return
	 */
	public  float setVarianz(int mittelWert, int[] block) {		
		float varianzDomain = 0;
			for (int i = 0; i < block.length; i++) {
				// subtract average value from current value
				float greyD = block[i] - mittelWert;;
				varianzDomain += greyD*greyD;
			
			}
			return varianzDomain;
	}
}
