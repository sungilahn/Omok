import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jack {
	private int depth = 2*(2)+1, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point,Map<List<Point>,Integer>> lookup; // threat space (incl. 0) -> sequence -> score
	private Map<List<Point>,List<List<PI>>> seqs; // sequence -> threat space lines -> space & score
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

		public String toString() {
			return "<("+p.x+", "+p.y+"), "+i+">";
		}
	}

	// constructor
	public Jack() {
		board = new int[19][19];
		lookup = new HashMap<>();
		seqs = new HashMap<>();
		if (오목.TEST) {
			List<Point> part1 = new ArrayList<>();
			part1.add(new Point(4,9));
			part1.add(new Point(7,2));
			List<List<PI>> part2 = new ArrayList<>();
			List<PI> part3 = new ArrayList<>();
			part3.add(new PI(new Point(3,9),8));
			part3.add(new PI(new Point(2,9),8));
			part3.add(new PI(new Point(1,9),8));
			part3.add(new PI(new Point(4,10),8));
			part3.add(new PI(new Point(4,11),8));
			part3.add(new PI(new Point(4,12),8));
			part3.add(new PI(new Point(4,13),8));
			part2.add(part3);
			List<PI> part4 = new ArrayList<>();
			part4.add(new PI(new Point(6,2),8));
			part4.add(new PI(new Point(5,2),8));
			part4.add(new PI(new Point(4,2),8));
			part4.add(new PI(new Point(3,2),8));
			part4.add(new PI(new Point(7,3),8));
			part4.add(new PI(new Point(7,4),8));
			part4.add(new PI(new Point(7,5),8));
			part4.add(new PI(new Point(7,6),8));
			part2.add(part4);
			seqs.put(part1,part2);
			board[4][9] = 1;
		}
	}

	// officially adds point, modifying the actual seqs and lookup
	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0-turn;
		// add point to lookup and seqs
		seqs = step(x,y,seqs,lookup,turn);
		lookup = hash(seqs);
	}

	// TODO: implement new sequences
	// modifies sequences, threat spaces, and scores given a new point
	private Map<List<Point>,List<List<PI>>> step(int x, int y, Map<List<Point>,List<List<PI>>> seqs,
												 Map<Point,Map<List<Point>,Integer>> lookup, int turn) {
		Map<List<Point>,List<List<PI>>> result = new HashMap<>();
		// first, alternate scores as ones that are affected and not affected both need to alternate scores
		for (List<Point> seq : seqs.keySet()) {
			List<List<PI>> updatedList = new ArrayList<>();
			for (List<PI> seqFrag : seqs.get(seq)) {
				List<PI> newList = new ArrayList<>(seqFrag);
				// if color of sequence is the same as the color of whoever just put down their stone
				if (board[seq.get(0).x][seq.get(0).y] == turn) {
					for (PI spaceScore : newList) {
						// multiply all scores by 2
						spaceScore.setI(spaceScore.getI() * 2);
					}
				} else {
					for (PI spaceScore : newList) {
						spaceScore.setI(spaceScore.getI() / 2);
					}
				}
				updatedList.add(newList);
			}
			result.put(seq,updatedList);
		}
		// lookup the point and see which ones it affect
		if (lookup.containsKey(new Point(x,y))) {
			// do stuff with those sequences affected
		} else {
			// this is a new threat 'sequence' containing only one point. Goes 8-way. Check for blockages.
			// TODO: how do I check for blockages in all 8 directions without checking 8 sides manually?
		}
		return result;
	}

	// calculates lookup given a seqs
	private Map<Point,Map<List<Point>,Integer>> hash(Map<List<Point>,List<List<PI>>> seqs) {
		Map<Point,Map<List<Point>,Integer>> result = new HashMap<>();
		return result;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("seqs: "+seqs.toString());
		System.out.println("lookup: "+lookup.toString());
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}
}