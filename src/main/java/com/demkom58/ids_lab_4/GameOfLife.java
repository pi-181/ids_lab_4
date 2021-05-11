package com.demkom58.ids_lab_4;

public class GameOfLife {
    public byte[][] initialBoard;
    public int numRows;
    public int numCols;
    private GridWorkerThread[] pool;
    private int numThreads;
    private byte[][] grid;
    private byte[][] nextGrid;
    private final byte[][] tempGrid;

    public GameOfLife(int numRows, int numCols, int numThreads) {
        this.grid = new byte[numRows][numCols];
        this.initialBoard = new byte[numRows][numCols];
        this.nextGrid = new byte[numRows][numCols];
        this.tempGrid = new byte[numRows][numCols];
        this.numRows = numRows;
        this.numCols = numCols;
        this.numThreads = numThreads;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setThreadNumber(int numThreads) {
        this.numThreads = numThreads;
    }

    public void configureThreads() {
        this.pool = new GridWorkerThread[this.numThreads];

        int rRangeMod = this.numRows % numThreads;
        int rRange = this.numRows / numThreads;

        for (int i = 0; i < numThreads; i++) {
            final int sr = i * rRange;
            int er = (i + 1) * rRange - 1;

            if (i == numThreads - 1)
                er += rRangeMod;

            this.pool[i] = new GridWorkerThread(i, this, this.nextGrid, sr, 0, er, numCols - 1);
        }
    }

    public void replaceGrid(byte[][] newGrid) {
        try {
            for (int r = 0; r < numRows; r++) {
                for (int c = 0; c < numCols; c++) {
                    this.grid[r][c] = newGrid[r][c];
                    this.initialBoard[r][c] = newGrid[r][c];
                }
            }
        } catch (Exception e) {
            System.err.format("Likely invalid grid dimensions");
            System.exit(1);
        }
    }

    public byte[][] getGrid() {
        byte[][] retGrid = new byte[numRows][numCols];

        for (int r = 0; r < numRows; r++)
            for (int c = 0; c < numCols; c++)
                retGrid[r][c] = this.grid[r][c];

        return retGrid;
    }

    private int calculateCorner(int r, int c) {
        int topBound = r;
        int bottomBound = r;
        int leftBound = c;
        int rightBound = c;
        int n = numRows - 1;
        int m = numCols - 1;
        int numNeighbors = 0;

        if (r == 0 && c == 0) { // top left corner
            bottomBound++;
            rightBound++;
            numNeighbors += grid[n][m];
            numNeighbors += grid[0][m] + grid[1][m];
            numNeighbors += grid[n][0] + grid[n][1];
        } else if (r == 0 && c == m) { // top right corner
            bottomBound++;
            leftBound--;
            numNeighbors += grid[n][0];
            numNeighbors += grid[0][0] + grid[1][0];
            numNeighbors += grid[n][m] + grid[n][m - 1];
        } else if (r == n && c == 0) { // bottom left corner
            topBound--;
            rightBound++;
            numNeighbors += grid[0][m];
            numNeighbors += grid[0][0] + grid[0][1];
            numNeighbors += grid[n][m] + grid[n - 1][m];
        } else if (r == n && c == m) { // bottom right corner
            topBound--;
            leftBound--;
            numNeighbors += grid[0][0];
            numNeighbors += grid[0][m] + grid[0][m - 1];
            numNeighbors += grid[n][0] + grid[n - 1][0];
        } else return -1; // Not a corner

        for (int i = topBound; i <= bottomBound; i++) {
            for (int j = leftBound; j <= rightBound; j++) {
                if (!(i == r && j == c)) {
                    numNeighbors += grid[i][j];
                }
            }
        }
        return numNeighbors;
    }

    public byte checkState(int r, int c) {
        boolean isAlive = (this.grid[r][c] == 1);
        int numNeighbors = getNumberOfNeighbors(r, c);

        // Apply rules of the game to determine state of the cell
        if (isAlive && (numNeighbors < 2 || numNeighbors > 3)) {
            return 0;
        } else if (isAlive && (numNeighbors == 2 || numNeighbors == 3)) {
            return 1;
        } else if (!isAlive && numNeighbors == 3) {
            return 1;
        } else {
            return this.grid[r][c];
        }
    }

    private int getNumberOfNeighbors(int r, int c) {
        boolean leftEdge = (c == 0);
        boolean rightEdge = (c == numCols - 1);
        boolean topEdge = (r == 0);
        boolean bottomEdge = (r == numRows - 1);

        int topBound = r - 1;
        int bottomBound = r + 1;
        int leftBound = c - 1;
        int rightBound = c + 1;
        int numNeighbors = 0;

        // Check for Cell not on the perimeter
        if (!topEdge && !bottomEdge && !leftEdge && !rightEdge) {
            for (int i = topBound; i <= bottomBound; i++) {
                for (int j = leftBound; j <= rightBound; j++) {
                    if (!(i == r && j == c)) { // Don't check (r,c) in grid
                        numNeighbors += this.grid[i][j];
                    }
                }
            }
            return numNeighbors;
        }

        // Return the amount of neighbors for a corner cell
        int cornerResult = calculateCorner(r, c);
        if (cornerResult != -1)
            return cornerResult;

        // Cell is not on the corner nor in the middle.
        // Access the Cell's that are wrapped and get the proper bounds.
        if (topEdge) {
            topBound = r;
            bottomBound = r + 1;
            for (int i = leftBound; i <= rightBound; i++) { // loop across bottom
                numNeighbors += this.grid[numRows - 1][i];
            }
        } else if (bottomEdge) {
            topBound = r - 1;
            bottomBound = r;
            for (int i = leftBound; i <= rightBound; i++) { // loop across top
                numNeighbors += this.grid[0][i];
            }
        } else if (leftEdge) {
            leftBound = c;
            rightBound = c + 1;
            for (int i = topBound; i <= bottomBound; i++) { // loop across right
                numNeighbors += this.grid[i][numCols - 1];
            }
        } else { // rightEdge
            leftBound = c - 1;
            rightBound = c;
            for (int i = topBound; i <= bottomBound; i++) { // loop across left
                numNeighbors += this.grid[i][0];
            }
        }

        // Get the Cell's neighbors which are not wrapped
        for (int i = topBound; i <= bottomBound; i++) {
            for (int j = leftBound; j <= rightBound; j++) {
                if (!(i == r && j == c)) { // Don't check (r,c) in grid
                    numNeighbors += this.grid[i][j];
                }
            }
        }
        return numNeighbors;
    }

    public void play(int stepCount) {
        // If multithreaded, handle the threads
        System.out.println(numThreads + " threads handle...");
        for (int i = 0; i < stepCount; i++) {
            configureThreads();
            // Start all of the threads for computation
            for (int t = 0; t < this.numThreads; t++)
                this.pool[t].start();
        }
    }

    public boolean isAlive(int r, int c) {
        return this.grid[r][c] == 1;
    }

    public void joinThreads() {
        try {
            for (GridWorkerThread t : this.pool) t.join();
            this.grid = this.nextGrid;
            this.nextGrid = this.tempGrid;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
