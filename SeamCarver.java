/* Michele Pashby
    CSI 143 Applied Algorithms
    Assignment 3: Seam Carving

    Compilation Instructions:
    javac SeamCarver.java
    java SeamCarver

    When prompted, enter desired width and height for image reduction
    To select a different original image from the Images folder, change String f in main
    (ex: for file "dog.png", String f = "dog")

    Program is coded for inputting/outputting PNG files
 */


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class SeamCarver {

    public static void main(String args[]) throws IOException {
        String f = "zag"; //file name
        //get image file
        File file = new File("Images/" + f +".png");
        BufferedImage image = ImageIO.read(file);

        //get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        //print image dimensions
        System.out.println("Img size(w x h) is: "+ width + " x " + height);

        int[] sizes = getNewSize(width,height);  //array of old and new dimensions
        int newW = sizes[0];
        int newH = sizes[1];
        Color[][] rgbVals = getRGBValues(width,height,image); //array of rgb values of pixels

        //compute reduction with sequential algorithm
        long seqStart = System.nanoTime(); //record running time
        Color[][] newImg = sequentialCarve(Arrays.copyOf(rgbVals,height),width,height,newW,newH);
        BufferedImage img = convertToImage(newImg,newW,newH);
        outputImage(img,f);
        long seqEnd = System.nanoTime();

        //compute reduction with "optimal" algorithm
        long optStart = System.nanoTime();
        BufferedImage img2 = optimalCarve(Arrays.copyOf(rgbVals,height),width, height,newW,newH);
        File file2 = new File("Images/"+f+"OptimalOutput.png");
        ImageIO.write(img2, "PNG", file2);
        long optEnd = System.nanoTime();

        //print running times
        System.out.println("Time for Sequential: "+ (seqEnd-seqStart));
        System.out.println("Time for Optimal: "+ (optEnd-optStart));
    }

    public static void outputImage(BufferedImage image, String f) throws IOException {
        //output a sequential image
        File file = new File("Images/"+f+"SequentialOutput.png");
        ImageIO.write(image, "PNG", file);
    }

    //get new dimensions from user
    public static int[] getNewSize(int oldW, int oldH) {
        Scanner scanner= new Scanner(System.in);
        int newW;
        int newH;
        while (true) {
            System.out.print("Enter new width: ");
            newW = scanner.nextInt(); //new number of columns
            System.out.print("Enter new height: ");
            newH = scanner.nextInt(); //new num of rows

            //check that dimensions are valid for img shrinking
            if ((oldW >= newW) && (oldH >= newH) && (newW >0) && (newH > 0)) {
                //both new dimensions are smaller and positive, valid
                break;
            }
            System.out.println("Invalid new dimensions for image reduction.");
            //repeat loop until dimensions are valid
        }
        //close input scanner
        scanner.close();
        //put dimensions into array
        int sizes[] = new int[]{newW, newH};
        return sizes;
    }

    //obtain array of rgb color values for each pixel
    public static Color[][] getRGBValues(int w, int h, BufferedImage image) {
        Color rgbVal[][] = new Color[h][w];
        for (int i = 0; i < h; i++) { //for each row
            for (int j = 0; j < w; j++) { //for each col in row
                int color = image.getRGB(j, i);  //color number
                int red = (color >> 16) & 0xff;  //convert to rgb numbers
                int green = (color >> 8) & 0xff;
                int blue = (color) & 0xff;
                //store in rgb array
                rgbVal[i][j] = new Color(red, green, blue);
            }
        }
        return rgbVal;
    }

    public static Color[][] sequentialCarve(Color[][]rgbVals, int w, int h, int newW, int newH) {
        int cw = w; //current width
        int ch = h; //current height
        Color[][] crgb = Arrays.copyOf(rgbVals,h); //current rgb table
        //do all Vertical Carving first
        while (cw != newW) { //keep carving until desired width
            //initialize DP table to energies of each pixel
            int[][] dptable = getEnergyArray(crgb, ch, cw);
            //find vertical seam
            int[]seam= vSeamDp(dptable, cw, ch);  //get pixels in min seam
            crgb= vSeamCut(crgb,seam,cw,ch); //cut out seam
            cw -=1; //subtact from current w
        }

        //Do all horizontal seams
        while (ch != newH) { //keep carving until desired width
            //initialize DP table to energies of each pixel
            int[][] dptable = getEnergyArray(crgb, ch, cw);
            //find vertical seam
            int[]seam= hSeamDp(dptable, cw, ch);  //get pixels in min seam
            crgb= hSeamCut(crgb,seam,cw,ch); //cut out seam
            ch -=1; //subtact from current w
        }

        return crgb;
    }

    public static BufferedImage optimalCarve(Color[][]rgbVals, int w, int h, int newW, int newH){
        //current rgb table

        int wDif = w - newW; //difference between w values
        int hDif = h - newH;
        //dp table for entry(i,j) means optimal path for h-i x w-j
        int[][]dpTable = new int[hDif+1][wDif+1];
        //results table for output image for each operation
        Color[][][][] cAr = new Color[hDif+1][wDif+1][hDif+1][wDif+1];

        Color[][] crgb =Arrays.copyOf(rgbVals,h); //current rgb table
        //initialize first row, (0,0) empty bc no seam carved
        for (int j = 1; j<=wDif; j++){
            //can only vertical seam in first row
            int[][] seamtable = getEnergyArray(crgb, h, w-j+1); //initialize table to energies
            int[]seam = vSeamDp(seamtable,w-j+1,h); //before seam removal
            crgb= vSeamCut(crgb,seam,w-j+1,h);

            dpTable[0][j] = seam[h]; //store cost for the seam
            cAr[0][j] = Arrays.copyOf(crgb, h);

            //imAr[0][j] = convertToImage(crgb,w-j, h); //already cut
        }


        //initialize first column
        Color[][] crgb2 = Arrays.copyOf(rgbVals,h);
        for (int i=1; i<=hDif; i++) {
            //horizontal seams only
            int[][] seamtable2 = getEnergyArray(crgb2, h-i+1, w); //initialize table to energies
            int[]seam = hSeamDp(seamtable2,w,h-i+1); //before seam removal
            crgb2= hSeamCut(crgb2,seam,w,h-i+1);
            dpTable[i][0] = seam[w]; //store cost for the seam
            cAr[i][0] = Arrays.copyOf(crgb2,h-i);
            //imAr[i][0] = convertToImage(crgb2,w,h-i);
        }

        //DP magic starts here
        for (int i=1; i<=hDif; i++) {
            for (int j=1; j<=wDif; j++){
                int top = dpTable[i-1][j]; //min cost at cell above (has one less row)
                int left = dpTable[i][j-1]; //min cost at cell left (one less column)

                //compare costs
                if (top< left) { //vertical
                    //get previous image
                    Color[][]tempRgb =Arrays.copyOf(cAr[i-1][j], h-(i-1));
                    int[][]tempDp = getEnergyArray(tempRgb,h-(i-1),w-j);
                    //do horizontal seam
                    int[]result = hSeamDp(tempDp,w-j,h-(i-1));
                    dpTable[i][j] = top + result[w-j]; //new cost
                    //store new image
                    Color[][]tempR = (hSeamCut(tempRgb,result,w-j,h-i+1));
                    cAr[i][j] = Arrays.copyOf(tempR,h-i);

                } else { //left<=top, prefer vertical seams in case of tie
                    //get previous image
                    Color[][] tempRgb2 = Arrays.copyOf(cAr[i][j-1], h-i);
                    int[][]tempDp2 = getEnergyArray(tempRgb2,h-i,w-(j-1));
                    //do vertical seam
                    int[]result2 = vSeamDp(tempDp2,w-(j-1), h-i);
                    dpTable[i][j] = left + result2[h-i]; //new cost
                    //store new image
                    Color[][] tempR2 = vSeamCut(tempRgb2,result2,w-(j-1),h-i);
                    cAr[i][j]= Arrays.copyOf(tempR2, h-i);
                }
            }
        }
        Color[][] finalPic = Arrays.copyOf(cAr[hDif][wDif], newH);
        BufferedImage finalImg = convertToImage(finalPic,newW,newH);
        return finalImg;
    }

    //get table of energies for each pixel
    public static int[][] getEnergyArray(Color[][]rgbVals, int h, int w){
        //get energies for each pixel
        int[][] energies = new int[h][w]; //array of all energies
        for (int i=0; i<h; i++){
            for (int j=0; j<w; j++){
                int e = getEnergy(rgbVals,i,j,w,h);
                energies[i][j] = e;
            }
        }
        return energies;
    }

    //get the energy of a pixel (i,j)
    public static int getEnergy(Color[][] rgbVals, int i, int j, int w, int h) {
        //energy computed by color difference it has between surrounding pixels
        Color c1 = rgbVals[i][j];
        int difference = 0; //store color difference
        if (i>0) { //not first row
            Color c2 = rgbVals[i-1][j]; //above
            difference += Math.abs(c1.getRed() - c2.getRed()) +
                    Math.abs(c1.getGreen() - c2.getGreen()) + Math.abs(c1.getBlue() - c2.getBlue());
            if (j>0){ //top left
                Color c6 = rgbVals[i-1][j-1];
                difference += Math.abs(c1.getRed() - c6.getRed()) +
                        Math.abs(c1.getGreen() - c6.getGreen()) + Math.abs(c1.getBlue() - c6.getBlue());
            }
            if (j<w-1) {
                Color c7 = rgbVals[i-1][j+1]; //top right
                difference += Math.abs(c1.getRed() - c7.getRed()) +
                        Math.abs(c1.getGreen() - c7.getGreen()) + Math.abs(c1.getBlue() - c7.getBlue());
            }
        }
        if (i<h-1) { //not last row
            Color c3 = rgbVals[i+1][j]; //below
            difference+=Math.abs(c1.getRed() - c3.getRed()) +
                    Math.abs(c1.getGreen()-c3.getGreen()) + Math.abs(c1.getBlue() - c3.getBlue());
            if (j>0){ //bot left
                Color c8 = rgbVals[i+1][j-1];
                difference += Math.abs(c1.getRed() - c8.getRed()) +
                        Math.abs(c1.getGreen() - c8.getGreen()) + Math.abs(c1.getBlue() - c8.getBlue());
            }
            if (j<w-1) {
                Color c9 = rgbVals[i+1][j+1]; //bot right
                difference += Math.abs(c1.getRed() - c9.getRed()) +
                        Math.abs(c1.getGreen() - c9.getGreen()) + Math.abs(c1.getBlue() - c9.getBlue());
            }
        }
        if (j>0) { //not first column
            Color c4 = rgbVals[i][j-1]; //left
            difference+=Math.abs(c1.getRed() - c4.getRed()) +
                    Math.abs(c1.getGreen()-c4.getGreen()) + Math.abs(c1.getBlue() - c4.getBlue());
        }
        if (j<w-1) { //not last column
            Color c5 = rgbVals[i][j+1]; //
            difference+=Math.abs(c1.getRed() - c5.getRed()) +
                    Math.abs(c1.getGreen()-c5.getGreen()) + Math.abs(c1.getBlue() - c5.getBlue());
        }

        return difference;
    }

    //create dp table for horizontal seam
    public static int[] hSeamDp(int[][] dpTable, int w, int h) {

        int[][] results=new int[h][w+1]; //table to keep track i values of min path
        int[][] tempResults=new int[h][w+1]; //keep vals for current row
        //initialize results table
        for(int i = 0; i < h; i++){
            results[i][0] = i;
        }

        for (int j=1; j<w; j++) { //for each row
            for (int i=0; i<h; i++){  //for each column

                int top = Integer.MAX_VALUE; //max value if i first row
                int bot = Integer.MAX_VALUE; //max val if i last row

                if (i>0) { //i not first row
                    top = dpTable[i-1][j-1];  //pixel to top left
                }
                int mid = dpTable[i][j-1];  //pixel to mid left

                if (i<h-1) { //not last row
                    bot = dpTable[i+1][j-1]; //pixel to bot left
                }
                //find min
                if (top<mid && top<=bot) { //left is min pixel
                    //save past coordinate values of min seam
                    tempResults[i] = Arrays.copyOf(results[i-1],w+1);
                    dpTable[i][j] += top; //min+energy

                } else if (bot<top && bot <mid){ //right is min
                    tempResults[i] = Arrays.copyOf(results[i+1], w+1);
                    dpTable[i][j] += bot;

                } else { //mid is min
                    tempResults[i] = Arrays.copyOf(results[i], w+1);
                    dpTable[i][j] += mid;
                }
                tempResults[i][j] = i; //save current position in results
            }
            results = Arrays.copyOf(tempResults, h);
        }
        //get min energy sum in last row
        int minIndex = hSeamFindMin(dpTable,w,h);
        int minVal = dpTable[minIndex][w-1];
        //save cost as last element
        results[minIndex][w] = minVal;
        //returns coordinates of minSeam
        return results[minIndex];
    }

    //create dp table for vertical seam
    public static int[] vSeamDp(int[][] dpTable, int w, int h) {
        //w=width, h=heigh
        //M(i,j) = e(i,j) + min(M(i-1, j-1), M(i-1,j) M(i-1, j+1)t

        //table to keep track of min path, for each row: result[i]=j for (i,j) of each min pixel
        int[][] results=new int[w][h+1]; //rotated/ transposed
        int[][] tempResults = new int[w][h+1]; //save results for current row
        for(int j = 0; j < w; j++){ //initialize first column
            results[j][0] = j; //initialize first column with i index val
        }

        for (int i=1; i<h; i++) { //for each row
            for (int j=0; j<w; j++){  //for each pixel in the row
                int left = Integer.MAX_VALUE; //max val in case j is leftmost
                int right = Integer.MAX_VALUE; //max val in case j rightmost
                //enter values for DP table, assume pre-filled with energies
                if (j>0) { //not first in a row
                    left = dpTable[i-1][j-1];  //pixel to top left
                }

                int mid = dpTable[i-1][j];  //pixel to top mid

                if (j < w-1){ //not last in a row
                    right = dpTable[i-1][j+1]; //pixel to top right
                }

                //find min value
                if ((left<mid) && (left<=right)) {  //left is min val
                    //save locations array of previous min
                    tempResults[j]= Arrays.copyOf(results[j-1], h+1);
                    dpTable[i][j] += left;  //save min + energy

                } else if ((right<left) && (right < mid)){ //right is min val
                    tempResults[j]= Arrays.copyOf(results[j+1], h+1);
                    dpTable[i][j] += right;

                } else { //mid is min val (prefer mid if mid==L or mid==R)
                    //don't need to take another row's min locations
                    tempResults[j]=Arrays.copyOf(results[j],h+1);
                    dpTable[i][j] += mid;
                }
                //input new min into results
                tempResults[j][i]=j;
            }
            //update results to include current row
            results = Arrays.copyOf(tempResults, w);
        }

        //get min energy sum in last row
        int minIndex = vSeamFindMin(dpTable,w,h);
        int minVal = dpTable[h-1][minIndex];
        //save cost as last element
        results[minIndex][h] = minVal;
        //returns coordinates of minSeam
        return results[minIndex];
    }

    //find min value in vertical DP table last row and return its index
    public static int vSeamFindMin(int[][]dptable, int w, int h){
        //find min val for bottom row
        int minPosition = 0;
        int minValue = dptable[h-1][0]; //bottom left
        for (int j=1; j< w; j++) {
            if (dptable[h-1][j] < minValue) {
                //new smallest val
                minValue = dptable[h-1][j];
                minPosition = j;
            }
        }
        return minPosition;
    }

    //find min value in horizontal DP table last row and return its index
    public static int hSeamFindMin(int[][]dptable, int w, int h){
        //find min val for last column
        int minPosition = 0;
        int minValue = dptable[0][w-1]; //top right
        for (int i=1; i< h; i++) { //check each row
            if (dptable[i][w-1] < minValue) {
                //new smallest val
                minValue = dptable[i][w-1];
                minPosition = i;
            }
        }
        return minPosition; //which row index
    }


    //cut out vertical seam from rgb values
    public static Color[][] vSeamCut(Color[][]rgbVal, int[] coordinates, int w, int h) {
        //coordinates= array of j (width) so for (i,j): coor[i] = j
        //new color array after seam cut
        Color[][] newRbg = new Color[h][w-1];
        for (int i=0; i<h; i++) {
            //get pixels for the row
            Color[] t = Arrays.copyOf(rgbVal[i], rgbVal[i].length);
            ArrayList<Color> tempRgb = new ArrayList<>(Arrays.asList(t));
            int remove = coordinates[i]; //remove pixel at [i][j]
            tempRgb.remove(remove);
            newRbg[i]= Arrays.copyOf(tempRgb.toArray(new Color[0]), w-1);
        }
        return newRbg;
    }

    //cut out horizontal seam
    public static Color[][] hSeamCut(Color[][]rgbVal, int[] coordinates, int w, int h) {
        //coor = coor[j] = i for (i,j)
        //new color array after seam cut
        Color[][] newRbg = new Color[h-1][w];
        for (int j=0; j<w; j++) {
            int remove = coordinates[j];
            int k=0; //current index
            for(int i=0; i<h; i++){
                if (i != remove) {  //not the pixel to be removed
                    newRbg[k][j]= rgbVal[i][j];
                    k++;
                }
            }
        }
        return newRbg;
    }

    public static BufferedImage convertToImage(Color[][]rgbVal, int w, int h) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        //traverse each pixel
        for (int i=0; i<w; i++) {
            for (int j=0; j<h;j++){
                Color c = rgbVal[j][i];
                int r = c.getRed();
                int g = c.getGreen();
                int b = c.getBlue();
                int color = (r << 16) | (g << 8) | b;
                image.setRGB(i, j, color);
            }
        }
        return image;
    }
}