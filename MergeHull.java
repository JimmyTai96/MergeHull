

import java.util.*;
import java.awt.geom.*;

public class MergeHull {
	
	/**
	 *	This entire algorithm assumes that no two points line on the same
	 *  vertical line
	 *  That is, no two points have the same x value
	 *
	 * @param originalPoints is a List(Vector) of Point2Ds
	 * @return  List(Vector) is a list of Point2Ds, a subset of those in originalPoints, 
	 * that represent the convex hull. These points are in ccw order.
	 */
	public List<Point2D> computeHull(List<Point2D> originalPoints) {
		// Make a copy of the points so we don't destroy the original
		List<Point2D> points = new ArrayList<Point2D>(originalPoints);

		// And note that Point2D doesn't implement Comparable so we have to provide an
		// external Comparator
		Collections.sort(points, new Comparator<Point2D>() {

			@Override
			public int compare(Point2D p0, Point2D p1) {
				
				// We care about sorting in the x-coord so this is our first tie-breaker
				int result = (int)(p0.getX() - p1.getX());
				if (result == 0) {
					// Use the y-coord as the 2nd (and last tie-breaker)
					result = (int)(p0.getY() - p1.getY());
				}
				
				return result;
			}
		});
		
			
		// The result is a List of Point2Ds that contain the hull sections in
		// ccw order
		List<Point2D> result = recursiveMergeHull(points);
		for(int i = 0; i <result.size(); i ++){
			System.out.print( result.get(i) + "  ");
		}
		System.out.println();
		return result;
	}

	/**
	 * Returns true if the given tangent line is the lower tangent to the hull.
	 * @param tangent is a lower tangent if both the neighboring points are higher (ccw) to the tangent.
	 * Note that this method can also be used to find the upper tangent by reversing the direction
	 * of the tangent line.
	 * @param hull List of points that make the hull
	 * @param pointIndex of where the lowerTangent connects to the hull is given
	 * because it is unknown which end of the tangent line is connected to the hull (one
	 * could figure it out, but that would require work).
	 */

	private boolean isCWTangentAtPoint(Line2D tangent, List<Point2D> hull, int pointIndex) {

		return ((tangent.relativeCCW(hull.get((pointIndex + hull.size() - 1) % hull.size())) >= 0) && (tangent.relativeCCW(hull.get((pointIndex + 1) % hull.size())) >= 0));

	}
	/**
	* find leftmost point in list of points
	* @param points list of points
	* @return index of the leftmost point
	*/
	private int leftMostIndex(List<Point2D> points) {
		int result = 0;//this is as far left as can go - no negative values for coordinates for pixels

		for (int i = 0; i < points.size(); i++) {
			if ((points.get(i)).getX() < (points.get(result)).getX()) {
				result = i;
			}
		}

		return result;
	}
	/**
	* find rightmost point in list of points
	* @param points list of points
	* @return index of the rightmost point
	*/
	private int rightMostIndex(List<Point2D> points) {
		int result = 0;

		for (int i = 0; i < points.size(); i++) {
			if ((points.get(i)).getX() > (points.get(result)).getX()) {
				result = i;
			}
		}

		return result;
	}
	
