import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Jack {
	private int depth = 2*(2)+1, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board;
	private int offset, square;
	// TODO: add monte carlo random search and increase the depth
	// TODO: add machine learning component
	// TODO: threat depth search using threat scores, detecting any dangerous patterns
	// TODO: add alpha-beta pruning for minimax tree (whatever that is)

	// constructor
	public Jack(int offset, int square) {
		board = new int[19][19];
		this.offset = offset;
		this.square = square;
	}

	public void addPoint(Point p) {
		board[Math.round((p.x-offset+square/2)/square)][Math.round((p.y-offset+square/2)/square)] = turn;
		turn = 0-turn;
		// analyze pieces
	}

	private boolean legalMove(Point p) {
		// array-implementation of legalmove
		return true;
	}

	// search for threat space for me and for the other player
	private java.util.List<Point> threatSpaceSearch() {
		java.util.List<Point> result = new ArrayList<Point>();
		return result;
	}

	// return the probability of the AI winning
	private double winProbability(int iter) {
		return 1/2;
	}

	// returns the list of points that need the fewest points to form a 5-row
	// all lists are of same length, and there may be more than one list of least points
	private Set<java.util.List<Point>> least5(int color) {
		Set<java.util.List<Point>> result = new HashSet<java.util.List<Point>>();
		return result;
	}//commit

	public void test() {
		System.out.println("test!");
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}
}