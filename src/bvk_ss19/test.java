package bvk_ss19;

import java.nio.ByteBuffer;

public class test {
	
	public static void main(String[] args) {
		test();
	}
	
	
	public static void test() {
		float[][] arr = { {1.2f,2.3f,2.4f}, {4.7f,4.8f,9.8f} };
		
		int[][][] test = new int[arr.length][arr[0].length][4];
		
		for(int row=0;row<arr.length;row++) {
			for(int column=0;column<arr[0].length;column++) {
				
				//int a = 1067030938;

	         	int intBits =  Float.floatToIntBits(arr[row][column]);     
	         	//System.out.println(intBits);
	         	byte b = (byte) intBits;
	         	
	         	int tmp = (b >> 24) & 0xff;
	         	//System.out.println(tmp);
	         	
	         	test[row][column][0] =   (byte) (intBits >> 24);
	         	test[row][column][1] =	 (byte) (intBits >> 16);
	         	test[row][column][2] =	 (byte) (intBits >> 8);
	        	test[row][column][3] =	  (byte) (intBits);
	          
	            
			}
		}
		
		float[][] res = new float[arr.length][arr[0].length];
//		
//	    // -------- convert byte to float
		for(int row=0;row<test.length;row++) {
			for(int column=0;column<test[0].length;column++) {
				
				int intBits = test[row][column][0] << 24 | (test[row][column][1] & 0xFF) << 16 
						| (test[row][column][2] & 0xFF) << 8 | (test[row][column][3] & 0xFF);
				
        	    float number = Float.intBitsToFloat(intBits);
        	    res[row][column] = number;
            }
        }
	   	for(int row=0;row<res.length;row++) {
			for(int column=0;column<res[0].length;column++) {
			  System.out.println(res[row][column]);
			}
		}

}
}