	/** 
	 * This takes in a List of Point2Ds, sorted by their X-coordinate
	 * This outputs a List of hull points (a subset of points) in ccw order
	 * @param points list of poits in sorted x-coordinate order
	 * @return a list of point in the hull in ccw order
	 */
	private List<Point2D> recursiveMergeHull(List<Point2D> points) {

		List<Point2D> result = new ArrayList<Point2D>();//to be returned

		// Test for the base case -- a single point -- see if list size is one - easy base case
		// if size is one
		if (points.size() == 1) {
			//return the single point that was passed in b/c a single point is its own convex hull
			return points;
		} else {
			// else recursive step
			// Divide the points into two sets leftPoints and rightPoints,
			List<Point2D> leftPoints = new ArrayList<Point2D>();
			List<Point2D> rightPoints = new ArrayList<Point2D>();
			// each containing 1/2 of the points -- to the left and right
			// of a vertical line down the middle
			leftPoints.addAll(points.subList(0, points.size()/2));
			rightPoints.addAll(points.subList(points.size()/2, points.size()));
			// can use addAll (fromIndex is inclusive, toIndex is exclusive (up to but not including))
			// note: subList returns a List, but a List is an Interface that Vector
			// implements

			// Recursively call recursiveMergeHull each of the sublists
			List<Point2D> leftHull = recursiveMergeHull(leftPoints);
			List<Point2D> rightHull = recursiveMergeHull(rightPoints);

			//Next we need to merge the two hull together -- this is where all the work takes place
				/* Note: everything in this comment is just informative.
				 This step must be a O(n) algorithm here to make the overall
				 performance of mergeHull be O(n log(n))
				 That is, we can't just try all possible pairs of points -- this
				 would be a O(n^2) algorithm
				 So you need to find the two tangent lines
				 The idea is to start with the rightmost point in the leftHull and
				 the leftmost point in the rightHull. Then "walk" this line down (up for top),
				 alternating until the lower ( or upper) tangent is reached.
				 For this we can assume that each hull is in ccw order*/

			// Find the starting points for the walking
			int leftSetRightPoint = rightMostIndex(leftHull);
			int rightSetLeftPoint = leftMostIndex(rightHull);

			// Find the bottom tangent line first use walkTangent
			Line2D bottomTangentLine = walkTangent(leftSetRightPoint, rightSetLeftPoint, leftHull, rightHull);

			// Find the upper tangent line next use walkTangent
			Line2D upperTangentLine = walkTangent(rightSetLeftPoint, leftSetRightPoint, rightHull, leftHull);

			// Generate the complete Hull -- watch that zero break!
			// Remember that the complete hull needs to go ccw. Each subhull
			// already goes ccw.
			// Grab the indices from the two returned hulls since we want to work
			// with indices into the hull vectors rather than with points when we
			// actually put the hulls together
			//can use getP1() and getP2() see:
			//https://docs.oracle.com/javase/8/docs/api/java/awt/geom/Line2D.html

			int bottomLeftIndex = leftHull.indexOf(bottomTangentLine.getP1());
			int upperLeftIndex = leftHull.indexOf(upperTangentLine.getP2());

			int bottomRightIndex = rightHull.indexOf(bottomTangentLine.getP2());
			int upperRightIndex = rightHull.indexOf(upperTangentLine.getP1());
			// Grab all the points from the right hull from bottom to top
			// (including both)

			for (int i = bottomRightIndex; i != upperRightIndex; i = (i+1)%rightHull.size()) {
				result.add(rightHull.get(i));
			}
			result.add(rightHull.get(upperRightIndex));
			System.out.println("[" + rightHull.get(upperRightIndex).getX() + ", " + rightHull.get(upperRightIndex).getY() + "]");

			// Need to do this last one out of the loop so I don't get an infinite
			// loop in the case where the hull consists of a single point


			// Grab all the points from the left hull from bottom to top
			// (including both)
			for (int i = upperLeftIndex; i != bottomLeftIndex; i = (i+1)%leftHull.size()) {
				result.add(leftHull.get(i));
			}
			result.add(leftHull.get(bottomLeftIndex));
			System.out.println("[" + leftHull.get(bottomLeftIndex).getX() + ", " + leftHull.get(bottomLeftIndex).getY() + "]");

			// Need to do this last one out of the loop so I don't get an infinite
			// loop in the case where the hull consists of a single point

			// end of if recursive step
		}

		return result;
	}

	

	/**
	*  This takes the initial tangent line indices and the two hulls and walks.
	*  It walks the tangent down so it is a lower tangent
	*  Can be used to find the upper tangent by reversing the input tangent line
	*  and hulls
	* @param leftTangentPointIndex leftmost point in left hull
	* @param rightTangentPointIndex rightmost point in right hull
	* @param leftHull the list of points of the left hull ccw
	* @param rightHull the list of points of the right hull ccw
	* @return Line2D that is the tangent line
	*/
	private Line2D walkTangent(int leftTangentPointIndex, int rightTangentPointIndex, List<Point2D> leftHull, List<Point2D> rightHull) {
		Line2D tangentLine = new Line2D.Double(leftHull.get(leftTangentPointIndex), rightHull.get(rightTangentPointIndex));

		while (!(isCWTangentAtPoint(tangentLine, leftHull, leftTangentPointIndex) && isCWTangentAtPoint(tangentLine, rightHull, rightTangentPointIndex))) {

			// Walk the left point lower
			while (!(isCWTangentAtPoint(tangentLine, leftHull, leftTangentPointIndex))) {
				leftTangentPointIndex = (leftTangentPointIndex + leftHull.size() - 1) % leftHull.size();
				tangentLine = new Line2D.Double(leftHull.get(leftTangentPointIndex), rightHull.get(rightTangentPointIndex));
			}

			// Walk the right point lower
			while (!(isCWTangentAtPoint(tangentLine, rightHull, rightTangentPointIndex))) {
				rightTangentPointIndex = (rightTangentPointIndex + 1) % rightHull.size();
				tangentLine = new Line2D.Double(leftHull.get(leftTangentPointIndex), rightHull.get(rightTangentPointIndex));
			}

		}

		return tangentLine;
	}
}
