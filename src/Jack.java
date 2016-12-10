import java.awt.*;
import java.util.*;
import java.util.List;

public class Jack {
	private int depth = 2*(2)+1, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point,Map<List<Point>,Integer>> lookup; // threat space (incl. 0) -> sequence -> score
	private Map<List<Point>,List<List<PI>>> seqs; // sequence -> threat space -> score
	// TODO: add monte carlo random search and increase the depth
	// TODO: add machine learning component
	// TODO: threat depth search using threat scores, detecting any dangerous patterns
	// TODO: add alpha-beta pruning for minimax tree (whatever that is)

	// custom data type
	public class PI {
		private Point p;
		private int i;

		public PI (Point p, int i) {
			this.p = p; this.i =i;
		}

		public Point getP() {
			return p;
		}

		public void setP(Point p) {
			this.p = p;
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}
	}

	// constructor
	public Jack() {
		board = new int[19][19];
		lookup = new HashMap<Point,Map<List<Point>,Integer>>();
		seqs = new HashMap<List<Point>,List<List<PI>>>();
	}

	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0-turn;
		// add point to lookup and seqs
		seqs = step(x,y,seqs);
		lookup = hash(seqs);
	}

	// forms sequences, threat spaces, and scores
	private Map<List<Point>,List<List<PI>>> step(int x, int y, Map<List<Point>,List<List<PI>>> seqs) {
		Map<List<Point>,List<List<PI>>> result = new HashMap<List<Point>,List<List<PI>>>();
		return result;
	}

	// calculates lookup given a seqs
	private Map<Point,Map<List<Point>,Integer>> hash(Map<List<Point>,List<List<PI>>> seqs) {
		Map<Point,Map<List<Point>,Integer>> result = new HashMap<Point,Map<List<Point>,Integer>>();
		return result;
	}

	public void test() {
		System.out.println("test!");
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}
}