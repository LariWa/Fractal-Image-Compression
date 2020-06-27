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

import javafx.beans.value.ChangeListener ;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class RLEAppController {
	
	private static final String initialFileName = "LenaGrey.png";
	private static File fileOpenPath = new File(".");

	private RasterImage sourceImage;
	private String sourceFileName;
		
	private RasterImage rleImage;

	ObservableList list = FXCollections.observableArrayList();    
	
	@FXML
    private ImageView sourceImageView;

    @FXML
    private Label sourceInfoLabel;

    @FXML
    private ImageView rleImageView;

    @FXML
    private ImageView decodedImageView;
    
    @FXML
    private ImageView bestFitCollage;
    
    @FXML
    private Label rleInfoLabel;

    @FXML
    private Label messageLabel;
    
    @FXML
    private ChoiceBox blockSize;
    
    @FXML
    private Label mse;

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
		loadData();
	}
	
	private void loadAndDisplayImage(File file) {
		sourceFileName = file.getName();
		messageLabel.setText("Opened image " + sourceFileName);
		sourceImage = new RasterImage(file);
		sourceImage.setToView(sourceImageView);
		sourceInfoLabel.setText("");
		rleImage = new RasterImage(sourceImage.width, sourceImage.height);
		rleImage.setToView(rleImageView);
	}
	
	@FXML
	public void saveRLEImage() {
		//FractalCompression.decoder(sourceImage.width, sourceImage.height);
	}
	
	@FXML
	public void openRLEImage() {
		FractalCompression.showCodebuch(sourceImage).setToView(rleImageView);
	}
	
	@FXML
	public void openDecodedImage() {
		
		try {
			DataOutputStream ouputStream = new DataOutputStream(new FileOutputStream("unknown.run"));
			FractalCompression.encode(sourceImage, ouputStream).setToView(bestFitCollage);
		}
		 catch (Exception e) {
 			e.printStackTrace();
 		}
		
		try {
			DataInputStream inputStream = new DataInputStream(new FileInputStream("unknown.run"));
			FractalCompression.decode(inputStream).setToView(decodedImageView);
			mse.setText("MSE " + FractalCompression.getAvgError());
		}
		catch (Exception e) {
	 			e.printStackTrace();
	 		}
			
	}
	
	@FXML
	public void adjustBlockSize(){
		System.out.println(blockSize.getValue().toString());		 
	}
	
	private void loadData() {
		list.removeAll(list);
		
		int a = 4;
		int b = 8;
		int c = 16;
		int d = 32;
		
		list.addAll(a,b,c,d);
		blockSize.getItems().addAll(list);
	}
	
	
	
	}

