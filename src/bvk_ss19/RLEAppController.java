// BVK Ue1 SS2019 Vorgabe
//
// Copyright (C) 2018 by Klaus Jung
// All rights reserved.
// Date: 2018-03-28

package bvk_ss19;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class RLEAppController {
	
	private static final String initialFileName = "LenaGrey.png";
	private static File fileOpenPath = new File(".");

	private RasterImage sourceImage;
	private String sourceFileName;
	
	private RasterImage rleImage;
	private long rleImageFileSize;

    @FXML
    private ImageView sourceImageView;


    @FXML
    private Label sourceInfoLabel;

    @FXML
    private ImageView rleImageView;

    @FXML
    private ImageView domainApproxImageView;
    
    @FXML
    private Label rleInfoLabel;

    @FXML
    private Label messageLabel;

    @FXML
    void openImage() {
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setInitialDirectory(fileOpenPath); 
    	fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images (*.jpg, *.png, *.gif)", "*.jpeg", "*.jpg", "*.png", "*.gif"));
    	File selectedFile = fileChooser.showOpenDialog(null);
    	if(selectedFile != null) {
    		fileOpenPath = selectedFile.getParentFile();
    		loadAndDisplayImage(selectedFile);
    		messageLabel.getScene().getWindow().sizeToScene();;
    	}
    }

	@FXML
	public void initialize() {
		loadAndDisplayImage(new File(initialFileName));		
	}
	
	private void loadAndDisplayImage(File file) {
		sourceFileName = file.getName();
		messageLabel.setText("Opened image " + sourceFileName);
		sourceImage = new RasterImage(file);
		sourceImage.setToView(sourceImageView);
		sourceInfoLabel.setText("");
		rleImage = new RasterImage(sourceImage.width, sourceImage.height);
		rleImage.setToView(rleImageView);
		compareImages();
	}
	
	private void compareImages() {
		if(sourceImage.argb.length != rleImage.argb.length || rleImageFileSize == 0) {
			rleInfoLabel.setText("");
			return;
		}
		double mse = rleImage.getMSEfromComparisonTo(sourceImage);
		rleInfoLabel.setText(String.format("MSE = %.1f", mse));
	}
	
//	@FXML
//	public void saveRLEImage() {
//    	FileChooser fileChooser = new FileChooser();
//    	fileChooser.setInitialDirectory(fileOpenPath);
//    	fileChooser.setInitialFileName(sourceFileName.substring(0, sourceFileName.lastIndexOf('.')) + ".run");
//    	fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("RLE Images (*.run)", "*.run"));
//    	File selectedFile = fileChooser.showSaveDialog(null);
//    	if(selectedFile != null) {
//    		try {
//    			DataOutputStream ouputStream = new DataOutputStream(new FileOutputStream(selectedFile));
//    			long startTime = System.currentTimeMillis();
//    			RLE.encodeImage(sourceImage, ouputStream);
//    			long time = System.currentTimeMillis() - startTime;
//    			messageLabel.setText("Encoding in " + time + " ms");
//    		} catch (Exception e) {
//    			e.printStackTrace();
//    		}
//    	}
//	}
	
	@FXML
	public void saveRLEImage() {
		RLE.decoder(sourceImage);
	}
	
	@FXML
	public void openRLEImage() {
//    	FileChooser fileChooser = new FileChooser();
//    	fileChooser.setInitialDirectory(fileOpenPath);
//    	fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("RLE Images (*.run)", "*.run"));
//    	File selectedFile = fileChooser.showOpenDialog(null);
//    	if(selectedFile != null) {
//    		rleImageFileSize = selectedFile.length();
//    		try {
//    			DataInputStream inputStream = new DataInputStream(new FileInputStream(selectedFile));
//    			long startTime = System.currentTimeMillis();
//    			rleImage = RLE.decodeImage(inputStream);
//    			long time = System.currentTimeMillis() - startTime;
//    			messageLabel.setText("Decoding in " + time + " ms");
//    			rleImage.setToView(rleImageView);
//    			compareImages();
//    		} catch (Exception e) {
//    			e.printStackTrace();
//    		}
//    	}
        RLE.createRangebloecke(sourceImage).setToView(rleImageView);
		//RLE.domainApprox(sourceImage).setToView(rleImageView);
	}
	
	
		
	

	@FXML
	public void openDomainApprox() {
		 RLE.decoder(sourceImage).setToView(domainApproxImageView);
	    //RLE.domainBlockApproxAufgabe2(sourceImage).setToView(domainApproxImageView);
		// RLE.showCodebuch(sourceImage).setToView(domainApproxImageView);
		// RLE.domainBlockApprox(sourceImage).setToView(domainApproxImageView);
	}
}
