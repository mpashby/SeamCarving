/* Michele Pashby
    CSI 143 Applied Algorithms
    Assignment 3: Seam Carving

    Compilation Instructions:
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SeamCarver {

    public static void main(String args[]) throws IOException {
        //get image file
        File file = new File("./image.jpg");
        BufferedImage image = ImageIO.read(file);

        //get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        //print image dimensions
        System.out.println("Img size(w x h) is: "+ width + " x " + height);

        int[] sizes = getNewSize(width,height);  //array of old and new dimensions
        Color[][] rgbVals = getRGBValues(width,height,image); //array of rgb values of pixels
    }

    //get new dimensions from user
    public static int[] getNewSize(int oldW, int oldH) {
        Scanner scanner= new Scanner(System.in);
        int newW;
        int newH;
        while (true) {
            System.out.println("Enter new width: ");
            newW = scanner.nextInt(); //new number of columns
            System.out.println("Enter new height: ");
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
        int sizes[] = new int[]{oldW, oldH, newW, newH};
        return sizes;
    }

    //obtain array of rgb color values for each pixel
    public static Color[][] getRGBValues(int w, int h, BufferedImage image) {
        Color rgbVal[][] = new Color[w][h];
        for (int i = 0; i < w; i++) { //for each column
            for (int j = 0; j < row; j++) { //for each row in said col
                int color = image.getRGB(i, j);  //color number
                int red = (color >> 16) & 0xff;  //convert to rgb numbers
                int green = (color >> 8) & 0xff;
                int blue = (color) & 0xff;
                //store in rgb array
                rgbVal[i][j] = new Color(red, green, blue);
            }
        }
        return rgbVal;
    }

    //get the energy of a pixel compared to another
    public static int getEnergy(Color[][] rgbVals, int i, int j, int i2, int j2) {
        //get color of both pixels
        Color c1 = rgbVals[i][j];
        Color c2 = rgbVals[i2][j2];

        //compare differences in r,g,b
        int rDif = c1.getRed() - c2.getRed();
        int gDif = c1.getGreen() - c2.getGreen();
        int bDif = c1.getBlue() - c2.getBlue();

        return rDif + gDif + bDif;   //total difference
    }

    public static void dpMagic(Color[][] rgbVals, int col, int row){
        int dpTable[][] = new int[col][row];
    }

    //create dp table for vertical seam
    public static int[][] vSeamDp(int[][] dpTable3, int col, int row) {

        //M(i,j) = e(i,j) + min(M(i-1, j-1), M(i-1,j) M(i-1, j=1)
        int[][] dpTable=new int[col][row];
        int[][] results=new int[col][row]; //table to keep track i values of min path
        //initialize results table
        for(int i = 0; i < col; i++){
            results[i][0] = i;
        }

        for (int j=1; j<row; j++) { //for each row
            for (int i=0; i<col; i++){  //for each column

                int left = 100; //max value if i first column
                int right = 100; //max val if i last column
                if (i>0) { //i not first column
                    left = dpTable[i-1][j-1];  //pixel to top left
                }
                int mid = dpTable[i][j-1];  //pixel to top mid
                if (i<col-1) { //not last column
                    right = dpTable[i+1][j-1]; //pixel to top right
                }
                //find min
                if (left<mid && left<=right) { //left is min pixel
                    results[i] = results[i-1]; //save past coordinate values of min seam
                    dpTable[i][j] += left; //min+energy
                } else if (right<left && right <mid){ //right is min
                    results[i] = results[i+1];
                    dpTable[i][j] += right;
                } else { //mid is min
                    dpTable[i][j] += mid;
                }
                results[i][j] = i; //save current position in results

            }
        }
        return results;
    }

    //create dp table for horizontal seam
    public static int[][] hSeamDp(int[][] dpTable, int col, int row) {

        //M(i,j) = e(i,j) + min(M(i-1, j-1), M(i-1,j) M(i-1, j+1)
        int[][] results=new int[col][row]; //table to keep track of min path
        for(int j = 0; j < row; j++){ //initialize first column
            results[0][j] = j; //figure out how results table works for horiontal
        }

        for (int i=1; i<col; i++) { //for each column
            for (int j=0; j<row; j++){  //for each row
                int bot = 100; //max val in case j is last row
                int top = 100; //max val in case j first row
                if (j<row-1) { //not last row
                    bot = dpTable[i-1][j+1];  //pixel to bottom left
                }

                int mid = dpTable[i-1][j];  //pixel to mid left

                if (j>0){
                    top = dpTable[i-1][j-1]; //pixel to top left
                }

                //find min value
                if (bot<mid && bot<top) {  //bottom is min val
                    results[j]=results[j+1];  //save location of previous min
                    dpTable[i][j] += bot;  //save min + energy
                } else if (top<bot && top < mid){ //top is min val
                    results[j]=results[j-1];
                    dpTable[i][j] += top;
                } else { //mid is min val
                    dpTable[i][j] += mid;
                }
                results[i][j]=j;


            }
        }
        return dpTable;
    }

    //find min value in DP table and return coordinates of pixels in seam
    public static int[] vSeamFind(int[][]dptable, int col, int row){

        //find min val for bottom row
        int minPosition = 0;
        int minValue = dptable[0][row-1];

        for (int i=1; i< col; i++) {
            if (dptable[i][row-1] < minValue) {
                minValue = dptable[i][row-1];
                minPosition = i;
            }
        }

        //save coordinates to be deleted
        int[] elims = new int[row];

        int ci = minPosition; //current i index
        int cj = row-1; //current j index

        elims[cj] = ci;

        //traverse table to get the min seam
        for (int k = 0; k<row; k++) {

            int left = dptable[ci-1][cj-1];  //pixel to top left
            int mid = dptable[ci][cj-1];  //pixel to top mid
            int right = dptable[ci+1][cj-1]; //pixel to top right
            //find nxt Min value
            if (left <= mid && left <= right) {
                ci = ci-1;
            } else if (right<=left && right <=mid) {
                ci = ci+1;
            }
            cj = cj-1;
            elims[cj] = ci;
        }
        return elims;
    }

    //cut out seam from rgb values
    public static Color[][] vSeamCut(Color[][]rgbVal, int[] coordinates, int c, int r) {


        for (int j=0; j<r; j++) {
            ArrayList<Color> tempRgb = (ArrayList<Color>) Arrays.asList(rgbVal[j]);
            int remove = coordinates[j]; //remove i,j
            tempRgb.remove(remove);
            
        }

        return rgbVal;
    }
}


//set up array for DP
//calculate energies for each
//dynamic programming magic
//cut out the seam with min
//program that puts them together
//keep cutting seams until desired size

//output new pictures