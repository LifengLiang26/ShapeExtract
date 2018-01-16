package com.liang.shapeextractor;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by liang on 1/14/18.
 */

public class ShapeExtractor {
    static private String TAG = ShapeExtractor.class.getSimpleName();
    private File file;
    private int row;
    private int col;
    private int recordN;
    byte[][] data;
    int[][] island;
    private Set<Integer> islandKeys;
    private static final int int0x01 = 0x01;
    private static final int int0x02 = 0x02;
    private List<CoastLine> islandPolygon;

    public ShapeExtractor(File file, int col, int row, int recordN) {
        this.file = file;
        this.row = row;
        this.col = col;
        this.recordN = recordN;
        data = new byte[row][col*recordN*4];
        island = new int[row][col];
        islandKeys = new HashSet<>();
        loadRawData();
        buildIslands();
        extractCoastLines();
    }

    private int byte4ToInt(byte[] bytes, int off) {
        int b0 = bytes[off] & 0xFF;
        int b1 = bytes[off + 1] & 0xFF;
        int b2 = bytes[off + 2] & 0xFF;
        int b3 = bytes[off + 3] & 0xFF;
        int ret = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        return ret;
    }

    private int[] byte4ToIntArray(byte[] bytes, int off, int num){
        int[] ret = new int[num];
        for(int i = 0; i < num; i++){
            ret[i] = byte4ToInt( bytes, off + i*4);
        }
        return ret;
    }

