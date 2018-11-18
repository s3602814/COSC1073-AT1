// Programming 1 2017 S2 Assignment 1A | S3602814 Thien Nguyen
// Adapted by Caspar and Ross from original Robot code written by Dr Charles Thevathayan

package control;

import robot.Robot;

public class RobotControl implements Control {
	// Internally track arm component sizes
	private int height = Control.INITIAL_HEIGHT;
	private int width = Control.INITIAL_WIDTH;
	private int depth = Control.INITIAL_DEPTH;

	private int[] barHeights;
	private int[] blockHeights;

	private Robot robot;

	// ADDED FOR ASSIGNMENT 1A
	public static final int IS_AN_EVEN_NUMBER = 2;
	public static final int DIVIDE_BY_TWO_CLEANLY = 0;

	private int blockIndex;
	public static final int NEXT_SRC_BLOCK_INDEX = 2;

	private int optimalHeight, tallestBar, temp;

	// Store src heights and index for blockHeights[]
	private int src1Height = 0, src1Index = 0, src2Height = 0, src2Index = 0;

	// Change target source
	public static final boolean TARGET_SRC1 = true;
	public static final boolean TARGET_SRC2 = false;
	private boolean srcToggle = TARGET_SRC1;

	// Track bar heights and current bar column
	private int barTally[] = new int[Control.MAX_BARS];
	private int barIndex = -1;
	private int barColumn = 1;

	// Change bar target direction
	public static final boolean BAR_LEFT_TO_RIGHT = true;
	public static final boolean BAR_RIGHT_TO_LEFT = false;
	private boolean barToggle = BAR_LEFT_TO_RIGHT;

	// Called by RobotImpl
	@Override
	public void control(Robot robot, int barHeightsDefault[], int blockHeightsDefault[]) {
		this.robot = robot;

		this.barHeights = new int[] { 3, 4, 1, 5, 2, 3, 2, 6 };
		this.blockHeights = new int[] { 3, 2, 1, 2, 1, 1, 2, 2, 1, 1, 2, 1, 2, 3 };
		//this.barHeights = barHeightsDefault;
		//this.blockHeights = blockHeightsDefault;

		// Initialise the robot
		robot.init(this.barHeights, this.blockHeights, height, width, depth);

		// ADD ASSIGNMENT PART A METHOD CALL(S) HERE
		prepareRobot();

		// Transfer all blocks from the sources to the bars
		for (int turn = 0; turn < blockHeights.length; turn++) {
			setBlockIndex(turn);
			optimisePickHeight();

			// Pick block from source
			if (srcToggle == TARGET_SRC1) {
				armContractWidth(Control.SRC1_COLUMN);
				pickBlock(src1Height, blockIndex);
			} else {
				armExtendWidth(Control.SRC2_COLUMN);
				pickBlock(src2Height, blockIndex);
			}

			optimiseDropHeight(blockIndex);
			selectBar();
			dropBlock(blockIndex);
			changeSource();
		}

		// Set the arm width back to its intitial size
		armContractWidth(Control.INITIAL_WIDTH);
	}

	// CORE ARM MOVEMENTS
	// Includes methods that modifies the robot's height, width, and depth
	private void armExtendHeight(int height) {
		while (this.height < height) {
			robot.up();
			this.height++;
		}
	}

	private void armContractHeight(int height) {
		while (this.height > height) {
			robot.down();
			this.height--;
		}
	}

	private void armExtendWidth(int width) {
		while (this.width < width) {
			robot.extend();
			this.width++;
		}
	}

	private void armContractWidth(int width) {
		while (this.width > width) {
			robot.contract();
			this.width--;
		}
	}

	private void armExtendDepth(int depth) {
		while (this.height - this.depth > depth + Control.MIN_BLOCKS) {
			robot.lower();
			this.depth++;
		}
	}

	private void armContractDepth() {
		while (this.depth > Control.MIN_DEPTH) {
			robot.raise();
			this.depth--;
		}
	}

	// ROBOT PREPARATION
	// Primes the robot by analysing the given block and bar heights
	private void prepareRobot() {
		// Record src heights and block index for blockHeights[]
		for (int index = 0; index < blockHeights.length; index++) {
			if (index % IS_AN_EVEN_NUMBER == DIVIDE_BY_TWO_CLEANLY) {
				src1Index = index;
				src1Height += blockHeights[index];
			} else {
				src2Index = index;
				src2Height += blockHeights[index];
			}
		}

		// Record initial bar heights
		for (int index = 0; index < barHeights.length; index++)
			barTally[index] = barHeights[index];
	}

	// Identifies the next block to collect in blockHeights[]
	private void setBlockIndex(int turn) {
		// Select from src1 if the turn number is even, else select from src2
		if (turn % IS_AN_EVEN_NUMBER == DIVIDE_BY_TWO_CLEANLY) {
			blockIndex = src1Index;
			src1Index -= NEXT_SRC_BLOCK_INDEX;
		} else {
			blockIndex = src2Index;
			src2Index -= NEXT_SRC_BLOCK_INDEX;
		}
	}

	private void optimisePickHeight() {
		// Identify the tallest column and set it into optimalHeight variable
		optimalHeight = src1Height;
		if (src2Height > optimalHeight)
			optimalHeight = src2Height;
		for (int index = 0; index < barTally.length; index++) {
			if (barTally[index] > tallestBar)
				tallestBar = (barTally[index]);
		}
		if (tallestBar > optimalHeight)
			optimalHeight = tallestBar;

		// Change the robot's height based on optimalHeight
		if (this.height > optimalHeight)
			armContractHeight(optimalHeight + Control.MIN_BLOCKS);
		if (this.height < optimalHeight)
			armExtendHeight(optimalHeight + Control.MIN_BLOCKS);
	}

	private void optimiseDropHeight(int blockIndex) {
		temp = tallestBar + blockHeights[blockIndex];
		if (this.height - Control.MIN_BLOCKS < temp) {
			armExtendHeight(temp + Control.MIN_BLOCKS);
		}
	}

	// ROBOT FUNCTIONS
	// Picks block straignt from the source
	private void pickBlock(int srcHeight, int blockIndex) {
		armExtendDepth(srcHeight);
		if (srcToggle == TARGET_SRC1)
			src1Height -= blockHeights[blockIndex];
		else
			src2Height -= blockHeights[blockIndex];
		robot.pick();
		armContractDepth();
	}

	private void selectBar() {
		// Selects the next bar to place block in
		if (barToggle == BAR_LEFT_TO_RIGHT) {
			barColumn++;
			barIndex++;
		} else {
			barColumn--;
			barIndex--;
		}

		// Reverse bar direction once barColumn reaches a src column
		if (barColumn == Control.SRC1_COLUMN) {
			barToggle = TARGET_SRC1;
			barColumn++;
			barIndex++;
		}
		if (barColumn == Control.SRC2_COLUMN) {
			barToggle = TARGET_SRC2;
			barColumn--;
			barIndex--;
		}

		// Move arm to the specified bar column
		if (srcToggle == TARGET_SRC1)
			armExtendWidth(barColumn);
		else
			armContractWidth(barColumn);
	}

	private void dropBlock(int blockIndex) {
		barTally[barIndex] += blockHeights[blockIndex];
		armExtendDepth(barTally[barIndex]);
		robot.drop();
		armContractDepth();
	}

	private void changeSource() {
		if (srcToggle == TARGET_SRC1) {
			srcToggle = TARGET_SRC2;
		} else {
			srcToggle = TARGET_SRC1;
		}
	}

}
