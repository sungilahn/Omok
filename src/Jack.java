import java.awt.*;
import java.util.*;
import java.util.List;

public class Jack {
	private int depth = 2*(2)+1, turn = 1; // -1 for white, 1 for black. Depth should be odd
	private int[][] board; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point, List<PI>> lookup; // threat space (incl. 0) -> threats -> score
	private Map<Point, List<List<PI>>> seqs; // threat -> threat space lines -> space & score
	// TODO: threat depth search using threat scores, detecting any dangerous patterns
	// TODO: add alpha-beta pruning in minimax tree for winningmove
	// NOTE: Jack maximizes points for whoever has the current turn

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
	}

	// officially adds point, modifying the actual seqs and lookup
	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0 - turn;
		// add point to lookup and seqs
		seqs = step(x,y,seqs,lookup,turn);
		lookup = hash(seqs);
	}

	// modifies sequences, threat spaces, and scores given a new point
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> seqs,
												 Map<Point, List<PI>> lookup, int turn) {
		Map<Point, List<List<PI>>> result = new HashMap<>();
		// first, alternate scores as ones that are affected and not affected both need to alternate scores
		for (Point seq : seqs.keySet()) {
			List<List<PI>> updatedList = new ArrayList<>();
			for (List<PI> seqFrag : seqs.get(seq)) {
				List<PI> newList = new ArrayList<>(seqFrag);
				// if color of sequence is the same as the color of whoever just put down their stone
				if (board[seq.x][seq.y] == turn) {
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
		Point tmp = new Point(x, y);
		if (lookup.containsKey(tmp)) {
			// TODO: do stuff with those sequences affected
		} else {
			// this is a new threat 'sequence' containing only one point (goes 8-way)
			int[] xfactor = {1, 1}, yfactor = {0, 1};
			List<List<PI>> threatLines = new ArrayList<>();
			for (int i=0; i<4; i++) {
				for (int j=0; j<=1; j++) {
					List<PI> threatLine = new ArrayList<>();
					boolean clash = false;
					for (int k=1; k<=5; k++) {
						if (!clash) {
							int xt = x + k * xfactor[j];
							int yt = y + k * yfactor[j];
							if (0<=xt && xt<19 && 0<=yt && yt<19 /*&& board[xt][yt] == 0*/) {
								if (k != 5) {
									threatLine.add(new PI(new Point(xt, yt), -4 * turn));
								} else {
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else {
								if (!threatLine.isEmpty()) {
									threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
								}
								clash = true;
							}
						}
					}
					if (!threatLine.isEmpty()) threatLines.add(threatLine);
				}
				// rotate 90° left
				int[] temp = Arrays.copyOf(xfactor,2);
				for (int j=0; j<=1; j++) {
					xfactor[j] = 0 - yfactor[j];
					yfactor[j] = temp[j];
				}
			}
			Point point = tmp;
			result.put(point, threatLines);
		}
		return result;
	}

	// calculates lookup given a seqs
	private Map<Point, List<PI>> hash(Map<Point, List<List<PI>>> seqs) {
		Map<Point, List<PI>> result = new HashMap<>();
		for (Point seq : seqs.keySet()) {
			for (List<PI> threatLine : seqs.get(seq)) {
				for (PI threat : threatLine) {
					if (!result.containsKey(threat.getP())) {
						List<PI> temp = new ArrayList<>();
						temp.add(new PI(seq, threat.getI()));
						result.put(threat.getP(), temp);
					} else {
						result.get(threat.getP()).add(new PI(seq, threat.getI()));
					}
				}
			}
		}
		return result;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("seqs: "+seqs.toString());
		System.out.println("lookup: "+lookup.toString());
		System.out.println(lookup.keySet().size());
	}

	// return the best move
	public Point winningMove() {
		Point result = new Point(10,10);
		return result;
	}
}