    private boolean isIntEqual(int[] a, int[] b){
        if(a != null && b != null && a.length == b.length){
            int length = a.length;
            for(int i = 0; i < length; i++){
                if(a[i] != b[i]){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isBytesEqual(byte[] a, int aPosition, byte[] b, int bPosition, int length ){
        if(a == null && b == null && a.length < aPosition+length && b.length < bPosition+length){
            return false;
        }
        for(int i = 0; i < length; i++){
            if((a[aPosition+i]^b[bPosition+i]) != 0){
                return false;
            }
        }
        return true;
    }
    private boolean isIntEqual(int a, int b){
        return a==b;
    }

    private int getValue(int r, int c) {
        if (r < data.length && c < data[r].length) {
            return data[r][c];
        } else {
            return -1;
        }
    }

    private void buildIslands() {
        int record_length = recordN*4;

        int roof = 4;// Start from 4, because 0 is default, 1 & 2 & 3 are used by coast extraction
        for (int r = 0; r < row; r++) {
            for (int c = 0; c < col; c++) {
                //check with left cub
                if (c == 0) {//if no left, will check below
                    if (r > 0) {//if it has below, will check with below
                        if (isBytesEqual(data[r],c*record_length,data[r-1],c*record_length,record_length)) {//if equal to below, use same roof
                            island[r][c] = island[r - 1][c];
                        } else {// if not equal, use a new roof
//                            Log.d(TAG, "No left and below value is " + below + ";cur value is" + cur);
                            island[r][c] = roof++;
                        }
                    } else {//there is no below, use a new roof
//                        Log.d(TAG, "No below, cur value is" + cur);
                        island[r][c] = roof++;
                    }
                } else {//if there is a left
                    if (isBytesEqual(data[r],(c-1)*record_length,data[r],c*record_length,record_length)) {
                        island[r][c] = island[r][c - 1];

                        if (r > 0) {//if have the below, check if color is same and its roof is different, need to change to same roof
                            int belowRoof = island[r - 1][c];
                            // if data is equal but roof are different, change the roof same as above
                            if (isBytesEqual(data[r-1], c*record_length, data[r], c*record_length, record_length) && belowRoof != island[r][c]) {
//                                Log.d(TAG, "Merge :"+island[r-1][c]+" into "+island[r][c]);
                                for (int i = 0; i < r; i++) {
                                    for (int j = 0; j < col; j++) {
                                        if (island[i][j] == belowRoof) {
                                            island[i][j] = island[r][c];
                                        }
                                    }
                                }
                                for (int k = 0; k < c; k++) {
                                    if (island[r][k] == belowRoof) {
                                        island[r][k] = island[r][c];
                                    }
                                }
                            }
                        }
                    } else {// if isn't equal to left, will check with below
//                        Log.d(TAG, "row: "+r+", Col: "+"="+c);
                        if (r > 0) {// there is a below
                            if (isBytesEqual(data[r],c*record_length,data[r-1],c*record_length,record_length)) {//if equal to below, set the roof as below
                                island[r][c] = island[r - 1][c];
                            } else { // if not equal to below, set a new roof
//                                Log.d(TAG, "Adding new value: "+roof+1+", below value is "+ below+";cur value is"+cur);
                                island[r][c] = roof++;
                            }
                        } else {// if there is no below, create a new roof
//                            Log.d(TAG, "No below, Adding new value, cur value is"+cur);
                            island[r][c] = roof++;
                        }
                    }
                }
            }

        }
        Log.d(TAG, "the island data");
        for (int i = 0; i < island.length; i++) {
            for (int j = 0; j < island[i].length; j++) {
                if (!islandKeys.contains(island[i][j])) {
                    islandKeys.add(island[i][j]);
                }
            }
        }
        printMatrix(island, row, col, true);
        Log.d(TAG, "");
    }

    public List<Integer> getIslandKeys(){
        List<Integer> list = new ArrayList<Integer>(islandKeys.size());
        list.addAll(islandKeys);
        return list;
    }

    static class Lpoint {
        int x;
        int y;

        public Lpoint(int l, int ll) {
            x = l;
            y = ll;
        }

        public Lpoint(Lpoint other) {
            x = other.x;
            y = other.y;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void copy(Lpoint lpoint) {
            this.x = lpoint.x;
            this.y = lpoint.y;
        }

        @Override
        public int hashCode() {//only support 64k*64k
            return (x << 16 | 0xFF00) + y;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Lpoint) {
                Lpoint lp = (Lpoint) o;
                if (x == lp.x && y == lp.y) {
                    return true;
                } else {
                    return false;
                }
            }
            return super.equals(o);
        }
    }

    static class CoastLine {
        List<Lpoint> outerCoast;
        List<List<Lpoint>> innerCoast;

        public List<Lpoint> getOuterCoast() {
            return outerCoast;
        }

        public void setOuterCoast(List<Lpoint> outerCoast) {
            this.outerCoast = outerCoast;
        }

        public List<List<Lpoint>> getInnerCoast() {
            return innerCoast;
        }

        public void setInnerCoast(List<List<Lpoint>> innerCoast) {
            this.innerCoast = innerCoast;
        }
    }

    private void extractCoastLines(){
        if(islandKeys.size() > 0) {
            islandPolygon = new LinkedList<>();
            Iterator<Integer> iterator = islandKeys.iterator();
            int key = -1;
            int current = 0;
            int[][] data1 = new int[row+1][col+1];
            int[][] edges = new int[row + 1][col + 1];
            while (iterator.hasNext()) {
                key = iterator.next();
                //step 0: reset data[][]
                for (int i = 0; i <= row; i++) {
                    for (int j = 0; j <= col; j++) {
                        data1[i][j] = 0;
                    }
                }

                //step 1: load the island raw data to data
                for (int i = 0; i < row; i++) {
                    for (int j = 0; j < col; j++) {
                        if (island[i][j] == key) {
                            data1[i][j] = island[i][j];
                        } else {
                            data1[i][j] = 0;
                        }
                    }
                }
                Log.d(TAG, "the island raw data with key :"+key);
                printMatrix(data1,row,col,true);
                // find the coastline
                // step 1: from up to down to find the edge
                //
                //  0x02 |__
                //       0x01
                //
                for (int i = 0; i <= row; i++) {
                    for (int j = 0; j <= col; j++) {
                        edges[i][j] = 0;
                    }
                }
                // from left to right
                for (int i = 0; i < row; i++) {
                    current = 0;// reset the current value
                    for (int j = 0; j <= col; j++) {
                        if (data1[i][j] != current) { //inner edge, from 0 to island
                            current = data1[i][j];
                            edges[i][j] = int0x02;
                        }
                    }
                }
                //from up to down
                for (int i = 0; i < col; i++) {
                    current = 0;
                    for (int j = 0; j <= row; j++) {
                        if (data1[j][i] != current) { //inner edge, from 0 to island
                            current = data1[j][i];
                            edges[j][i] |= int0x01;
                        }
                    }
                }
                Log.d(TAG,"island edge: ");
                printMatrix(edges,row+1,col+1,true);

                int start_r = -1; // start point
                int start_c = -1;
                for (int i = 0; i <= row; i++) {
                    for (int j = 0; j <= col; j++) {
                        if (edges[i][j] != 0) {
                            start_r = i;
                            start_c = j;
                            break;
                        }
                    }
                    if (start_c != -1 && start_r != -1) {
                        break;
                    }
                }
                int current_r;//current point of the path
                int current_c;
                int pre_r;// previous point which was added into the loop
                int pre_c;
                int mid_r; // the middle point
                int mid_c;
                CoastLine coastLine = new CoastLine();
                //found a loop
                while (start_c != -1 && start_r != -1) {
                    List<Lpoint> lpoints = new LinkedList<>();
                    pre_r = mid_r = -1;
                    pre_c = mid_c = -1;
                    current_r = start_r;
                    current_c = start_c;

                    while (true) {
                        //find the next point, checking if the current point is in a straight line, if not, add to path
                        if(pre_r == -1 && pre_c == -1){
                            lpoints.add(new Lpoint(current_r,current_c));
                            pre_r = current_r;
                            pre_c = current_c;
                        }
                        else if(pre_r  == mid_r && current_r != mid_r) {// the last saved point and middle point is in horizontal
                            // if the current point and mid point isn't in horizontal, need to save middle
                            lpoints.add(new Lpoint(mid_r, mid_c));
                            pre_r = mid_r;
                            pre_c = mid_c;
                            mid_r = current_r;
                            mid_c = current_c;
                        }
                        else if(pre_c == mid_c && current_c != mid_c){// the last saved point and middle point is in vertical
                            // if the current point and mid point isn't in vertical, need to save middle
                            lpoints.add(new Lpoint(mid_r, mid_c));
                            pre_r = mid_r;
                            pre_c = mid_c;
                            mid_r = current_r;
                            mid_c = current_c;
                        }
                        else{
                            mid_r = current_r;
                            mid_c = current_c;
                        }

                        if ((edges[current_r][current_c] & int0x01) == int0x01) {//bottom (_) point, so next point will be right next
                            edges[current_r][current_c] ^= int0x01;
                            current_c = current_c + 1;
                        }
                        //this is the case that current point is |, so the next point will be above it.
                        else if ((edges[current_r][current_c] & int0x02) == int0x02) {
                            edges[current_r][current_c] ^= int0x02;
                            current_r = current_r + 1;
                        }
                        //this is the case that current point is right up corner or backward to this point, need to backward
                        else {
                            if (current_c > 0 && (edges[current_r][current_c-1] & int0x01) == int0x01) {
                                current_c = current_c - 1;
                                edges[current_r][current_c] ^= int0x01;
                            } else if (current_r > 0 && (edges[current_r-1][current_c] & int0x02) == int0x02) {
                                current_r = current_r - 1;
                                edges[current_r][current_c] ^= int0x02;
                            } else {
                                Log.e(TAG, "Something is wrong, current_r = " + current_r + "; current_c = " + current_c + " cannot find the next point");
                                break;
                            }
                        }
                        if (start_c == current_c && start_r == current_r) {
                            if((pre_r  == mid_r && current_r != mid_r) || (pre_c == mid_c && current_c != mid_c)) {
                                // if the middle point and current point and previous point aren't in a straight line, save the middle point
                                lpoints.add(new Lpoint(mid_r, mid_r));
                            }
                            lpoints.add(new Lpoint(current_r, current_c));
                            Log.d(TAG, "Complete composing a loop");
                            if (coastLine.getOuterCoast() == null) {
                                Log.d(TAG, "Adding to Outer loop");
                                coastLine.setOuterCoast(lpoints);
                            } else {
                                if (coastLine.getInnerCoast() == null) {
                                    coastLine.setInnerCoast(new LinkedList<List<Lpoint>>());
                                }
                                Log.d(TAG, "Adding to Inner loop");
                                coastLine.getInnerCoast().add(lpoints);
                            }
                            break;
                        }

                    }
                    start_r = -1;
                    start_c = -1;
                    for (int i = 0; i <= row; i++) {
                        for (int j = 0; j <= col; j++) {
                            if (edges[i][j] != 0) {
                                start_r = i;
                                start_c = j;
                                break;
                            }
                        }
                        if (start_c != -1 && start_r != -1) {
                            break;
                        }
                    }
                }
                islandPolygon.add(coastLine);
            }
        }
    }
    public void loadRawData() {
        RandomAccessFile in = null;
        try {
            in = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
            return;
        }
        try {
            for (int i = 0; i < row; i++) {
                int length = in.read(data[i]);
                if (length != col * recordN * 4) {
                    Log.e(TAG, "Data read back size is wrong, expected: "+col*recordN*4+"actual :"+length);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            if (in == null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "fail to close the file");
                }

            }
        }
//        StringBuffer stringBuffer = new StringBuffer();
//        int color = 0;
//        Map<Integer,Integer> colors= new HashMap<>();
//        for(int i = 0; i < row; i++){
//            stringBuffer.setLength(0);
//            stringBuffer.append("line "+i+":"+'\t');
//            for(int j = 0; j < col; j++){
//                if(colors.containsKey(matrix[i][j])){
//                    color = colors.get(matrix[i][j]);
//                }
//                else{
//                    color++;
//                    colors.put(matrix[i][j],color);
//                }
//                stringBuffer.append(color);
//                stringBuffer.append('\t');
//            }
//            Log.d(TAG, stringBuffer.toString());
//        }
    }

    public void printMatrix() {
        printMatrix(data, row, col, recordN);
        Log.d(TAG, "island area");
        printMatrix(island, row, col, true);
    }

    private void printMatrix(byte[][] mmatrix, int row, int col, int recordS) {
        StringBuffer stringBuffer = new StringBuffer();
        int color = 0;
        Map<byte[], Integer> colors = new HashMap<>();
        for (int i = 0; i < row; i++) {
            stringBuffer.setLength(0);
            stringBuffer.append("line " + i + ":" + '\t');
            for (int j = 0; j < col; j++) {
                for(int k = 0; k < recordS; k++){
                    stringBuffer.append(mmatrix[i][j*recordS*4+k]);
                }
                stringBuffer.append('\t');
            }
            Log.d(TAG, stringBuffer.toString());
        }
    }
    private void printMatrix(int[][] mmatrix, int row, int col, boolean origin){
        StringBuffer stringBuffer = new StringBuffer();
        int color = 0;
        Map<Integer,Integer> colors= new HashMap<>();
        for(int i = 0; i < row; i++){
            stringBuffer.setLength(0);
            stringBuffer.append("line "+i+":"+'\t');
            for(int j = 0; j < col; j++){
                if(colors.containsKey(mmatrix[i][j])){
                    color = colors.get(mmatrix[i][j]);
                }
                else{
                    color++;
                    colors.put(mmatrix[i][j],color);
                }
                if(origin){
                    stringBuffer.append(mmatrix[i][j]);
                }
                else{
                    stringBuffer.append(color);
                }
                stringBuffer.append('\t');
            }
            Log.d(TAG, stringBuffer.toString());
        }
    }


}
